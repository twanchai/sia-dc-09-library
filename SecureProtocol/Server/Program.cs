using System;
using System.Threading;
using System.Threading.Tasks;
using SecureProtocol;

// =====================================================================
//  SecureProtocol — Server (RCT / Receiver Central Terminal)
//
//  Config file (config.json — same directory as executable):
//    {
//      "Port":      50005,
//      "HmacKey":   "<64 hex chars = 32 bytes>",
//      "AesKey":    "<64 hex chars = 32 bytes>",
//      "DscAesKey": "<32 hex chars OR 16 ASCII chars — omit for factory default>"
//    }
//
//  Usage:
//    dotnet run
//      → loads config.json; generates one if missing
//
//    dotnet run -- --genkey
//      → (re)generate config.json with fresh random keys, then exit
//
//  Frame auto-detection:
//    byte[0] == 0x5F  → DSC TL-series binary (Contact ID / SIA, AES-128-CBC, CRC32)
//    byte[0] == 0x0A  → SIA DC-09 standard (LF…CR frame)
//    otherwise        → SecurePacket (HMAC-SHA256, AES-256-GCM)
// =====================================================================

const string ConfigFile = "config.json";

// ── --genkey flag ────────────────────────────────────────────────────────────
if (args.Length == 1 && args[0] == "--genkey")
{
    var gen = AppConfig.GenerateAndSave(ConfigFile);
    Console.WriteLine($"[Server] Generated new keys → {ConfigFile}");
    Console.WriteLine($"  HmacKey  : {gen.HmacKey}");
    Console.WriteLine($"  AesKey   : {gen.AesKey}");
    Console.WriteLine($"  DscAesKey: (factory default — zeros)");
    Console.WriteLine($"\nCopy the same HmacKey + AesKey into the Client's config.json.");
    return;
}

// ── Load config ───────────────────────────────────────────────────────────────
AppConfig cfg;
try
{
    cfg = AppConfig.Load(ConfigFile);
    Console.WriteLine($"[Server] Loaded config from {ConfigFile}");
}
catch (System.IO.FileNotFoundException)
{
    Console.WriteLine($"[Server] config.json not found — generating with random keys...");
    cfg = AppConfig.GenerateAndSave(ConfigFile);
    Console.WriteLine($"[Server] Created {ConfigFile}");
    Console.WriteLine($"  HmacKey  : {cfg.HmacKey}");
    Console.WriteLine($"  AesKey   : {cfg.AesKey}");
    Console.WriteLine($"\nCopy both keys into the Client's config.json, then restart.\n");
}
catch (Exception ex)
{
    Console.ForegroundColor = ConsoleColor.Red;
    Console.WriteLine($"[Server] Config error: {ex.Message}");
    Console.ResetColor();
    return;
}

int    port      = cfg.Port;
byte[] hmacKey   = cfg.GetHmacKeyBytes();
byte[] aesKey    = cfg.GetAesKeyBytes();
byte[] dscAesKey = cfg.GetDscAesKeyBytes();

Console.WriteLine($"[Server] Port      : {port}");
Console.WriteLine($"[Server] HmacKey   : {cfg.HmacKey[..16]}... ({hmacKey.Length} bytes)");
Console.WriteLine($"[Server] AesKey    : {cfg.AesKey[..16]}... ({aesKey.Length} bytes)");
Console.WriteLine($"[Server] DscAesKey : {(cfg.DscAesKeyIsDefault() ? "factory default (zeros)" : cfg.DscAesKey[..8] + "...")}");

// ── Start server ──────────────────────────────────────────────────────────────
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

Console.WriteLine("[Server] Press Ctrl+C to stop.\n");

await server.StartAsync(cts.Token);
