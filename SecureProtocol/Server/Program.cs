using System;
using System.Threading;
using System.Threading.Tasks;
using SecureProtocol;

// =====================================================================
//  SecureProtocol — Server (RCT / Receiver Central Terminal)
//
//  Usage:
//    dotnet run
//      → auto-generate SecurePacket keys, DSC support disabled
//
//    dotnet run -- <port> <hmacHex> <aesHex> [dscAes128Hex]
//      → use provided keys; supply dscAes128Hex (32 hex chars = 16 bytes)
//         to enable DSC TL-series binary frame support
//
//  Frame auto-detection:
//    byte[0] == 0x5F  → DSC binary path  (AES-128-CBC, CRC32)
//    otherwise        → SecurePacket path (HMAC-SHA256, AES-256-GCM)
// =====================================================================

int    port;
byte[] hmacKey;
byte[] aesKey;
byte[]? dscAesKey = null;

if (args.Length >= 3)
{
    port    = int.Parse(args[0]);
    hmacKey = Convert.FromHexString(args[1]);
    aesKey  = Convert.FromHexString(args[2]);
    dscAesKey = args.Length >= 4
        ? ParseDscKey(args[3])
        : new byte[16];   // default: 16 zeros (factory default)
    Console.WriteLine($"[Server] Starting on port {port}.");
}
else
{
    port    = 3061;
    hmacKey = CryptoHelper.GenerateHmacKey();
    aesKey  = CryptoHelper.GenerateAesKey();

    Console.WriteLine("=== SecureProtocol Server ===");
    Console.WriteLine("[Server] Keys generated. Share with Client:");
    Console.WriteLine($"  HMAC : {Convert.ToHexString(hmacKey)}");
    Console.WriteLine($"  AES  : {Convert.ToHexString(aesKey)}");
    Console.WriteLine($"\n  SecurePacket client:");
    Console.WriteLine($"    dotnet run -- 127.0.0.1 {port} {Convert.ToHexString(hmacKey)} {Convert.ToHexString(aesKey)}");
    Console.WriteLine($"\n  DSC support: ENABLED with factory-default key (16 zeros)");
    Console.WriteLine($"    Override: dotnet run -- {port} {Convert.ToHexString(hmacKey)} {Convert.ToHexString(aesKey)} <dscKeyHexOrAscii>\n");
    dscAesKey = new byte[16];  // default zeros
}

var cts    = new CancellationTokenSource();
var server = new SecureReceiver(port, hmacKey, aesKey, dscAesKey);

server.OnLog += msg =>
{
    var color = msg.Contains("✅") ? ConsoleColor.Green
              : msg.Contains("❌") ? ConsoleColor.Red
              : ConsoleColor.Gray;
    Console.ForegroundColor = color;
    Console.WriteLine(msg);
    Console.ResetColor();
};

Console.CancelKeyPress += (_, e) =>
{
    e.Cancel = true;
    Console.WriteLine("\n[Server] Shutting down...");
    cts.Cancel();
};

Console.WriteLine($"[Server] DSC key: {(IsDefaultKey(dscAesKey!) ? "factory default (zeros)" : "custom")}");
Console.WriteLine("[Server] Press Ctrl+C to stop.\n");

await server.StartAsync(cts.Token);

// ── Helpers ──────────────────────────────────────────────────────────────────

/// <summary>
/// Accept key as 32-char hex OR 16-char ASCII.
/// Everything else throws.
/// </summary>
static byte[] ParseDscKey(string input)
{
    input = input.Trim();
    if (input.Length == 32 && IsHex(input))
        return Convert.FromHexString(input);
    if (input.Length == 16)
        return System.Text.Encoding.ASCII.GetBytes(input);
    throw new ArgumentException(
        $"DSC key must be 32 hex chars or 16 ASCII chars, got {input.Length} chars: \"{input}\"");
}

static bool IsHex(string s)
{
    foreach (var c in s)
        if (!Uri.IsHexDigit(c)) return false;
    return true;
}

static bool IsDefaultKey(byte[] key)
{
    foreach (var b in key)
        if (b != 0) return false;
    return true;
}
