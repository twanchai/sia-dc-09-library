using System;
using System.Threading.Tasks;
using SecureProtocol;

// =====================================================================
//  SecureProtocol — Client (SPT / Signal Path Transmitter)
//
//  Usage:
//    dotnet run
//      → dev mode, auto keys, DSC factory-default key (zeros)
//
//    dotnet run -- <host> <port> <hmacHex> <aesHex> [dscAes128Hex]
//      → full mode; supply dscAes128Hex to override DSC key
//
//  Commands:
//    SecurePacket path:
//      send <msg>                     — AES-256-GCM + HMAC-SHA256
//      tamper <msg>                   — corrupt payload (test rejection)
//      replay                         — send with old seq=1
//
//    DSC Contact ID path:
//      dsc-cid <event> [acct] [part] [zone] [q]
//                                     — DSC binary + Contact ID payload
//                                       event = 3-digit code (130=Burg, 110=Fire, 120=Panic,
//                                               401=Armed, 602=Test)
//                                       acct  = account code (default 9999)
//                                       part  = partition   (default 1)
//                                       zone  = zone/user   (default 1)
//                                       q     = qualifier   1=alarm 3=restore (default 1)
//      Examples:
//        dsc-cid 130            → Burglary alarm, acct 9999, part 1, zone 1
//        dsc-cid 130 1234 1 5   → Burglary, acct 1234, part 1, zone 5
//        dsc-cid 130 9999 1 1 3 → Burglary restore
//
//    DSC raw path:
//      dsc <payload>                  — raw DSC binary frame (plaintext payload)
//      dsc-badcrc <payload>           — DSC frame with corrupted CRC
//      dsc-badkey <payload>           — DSC frame encrypted with wrong key
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
Console.WriteLine("[Client] Commands: send | tamper | replay | dsc-cid | dsc | dsc-badcrc | dsc-badkey | quit\n");
Console.WriteLine("  dsc-cid <event> [acct] [part] [zone] [q]");
Console.WriteLine("    e.g.  dsc-cid 130            → Burglary alarm acct=9999 part=1 zone=1");
Console.WriteLine("          dsc-cid 110 9999 1 2   → Fire alarm zone 2");
Console.WriteLine("          dsc-cid 130 9999 1 1 3 → Burglary restore\n");

while (true)
{
    Console.Write("> ");
    var line = Console.ReadLine();
    if (line is null || line.Equals("quit", StringComparison.OrdinalIgnoreCase)) break;

    var tokens = line.Split(' ', StringSplitOptions.RemoveEmptyEntries);
    var cmd    = tokens.Length > 0 ? tokens[0].ToLowerInvariant() : "";
    var body   = tokens.Length > 1 ? string.Join(' ', tokens[1..]) : "test";

    try
    {
        string ack;

        if (cmd == "dsc-cid")
        {
            // dsc-cid <event> [acct] [part] [zone] [q]
            int  eventCode = tokens.Length > 1 ? int.Parse(tokens[1])       : 130;
            string account = tokens.Length > 2 ? tokens[2]                  : "9999";
            int  partition = tokens.Length > 3 ? int.Parse(tokens[3])       : 1;
            int  zone      = tokens.Length > 4 ? int.Parse(tokens[4])       : 1;
            char qualifier = tokens.Length > 5 ? tokens[5][0]               : '1';

            ack = await sender.SendDscContactIdAsync(
                account, eventCode, partition, zone, qualifier, dscAesKey);
        }
        else if (cmd.StartsWith("dsc"))
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
