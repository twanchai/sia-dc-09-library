using System;
using System.Collections.Generic;
using System.Threading.Tasks;
using SecureProtocol;

// =====================================================================
//  SecureProtocol — Client (SPT / Signal Path Transmitter)
//
//  Config file (config.json — same directory as executable):
//    {
//      "Host":      "127.0.0.1",
//      "Port":      50005,
//      "HmacKey":   "<64 hex chars = 32 bytes>",
//      "AesKey":    "<64 hex chars = 32 bytes>",
//      "DscAesKey": "<32 hex chars OR 16 ASCII chars — omit for factory default>"
//    }
//
//  Commands:
//    SecurePacket path:
//      send <msg>                     — AES-256-GCM + HMAC-SHA256
//      tamper <msg>                   — corrupt payload (test rejection)
//      replay                         — send with old seq=1
//
//    DSC Contact ID path:
//      dsc-cid <event> [acct] [part] [zone] [q]
//        event = 3-digit code (130=Burg, 110=Fire, 120=Panic, 401=Armed, 602=Test)
//        acct  = account code        (default: 9999)
//        part  = partition number    (default: 1)
//        zone  = zone/user number    (default: 1)
//        q     = qualifier           1=alarm  3=restore  (default: 1)
//      Examples:
//        dsc-cid 130              → Burglary alarm, acct=9999, part=1, zone=1
//        dsc-cid 110 9999 1 2     → Fire alarm, zone 2
//        dsc-cid 130 9999 1 1 3   → Burglary restore
//
//    DSC raw path:
//      dsc <payload>                  — raw DSC frame (plaintext payload)
//      dsc-badcrc <payload>           — DSC frame with corrupted CRC
//      dsc-badkey <payload>           — DSC frame with wrong key
//
//    quit / Ctrl+C
// =====================================================================

const string ConfigFile = "config.json";

// ── Load config ───────────────────────────────────────────────────────────────
AppConfig cfg;
try
{
    cfg = AppConfig.Load(ConfigFile);
    Console.WriteLine($"[Client] Loaded config from {ConfigFile}");
}
catch (Exception ex)
{
    Console.ForegroundColor = ConsoleColor.Red;
    Console.WriteLine($"[Client] Config error: {ex.Message}");
    Console.ResetColor();
    Console.WriteLine("Run the Server once to generate config.json, then copy HmacKey + AesKey here.");
    return;
}

string host      = cfg.Host;
int    port      = cfg.Port;
byte[] hmacKey   = cfg.GetHmacKeyBytes();
byte[] aesKey    = cfg.GetAesKeyBytes();
byte[] dscAesKey = cfg.GetDscAesKeyBytes();

Console.WriteLine($"[Client] Target    : {host}:{port}");
Console.WriteLine($"[Client] HmacKey   : {cfg.HmacKey[..16]}... ({hmacKey.Length} bytes)");
Console.WriteLine($"[Client] AesKey    : {cfg.AesKey[..16]}... ({aesKey.Length} bytes)");
Console.WriteLine($"[Client] DscAesKey : {(cfg.DscAesKeyIsDefault() ? "factory default (zeros)" : cfg.DscAesKey[..8] + "...")}");
Console.WriteLine();
Console.WriteLine("Commands: send | tamper | replay | dsc-cid | dsc | dsc-badcrc | dsc-badkey | quit");
Console.WriteLine("  dsc-cid <event> [acct] [part] [zone] [q]");
Console.WriteLine("    e.g.  dsc-cid 130            → Burglary alarm acct=9999 part=1 zone=1");
Console.WriteLine("          dsc-cid 110 9999 1 2   → Fire alarm zone 2");
Console.WriteLine("          dsc-cid 130 9999 1 1 3 → Burglary restore");
Console.WriteLine("  ↑ ↓  = history   ← →  = cursor   Home/End   Esc = clear\n");

var sender = new SecureSender(host, port, hmacKey, aesKey);
sender.OnLog += Console.WriteLine;

// ── REPL ──────────────────────────────────────────────────────────────────────
var history = new List<string>();

