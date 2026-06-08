using System;
using System.Threading;
using System.Threading.Tasks;
using SecureProtocol;

// =====================================================================
//  SecureProtocol — Server (RCT / Receiver Central Terminal)
//
//  Usage:
//    dotnet run                          ← auto-generate keys, print them
//    dotnet run -- <port> <hmacHex> <aesHex>  ← use provided keys
//
//  The server prints its HMAC and AES keys on startup.
//  Pass those keys to the Client to establish a secure session.
// =====================================================================

int    port;
byte[] hmacKey;
byte[] aesKey;

if (args.Length >= 3)
{
    port    = int.Parse(args[0]);
    hmacKey = Convert.FromHexString(args[1]);
    aesKey  = Convert.FromHexString(args[2]);
    Console.WriteLine($"[Server] Starting on port {port} with provided keys.");
}
else
{
    port    = 9876;
    hmacKey = CryptoHelper.GenerateHmacKey();
    aesKey  = CryptoHelper.GenerateAesKey();

    Console.WriteLine("=== SecureProtocol Server ===");
    Console.WriteLine("[Server] Keys generated. Share with client:");
    Console.WriteLine($"  HMAC : {Convert.ToHexString(hmacKey)}");
    Console.WriteLine($"  AES  : {Convert.ToHexString(aesKey)}\n");
    Console.WriteLine($"  Client command:");
    Console.WriteLine($"    dotnet run -- 127.0.0.1 {port} {Convert.ToHexString(hmacKey)} {Convert.ToHexString(aesKey)}\n");
}

var cts    = new CancellationTokenSource();
var server = new SecureReceiver(port, hmacKey, aesKey);

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
