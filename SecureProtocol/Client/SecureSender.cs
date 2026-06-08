using System;
using System.Net.Sockets;
using System.Text;
using System.Threading;
using System.Threading.Tasks;

namespace SecureProtocol
{
    /// <summary>
    /// TCP Client — สร้าง, encrypt, sign และส่ง SecurePacket
    /// รองรับ tamper mode สำหรับ test
    /// </summary>
    public class SecureSender
    {
        private readonly string _host;
        private readonly int    _port;
        private readonly byte[] _hmacKey;
        private readonly byte[] _aesKey;
        private uint _seq = 0;

        public event Action<string>? OnLog;

        public SecureSender(string host, int port, byte[] hmacKey, byte[] aesKey)
        {
            _host    = host;
            _port    = port;
            _hmacKey = hmacKey;
            _aesKey  = aesKey;
        }

        /// <summary>
        /// ส่ง message ปกติ
        /// </summary>
        public async Task<string> SendAsync(string message, PacketType type = PacketType.Data, CancellationToken ct = default)
            => await InternalSendAsync(Encoding.UTF8.GetBytes(message), type, tamper: false, replaySeq: null, ct);

        /// <summary>
        /// ส่ง packet ที่ถูก tamper (flip 1 byte ใน payload) — ใช้ test HMAC/GCM rejection
        /// </summary>
        public async Task<string> SendTamperedAsync(string message, CancellationToken ct = default)
            => await InternalSendAsync(Encoding.UTF8.GetBytes(message), PacketType.Data, tamper: true, replaySeq: null, ct);

        /// <summary>
        /// ส่ง packet ซ้ำด้วย sequence number เก่า — ใช้ test replay protection
        /// </summary>
        public async Task<string> SendReplayAsync(string message, uint replaySeq, CancellationToken ct = default)
            => await InternalSendAsync(Encoding.UTF8.GetBytes(message), PacketType.Data, tamper: false, replaySeq: replaySeq, ct);

        private async Task<string> InternalSendAsync(byte[] payload, PacketType type, bool tamper, uint? replaySeq, CancellationToken ct)
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

            // Sign
            var signable = pkt.GetSignableBytes();
            pkt.Hmac = CryptoHelper.Sign(signable, _hmacKey);

            // Tamper: flip byte หลัง sign
            if (tamper && pkt.EncryptedPayload.Length > 0)
                pkt.EncryptedPayload[0] ^= 0xFF;

            var rawPkt = pkt.Serialize();

            // Frame: [4-byte length][packet bytes]
            var frame = new byte[4 + rawPkt.Length];
            BitConverter.GetBytes(rawPkt.Length).CopyTo(frame, 0);
            rawPkt.CopyTo(frame, 4);

            using var tcp = new TcpClient();
            await tcp.ConnectAsync(_host, _port, ct);
            await using var stream = tcp.GetStream();
            await stream.WriteAsync(frame, ct);

            Log($"[Client] Sent seq={seq} type={type} tamper={tamper} replay={replaySeq.HasValue}");

            // Read ACK
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
