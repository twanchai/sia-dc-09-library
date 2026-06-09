using System;
using System.IO;
using System.Security.Cryptography;

namespace SecureProtocol
{
    /// <summary>
    /// DSC TL-series (TL280, TL265, TL280RE) proprietary binary frame wrapper.
    ///
    /// Frame layout (28 bytes minimum):
    ///   [00]     0x5F       — Sync / frame-type marker
    ///   [01-02]  uint16 BE  — Sequence number
    ///   [03-05]  3 bytes    — Account hash (derived from account number)
    ///   [06-07]  uint16 BE  — Flags / option bits
    ///   [08-23]  16 bytes   — AES-128-CBC encrypted SIA DC-09 payload (1 block, IV=zeros)
    ///   [24-27]  uint32 BE  — CRC32 over bytes [00-23]
    /// </summary>
    public sealed class DscEnvelope
    {
        public const byte SyncByte       = 0x5F;
        public const int  FrameMinLength = 28;
        public const int  PayloadOffset  = 8;
        public const int  PayloadLength  = 16;   // AES-128 = 1 block
        public const int  CrcOffset      = 24;

        // --- Fields ---
        public byte   FrameType    { get; init; } = SyncByte;
        public ushort Sequence     { get; init; }
        public byte[] AccountHash  { get; init; } = new byte[3];   // bytes [03-05]
        public ushort Flags        { get; init; }
        public byte[] CipherBlock  { get; init; } = new byte[PayloadLength]; // AES-128 CBC block
        public uint   Crc32        { get; init; }

        // ---- Factory ----

        /// <summary>Returns true if raw bytes look like a DSC binary frame.</summary>
        public static bool IsDscFrame(ReadOnlySpan<byte> data)
            => data.Length >= FrameMinLength && data[0] == SyncByte;

        /// <summary>Parse raw bytes into a DscEnvelope.</summary>
        public static DscEnvelope Parse(byte[] data)
        {
            if (data.Length < FrameMinLength)
                throw new ArgumentException($"DSC frame too short: {data.Length} < {FrameMinLength}");
            if (data[0] != SyncByte)
                throw new ArgumentException($"Not a DSC frame: expected 0x5F, got 0x{data[0]:X2}");

            return new DscEnvelope
            {
                FrameType   = data[0],
                Sequence    = (ushort)((data[1] << 8) | data[2]),
                AccountHash = data[3..6],
                Flags       = (ushort)((data[6] << 8) | data[7]),
                CipherBlock = data[PayloadOffset..(PayloadOffset + PayloadLength)],
                Crc32       = (uint)((data[24] << 24) | (data[25] << 16) | (data[26] << 8) | data[27])
            };
        }

        // ---- CRC ----

        /// <summary>Compute CRC32 over bytes [0..23] of the frame.</summary>
        public static uint ComputeCrc(byte[] data)
        {
            uint crc = 0xFFFFFFFF;
            for (int i = 0; i < CrcOffset; i++)
            {
                crc ^= data[i];
                for (int j = 0; j < 8; j++)
                    crc = (crc & 1) != 0 ? (crc >> 1) ^ 0xEDB88320 : crc >> 1;
            }
            return ~crc;
        }

        /// <summary>Verify CRC32 of this envelope.</summary>
        public bool VerifyCrc(byte[] rawFrame) => ComputeCrc(rawFrame) == Crc32;

        // ---- AES-128-CBC ----

        /// <summary>
        /// Decrypt CipherBlock with AES-128-CBC.
        /// DSC uses IV = 16 zero bytes by default.
        /// Key = 16 bytes derived from installer code + account number.
        /// </summary>
        public byte[] Decrypt(byte[] aes128Key, byte[]? iv = null)
        {
            ValidateKey(aes128Key);
            iv ??= new byte[16];

            using var aes = Aes.Create();
            aes.Mode    = CipherMode.CBC;
            aes.Padding = PaddingMode.Zeros;
            aes.Key     = aes128Key;
            aes.IV      = iv;

            using var decryptor = aes.CreateDecryptor();
            using var ms  = new MemoryStream(CipherBlock);
            using var cs  = new CryptoStream(ms, decryptor, CryptoStreamMode.Read);
            using var out_= new MemoryStream();
            cs.CopyTo(out_);
            return TrimNullBytes(out_.ToArray());
        }

