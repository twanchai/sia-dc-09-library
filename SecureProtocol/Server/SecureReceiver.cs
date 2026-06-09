using System;
using System.IO;
using System.Net;
using System.Net.Sockets;
using System.Text;
using System.Threading;
using System.Threading.Tasks;

namespace SecureProtocol
{
    /// <summary>
    /// TCP Server — รอรับ packet, validate, decrypt, และ log ผล
    /// รองรับ multi-client แบบ async
    ///
    /// Auto-detect frame format:
    ///   • byte[0] == 0x5F                → DSC TL-series binary envelope → unwrap + AES-128
    ///   • byte[0..3] == "SECP" (0x53455043) → SecurePacket (custom protocol)
    /// </summary>
    public class SecureReceiver
    {
        private readonly int     _port;
        private readonly byte[]  _hmacKey;
        private readonly byte[]  _aesKey;
        private readonly byte[]? _dscAesKey;   // null = DSC frames not accepted
        private TcpListener? _listener;

        public event Action<string>? OnLog;

        /// <param name="port">TCP port to listen on</param>
        /// <param name="hmacKey">32-byte HMAC key for SecurePacket path</param>
        /// <param name="aesKey">32-byte AES-256 key for SecurePacket path</param>
        /// <param name="dscAesKey">16-byte AES-128 key for DSC path (null = reject DSC frames)</param>
        public SecureReceiver(int port, byte[] hmacKey, byte[] aesKey, byte[]? dscAesKey = null)
        {
            _port      = port;
            _hmacKey   = hmacKey;
            _aesKey    = aesKey;
            _dscAesKey = dscAesKey;
        }

        public async Task StartAsync(CancellationToken ct = default)
        {
            _listener = new TcpListener(IPAddress.Any, _port);
            _listener.Start();
            Log($"[Server] Listening on TCP 0.0.0.0:{_port}  (all interfaces)");
            Log($"[Server] DSC support: {(_dscAesKey != null ? "ENABLED" : "disabled")}");

            while (!ct.IsCancellationRequested)
            {
                TcpClient client;
                try { client = await _listener.AcceptTcpClientAsync(ct); }
                catch (OperationCanceledException) { break; }

                _ = HandleClientAsync(client, ct);
            }

            _listener.Stop();
            Log("[Server] Stopped.");
        }

        private async Task HandleClientAsync(TcpClient client, CancellationToken ct)
        {
            var remote = client.Client.RemoteEndPoint?.ToString() ?? "unknown";
            Log($"┌─ [{Ts()}] CONNECTED  {remote}");

            var validator = new PacketValidator(_hmacKey, _aesKey);

            try
            {
                await using var stream = client.GetStream();

                while (!ct.IsCancellationRequested)
                {
                    // ── Step 1: Read byte[0] — frame type detection ────────────────
                    var firstByte = new byte[1];
                    if (!await ReadExactAsync(stream, firstByte, ct)) break;

                    Log($"│  [{Ts()}] Step 1  RX byte[0] = 0x{firstByte[0]:X2} ('{PrintChar(firstByte[0])}')");

                    if (firstByte[0] == DscEnvelope.SyncByte)
                    {
                        Log($"│  [{Ts()}]         → DSC TL-series binary frame detected");
                        await HandleDscFrameAsync(stream, firstByte[0], remote, ct);
                    }
                    else if (firstByte[0] == 0x0A)
                    {
                        Log($"│  [{Ts()}]         → SIA DC-09 standard frame detected (LF)");
                        await HandleSiaFrameAsync(stream, remote, ct);
                    }
                    else
                    {
                        Log($"│  [{Ts()}]         → SecurePacket path (length-prefix)");

                        // ── Step 2: Read remaining 3 bytes of length prefix ────────
                        var lenRest = new byte[3];
                        if (!await ReadExactAsync(stream, lenRest, ct)) break;

                        var lenBuf = new byte[4];
                        lenBuf[0] = firstByte[0];
                        lenRest.CopyTo(lenBuf, 1);
                        int totalLen = BitConverter.ToInt32(lenBuf, 0);

                        Log($"│  [{Ts()}] Step 2  Length prefix = {lenBuf.Hex()} → {totalLen} bytes");

                        if (totalLen <= 0 || totalLen > 64 * 1024)
                        {
                            var botName = DetectBot(lenBuf);
                            Log($"└─ [{Ts()}] ⚠️  BOT DETECTED from {remote}");
                            Log($"   dump: {HexAsciiDump(lenBuf, 4)}");
                            Log($"   → {botName}");
                            Log($"   ข้ามการเชื่อมต่อนี้ — รอ connection ถัดไป");
                            break;
                        }

                        // ── Step 3: Read packet body ───────────────────────────────
                        var data = new byte[totalLen];
                        if (!await ReadExactAsync(stream, data, ct)) break;
                        Log($"│  [{Ts()}] Step 3  RX {totalLen} bytes");
                        Log($"│           dump: {HexAsciiDump(data)}");

                        await HandleSecurePacketAsync(stream, data, validator, remote, ct);
                    }
                }
            }
            catch (IOException)
            {
                // Peer closed / reset connection — normal for scanners and short-lived clients
            }
            catch (Exception ex) when (ex is not OperationCanceledException)
            {
                Log($"└─ [{Ts()}] ERROR  {remote}: {ex.Message}");
            }
            finally
            {
                client.Close();
                Log($"└─ [{Ts()}] DISCONNECTED  {remote}");
            }
        }