while (true)
{
    var line = ReadLine(history);
    if (line is null || line.Equals("quit", StringComparison.OrdinalIgnoreCase)) break;
    if (string.IsNullOrWhiteSpace(line)) continue;

    var tokens = line.Split(' ', StringSplitOptions.RemoveEmptyEntries);
    var cmd    = tokens.Length > 0 ? tokens[0].ToLowerInvariant() : "";
    var body   = tokens.Length > 1 ? string.Join(' ', tokens[1..]) : "test";

    try
    {
        string ack;

        if (cmd == "dsc-cid")
        {
            int    eventCode = tokens.Length > 1 ? int.Parse(tokens[1]) : 130;
            string account   = tokens.Length > 2 ? tokens[2]            : "9999";
            int    partition = tokens.Length > 3 ? int.Parse(tokens[3]) : 1;
            int    zone      = tokens.Length > 4 ? int.Parse(tokens[4]) : 1;
            char   qualifier = tokens.Length > 5 ? tokens[5][0]         : '1';

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

// ── History-aware readline ────────────────────────────────────────────────────

static string? ReadLine(List<string> history, string prompt = "> ")
{
    // Non-interactive (piped input) — fall back to standard readline
    if (Console.IsInputRedirected)
    {
        Console.Write(prompt);
        return Console.ReadLine();
    }

    var buf     = new List<char>();
    int cursor  = 0;
    int histIdx = history.Count;   // points past end = "new input"
    string saved = "";             // current draft saved while browsing history

    Console.Write(prompt);

    while (true)
    {
        ConsoleKeyInfo k;
        try   { k = Console.ReadKey(intercept: true); }
        catch { return null; }

        switch (k.Key)
        {
            // ── Submit ────────────────────────────────────────────────────
            case ConsoleKey.Enter:
                Console.WriteLine();
                var result = new string(buf.ToArray());
                if (!string.IsNullOrWhiteSpace(result) &&
                    (history.Count == 0 || history[^1] != result))
                    history.Add(result);
                return result;

            // ── Ctrl+C ────────────────────────────────────────────────────
            case ConsoleKey.C when k.Modifiers.HasFlag(ConsoleModifiers.Control):
                Console.WriteLine("^C");
                return null;

            // ── Escape — clear line ───────────────────────────────────────
            case ConsoleKey.Escape:
                buf.Clear(); cursor = 0; histIdx = history.Count; saved = "";
                Redraw(prompt, buf, cursor);
                break;

            // ── Backspace ─────────────────────────────────────────────────
            case ConsoleKey.Backspace:
                if (cursor > 0) { buf.RemoveAt(--cursor); Redraw(prompt, buf, cursor); }
                break;

            // ── Delete ────────────────────────────────────────────────────
            case ConsoleKey.Delete:
                if (cursor < buf.Count) { buf.RemoveAt(cursor); Redraw(prompt, buf, cursor); }
                break;

            // ── Cursor movement ───────────────────────────────────────────
            case ConsoleKey.LeftArrow:
                if (cursor > 0)
                    Console.SetCursorPosition(prompt.Length + --cursor, Console.CursorTop);
                break;

            case ConsoleKey.RightArrow:
                if (cursor < buf.Count)
                    Console.SetCursorPosition(prompt.Length + ++cursor, Console.CursorTop);
                break;

            case ConsoleKey.Home:
                cursor = 0;
                Console.SetCursorPosition(prompt.Length, Console.CursorTop);
                break;

            case ConsoleKey.End:
                cursor = buf.Count;
                Console.SetCursorPosition(prompt.Length + cursor, Console.CursorTop);
                break;

            // ── History navigation ────────────────────────────────────────
            case ConsoleKey.UpArrow:
                if (histIdx > 0)
                {
                    if (histIdx == history.Count)       // save draft before leaving
                        saved = new string(buf.ToArray());
                    SetBuf(prompt, buf, history[--histIdx], ref cursor);
                }
                break;

            case ConsoleKey.DownArrow:
                if (histIdx < history.Count)
                {
                    ++histIdx;
                    var text = histIdx == history.Count ? saved : history[histIdx];
                    SetBuf(prompt, buf, text, ref cursor);
                }
                break;

            // ── Printable character ───────────────────────────────────────
            default:
                if (!char.IsControl(k.KeyChar))
                {
                    buf.Insert(cursor++, k.KeyChar);
                    Redraw(prompt, buf, cursor);
                }
                break;
        }
    }
}

// Redraw entire editable area and reposition cursor
static void Redraw(string prompt, List<char> buf, int cursor)
{
    int row = Console.CursorTop;
    Console.SetCursorPosition(prompt.Length, row);
    Console.Write(new string(buf.ToArray()) + " "); // trailing space clears deleted chars
    Console.SetCursorPosition(prompt.Length + cursor, row);
}

// Replace buffer contents and move cursor to end
static void SetBuf(string prompt, List<char> buf, string text, ref int cursor)
{
    buf.Clear();
    buf.AddRange(text);
    cursor = buf.Count;
    Redraw(prompt, buf, cursor);
}
