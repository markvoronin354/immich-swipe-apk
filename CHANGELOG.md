# Changelog

## 4.2.0 - 2026-10-21

A security and reliability release. Highlights: saved API keys are now encrypted, failed syncs no
longer get lost, and several data-loss bugs are fixed.

### Security and privacy
- **API keys are encrypted at rest.** Keys saved in the app are protected with a hardware-backed
  Android Keystore key. Existing keys are encrypted automatically on first launch. If the Keystore
  fails, encryption is retried once, and a key that has become unusable is replaced. Downgrading to an
  older version means logging in again, because older versions cannot read the encrypted key.
- **Warning before sending your key over plain HTTP to a public server.** You now have to confirm
  first. Private networks (home or office LAN addresses, local names, Tailscale) connect without a
  prompt.
- **The API key is no longer placed in download and share URLs.** It is only ever sent as a header.
- **The API key is no longer held in screen state.** Images, video, downloads, sharing and the
  avatars of saved accounts are now authenticated in one place, and the key is attached only to
  requests going to the matching server. It is never sent to any other host.
- **Explicit backup rules.** Nothing is backed up or transferred to another device. Stored keys are
  bound to the device anyway.

### Fixed
- **Importing a database could delete other accounts' data.** Import now only touches the accounts
  that appear in the file, and imported sync history no longer overwrites existing history.
- **Failed syncs were reported as done.** Deletes, archives, locks and favorites that fail now stay
  pending, and you get a warning. Previously the screen removed the photos while they still existed on
  the server.
- **Statistics are no longer inflated by failed syncs.** The sync history counts only what was really
  applied. Kept photos are counted once, after everything succeeded.
- **A failed update of the optional SKIP album is now reported** instead of being ignored.
- **A local cleanup problem after a successful server change no longer counts as a failed sync.**
- **Albums with more than 5000 photos were cut short** after every sync. The size caps are removed.
- **An interrupted sync could delete cached entries** of photos that had simply not been re-fetched
  yet. The cleanup now only runs after a complete sync.
- **Cancelled work was treated as an error** in about 40 places and kept running. Cancellation is now
  respected.
- **Retries are safer.** Only temporary failures are retried, and never requests that could be applied
  twice, such as creating an album or rotating a photo. A wrong API key now fails immediately instead
  of after several seconds of backoff.
- **Android 10:** deleting local copies after a server delete no longer crashes.
- **Android 8 and 9:** downloads now ask for the storage permission they need.
- **Downgrades and missing migrations no longer silently wipe your sorting progress.**
- **Logs:** the log file is written safely from any thread, and background work is logged too.
- **Cleanup reminder:** the background job reuses the app's existing objects instead of creating new
  connections on every run.
- **Best Shot:** the face model loads only when needed, is freed afterwards, and runs one detection
  at a time.
- **Home screen:** when a count cannot be loaded, the previous number is kept instead of showing 0.
- **The SKIP feature is fully disabled** behind a single switch. No SKIP album is read, created or
  cleaned up on your server.
- **Text:** hard-coded texts moved into translatable resources, and reminder and bulk-selection
  messages use correct singular and plural forms in English, French and Spanish.

### Changed
- **Image loading updated to Coil 3** (from 2.7.0).
- **Cache sizes adapt to your device.** The video cache and the cache for shared files are 256 MB,
  512 MB or 1 GB depending on the device's memory, and never more than a fifth of the free space.
- **Optional split APKs.** Building with `-PabiSplits=true` produces one small APK per CPU type
  (about 21 MB for arm64) next to the universal APK. Normal builds are unchanged.
- **Gradle updated to 9.8.2.**

### Under the hood
- Added a GitHub Actions workflow running unit tests, lint, static analysis and a build of the
  instrumented tests.
- Added detekt (2.0 alpha) with a baseline, so only new findings fail the build.
- Room schemas are exported, and a migration test setup is in place for future database changes.
- Repositories report failures as typed results instead of returning null, false or 0.
- Swipe settings are one typed object instead of an untyped merge of 22 flows.
- The login screen's logic is testable and covered by tests.
- The unit test suite grew from 4 to 71 tests.
- Simpler dependency injection, unused strings and resources removed, remaining French comments
  translated, and lint warnings cleaned up.

### Known limits
- Plain HTTP is still allowed, because many self-hosted servers are reached by LAN address without
  TLS. It is only flagged for public hosts.
- Video playback can still follow a server redirect to another host with the key attached. This was
  already the case before and is unchanged.