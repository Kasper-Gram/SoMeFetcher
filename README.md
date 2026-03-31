# SoMeFetcher

[![CI](https://github.com/Kasper-Gram/SoMeFetcher/actions/workflows/build.yml/badge.svg)](https://github.com/Kasper-Gram/SoMeFetcher/actions/workflows/build.yml)

An Android app that aggregates RSS/Atom feeds and device app notifications into a single, time-boxed daily digest — so you spend less time checking multiple apps and more time actually reading.

---

## Features

| Feature | Details |
|---------|---------|
| **RSS / Atom support** | Subscribe to any RSS 2.0 or Atom 1.0 feed |
| **Notification capture** | Pull Android app notifications into the same digest |
| **App allowlist** | Choose exactly which apps may contribute notifications |
| **Multiple digest times** | Schedule up to 3 daily delivery times (morning, noon, evening) |
| **Feed validation** | URLs are verified against a live feed before being saved |
| **Auto-pruning** | Old items are removed on a configurable retention schedule (default: 30 days) |
| **OPML import / export** | Export your subscriptions as a standard OPML 2.0 file; import feeds from any OPML file |
| **Last-synced indicator** | Each feed source shows a relative "Last synced" time and a warning badge when stale |
| **Material You UI** | Material 3 components, edge-to-edge layout, DayNight theme |
| **Boot-resilient** | Digest schedule survives device reboots via `BootReceiver` |
| **Resilient background worker** | `DigestWorker` retries on transient network failures with exponential back-off |

---

## Architecture

```
app/src/main/java/com/github/kasper_gram/somefetcher/
├── data/          Room entities (FeedItem, FeedSource), DAOs, AppDatabase
├── feed/          FeedParser — OkHttp + Rome RSS/Atom parsing; OPMLManager — OPML 2.0 import/export
├── receiver/      BootReceiver — reschedules WorkManager on boot
├── repository/    DigestRepository — single source of truth for UI and workers
├── service/       SoMeNotificationService — NotificationListenerService
├── ui/
│   ├── digest/    DigestFragment + DigestViewModel + DigestAdapter
│   └── settings/  SettingsFragment, AllowedAppsFragment, and their ViewModels
└── worker/        DigestWorker + DigestScheduler (WorkManager)
```

**Stack:** Kotlin · MVVM · Room · LiveData / StateFlow · WorkManager · OkHttp · Rome · Paging 3 · Material 3

---

## Getting Started

### Prerequisites

| Tool | Version |
|------|---------|
| Android Studio | Ladybug (2024.2) or later |
| JDK | 11 |
| Android SDK | API 35 (compile), API 26 minimum |

### Build

```bash
# Debug build
./gradlew assembleDebug

# Run unit tests
./gradlew test

# Run lint
./gradlew lint
```

### Runtime permissions

| Permission | Purpose |
|------------|---------|
| `BIND_NOTIFICATION_LISTENER_SERVICE` | Capturing app notifications |
| `POST_NOTIFICATIONS` (API 33+) | Posting the daily digest notification |
| `RECEIVE_BOOT_COMPLETED` | Re-scheduling the digest job after reboot |
| Internet | Fetching RSS / Atom feeds |

Grant **Notification Access** via *Settings → Digest Settings → Grant notification access*.

---

## OPML Import / Export

SoMeFetcher supports the [OPML 2.0](http://opml.org/spec2.opml) standard for migrating feed subscriptions to and from other RSS readers.

### Export

1. Open the **Settings** screen.
2. Scroll to the **OPML Subscriptions** card and tap **Export OPML**.
3. The system share sheet appears — save to Files, send by email, or open in another reader.

The exported file is a standard OPML 2.0 document compatible with Feedly, Inoreader, NetNewsWire, and other readers.

### Import

1. Open the **Settings** screen.
2. Scroll to the **OPML Subscriptions** card and tap **Import OPML**.
3. Pick an `.opml` or `.xml` file using the system file picker.
4. SoMeFetcher parses every `<outline xmlUrl="…">` element and inserts any feed that is not already saved, skipping duplicates.
5. A Snackbar confirms how many sources were added and how many were skipped.

---

## Contributing

1. Fork the repository and create a feature branch.
2. Follow the existing MVVM structure — new UI logic goes in a ViewModel, data access goes through `DigestRepository`.
3. **Database schema changes:** bump the `version` in `@Database`, add an explicit `Migration` object in `AppDatabase.kt`, and commit the generated schema JSON under `app/schemas/`. Never use `fallbackToDestructiveMigration()`.
4. Run `./gradlew lint test` before opening a pull request. The CI pipeline enforces this on every PR.
5. Open a PR against `main` with a clear description of what and why.

See [TODO.md](TODO.md) for a prioritised backlog of planned work.

---

## License

This project does not currently specify a license. Please contact the repository owner before using this code in other projects.
