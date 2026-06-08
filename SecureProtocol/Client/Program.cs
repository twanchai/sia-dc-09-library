using System;
using System.Threading.Tasks;
using SecureProtocol;

// =====================================================================
//  SecureProtocol — Client (SPT / Signal Path Transmitter)
//
//  Usage:
//    dotnet run
//      → dev mode, auto keys, no DSC
//
//    dotnet run -- <host> <port> <hmacHex> <aesHex> [dscAes128Hex]
//      → full mode; supply dscAes128Hex to enable DSC commands
//
//  Commands:
//    SecurePacket path:
//      send <msg>         — AES-256-GCM + HMAC-SHA256
//      tamper <msg>       — corrupt payload (test rejection)
//      replay             — send with old seq=1
//
//    DSC TL-series path (requires dscAes128Hex):
//      dsc <msg>          — DSC binary frame, AES-128-CBC + CRC32
//      dsc-badcrc <msg>   — DSC frame with corrupted CRC
//      dsc-badkey <msg>   — DSC frame encrypted with wrong key
//
//    quit
// =====================================================================

string host;
int    port;
byte[] hmacKey;
byte[] aesKey;
byte[]? dscAesKey = null;

if (args.Length >= 4)
{
    host    = args[0];
    port    = int.Parse(args[1]);
    hmacKey = Convert.FromHexString(args[2]);
    aesKey  = Convert.FromHexString(args[3]);
    dscAesKey = args.Length >= 5 ? ParseDscKey(args[4]) : new byte[16];
    Console.WriteLine($"[Client] Connecting to {host}:{port}");
    Console.WriteLine($"[Client] DSC key: {(IsDefaultKey(dscAesKey) ? "factory default (zeros)" : "custom")}");
}
else
{
    host    = "127.0.0.1";
    port    = 3061;
    hmacKey = CryptoHelper.GenerateHmacKey();
    aesKey  = CryptoHelper.GenerateAesKey();
    Console.WriteLine("[Client] Dev mode — generated keys:");
    Console.WriteLine($"  HMAC : {Convert.ToHexString(hmacKey)}");
    Console.WriteLine($"  AES  : {Convert.ToHexString(aesKey)}\n");
}

var sender = new SecureSender(host, port, hmacKey, aesKey);
sender.OnLog += Console.WriteLine;

Console.WriteLine($"[Client] Target {host}:{port}");
Console.WriteLine("[Client] Commands: send | tamper | replay | dsc | dsc-badcrc | dsc-badkey | quit\n");

while (true)
{
    Console.Write("> ");
    var line = Console.ReadLine();
    if (line is null || line.Equals("quit", StringComparison.OrdinalIgnoreCase)) break;

    var parts = line.Split(' ', 2);
    var cmd   = parts[0].ToLowerInvariant();
    var body  = parts.Length > 1 ? parts[1] : "test";

    try
    {
        string ack;

        if (cmd.StartsWith("dsc"))
        {
            ack = cmd switch
            {
                "dsc"        => await sender.SendDscAsync(body, dscAesKey),
                "dsc-badcrc" => await sender.SendDscTamperedCrcAsync(body, dscAesKey),
                "dsc-badkey" => await sender.SendDscWrongKeyAsync(body, dscAesKey),
                _            => await sender.SendDscAsync(body, dscAesKey)
            };
        }
        else
        {
            ack = cmd switch
            {
                "send"   => await sender.SendAsync(body),
                "tamper" => await sender.SendTamperedAsync(body),
                "replay" => await sender.SendReplayAsync(body, replaySeq: 1),
                _        => await sender.SendAsync(line)
            };
        }

        Console.WriteLine($"[Client] Server: {ack}\n");
    }
    catch (Exception ex)
    {
        Console.WriteLine($"[Client] Error: {ex.Message}\n");
    }
}

Console.WriteLine("[Client] Disconnected.");

// ── Helpers ──────────────────────────────────────────────────────────────────

static byte[] ParseDscKey(string input)
{
    input = input.Trim();
    if (input.Length == 32 && IsHex(input))
        return Convert.FromHexString(input);
    if (input.Length == 16)
        return System.Text.Encoding.ASCII.GetBytes(input);
    throw new ArgumentException(
        $"DSC key must be 32 hex chars or 16 ASCII chars, got {input.Length}: \"{input}\"");
}

static bool IsHex(string s)
{
    foreach (var c in s) if (!Uri.IsHexDigit(c)) return false;
    return true;
}

static bool IsDefaultKey(byte[] key)
{
    foreach (var b in key) if (b != 0) return false;
    return true;
}
