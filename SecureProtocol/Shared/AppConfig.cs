using System;
using System.IO;
using System.Text;
using System.Text.Json;
using System.Text.Json.Serialization;

namespace SecureProtocol
{
    /// <summary>
    /// Pre-shared key configuration loaded from a JSON file.
    ///
    /// File format (config.json):
    /// {
    ///   "Port":      50005,
    ///   "Host":      "127.0.0.1",
    ///   "HmacKey":   "64-char hex  (32 bytes)",
    ///   "AesKey":    "64-char hex  (32 bytes)",
    ///   "DscAesKey": "32-char hex OR 16-char ASCII  (16 bytes, optional)"
    /// }
    ///
    /// DscAesKey may be omitted or left empty to use factory default (16 zeros).
    /// </summary>
    public sealed class AppConfig
    {
        [JsonPropertyName("Port")]
        public int Port { get; set; } = 50005;

        [JsonPropertyName("Host")]
        public string Host { get; set; } = "127.0.0.1";

        [JsonPropertyName("HmacKey")]
        public string HmacKey { get; set; } = "";

        [JsonPropertyName("AesKey")]
        public string AesKey { get; set; } = "";

        /// <summary>
        /// 32-char hex (= 16 bytes) OR 16-char ASCII.
        /// Empty / omitted → factory default (16 zero bytes).
        /// </summary>
        [JsonPropertyName("DscAesKey")]
        public string DscAesKey { get; set; } = "";

        // ── Parsed byte[] accessors ───────────────────────────────────────────

        public byte[] GetHmacKeyBytes()
        {
            if (string.IsNullOrWhiteSpace(HmacKey))
                throw new InvalidOperationException("HmacKey is missing in config.json");
            return Convert.FromHexString(HmacKey);
        }

        public byte[] GetAesKeyBytes()
        {
            if (string.IsNullOrWhiteSpace(AesKey))
                throw new InvalidOperationException("AesKey is missing in config.json");
            return Convert.FromHexString(AesKey);
        }

        public byte[] GetDscAesKeyBytes()
        {
            var s = (DscAesKey ?? "").Trim();
            if (s.Length == 0)                    return new byte[16];   // factory default
            if (s.Length == 32 && IsHex(s))       return Convert.FromHexString(s);
            if (s.Length == 16)                   return Encoding.ASCII.GetBytes(s);
            throw new InvalidOperationException(
                $"DscAesKey must be 32 hex chars or 16 ASCII chars, got {s.Length}: \"{s}\"");
        }

        public bool DscAesKeyIsDefault()
        {
            var k = GetDscAesKeyBytes();
            foreach (var b in k) if (b != 0) return false;
            return true;
        }

        // ── Loader ────────────────────────────────────────────────────────────

        /// <summary>
        /// Load config from <paramref name="path"/>.
        /// Throws a descriptive exception if the file is missing or malformed.
        /// </summary>
        public static AppConfig Load(string path = "config.json")
        {
            var fullPath = Path.GetFullPath(path);
            if (!File.Exists(fullPath))
                throw new FileNotFoundException(
                    $"Config file not found: {fullPath}\n" +
                    "Create config.json next to the executable with HmacKey and AesKey.", fullPath);

            var json = File.ReadAllText(fullPath);
            var cfg  = JsonSerializer.Deserialize<AppConfig>(json,
                           new JsonSerializerOptions { PropertyNameCaseInsensitive = true })
                       ?? throw new InvalidDataException("config.json is empty or invalid JSON.");

            // Validate required fields
            if (string.IsNullOrWhiteSpace(cfg.HmacKey))
                throw new InvalidDataException("config.json: HmacKey is required.");
            if (string.IsNullOrWhiteSpace(cfg.AesKey))
                throw new InvalidDataException("config.json: AesKey is required.");
            if (cfg.HmacKey.Length != 64 || !IsHex(cfg.HmacKey))
                throw new InvalidDataException("config.json: HmacKey must be 64 hex chars (32 bytes).");
            if (cfg.AesKey.Length != 64 || !IsHex(cfg.AesKey))
                throw new InvalidDataException("config.json: AesKey must be 64 hex chars (32 bytes).");

            return cfg;
        }

        /// <summary>
        /// Generate a new config.json with fresh random keys and save it to <paramref name="path"/>.
        /// </summary>
        public static AppConfig GenerateAndSave(string path = "config.json",
                                                int port = 50005, string host = "127.0.0.1")
        {
            var cfg = new AppConfig
            {
                Port      = port,
                Host      = host,
                HmacKey   = Convert.ToHexString(CryptoHelper.GenerateHmacKey()),
                AesKey    = Convert.ToHexString(CryptoHelper.GenerateAesKey()),
                DscAesKey = ""   // empty = factory default (16 zeros)
            };

            var json = JsonSerializer.Serialize(cfg,
                new JsonSerializerOptions { WriteIndented = true });
            File.WriteAllText(path, json);
            return cfg;
        }

        private static bool IsHex(string s)
        {
            foreach (var c in s) if (!Uri.IsHexDigit(c)) return false;
            return true;
        }
    }
}