        // ── DSC frame handler ────────────────────────────────────────────────────

        private async Task HandleDscFrameAsync(NetworkStream stream, byte syncByte,
                                               string remote, CancellationToken ct)
        {
            // ── Step 2: Read remaining 27 bytes ───────────────────────────────────
            var rest = new byte[DscEnvelope.FrameMinLength - 1];
            if (!await ReadExactAsync(stream, rest, ct)) return;

            var raw = new byte[DscEnvelope.FrameMinLength];
            raw[0] = syncByte;
            rest.CopyTo(raw, 1);

            Log($"│  [{Ts()}] Step 2  RX {raw.Length} bytes (DSC frame complete)");
            Log($"│           dump: {HexAsciiDump(raw)}");
            Log($"│           full: {raw.Hex()}");

            // ── Step 3: Parse envelope fields ─────────────────────────────────────
            DscEnvelope env;
            try
            {
                env = DscEnvelope.Parse(raw);
                Log($"│  [{Ts()}] Step 3  Parse OK — seq={env.Sequence} flags=0x{env.Flags:X4} " +
                    $"acct={env.AccountHash.Hex()} cipher={env.CipherBlock.HexHead(8)}...");
            }
            catch (Exception ex)
            {
                Log($"└─ [{Ts()}] Step 3  FAIL — parse error: {ex.Message}");
                await SendAckAsync(stream, false, "DSC_PARSE_ERROR", ct);
                return;
            }

            // ── Step 4: CRC32 verify ───────────────────────────────────────────────
            uint computed = DscEnvelope.ComputeCrc(raw);
            bool crcOk    = computed == env.Crc32;
            Log($"│  [{Ts()}] Step 4  CRC32 computed=0x{computed:X8} frame=0x{env.Crc32:X8} → {(crcOk ? "✅ OK" : "❌ FAIL")}");

            if (!crcOk)
            {
                Log($"└─ [{Ts()}] Step 4  FAIL — frame corrupted/tampered");
                await SendAckAsync(stream, false, "DSC_CRC_FAIL", ct);
                return;
            }

            // ── Step 5: AES-128-CBC decrypt (or plaintext if AES not configured) ───
            if (_dscAesKey is null)
            {
                Log($"└─ [{Ts()}] Step 5  FAIL — DSC key not configured");
                await SendAckAsync(stream, false, "DSC_NOT_CONFIGURED", ct);
                return;
            }

            byte[] siaPayload;
            bool keyIsDefault = IsAllZeros(_dscAesKey);

            if (keyIsDefault)
            {
                // AES not programmed on device → payload sent as plaintext
                siaPayload = TrimNulls(env.CipherBlock);
                var raw16  = Encoding.ASCII.GetString(env.CipherBlock).TrimEnd('\0');
                Log($"│  [{Ts()}] Step 5  AES not configured — reading payload as plaintext");
                Log($"│           raw (16 bytes): \"{raw16}\"");

                // Try Contact ID decode
                var cid = ContactIdParser.TryParse(env.CipherBlock);
                if (cid != null)
                {
                    Log($"│  [{Ts()}] Step 5  ✅ Contact ID decoded:");
                    Log($"│           {cid}");
                    Log($"│           → Account  : {cid.AccountRaw} ({cid.Account})");
                    Log($"│           → Qualifier: [{cid.Qualifier}] {cid.QualText}");
                    Log($"│           → Event    : [{cid.EventCode:000}] {cid.EventName}  (raw: {cid.EventRaw})");
                    Log($"│           → Partition: {cid.Group:00}  Zone/User: {cid.Zone:000}");
                    Log($"│           → Checksum : {(cid.CsValid ? "✅ valid" : "⚠️ invalid")}");
                }
                else
                {
                    Log($"│  [{Ts()}] Step 5  ⚠️  Not Contact ID — raw ASCII above");
                }
            }
            else
            {
                try
                {
                    siaPayload = env.Decrypt(_dscAesKey);
                    var siaText = Encoding.ASCII.GetString(siaPayload).TrimEnd('\0');
                    Log($"│  [{Ts()}] Step 5  AES-128-CBC decrypt ✅ OK — {siaPayload.Length} bytes");
                    Log($"│           decrypted: \"{siaText}\"");

                    var cid = ContactIdParser.TryParse(siaPayload);
                    if (cid != null)
                    {
                        Log($"│           ✅ Contact ID: {cid}");
                    }
                }
                catch (Exception ex)
                {
                    Log($"└─ [{Ts()}] Step 5  FAIL — AES-128-CBC decrypt error: {ex.Message}");
                    Log($"│           (wrong key or corrupted cipher block)");
                    await SendAckAsync(stream, false, "DSC_DECRYPT_FAIL", ct);
                    return;
                }
            }

            Log($"└─ [{Ts()}] ✅ DSC ACCEPT — sending ACK");
            await SendAckAsync(stream, true, "DSC_OK", ct);
        }

