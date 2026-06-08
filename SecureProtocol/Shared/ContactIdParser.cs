using System;
using System.Text;

namespace SecureProtocol
{
    /// <summary>
    /// Contact ID (CID) decoder for DSC PowerSeries Neo / TL-series communicator.
    ///
    /// Frame format — 16 ASCII hex chars (no spaces):
    ///
    ///   Position  Field   Size  Description
    ///   ────────  ──────  ────  ────────────────────────────────────────────
    ///   0–3       ACCT    4     Account number (hex, A → 0)
    ///   4–5       MT      2     Message type — always "18" (standard CID)
    ///   6         Q       1     Qualifier  1=alarm 3=restore 6=status
    ///   7–9       EEE     3     Event code (hex, A → 0)
    ///   10–11     GG      2     Group / partition (00 = system)
    ///   12–14     ZZZ     3     Zone / user (000 = system)
    ///   15        S       1     Checksum (A → 0 before mod-15 check)
    ///
    /// Note: DTMF heritage uses 'A' to represent the digit 0.
    ///       All numeric fields must decode 'A' → '0' before parsing.
    /// </summary>
    public static class ContactIdParser
    {
        /// <summary>
        /// Try to parse a Contact ID message from raw bytes.
        /// Returns null if the bytes don't look like valid CID.
        /// </summary>
        public static ContactIdMessage? TryParse(byte[] raw)
        {
            if (raw.Length < 16) return null;

            // Convert to ASCII, take first 16 chars
            var s = Encoding.ASCII.GetString(raw, 0, 16).ToUpperInvariant();

            // Sanity checks
            if (s.Length < 16)                     return null;
            if (s[4..6] != "18")                   return null;  // message type
            char q = s[6];
            if (q != '1' && q != '3' && q != '6') return null;  // qualifier

            // Fields (raw — may contain 'A' as zero)
            string acctRaw = s[0..4];
            string evtRaw  = s[7..10];
            string grpRaw  = s[10..12];
            string zoneRaw = s[12..15];
            char   csRaw   = s[15];

            // Decode: 'A' → '0' for all numeric fields
            string acctDec = acctRaw.Replace('A', '0');
            string evtDec  = evtRaw .Replace('A', '0');
            string grpDec  = grpRaw .Replace('A', '0');
            string zoneDec = zoneRaw.Replace('A', '0');

            if (!int.TryParse(acctDec, out int account)) return null;
            if (!int.TryParse(evtDec,  out int evtCode)) return null;
            if (!int.TryParse(grpDec,  out int group))   return null;
            if (!int.TryParse(zoneDec, out int zone))    return null;

            // Qualifier text
            string qualifier = q switch
            {
                '1' => "New Event / Alarm",
                '3' => "Restore / Close",
                '6' => "Status Report",
                _   => "Unknown"
            };

            string eventName = LookupEvent(evtCode);

            // Verify checksum (optional — log result but don't reject)
            bool csOk = VerifyChecksum(s);

            return new ContactIdMessage(
                account, acctRaw,
                q, qualifier,
                evtCode, evtRaw, eventName,
                group, zone,
                csOk, s);
        }

        // ── Checksum ─────────────────────────────────────────────────────────────
        // Sum all 16 hex values (A=0, B=11, ..., F=15). Result must be divisible
        // by 15 (i.e., sum mod 15 == 0) after the checksum digit fills the gap.

        private static bool VerifyChecksum(string s)
        {
            int sum = 0;
            foreach (char c in s)
                sum += HexVal(c);
            return sum % 15 == 0;
        }

        private static int HexVal(char c) => c switch
        {
            'A' => 0,          // CID zero
            'B' => 11,
            'C' => 12,
            'D' => 13,
            'E' => 14,
            'F' => 15,
            >= '0' and <= '9' => c - '0',
            _ => 0
        };

        // ── Event code lookup ─────────────────────────────────────────────────────
        // Source: SIA DC-05-1999.09 Contact ID standard + DSC PowerSeries Neo manual.
        // Event code is 3 decimal digits (0-999); 'A'→0 substitution already done.

