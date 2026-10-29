# pubg fix.md — Permanent Fix at Prevention Guide para sa PUBG Mobile Infinite Loading

> Para kay LO — gabay at automated rescue para hinding-hindi na maipit sa loading helmet ang PUBG Mobile.

---

## 1. Ano ang Tunay na Dahilan ng Infinite "Loading... Loading..." Screen?

Noong ininspeksyon natin ang actual frame buffer at storage ng device mo, natukoy ang **dalawang kritikal na dahilan**:

### A. Corrupted `loginInfoFile.json` (Authentication Stalling)
- **Ang nangyari:** Ang file na `files/UE4Game/ShadowTrackerExtra/ShadowTrackerExtra/Saved/SaveGames/loginInfoFile.json` ay napatungan ng CVar patch keys (`+CVars=...`) sa halip na authentic OAuth login tokens ng Tencent.
- **Bakit na-stuck:** Pagkatapos ng "Checking for updates...", sinusubukang basahin ng Tencent login engine ang `loginInfoFile.json` para i-restore ang saved account mo. Dahil invalid JSON / CVar text ang laman, nag-hang ang authentication thread sa background habang nagpapakita ng umiikot na helmet ("Loading...").
- **Solusyon:** Burahin ang corrupted `loginInfoFile.json`. Sa oras na mawala ito, lalabas agad ang malinis na Login Screen (Twitter/FB/Play/Guest) at dideretso sa lobby!

### B. Maling CVar Injections sa Unreal Engine INI Files
- Ang paggawa ng mga file tulad ng `Engine.ini`, `DeviceProfile.ini`, `Scalability.ini`, o `Updater.ini` na may `[SystemSettings]` o `+CVars=` at mga setting tulad ng `r.Streaming.PoolSize=4096` ay nagdudulot ng syntax error sa UE4 parser.
- Sinusubukan ng laro na mag-allocate ng 4GB texture pool sa mobile RAM na nagdudulot ng renderer thread deadlock.

---

## 2. One-Command Instant Fix (Kapag naulit)

Kung sakaling mag-hang o mag-loading ulit ang PUBG Mobile, patakbuhin lang ito via ADB o Termux:

```bash
adb shell "
  BASE='/storage/emulated/0/Android/data/com.tencent.ig/files/UE4Game/ShadowTrackerExtra'
  
  # 1. Force stop game
  am force-stop com.tencent.ig
  
  # 2. Tanggalin ang sirang loginInfoFile.json
  rm -f \$BASE/ShadowTrackerExtra/Saved/SaveGames/loginInfoFile.json
  rm -f \$BASE/Saved/SaveGames/loginInfoFile.json
  
  # 3. Tanggalin ang mga extra/invalid INI files (iwanan lang ang UserCustom.ini)
  CFG='\$BASE/ShadowTrackerExtra/Saved/Config/Android'
  for f in \$CFG/*.ini; do
    case \"\$f\" in
      *UserCustom.ini|*GameUserSettings.ini) ;;
      *) rm -f \"\$f\" ;;
    esac
  done
  
  # 4. Tanggalin ang mga stale tmp/lock at .nomedia files
  rm -f \$BASE/ShadowTrackerExtra/Saved/*.tmp* \$BASE/ShadowTrackerExtra/Saved/*lock*
  rm -f \$BASE/ShadowTrackerExtra/Saved/Pandora/.nomedia
  
  # 5. I-unlock ang permissions
  chattr -R -i '/storage/emulated/0/Android/data/com.tencent.ig/files' 2>/dev/null
  chmod -R 777 '/storage/emulated/0/Android/data/com.tencent.ig/files' 2>/dev/null
  
  # 6. Ilunsad muli ang laro
  monkey -p com.tencent.ig -c android.intent.category.LAUNCHER 1
"
```

---

## 3. Mga Files na LIGTAS I-MOD vs HINDI DAPAT GALAWIN

| Directory / File | Pwede bang Galawin? | Paliwanag |
| :--- | :---: | :--- |
| `Saved/Config/Android/UserCustom.ini` |  **PWEDE** | Dito nilalagay ang 120/165/185 FPS CVars at recoil/combat mods. |
| `Saved/SaveGames/Active.sav` |  **PWEDE (GVAS Only)** | Binary file para sa FPS unlock. Huwag i-edit sa text editor. |
| `Saved/Config/Android/GameUserSettings.ini` |  **INGATAN** | Graphic quality lamang. Huwag lagyan ng `+CVars=`. |
| `Saved/SaveGames/loginInfoFile.json` |  **BAWAL** | Authentication tokens. Kapag nagalaw, mag-i-infinite loading. |
| `Saved/Pandora/` |  **BAWAL** | WebView login and in-game web events. Huwag lagyan ng `.nomedia`. |
| `Saved/Config/Android/Updater.ini` |  **BAWAL** | Updater metadata. Huwag patungan ng CVars. |
| `Saved/Config/Android/Engine.ini` |  **BAWAL** | Nagdudulot ng syntax crash sa UE4 mobile parser. |

---

## 4. Permanent Code Safeguards sa Launcher

Sa source code ng Game Launcher (`PubgConfigPatcher.java` at `clean_pubgm.sh`):
1. **Protected File Whitelist:** Ang `loginInfoFile.json`, `guest_uuid.dat`, at `LoginToken.dat` ay permanenteng naka-exempt at hindi na kailanman papatungan ng CVars.
2. **Auto-Cleanup sa App Startup:** Awtomatikong binubura ng launcher ang anumang invalid `Engine.ini` at corrupt locks bago i-boot ang laro.
3. **Smart Active.sav Validator:** Sinusuri kung buo ang `GVAS` header bago i-apply ang FPS unlock.

## 5. Unlock 165 FPS / 120 FPS via Latest PAK Method

Sa latest update ng PUBG Mobile (v4.6.1), ang game patch sequence ay umabot na sa `21605` (`game_patch_4.6.1.21605.pak`).
- Kung ang unlock PAK ay pinangalanang lumang patch number (tulad ng 21525 o 21572), **hindi ito gagana** dahil binabale-wala ito ng UE4 engine pabor sa 21605.
- Upang gumana ang 165/120 FPS unlock PAK:
  1. Dapat itong ipangalan na **mas mataas sa latest patch**, halimbawa:
     `game_patch_4.6.1.21606.pak`
  2. Ilagay ito sa:
     `/storage/emulated/0/Android/data/com.tencent.ig/files/UE4Game/ShadowTrackerExtra/ShadowTrackerExtra/Saved/Paks/`
  3. Awtomatikong imo-mount ito ng Unreal Engine bilang pinakamataas na priority patch, na nag-a-unlock ng 120 FPS / 144 FPS / 165 FPS nang hindi nagdudulot ng loading loop o black screen!