        // ── SIA DC-09 standard frame handler (LF...CR delimited) ────────────────

        private async Task HandleSiaFrameAsync(NetworkStream stream, string remote, CancellationToken ct)
        {
            // ── Step 2: Read until CR (0x0D) ──────────────────────────────────────
            using var ms  = new System.IO.MemoryStream();
            var buf       = new byte[1];
            int limit     = 2048;
            int bytesRead = 1; // LF already counted

            while (limit-- > 0)
            {
                if (!await ReadExactAsync(stream, buf, ct)) break;
                bytesRead++;
                if (buf[0] == 0x0D) break;
                ms.WriteByte(buf[0]);
            }

            var raw = ms.ToArray();
            var sia = Encoding.ASCII.GetString(raw);
            Log($"│  [{Ts()}] Step 2  RX {bytesRead} bytes total (LF + body + CR)");
            Log($"│           dump: {HexAsciiDump(raw)}");

            // ── Step 3: Parse SIA DC-09 fields ────────────────────────────────────
            // Format: CCCC 0LLL "ID" seq R<rcvr> L<pref> #<acct> [data] <ts>
            string crcField = sia.Length >= 4 ? sia[..4]  : "????";
            string lenField = sia.Length >= 8 ? sia[4..8] : "????";
            string body     = sia.Length >  8 ? sia[8..]  : "";

            bool lenOk = int.TryParse(lenField, out _);
            Log($"│  [{Ts()}] Step 3  SIA parse — CRC={crcField}  Len={lenField} ({(lenOk ? "✅" : "⚠️")})  Body=\"{body}\"");

            // ── Step 4: Send SIA ACK ───────────────────────────────────────────────
            var ack = Encoding.ASCII.GetBytes("\n00000000\"ACK\"0000\r");
            await stream.WriteAsync(ack, ct);
            Log($"└─ [{Ts()}] Step 4  ✅ SIA ACCEPT — sent ACK ({ack.Length} bytes)");
        }

