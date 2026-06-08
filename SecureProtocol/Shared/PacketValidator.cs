using System;

namespace SecureProtocol
{
    /// <summary>
    /// ชุดรับใหม่ — ตรวจสอบ packet ครบทุกชั้น:
    ///   1. Magic number
    ///   2. Protocol version
    ///   3. Timestamp freshness (replay protection)
    ///   4. Sequence number monotonicity (duplicate/replay protection)
    ///   5. HMAC-SHA256 integrity
    ///   6. AES-GCM decryption (authentication tag)
    /// </summary>
    public class PacketValidator
    {
        private readonly byte[] _hmacKey;
        private readonly byte[] _aesKey;
        private readonly TimeSpan _maxAge;
        private uint _lastSeq = 0;
        private bool _firstPacket = true;

        /// <param name="hmacKey">32-byte HMAC key ที่แชร์กับ sender</param>
        /// <param name="aesKey">32-byte AES-256 key</param>
        /// <param name="maxAge">อายุสูงสุดของ packet — ป้องกัน replay (default 30s)</param>
        public PacketValidator(byte[] hmacKey, byte[] aesKey, TimeSpan? maxAge = null)
        {
            _hmacKey = hmacKey;
            _aesKey  = aesKey;
            _maxAge  = maxAge ?? TimeSpan.FromSeconds(30);
        }

        /// <summary>
        /// Validate และ Decrypt packet
        /// คืน (result, plaintext) — plaintext จะเป็น null ถ้า invalid
        /// </summary>
        public (ValidationResult Result, byte[]? Plaintext) ValidateAndDecrypt(SecurePacket pkt)
        {
            // 1. Magic
            if (pkt.Magic != SecurePacket.MAGIC)
                return (ValidationResult.Fail($"Bad magic: 0x{pkt.Magic:X8}"), null);

            // 2. Version
            if (pkt.Version != SecurePacket.PROTOCOL_VERSION)
                return (ValidationResult.Fail($"Unsupported version: {pkt.Version}"), null);

            // 3. Timestamp freshness
            var packetTime = DateTimeOffset.FromUnixTimeMilliseconds(pkt.TimestampUtc).UtcDateTime;
            var age = DateTime.UtcNow - packetTime;
            if (Math.Abs(age.TotalMilliseconds) > _maxAge.TotalMilliseconds)
                return (ValidationResult.Fail($"Stale packet — age: {age.TotalSeconds:F1}s (max {_maxAge.TotalSeconds}s)"), null);

            // 4. Sequence number (anti-replay)
            if (!_firstPacket && pkt.SequenceNumber <= _lastSeq)
                return (ValidationResult.Fail($"Replay/duplicate — seq {pkt.SequenceNumber} <= last {_lastSeq}"), null);

            // 5. HMAC
            var signableBytes = pkt.GetSignableBytes();
            if (!CryptoHelper.VerifyHmac(signableBytes, pkt.Hmac, _hmacKey))
                return (ValidationResult.Fail("HMAC mismatch — packet tampered"), null);

            // 6. AES-GCM decrypt (tag ฝังอยู่ใน EncryptedPayload)
            byte[] plaintext;
            try
            {
                plaintext = CryptoHelper.Decrypt(pkt.EncryptedPayload, pkt.AesNonce, _aesKey);
            }
            catch (System.Security.Cryptography.CryptographicException ex)
            {
                return (ValidationResult.Fail($"Decryption failed (bad tag): {ex.Message}"), null);
            }

            // All checks passed — update state
            _lastSeq     = pkt.SequenceNumber;
            _firstPacket = false;

            return (ValidationResult.Ok(), plaintext);
        }

        /// <summary>Reset sequence state (เช่น เมื่อ reconnect)</summary>
        public void Reset()
        {
            _lastSeq     = 0;
            _firstPacket = true;
        }
    }
}
