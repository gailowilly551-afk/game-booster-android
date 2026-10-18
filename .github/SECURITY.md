# Security Policy

## Supported Versions

We actively provide security patches, stability hotfixes, and engine maintenance for the following versions of **Game Launcher / Game Booster Pro**:

| Version | Supported          | Status |
| ------- | ------------------ | ------ |
| 20.4.x  | :white_check_mark: | Current Release (Active Support) |
| 16.x.x  | :white_check_mark: | Critical Security Fixes Only |
| < 16.0  | :x:                | End of Life (Unsupported) |

---

## Reporting a Vulnerability

The Game Launcher development team takes software security, device safety, and user privacy seriously. If you discover a vulnerability or security flaw, please report it responsibly:

### 🔒 Preferred Method: GitHub Private Vulnerability Reporting
1. Navigate to [GitHub Security Advisories](https://github.com/willygailo/Game-Launcher/security/advisories/new).
2. Click **"Report a vulnerability"**.
3. Fill out the report form with:
   - Detailed description of the vulnerability.
   - Steps to reproduce or proof-of-concept (PoC).
   - Affected components (e.g., Shizuku UserService, JNI C++ bindings, config patchers).
   - Potential impact on user data or device safety.

### 📧 Alternate Contact
If you cannot use GitHub Advisories, you may reach out directly via:
- **Developer Profile**: [Willy Gailo Jr.](https://www.facebook.com/https.willy.jr.carnasa.gailo2026.2027)
- **GitHub Repository**: [willygailo/Game-Launcher](https://github.com/willygailo/Game-Launcher)

---

## Response Timeline & Disclosure Policy

When a vulnerability report is submitted:
- **Initial Acknowledgment**: Within **24 to 48 hours**.
- **Assessment & Triage**: Confirmation of severity and affected code within **3 business days**.
- **Patch Resolution**: High-priority patch authored, tested, and released within **7 days**.
- **Coordinated Disclosure**: Public advisory published only after an update is available to protect users.

---

## Security Architecture & Design Principles

Game Launcher is built with strict privacy and security principles:

1. **Zero Remote Telemetry & Local Data Privacy**:
   - Game Launcher runs completely client-side.
   - No user credentials, OAuth tokens, session cookies, or hardware identifiers are ever collected, logged, or sent to external servers.

2. **Game Credential & Session Protection**:
   - Sensitive game files (`Active.sav`, `LoginToken.dat`, `guest_uuid.dat`, `login_state.json`, and account settings) are strictly preserved and never overwritten or wiped.

3. **Shizuku & AIDL Privilege Containment**:
   - Privileged operations operate exclusively via standard Android IPC / Shizuku AIDL contracts.
   - Root and ADB shell commands are sanitized to prevent command injection and unauthorized directory access.

4. **Non-Destructive Safe Patching**:
   - Automatic configuration backup safety net (`ConfigBackupManager`) enables instant one-click rollback if any configuration is modified.

---

Thank you for helping keep Game Launcher and its community safe!