        // ── SecurePacket handler ─────────────────────────────────────────────────

        private async Task HandleSecurePacketAsync(NetworkStream stream, byte[] data,
                                                   PacketValidator validator, string remote,
                                                   CancellationToken ct)
        {
            // ── Step 4: Deserialize SecurePacket ──────────────────────────────────
            SecurePacket pkt;
            try
            {
                pkt = SecurePacket.Deserialize(data);
                Log($"│  [{Ts()}] Step 4  Deserialize ✅ OK — magic=0x{pkt.Magic:X8} " +
                    $"ver={pkt.Version} type={pkt.Type} seq={pkt.SequenceNumber}");
            }
            catch (Exception ex)
            {
                Log($"└─ [{Ts()}] Step 4  FAIL — deserialize error: {ex.Message}");
                await SendAckAsync(stream, false, "PARSE_ERROR", ct);
                return;
            }

            // ── Step 5: Timestamp freshness ────────────────────────────────────────
            var packetTime = DateTimeOffset.FromUnixTimeMilliseconds(pkt.TimestampUtc).UtcDateTime;
            var age        = DateTime.UtcNow - packetTime;
            Log($"│  [{Ts()}] Step 5  Timestamp age={age.TotalSeconds:F1}s  ts={packetTime:HH:mm:ss.fff}");

            // ── Step 6: HMAC-SHA256 verify ────────────────────────────────────────
            var signable     = pkt.GetSignableBytes();
            var hmacComputed = CryptoHelper.Sign(signable, _hmacKey);
            bool hmacOk      = CryptoHelper.VerifyHmac(signable, pkt.Hmac, _hmacKey);
            Log($"│  [{Ts()}] Step 6  HMAC-SHA256 expected={pkt.Hmac.HexHead(8)}... " +
                $"computed={hmacComputed.HexHead(8)}... → {(hmacOk ? "✅ OK" : "❌ FAIL")}");

            // ── Step 7: AES-256-GCM decrypt + full validation ─────────────────────
            var (result, plaintext) = validator.ValidateAndDecrypt(pkt);

            if (result.IsValid)
            {
                var msg = Encoding.UTF8.GetString(plaintext!);
                Log($"│  [{Ts()}] Step 7  AES-256-GCM decrypt ✅ OK — {plaintext!.Length} bytes");
                Log($"└─ [{Ts()}] ✅ ACCEPT seq={pkt.SequenceNumber} | \"{msg}\"");
                await SendAckAsync(stream, true, "OK", ct);
            }
            else
            {
                Log($"└─ [{Ts()}] Step 7  FAIL — {result.Reason}");
                await SendAckAsync(stream, false, result.Reason, ct);
            }
        }

        // ── Shared helpers ───────────────────────────────────────────────────────

        private static async Task SendAckAsync(NetworkStream stream, bool ok,
                                               string reason, CancellationToken ct)
        {
            var msg   = $"{(ok ? "ACK" : "NAK")}:{reason}";
            var bytes = Encoding.UTF8.GetBytes(msg);
            var frame = new byte[4 + bytes.Length];
            BitConverter.GetBytes(bytes.Length).CopyTo(frame, 0);
            bytes.CopyTo(frame, 4);
            await stream.WriteAsync(frame, ct);
        }

        private static async Task<bool> ReadExactAsync(NetworkStream stream,
                                                        byte[] buf, CancellationToken ct)
        {
            int offset = 0;
            while (offset < buf.Length)
            {
                int n;
                try { n = await stream.ReadAsync(buf.AsMemory(offset), ct); }
                catch (IOException) { return false; }   // connection reset / aborted by peer
                if (n == 0) return false;
                offset += n;
            }
            return true;
        }

