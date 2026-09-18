# Termkit

SSH client Android — terminal, SFTP file manager, dan host management dalam satu aplikasi ringan. Posisinya "Termius simple, tapi tetap niat": bukan prototype, bukan Termux, bukan emulator Linux lokal. Fokusnya cuma satu, koneksi remote yang cepat dan rapi.

## Fitur

- **Host Manager** — CRUD host lengkap (nama, host, port, username), auth password atau private key PEM + passphrase, pencarian, favorit, timestamp koneksi terakhir, dan indikator host aktif.
- **Terminal** — sesi shell interaktif dengan output monospace, history perintah (naik/turun), ukuran font bisa diatur, dan buffer output dibatasi supaya tidak OOM.
- **SFTP** — browse direktori remote, upload/download lewat SAF, buat folder, rename, hapus, dan refresh, lengkap dengan empty/loading/error state.
- **Settings** — tema Light/Dark/System, ukuran font terminal, keep-screen-on saat sesi aktif, disconnect semua sesi, dan hapus semua data host.
- Konfirmasi sebelum ganti host aktif atau menghapus apa pun, salin endpoint host, dan status koneksi (Offline/Connecting/Connected/Reconnecting/Error) yang konsisten di seluruh app.

## Stack

- Kotlin + Jetpack Compose (Material 3)
- Navigation Compose, ViewModel + StateFlow, Coroutines
- DataStore Preferences untuk penyimpanan host & settings
- SSHJ untuk SSH shell dan SFTP
- minSdk 26, targetSdk 34, compileSdk 34

## Build lokal

```
./gradlew assembleDebug
```

APK debug ada di `app/build/outputs/apk/debug/`.

## Ambil APK dari GitHub Actions

Setiap push, pull request, atau trigger manual (`workflow_dispatch`) akan menjalankan build di `.github/workflows/android.yml`. Buka tab **Actions** di repo, pilih run terbaru, lalu unduh artifact **termkit-debug-apk**.

Workflow menjalankan `gradle wrapper` sebelum build supaya `gradle-wrapper.jar` selalu fresh tanpa perlu commit file binary ke repo.

## Cara pakai

1. Buka tab **Hosts**, tap tombol tambah, isi nama, hostname, port, username, dan metode auth (password atau private key PEM).
2. Tap host di list untuk connect. Setelah berhasil, kamu otomatis diarahkan ke tab **Terminal**.
3. Kirim perintah dari input bawah, pakai tombol naik/turun untuk history, atau tab **SFTP** untuk kelola file di server yang sama.
4. Atur tema dan preferensi lain dari tab **Settings**.

## Catatan keamanan

- Password dan private key disimpan **lokal di perangkat**, di dalam DataStore Preferences, tanpa enkripsi tambahan di level aplikasi. Ini bukan brankas kredensial — jangan pakai di perangkat yang tidak kamu percaya.
- Verifikasi host key SSH pakai `PromiscuousVerifier` (menerima host key apa pun), diisolasi di `SshSessionManager`. Ini simplifikasi yang sengaja diambil supaya alur connect tetap mulus tanpa perlu UI trust-on-first-use; risikonya app rentan terhadap MITM kalau jaringan tidak dipercaya. Kalau butuh verifikasi ketat, ganti verifier ini dengan implementasi berbasis known_hosts.
- Tidak ada logging kredensial di mana pun dalam kode.

## Struktur proyek

```
app/src/main/java/com/termkit/app/
  data/model/   -> Host, ConnectionStatus, SftpEntry, AppSettings
  data/local/   -> DataStore untuk host & settings
  data/repo/    -> Repository layer
  data/ssh/     -> SshSessionManager (SSH + SFTP engine, SSHJ)
  ui/hosts/     -> Host list, form, card, ViewModel
  ui/terminal/  -> Layar terminal + ViewModel
  ui/sftp/      -> File manager + ViewModel
  ui/settings/  -> Preferensi aplikasi
  ui/components/-> Komponen shared (status chip, empty state, dialog)
  navigation/   -> NavHost + routes
```
