using System;
using System.Net.Sockets;
using System.Text;
using System.Threading;
using System.Threading.Tasks;

namespace SecureProtocol
{
    /// <summary>
    /// TCP Client — ส่งได้ 2 format:
    ///   1. SecurePacket  — AES-256-GCM + HMAC-SHA256 (custom protocol)
    ///   2. DSC binary    — AES-128-CBC + CRC32 (simulates DSC TL-series device)
    /// </summary>
    public class SecureSender
    {
        private readonly string  _host;
        private readonly int     _port;
        private readonly byte[]  _hmacKey;
        private readonly byte[]  _aesKey;
        private uint _seq = 0;
        private ushort _dscSeq = 0;

        public event Action<string>? OnLog;

        public SecureSender(string host, int port, byte[] hmacKey, byte[] aesKey)
        {
            _host    = host;
            _port    = port;
            _hmacKey = hmacKey;
            _aesKey  = aesKey;
        }

        // ── SecurePacket methods ─────────────────────────────────────────────────

        /// <summary>ส่ง SecurePacket ปกติ</summary>
        public Task<string> SendAsync(string message, PacketType type = PacketType.Data,
                                      CancellationToken ct = default)
            => InternalSendAsync(Encoding.UTF8.GetBytes(message), type,
                                 tamper: false, replaySeq: null, ct);

        /// <summary>ส่ง SecurePacket ที่ถูก tamper — test HMAC/GCM rejection</summary>
        public Task<string> SendTamperedAsync(string message, CancellationToken ct = default)
            => InternalSendAsync(Encoding.UTF8.GetBytes(message), PacketType.Data,
                                 tamper: true, replaySeq: null, ct);

        /// <summary>ส่ง SecurePacket ด้วย seq เก่า — test replay protection</summary>
        public Task<string> SendReplayAsync(string message, uint replaySeq,
                                            CancellationToken ct = default)
            => InternalSendAsync(Encoding.UTF8.GetBytes(message), PacketType.Data,
                                 tamper: false, replaySeq: replaySeq, ct);

        // ── DSC TL-series binary methods ─────────────────────────────────────────

        /// <summary>
        /// Simulate DSC TL-series device — ส่ง SIA payload ครอบด้วย DSC binary envelope.
        /// <param name="siaPayload">SIA DC-09 ASCII string (max 16 bytes — 1 AES block)</param>
        /// <param name="dscAes128Key">16-byte AES-128 key ที่ตรงกับ server</param>
        /// <param name="accountHash">3-byte account hash (optional, zeros if null)</param>
        /// </summary>
        public async Task<string> SendDscAsync(string siaPayload, byte[] dscAes128Key,
                                               byte[]? accountHash = null,
                                               CancellationToken ct = default)
        {
            var payloadBytes = Encoding.ASCII.GetBytes(siaPayload);
            var acctHash     = accountHash ?? new byte[3];
            var seq          = ++_dscSeq;

            var frame = DscEnvelope.Build(payloadBytes, dscAes128Key, seq, acctHash);

            Log($"[Client/DSC] Sending seq={seq} payload=\"{siaPayload}\" ({frame.Length} bytes)");
            return await RawSendAsync(frame, ct);
        }

        /// <summary>
        /// ส่ง DSC frame ที่ CRC ถูก corrupt — test server-side CRC32 rejection.
        /// </summary>
        public async Task<string> SendDscTamperedCrcAsync(string siaPayload, byte[] dscAes128Key,
                                                          CancellationToken ct = default)
        {
            var frame = DscEnvelope.Build(Encoding.ASCII.GetBytes(siaPayload),
                                          dscAes128Key, ++_dscSeq, new byte[3]);
            frame[24] ^= 0xFF;  // corrupt CRC byte
            Log($"[Client/DSC] Sending TAMPERED CRC frame ({frame.Length} bytes)");
            return await RawSendAsync(frame, ct);
        }

        /// <summary>
        /// ส่ง DSC frame encrypted ด้วย wrong key — test server-side decrypt failure.
        /// </summary>
        public async Task<string> SendDscWrongKeyAsync(string siaPayload, byte[] dscAes128Key,
                                                       CancellationToken ct = default)
        {
            var wrongKey = new byte[16];
            new Random().NextBytes(wrongKey);
            var frame = DscEnvelope.Build(Encoding.ASCII.GetBytes(siaPayload),
                                          wrongKey, ++_dscSeq, new byte[3]);
            Log($"[Client/DSC] Sending frame with WRONG KEY ({frame.Length} bytes)");
            return await RawSendAsync(frame, ct);
        }

        // ── Internals ────────────────────────────────────────────────────────────

        private async Task<string> InternalSendAsync(byte[] payload, PacketType type,
                                                      bool tamper, uint? replaySeq,
                                                      CancellationToken ct)
        {
            var (cipherWithTag, nonce) = CryptoHelper.Encrypt(payload, _aesKey);
            uint seq = replaySeq ?? ++_seq;

            var pkt = new SecurePacket
            {
                Type             = type,
                SequenceNumber   = seq,
                TimestampUtc     = DateTimeOffset.UtcNow.ToUnixTimeMilliseconds(),
                EncryptedPayload = cipherWithTag,
                AesNonce         = nonce
            };

            pkt.Hmac = CryptoHelper.Sign(pkt.GetSignableBytes(), _hmacKey);

            if (tamper && pkt.EncryptedPayload.Length > 0)
                pkt.EncryptedPayload[0] ^= 0xFF;

            var rawPkt = pkt.Serialize();
            // SecurePacket frame: [4-byte LE length][packet bytes]
            var frame = new byte[4 + rawPkt.Length];
            BitConverter.GetBytes(rawPkt.Length).CopyTo(frame, 0);
            rawPkt.CopyTo(frame, 4);

            Log($"[Client] Sent seq={seq} type={type} tamper={tamper} replay={replaySeq.HasValue}");
            return await RawSendAsync(frame, ct);
        }

        /// <summary>Open TCP, send raw frame bytes, read ACK, close.</summary>
        private async Task<string> RawSendAsync(byte[] frame, CancellationToken ct)
        {
            using var tcp = new TcpClient();
            await tcp.ConnectAsync(_host, _port, ct);
            await using var stream = tcp.GetStream();
            await stream.WriteAsync(frame, ct);
            return await ReadAckAsync(stream, ct);
        }

        private static async Task<string> ReadAckAsync(NetworkStream stream, CancellationToken ct)
        {
            var lenBuf = new byte[4];
            int n = await stream.ReadAsync(lenBuf.AsMemory(), ct);
            if (n < 4) return "(no ack)";
            int len = BitConverter.ToInt32(lenBuf, 0);
            var buf = new byte[len];
            await stream.ReadAsync(buf.AsMemory(), ct);
            return Encoding.UTF8.GetString(buf);
        }

        private void Log(string msg) => OnLog?.Invoke(msg);
    }
}