        private void Log(string msg) => OnLog?.Invoke(msg);

        public void Stop() => _listener?.Stop();

        // ── Bot detection ────────────────────────────────────────────────────────

        /// <summary>
        /// Match 4-byte length prefix against known bot/scanner signatures.
        /// Returns a human-readable name, or "Unknown scanner/probe" if no match.
        /// </summary>
        private static string DetectBot(byte[] lenBuf)
        {
            // Build a uint for easy comparison (first byte in lenBuf[0])
            if (lenBuf.Length < 4) return "Unknown probe";

            // Check ASCII patterns (bots sending HTTP verbs or brute-force strings)
            var ascii = System.Text.Encoding.ASCII.GetString(lenBuf);
            return ascii switch
            {
                "GET "  => "HTTP GET scanner",
                "POST"  => "HTTP POST scanner",
                "HEAD"  => "HTTP HEAD scanner",
                "CONN"  => "HTTP CONNECT proxy probe",
                "OPTI"  => "HTTP OPTIONS scanner",
                "PUT "  => "HTTP PUT scanner",
                "DELE"  => "HTTP DELETE scanner",
                "PATC"  => "HTTP PATCH scanner",
                "1111"  => "Brute-force digit probe (\"1111\")",
                "2222"  => "Brute-force digit probe (\"2222\")",
                "TEST"  => "Test/probe client (\"TEST\")",
                "PING"  => "PING probe",
                "\x16\x03\x01\x00" or "\x16\x03\x03\x00" => "TLS ClientHello (HTTPS scanner)",
                _       => lenBuf[0] == 0x00
                               ? "Null-byte / length probe"
                               : $"Unknown scanner (signature: {lenBuf[0]:X2} {lenBuf[1]:X2} {lenBuf[2]:X2} {lenBuf[3]:X2})"
            };
        }

        private static bool IsAllZeros(byte[] key) { foreach (var b in key) if (b != 0) return false; return true; }
        private static byte[] TrimNulls(byte[] data) { int len = data.Length; while (len > 0 && data[len-1] == 0) len--; return data[..len]; }

        private static string PrintAscii(byte[] buf)
        {
            var sb = new System.Text.StringBuilder();
            foreach (var b in buf) sb.Append(b >= 32 && b < 127 ? (char)b : '.');
            return sb.ToString();
        }

        // ── Hex+ASCII dump ───────────────────────────────────────────────────────
        /// <summary>
        /// Format up to <paramref name="take"/> bytes as a hex-editor line.
        /// Output: "XX XX XX XX XX XX XX XX  XX XX XX XX XX XX XX XX  |................|"
        /// </summary>
        private static string HexAsciiDump(byte[] data, int take = 16)
        {
            int n = Math.Min(data.Length, take);
            var hex = new StringBuilder();
            var asc = new StringBuilder();

            for (int i = 0; i < 16; i++)
            {
                if (i < n)
                {
                    hex.Append($"{data[i]:X2} ");
                    asc.Append(data[i] >= 32 && data[i] < 127 ? (char)data[i] : '.');
                }
                else
                {
                    hex.Append("   ");   // padding for short packets
                    asc.Append(' ');
                }
                if (i == 7) hex.Append(' ');  // mid-line gap
            }

            return $"{hex} |{asc}|  ({n} bytes)";
        }

        // ── Log helpers ──────────────────────────────────────────────────────────
        private static string Ts() => DateTime.Now.ToString("HH:mm:ss.fff");
        private static char   PrintChar(byte b) => b >= 32 && b < 127 ? (char)b : '.';
    }

    internal static class ByteExt
    {
        public static string Hex(this byte[] b)
            => b.Length == 0 ? "(empty)" : BitConverter.ToString(b).Replace("-", " ");

        public static string HexHead(this byte[] b, int n)
        {
            var take = b.Length < n ? b : b[..n];
            return BitConverter.ToString(take).Replace("-", " ");
        }
    }
}
