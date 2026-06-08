using System;
using System.Threading.Tasks;
using SecureProtocol;

// =====================================================================
//  SecureProtocol — Client (SPT / Signal Path Transmitter)
//
//  Usage:
//    dotnet run                                     ← dev mode (auto key)
//    dotnet run -- <host> <port> <hmacHex> <aesHex> ← connect to real server
//
//  Commands at the prompt:
//    send <message>    — send encrypted message
//    tamper <message>  — send tampered packet (test HMAC/GCM rejection)
//    replay            — replay seq=1 (test replay protection)
//    quit              — exit
// =====================================================================

string host;
int    port;
byte[] hmacKey;
byte[] aesKey;

if (args.Length >= 4)
{
    host    = args[0];
    port    = int.Parse(args[1]);
    hmacKey = Convert.FromHexString(args[2]);
    aesKey  = Convert.FromHexString(args[3]);
    Console.WriteLine($"[Client] Connecting to {host}:{port} with provided keys.");
}
else
{
    host    = "127.0.0.1";
    port    = 9876;
    hmacKey = CryptoHelper.GenerateHmacKey();
    aesKey  = CryptoHelper.GenerateAesKey();
    Console.WriteLine("[Client] Dev mode — generated new keys. Paste into Server when prompted:");
    Console.WriteLine($"  HMAC : {Convert.ToHexString(hmacKey)}");
    Console.WriteLine($"  AES  : {Convert.ToHexString(aesKey)}\n");
}

var sender = new SecureSender(host, port, hmacKey, aesKey);
sender.OnLog += Console.WriteLine;

Console.WriteLine($"[Client] Target {host}:{port}");
Console.WriteLine("[Client] Commands: send <msg> | tamper <msg> | replay | quit\n");

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
        var ack = cmd switch
        {
            "send"   => await sender.SendAsync(body),
            "tamper" => await sender.SendTamperedAsync(body),
            "replay" => await sender.SendReplayAsync(body, replaySeq: 1),
            _        => await sender.SendAsync(line)
        };
        Console.WriteLine($"[Client] Server: {ack}\n");
    }
    catch (Exception ex)
    {
        Console.WriteLine($"[Client] Error: {ex.Message}\n");
    }
}

Console.WriteLine("[Client] Disconnected.");