        // ---- Build (for Client / test) ----

        /// <summary>
        /// Create a DSC frame wrapping <paramref name="siaPayload"/> (must be ≤ 16 bytes, zero-padded to block).
        /// If <paramref name="aes128Key"/> is all zeros, the payload is sent as plaintext (AES not configured).
        /// </summary>
        public static byte[] Build(byte[] siaPayload, byte[] aes128Key,
                                   ushort sequence, byte[] accountHash,
                                   ushort flags = 0x0000, byte[]? iv = null)
        {
            ValidateKey(aes128Key);
            iv ??= new byte[16];

            // Pad payload to AES block size
            var padded = new byte[PayloadLength];
            Buffer.BlockCopy(siaPayload, 0, padded, 0,
                Math.Min(siaPayload.Length, PayloadLength));

            // If key is all zeros → AES not configured → send plaintext (matches server plaintext mode)
            byte[] cipher;
            if (IsAllZeros(aes128Key))
            {
                cipher = padded;
            }
            else
            {
                // AES-128-CBC encrypt
                using var aes = Aes.Create();
                aes.Mode    = CipherMode.CBC;
                aes.Padding = PaddingMode.Zeros;
                aes.Key     = aes128Key;
                aes.IV      = iv;

                using var encryptor = aes.CreateEncryptor();
                cipher = encryptor.TransformFinalBlock(padded, 0, padded.Length);
            }

            // Assemble frame (without CRC first)
            var frame = new byte[FrameMinLength];
            frame[0] = SyncByte;
            frame[1] = (byte)(sequence >> 8);
            frame[2] = (byte)(sequence & 0xFF);
            Buffer.BlockCopy(accountHash, 0, frame, 3, Math.Min(accountHash.Length, 3));
            frame[6] = (byte)(flags >> 8);
            frame[7] = (byte)(flags & 0xFF);
            Buffer.BlockCopy(cipher, 0, frame, PayloadOffset, PayloadLength);

            // Compute and append CRC32
            uint crc = ComputeCrc(frame);
            frame[24] = (byte)(crc >> 24);
            frame[25] = (byte)(crc >> 16);
            frame[26] = (byte)(crc >> 8);
            frame[27] = (byte)(crc & 0xFF);

            return frame;
        }

        // ---- Helpers ----

        private static void ValidateKey(byte[] key)
        {
            if (key.Length != 16)
                throw new ArgumentException($"DSC uses AES-128: key must be 16 bytes, got {key.Length}");
        }

        private static bool IsAllZeros(byte[] key)
        {
            foreach (var b in key) if (b != 0) return false;
            return true;
        }

        private static byte[] TrimNullBytes(byte[] data)
        {
            int len = data.Length;
            while (len > 0 && data[len - 1] == 0x00) len--;
            return data[..len];
        }

        public override string ToString() =>
            $"DscEnvelope {{ seq={Sequence}, flags=0x{Flags:X4}, " +
            $"acctHash={BitConverter.ToString(AccountHash)}, " +
            $"cipher={BitConverter.ToString(CipherBlock)}, crc=0x{Crc32:X8} }}";
    }

    /// <summary>
    /// Result after unwrapping a DSC frame on the server side.
    /// </summary>
    public record DscUnwrapResult(
        bool       IsDsc,
        bool       CrcValid,
        byte[]     SiaPayload,     // decrypted SIA DC-09 content (or raw bytes if not DSC)
        DscEnvelope? Envelope,
        string     Reason = "");
}
