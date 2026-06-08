using System;
using System.Security.Cryptography;
using System.Text;

namespace SecureProtocol
{
    /// <summary>
    /// Crypto helper: AES-256-GCM encryption + HMAC-SHA256 signing
    /// </summary>
    public static class CryptoHelper
    {
        // ---- AES-256-GCM ----

        /// <summary>สร้าง AES key แบบ random 256-bit</summary>
        public static byte[] GenerateAesKey() => RandomNumberGenerator.GetBytes(32);

        /// <summary>Encrypt ด้วย AES-256-GCM  คืน (ciphertext+tag, nonce)</summary>
        public static (byte[] CipherWithTag, byte[] Nonce) Encrypt(byte[] plaintext, byte[] key)
        {
            var nonce = RandomNumberGenerator.GetBytes(AesGcm.NonceByteSizes.MaxSize); // 12 bytes
            var ciphertext = new byte[plaintext.Length];
            var tag = new byte[AesGcm.TagByteSizes.MaxSize]; // 16 bytes

            using var aes = new AesGcm(key, AesGcm.TagByteSizes.MaxSize);
            aes.Encrypt(nonce, plaintext, ciphertext, tag);

            // ต่อ tag ไว้ท้าย ciphertext
            var result = new byte[ciphertext.Length + tag.Length];
            Buffer.BlockCopy(ciphertext, 0, result, 0, ciphertext.Length);
            Buffer.BlockCopy(tag, 0, result, ciphertext.Length, tag.Length);

            return (result, nonce);
        }

        /// <summary>Decrypt ด้วย AES-256-GCM — throw ถ้า tag ไม่ผ่าน (tamper detected)</summary>
        public static byte[] Decrypt(byte[] cipherWithTag, byte[] nonce, byte[] key)
        {
            int tagSize = AesGcm.TagByteSizes.MaxSize;
            int cipherLen = cipherWithTag.Length - tagSize;

            var ciphertext = new byte[cipherLen];
            var tag        = new byte[tagSize];
            Buffer.BlockCopy(cipherWithTag, 0,         ciphertext, 0, cipherLen);
            Buffer.BlockCopy(cipherWithTag, cipherLen, tag,        0, tagSize);

            var plaintext = new byte[cipherLen];
            using var aes = new AesGcm(key, tagSize);
            aes.Decrypt(nonce, ciphertext, tag, plaintext);  // throws CryptographicException on bad tag

            return plaintext;
        }

        // ---- HMAC-SHA256 ----

        public static byte[] GenerateHmacKey() => RandomNumberGenerator.GetBytes(32);

        /// <summary>คำนวณ HMAC-SHA256</summary>
        public static byte[] Sign(byte[] data, byte[] hmacKey)
            => HMACSHA256.HashData(hmacKey, data);

        /// <summary>ตรวจสอบ HMAC แบบ constant-time เพื่อป้องกัน timing attack</summary>
        public static bool VerifyHmac(byte[] data, byte[] expectedHmac, byte[] hmacKey)
        {
            var computed = HMACSHA256.HashData(hmacKey, data);
            return CryptographicOperations.FixedTimeEquals(computed, expectedHmac);
        }

        // ---- RSA Key Exchange (ใช้ส่ง AES key ครั้งแรก) ----

        public static (RSA PublicKey, RSA PrivateKey) GenerateRsaKeyPair(int bits = 2048)
        {
            var rsa = RSA.Create(bits);
            var pub = RSA.Create();
            pub.ImportRSAPublicKey(rsa.ExportRSAPublicKey(), out _);
            return (pub, rsa);
        }

        /// <summary>Encrypt AES key ด้วย RSA-OAEP-SHA256 (ส่งจาก client → server)</summary>
        public static byte[] RsaEncryptKey(byte[] aesKey, RSA recipientPublicKey)
            => recipientPublicKey.Encrypt(aesKey, RSAEncryptionPadding.OaepSHA256);

        /// <summary>Decrypt AES key ด้วย RSA private key</summary>
        public static byte[] RsaDecryptKey(byte[] encryptedKey, RSA privateKey)
            => privateKey.Decrypt(encryptedKey, RSAEncryptionPadding.OaepSHA256);
    }
}
