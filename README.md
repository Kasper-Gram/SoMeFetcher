# SoMeFetcher

An Android app that aggregates RSS/Atom feeds and device app notifications into a single, time-boxed daily digest — so you spend less time checking multiple apps and more time actually reading.

---

## Features

| Feature | Details |
|---------|---------|
| **RSS / Atom support** | Subscribe to any RSS 2.0 or Atom 1.0 feed |
| **Notification capture** | Pull Android app notifications into the same digest |
| **App allowlist** | Choose exactly which apps may contribute notifications |
| **Scheduled digest** | WorkManager background job fetches content at your chosen time |
| **Feed validation** | URLs are verified against a live feed before being saved |
| **Auto-pruning** | Old items are removed on a configurable retention schedule (default: 30 days) |
| **Material You UI** | Material 3 components, edge-to-edge layout, DayNight theme |
| **Boot-resilient** | Digest schedule survives device reboots via `BootReceiver` |

---

## Architecture

```
app/src/main/java/com/github/kasper_gram/somefetcher/
├── data/          Room entities (FeedItem, FeedSource), DAOs, AppDatabase
├── feed/          FeedParser — OkHttp + Rome RSS/Atom parsing
├── receiver/      BootReceiver — reschedules WorkManager on boot
├── repository/    DigestRepository — single source of truth for UI and workers
├── service/       SoMeNotificationService — NotificationListenerService
├── ui/
│   ├── digest/    DigestFragment + DigestViewModel + DigestAdapter
│   └── settings/  SettingsFragment, AllowedAppsFragment, and their ViewModels
└── worker/        DigestWorker + DigestScheduler (WorkManager)
```

**Stack:** Kotlin · MVVM · Room · LiveData / StateFlow · WorkManager · OkHttp · Rome · Material 3

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

## Contributing

1. Fork the repository and create a feature branch.
2. Follow the existing MVVM structure — new UI logic goes in a ViewModel, data access goes through `DigestRepository`.
3. Run `./gradlew lint test` before opening a pull request.
4. Open a PR against `main` with a clear description of what and why.

See [TODO.md](TODO.md) for a prioritised backlog of planned work.

---

## License

This project does not currently specify a license. Please contact the repository owner before using this code in other projects.
