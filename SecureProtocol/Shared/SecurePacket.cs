using System;
using System.Security.Cryptography;
using System.Text;
using System.Text.Json;

namespace SecureProtocol
{
    /// <summary>
    /// Packet structure สำหรับ secure protocol
    /// Layout: [Header 16 bytes][SequenceNum 4 bytes][Timestamp 8 bytes][PayloadLen 4 bytes][Payload N bytes][HMAC 32 bytes]
    /// </summary>
    public class SecurePacket
    {
        public const int MAGIC = 0x53454350;   // "SECP"
        public const byte PROTOCOL_VERSION = 0x01;
        public const int HMAC_SIZE = 32;       // HMAC-SHA256

        // --- Header ---
        public int    Magic           { get; set; } = MAGIC;
        public byte   Version         { get; set; } = PROTOCOL_VERSION;
        public PacketType Type        { get; set; }
        public ushort Flags           { get; set; }
        public uint   SequenceNumber  { get; set; }
        public long   TimestampUtc    { get; set; }   // Unix ms

        // --- Payload ---
        public byte[] EncryptedPayload { get; set; } = Array.Empty<byte>();
        public byte[] AesNonce         { get; set; } = Array.Empty<byte>();  // 12 bytes GCM

        // --- Integrity ---
        public byte[] Hmac { get; set; } = Array.Empty<byte>();

        // ---- Serialize (ไม่รวม HMAC) สำหรับคำนวณ HMAC ----
        public byte[] GetSignableBytes()
        {
            using var ms = new System.IO.MemoryStream();
            using var bw = new System.IO.BinaryWriter(ms);
            bw.Write(Magic);
            bw.Write(Version);
            bw.Write((byte)Type);
            bw.Write(Flags);
            bw.Write(SequenceNumber);
            bw.Write(TimestampUtc);
            bw.Write(AesNonce.Length);
            bw.Write(AesNonce);
            bw.Write(EncryptedPayload.Length);
            bw.Write(EncryptedPayload);
            return ms.ToArray();
        }

        // ---- Full serialize (รวม HMAC) ----
        public byte[] Serialize()
        {
            using var ms = new System.IO.MemoryStream();
            using var bw = new System.IO.BinaryWriter(ms);
            var body = GetSignableBytes();
            bw.Write(body);
            bw.Write(Hmac);
            return ms.ToArray();
        }

        // ---- Deserialize ----
        public static SecurePacket Deserialize(byte[] data)
        {
            using var ms = new System.IO.MemoryStream(data);
            using var br = new System.IO.BinaryReader(ms);

            var pkt = new SecurePacket
            {
                Magic          = br.ReadInt32(),
                Version        = br.ReadByte(),
                Type           = (PacketType)br.ReadByte(),
                Flags          = br.ReadUInt16(),
                SequenceNumber = br.ReadUInt32(),
                TimestampUtc   = br.ReadInt64()
            };

            int nonceLen = br.ReadInt32();
            pkt.AesNonce = br.ReadBytes(nonceLen);

            int payloadLen = br.ReadInt32();
            pkt.EncryptedPayload = br.ReadBytes(payloadLen);

            pkt.Hmac = br.ReadBytes(HMAC_SIZE);
            return pkt;
        }
    }

    public enum PacketType : byte
    {
        Handshake = 0x01,
        Data      = 0x02,
        Ack       = 0x03,
        Error     = 0xFF
    }

    /// <summary>
    /// Validation result พร้อม error detail
    /// </summary>
    public record ValidationResult(bool IsValid, string Reason = "")
    {
        public static ValidationResult Ok() => new(true);
        public static ValidationResult Fail(string reason) => new(false, reason);
    }
}
