using System;
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
    /// </summary>
    public class SecureReceiver
    {
        private readonly int _port;
        private readonly byte[] _hmacKey;
        private readonly byte[] _aesKey;
        private TcpListener? _listener;

        public event Action<string>? OnLog;

        public SecureReceiver(int port, byte[] hmacKey, byte[] aesKey)
        {
            _port    = port;
            _hmacKey = hmacKey;
            _aesKey  = aesKey;
        }

        public async Task StartAsync(CancellationToken ct = default)
        {
            _listener = new TcpListener(IPAddress.Any, _port);
            _listener.Start();
            Log($"[Server] Listening on port {_port}");

            while (!ct.IsCancellationRequested)
            {
                TcpClient client;
                try { client = await _listener.AcceptTcpClientAsync(ct); }
                catch (OperationCanceledException) { break; }

                _ = HandleClientAsync(client, ct);  // fire-and-forget per client
            }

            _listener.Stop();
            Log("[Server] Stopped.");
        }

        private async Task HandleClientAsync(TcpClient client, CancellationToken ct)
        {
            var remote = client.Client.RemoteEndPoint;
            Log($"[Server] Client connected: {remote}");

            var validator = new PacketValidator(_hmacKey, _aesKey);

            try
            {
                await using var stream = client.GetStream();

                while (!ct.IsCancellationRequested)
                {
                    // อ่าน 4 bytes (total length prefix)
                    var lenBuf = new byte[4];
                    if (!await ReadExactAsync(stream, lenBuf, ct)) break;
                    int totalLen = BitConverter.ToInt32(lenBuf, 0);

                    if (totalLen <= 0 || totalLen > 64 * 1024)
                    {
                        Log($"[Server] Invalid frame length {totalLen} — dropping client");
                        break;
                    }

                    var data = new byte[totalLen];
                    if (!await ReadExactAsync(stream, data, ct)) break;

                    // Deserialize + Validate
                    SecurePacket pkt;
                    try { pkt = SecurePacket.Deserialize(data); }
                    catch (Exception ex)
                    {
                        Log($"[Server] Deserialize error: {ex.Message}");
                        await SendAckAsync(stream, false, "PARSE_ERROR", ct);
                        continue;
                    }

                    var (result, plaintext) = validator.ValidateAndDecrypt(pkt);

                    if (result.IsValid)
                    {
                        var msg = Encoding.UTF8.GetString(plaintext!);
                        Log($"[Server] ✅ seq={pkt.SequenceNumber} type={pkt.Type} | \"{msg}\"");
                        await SendAckAsync(stream, true, "OK", ct);
                    }
                    else
                    {
                        Log($"[Server] ❌ seq={pkt.SequenceNumber} REJECTED: {result.Reason}");
                        await SendAckAsync(stream, false, result.Reason, ct);
                    }
                }
            }
            catch (Exception ex) when (ex is not OperationCanceledException)
            {
                Log($"[Server] Client {remote} error: {ex.Message}");
            }
            finally
            {
                client.Close();
                Log($"[Server] Client {remote} disconnected");
            }
        }

        private static async Task SendAckAsync(NetworkStream stream, bool ok, string reason, CancellationToken ct)
        {
            var msg = $"{(ok ? "ACK" : "NAK")}:{reason}";
            var bytes = Encoding.UTF8.GetBytes(msg);
            var frame = new byte[4 + bytes.Length];
            BitConverter.GetBytes(bytes.Length).CopyTo(frame, 0);
            bytes.CopyTo(frame, 4);
            await stream.WriteAsync(frame, ct);
        }

        private static async Task<bool> ReadExactAsync(NetworkStream stream, byte[] buf, CancellationToken ct)
        {
            int offset = 0;
            while (offset < buf.Length)
            {
                int n = await stream.ReadAsync(buf.AsMemory(offset), ct);
                if (n == 0) return false;
                offset += n;
            }
            return true;
        }

        private void Log(string msg) => OnLog?.Invoke(msg);

        public void Stop() => _listener?.Stop();
    }
}