        public static string LookupEvent(int code) => code switch
        {
            // ── Medical ──────────────────────────────────────────────
            100 => "Medical Alarm",
            101 => "Personal Emergency",
            102 => "Fail to Report In",
            // ── Fire ─────────────────────────────────────────────────
            110 => "Fire Alarm",
            111 => "Smoke Detector",
            112 => "Combustion",
            113 => "Water Flow",
            114 => "Heat Alarm",
            115 => "Pull Station",
            116 => "Duct Alarm",
            117 => "Flame Alarm",
            118 => "Near Alarm (Fire)",
            // ── Panic ────────────────────────────────────────────────
            120 => "Panic Alarm",
            121 => "Duress",
            122 => "Silent Panic",
            123 => "Audible Panic",
            // ── Burglary ─────────────────────────────────────────────
            130 => "Burglary Alarm",
            131 => "Perimeter Alarm",
            132 => "Interior Alarm",
            133 => "24-Hour Alarm",
            134 => "Entry / Exit Alarm",
            135 => "Day / Night Zone Alarm",
            136 => "Outdoor Alarm",
            137 => "Tamper Alarm",
            138 => "Near Alarm (Burg)",
            139 => "Intrusion Verifier",
            // ── General ──────────────────────────────────────────────
            140 => "General Alarm",
            143 => "Module Failure",
            144 => "Sensor Tamper",
            145 => "Module Tamper",
            // ── 24-Hour Non-Burglary ─────────────────────────────────
            150 => "24-Hour Non-Burglary",
            151 => "Gas Detected",
            152 => "Refrigeration",
            153 => "Loss of Heat",
            154 => "Water Leakage",
            155 => "Foil Break",
            156 => "Day Trouble",
            158 => "High Temperature",
            159 => "Low Temperature",
            161 => "Loss of Air Flow",
            // ── Supervisory ──────────────────────────────────────────
            200 => "Fire Supervisory",
            201 => "Low Water Pressure",
            202 => "Low CO2",
            203 => "Gate Valve Sensor",
            204 => "Low Water Level",
            205 => "Pump Activated",
            206 => "Pump Failure",
            // ── Trouble ──────────────────────────────────────────────
            300 => "System Trouble",
            301 => "AC Power Failure",
            302 => "Low Battery",
            303 => "RAM Checksum Bad",
            304 => "ROM Checksum Bad",
            305 => "System Reset",
            306 => "Program Changed",
            307 => "Self-Test Failure",
            308 => "System Shutdown",
            309 => "Battery Test Fail",
            310 => "Ground Fault",
            311 => "Battery Missing",
            312 => "Power Supply Overload",
            313 => "Sounder Trouble",
            320 => "Sounder/Relay",
            321 => "Bell 1",
            333 => "Exp. Module DC Loss",
            334 => "Exp. Module Low Battery",
            341 => "Exp. Module Reset",
            344 => "RF Receiver Jam",
            351 => "Telephone Line Fault",
            353 => "Long Range Radio Xmitter Fault",
            354 => "Failure to Communicate",
            356 => "Loss of Radio Supervision",
            357 => "Loss of Central Polling",
            358 => "Long Range Radio VSWR",
            // ── Openings / Closings ───────────────────────────────────
            401 => "Armed Away (Disarmed/Opened)",
            403 => "Auto Armed",
            406 => "Cancel",
            407 => "Remote Arm / Disarm",
            408 => "Quick Arm",
            409 => "Keyswitch Armed",
            411 => "Callback Requested",
            412 => "Success — Download/Access",
            413 => "Unsuccessful Access",
            421 => "Access Denied",
            422 => "Access Report by User",
            423 => "Forced Access",
            424 => "Egress Denied",
            425 => "Egress Granted",
            441 => "Armed Stay",
            442 => "Keyswitch Disarmed",
            450 => "Exception Opening",
            451 => "Early Opening",
            452 => "Late Opening",
            453 => "Failed to Open",
            454 => "Late Close",
            455 => "Failed to Close",
            456 => "Auto-Close with Bypass",
            // ── Bypasses ─────────────────────────────────────────────
            570 => "Zone Bypass",
            574 => "Group Bypass",
            575 => "Swinger Bypass",
            576 => "Access Zone Shunt",
            // ── General System ───────────────────────────────────────
            601 => "Manual Test",
            602 => "Periodic Test",
            603 => "Periodic RF Test",
            604 => "Fire Test",
            605 => "Status Report to Follow",
            607 => "Walk Test Begin",
            608 => "Walk Test End",
            621 => "Event Log Reset",
            622 => "Event Log 50% Full",
            623 => "Event Log 90% Full",
            625 => "Time / Date Changed",
            626 => "Program Mode Entry",
            627 => "Program Mode Exit",
            628 => "Programming Changed",
            // ── Misc ─────────────────────────────────────────────────
            _ => $"Unknown event ({code})"
        };
    }

    /// <summary>Decoded Contact ID message.</summary>
    public sealed class ContactIdMessage
    {
        public int    Account   { get; }   // numeric account
        public string AccountRaw { get; }  // raw 4-char string from frame
        public char   Qualifier { get; }   // '1','3','6'
        public string QualText  { get; }
        public int    EventCode { get; }
        public string EventRaw  { get; }   // raw 3-char string
        public string EventName { get; }
        public int    Group     { get; }   // partition (0 = system)
        public int    Zone      { get; }   // zone/user (0 = system)
        public bool   CsValid   { get; }
        public string RawFrame  { get; }

        public ContactIdMessage(int account, string accountRaw,
                                char qualifier, string qualText,
                                int eventCode, string eventRaw, string eventName,
                                int group, int zone,
                                bool csValid, string rawFrame)
        {
            Account    = account;
            AccountRaw = accountRaw;
            Qualifier  = qualifier;
            QualText   = qualText;
            EventCode  = eventCode;
            EventRaw   = eventRaw;
            EventName  = eventName;
            Group      = group;
            Zone       = zone;
            CsValid    = csValid;
            RawFrame   = rawFrame;
        }

        public override string ToString()
            => $"Account={AccountRaw} | {QualText} | [{EventCode:000}] {EventName} | " +
               $"Partition={Group:00}  Zone={Zone:000}  CRC={( CsValid ? "✅" : "⚠️")}";
    }
}
