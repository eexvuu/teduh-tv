# AGENTS.md

Panduan untuk agen AI (terutama Claude Code) di repo ini. Pengguna membaca [README.md](README.md) dan
[CONTRIBUTING.md](CONTRIBUTING.md); file ini berisi aturan yang sama dalam bentuk yang dibutuhkan agen.

**Bahasa:** bicara dengan pengguna dalam bahasa Indonesia yang sederhana. Pertanyaan harus bisa dijawab
dengan ya/tidak atau dengan membaca layar TV. Jangan minta pengguna mengetik perintah.

## Pertama: tugas apa yang diminta?

| Diminta untuk | Lakukan |
|---|---|
| Memasang Teduh TV di TV ("pasang Teduh TV di TV saya") | Ikuti [Memasang Teduh TV](#memasang-teduh-tv) di bawah, di TV sungguhan. Pakai skrip di `tools/`, launcher di `launcher/` dan add-on di `addons/prayer-times/`; jangan menulis skrip baru. Jangan mengubah docs, tes atau kode repo kecuali diminta. |
| Mengubah repo ini: daftar TV, perbaikan, tema, docs | Lewati bagian berikut dan baca mulai [Repo map](#repo-map). |

## Memasang Teduh TV

1. **Cek modelnya dulu.** Tanyakan merek, model dan isi layar Pengaturan → Tentang. Samsung (Tizen),
   LG (webOS), VIDAA, Roku: berhenti dan jelaskan bahwa Teduh tidak bisa jalan di sana; sarankan Google TV
   stick di port HDMI. Fire TV dan Android 5–7: hanya debloat.
2. **Debloat dan launcher:** ikuti [PROMPT.md](PROMPT.md) persis, langkah demi langkah. Semua aturan
   keselamatannya berlaku (ringkasannya di bawah). Cari daftar yang sudah teruji di `examples/README.md`.
   Dock dan grid diatur lewat tema atau `launcher/app/src/main/res/values/config.xml`.
3. **Waktu sholat** (setelah launcher jadi Home):
   - Tanyakan kota/kabupaten pengguna. Cari koordinatnya (lintang dan bujur, desimal) lalu konfirmasikan
     nama kotanya ke pengguna.
   - Isi `latitude`, `longitude` dan `place` di `addons/prayer-times/app/src/main/res/values/config.xml`.
     Biarkan `method` 20 (Kemenag RI) dan `school` 0 (Syafi'i). Tanyakan apakah mau pengingat saat masuk
     waktu sholat (`reminders` true atau false).
   - Build dan pasang add-on itu dengan langkah di `addons/prayer-times/README.md` (Windows: `gradlew.bat`).
     Kalau pengingat aktif, jalankan juga perintah izin overlay dari README tersebut.
   - Cek zona waktu TV dengan `getprop persist.sys.timezone`: harus sesuai kota (Asia/Jakarta untuk WIB,
     Asia/Makassar untuk WITA, Asia/Jayapura untuk WIT). Kalau tidak sesuai, minta pengguna mengubahnya di
     Pengaturan TV; jangan diubah lewat ADB.
   - **Cek:** pengguna menekan Home, launcher menanyakan kartu "Prayer Times" sekali, pengguna memilih
     **Show**, lalu kartu dibuka dan jadwal hari ini tampil dengan nama kota yang benar.
4. **Foto sendiri sebagai latar:**
   - Tanyakan apakah pengguna mau memasang foto sendiri, dan minta mereka menaruh fotonya di satu folder
     di komputer. Jangan mengambil foto dari internet tanpa izin.
   - Salin `themes/glass/` ke `tema-saya/` (folder ini di-gitignore, jadi foto tidak masuk repo). Hapus
     `wallpaper.jpg` bawaannya, lalu taruh foto pengguna di `tema-saya/wallpapers/`: JPEG, PNG atau WebP,
     maksimal 100 foto, masing-masing maksimal 10 MB, nama file hanya huruf, angka, `_`, `-` dan titik.
     Ganti nama file yang tidak memenuhi aturan itu.
   - Di `config.json` tema itu, isi `"wallpaper": true`, `"wallpaper_rotation_minutes": 30` dan
     `"wallpaper_dim": 40`. Tanyakan berapa menit sekali foto berganti dan urutan aplikasi di dock.
     Semua kuncinya ada di `themes/README.md`.
   - Kirim temanya dengan `tools/theme.sh tema-saya` (Windows: `tools/theme.ps1 -TvHost <ip> tema-saya`).
   - **Cek:** Home mulai ulang dalam beberapa detik dan foto pertama tampil di belakang dock.
5. **Serah terima:** minta pengguna mencoba remote asli (Home, Input HDMI, memutar video, tombol mic).
   Beri tahu cara mengembalikan semuanya (`tools/restore.sh` atau `tools/restore.ps1`) dan tutup ADB
   seperti yang dijelaskan di PROMPT.md.

### Aturan keselamatan yang tidak bisa ditawar

[PROMPT.md](PROMPT.md) adalah prosedurnya. Ringkasan aturan keselamatannya:

- **Nonaktifkan, jangan pernah uninstall.** `pm disable-user --user 0`, satu paket per pemanggilan shell.
  Tanpa `pm uninstall`, tanpa root, tanpa flash. Semua langkah harus bisa dibatalkan oleh `tools/restore.sh`.
- **Backup dulu.** Daftar paket, daftar yang dinonaktifkan, `getprop` dan settings ke `backup/` sebelum
  perubahan pertama. `tools/disable.sh` juga menulis catatannya sendiri di `~/.cleanyourtv/`.
- **Jangan sentuh yang dibutuhkan TV:** aplikasi TV dan layanan input di balik HDMI dan tuner, menu
  input/sumber, pairing remote dan Bluetooth, keyboard layar, Play Store dan Play Services. Kalau nama paket
  tidak jelas, baca `dumpsys package <name>`, lalu tanya.
- **Jangan pernah menonaktifkan paket inti Android:** permission controller, package installer,
  `com.android.providers.*`, `com.android.systemui`, `com.android.shell`, `com.android.settings`,
  `com.android.tv.settings`, `android`, dan yang disebut di PROMPT.md. `tools/disable.sh`, `disable.ps1`
  dan `check.py --fix` menolak daftar seperti itu.
- **Jangan pernah menjalankan paket pabrik atau tes** (`com.cvte.tv.hkcfac`, `mediatek.factorymenu.ui` dan
  sejenisnya), bahkan hanya untuk melihat.
- **Pertahankan Home guard.** Launcher bawaan baru dinonaktifkan setelah launcher baru terpasang dan sudah
  menjadi Home. `tools/disable.sh` dan `launcher/build-and-install.sh` sudah menjaganya. `FORCE_HOME=1`
  atau `FORCE_SETUPWRAITH=1` hanya dipakai dalam situasi yang disebut pesan skripnya, dengan persetujuan
  pengguna.
  - On a CVTE board (any `com.cvte.*` package), leave its stock launcher (`com.cvte.tv.launcher.*`) enabled
    unless the `examples/` folder for that exact board disables it. Make the new launcher Home with
    `cmd package set-home-activity`, reboot once, and confirm Home and ADB come back before anything else.
    Don't enable a remote-key helper in the same pass (#741).
- **Tanya dulu soal apa pun yang dipakai keluarga:** aplikasi streaming, asisten suara, Chromecast,
  Miracast, pembaruan firmware. Jadikan satu pertanyaan checklist.
- **Nonaktifkan dalam batch kecil** dan ambil screenshot di antaranya
  (`tools/tv.py --screenshot shots/01.png`). Live TV, HDMI dan Netflix memang menghasilkan layar hitam.
- **Berhenti dan jelaskan** kalau TV menolak: `pm disable-user` ditolak untuk semua paket, instalasi gagal
  verifikasi, atau SDK di bawah `minSdk` 26 milik launcher. Jangan mengulang terus-menerus.
- **Close ADB afterwards.** Wireless debugging and Network/ADB debugging off, and USB debugging unless
  the person asks to keep it; authorisations revoked. Wireless debugging has its own port
  (`adb mdns services` lists it as `_adb-tls-connect._tcp`): check it with
  `tools/adb-closed.py <tv ip> <port>` straight after switching it off, before the reboot, as it takes
  a new port each start. Then reboot: `tools/adb-closed.py <tv ip>` must report 5555 closed, with
  Wireless debugging still off. If 5555 still answers with USB debugging kept on, switch that off too.
  Only the tool's "closed" counts. Port 5555 is plaintext. (Dua butir berbahasa Inggris ini sengaja
  dibiarkan sama persis dengan aslinya; tes memeriksanya.)
- **Unduhan butuh persetujuan.** Sebutkan nama file, sumber dan ukurannya sebelum mengunduh JDK, Android
  SDK atau tool lain.

Backup (`backup/`), screenshot (`shots/`), catatan (`work/`), kunci ADB (`tools/adbkey/`) dan foto
pengguna (`tema-saya/`) ada di `.gitignore` dan tidak boleh di-commit.

## Repo map

| Path | Isinya |
|---|---|
| `README.md` | Halaman depan dalam bahasa Indonesia. |
| `PROMPT.md` | Prosedur debloat dan launcher dari CleanYourTV (bahasa Inggris), termasuk aturan keselamatannya. |
| `docs/GUIDE.md` | README lengkap CleanYourTV (bahasa Inggris): tools, launcher, restore, FAQ. Beberapa tes membacanya. |
| `CONTRIBUTING.md` | Panduan kontributor. |
| `examples/` | Satu folder per model TV yang sudah teruji: `debloat.txt`, `tv.json`, kadang `README.md` dan `config.json`. Indeksnya di `examples/README.md`, dibuat oleh `tools/gen_tables.py`. |
| `docs/` | `docs/TROUBLESHOOTING.md`, kontrak kartu modul (`docs/modules.md`), `docs/devices/` dan [workflows](#workflows). |
| `launcher/` | Launcher, paket `com.gathin.tvhome`: Java biasa, `minSdk` 26, tanpa izin `INTERNET`. |
| `addons/prayer-times/` | Add-on waktu sholat (kartu modul), proyek Gradle tersendiri. |
| `themes/` | `default/` dan `glass/`. Semua kunci tema ada di `themes/README.md`. |
| `tools/` | Klien ADB (`tv.py`) dan skrip debloat, restore dan tema, masing-masing dengan kembaran PowerShell. |
| `tests/` | Suite `unittest` Python: format daftar, tema, add-on, Home guard, tools terhadap TV palsu. |
| `.github/workflows/` | `ci.yml` (tes, build launcher, batas ukuran APK) dan `addons.yml`. |

## Workflows

Langkah-langkah di `docs/workflows/` (bahasa Inggris, dari CleanYourTV):

- [`tv-list.md`](docs/workflows/tv-list.md): menambah daftar debloat TV yang sudah teruji beserta `tv.json`.
- [`launcher-switch.md`](docs/workflows/launcher-switch.md): menambah pengaturan launcher yang default-nya mati.
- [`translation.md`](docs/workflows/translation.md): menambah atau melengkapi bahasa launcher.
- [`theme.md`](docs/workflows/theme.md): menambah tema dari kunci yang sudah ada.
- [`addon.md`](docs/workflows/addon.md): menambah aplikasi kartu modul tanpa mengubah `launcher/`.
- [`docs-notes.md`](docs/workflows/docs-notes.md): menambah catatan troubleshooting.

## Sebelum commit

| Cek | Perintah atau aturan |
|---|---|
| Tes, seperti di CI | `tools/venv/bin/python -m unittest discover -s tests -v` setelah `python3 -m venv tools/venv` dan `tools/venv/bin/pip install -r tools/requirements.txt`. Di `main`, CI memasang `CLEANYOURTV_STRICT=1`. Di Windows, tes yang menjalankan `tools/*.sh` dilewati karena butuh shell POSIX; CI menjalankannya. |
| Launcher, kalau `launcher/` diubah | JDK 17, Android SDK platform 34, build-tools 34.0.0. Di `launcher/`: `./gradlew --no-daemon testReleaseUnitTest lintRelease assembleRelease`. Lint harus nol error. |
| Batas ukuran APK | APK rilis launcher harus di bawah 589,824 B (576 KB). Inilah janji "ringan" Teduh. |
| Add-on, kalau `addons/` diubah | Di `addons/prayer-times/`: `./gradlew assembleRelease`. |

## Aturan tetap

- **Tidak ada APK atau binary jadi di repo.** Pengguna mem-build dari sumber.
- **Tidak ada jaringan di launcher:** tanpa izin `INTERNET`, tanpa analitik, tanpa layanan latar. Fitur yang
  butuh internet menjadi add-on, seperti add-on sholat.
- **Tidak ada dependensi launcher baru** dan tidak ada perubahan `minSdk` (26).
- **Aturan keselamatan** di `PROMPT.md`, Home guard di `tools/disable.sh`, `tools/check.py` dan
  `launcher/build-and-install.sh`, serta daftar paket yang dibutuhkan TV hanya diubah dengan sengaja,
  bersama perbaikan yang sama dari CleanYourTV.
- **Tidak ada rahasia atau identitas** di commit atau screenshot: tanpa alamat IP, nama Wi-Fi, email, nomor
  seri atau kunci ADB. Pakai `<tv ip>` atau `192.168.1.50` di docs.
- **Commit hanya atas nama penulis manusia.** Jangan menambahkan baris atribusi AI.
- **Kredit tetap ada:** `LICENSE`, `launcher/FONT-LICENSE-OFL.txt` dan bagian kredit di README untuk
  CleanYourTV (Gathin, MIT) dan pembuat add-on sholat.

## Mengambil pembaruan dari CleanYourTV

Teduh dibangun dari CleanYourTV commit `d7b0d4a`. Untuk mengambil perbaikan baru, bandingkan
`launcher/`, `tools/`, `PROMPT.md`, `docs/TROUBLESHOOTING.md` dan `examples/` dengan repo upstream, lalu
bawa perubahan yang relevan. Folder yang sengaja dibuang (terjemahan README, SHOWCASE, add-on dan tema
lain, tool performa) jangan dikembalikan.
