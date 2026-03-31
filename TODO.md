# SoMeFetcher — Prioritised TODO

This file tracks what has been built and what is planned next.  
Every planned item carries a priority tier and is labelled either **🍎 Low-hanging fruit** (quick win, ≤ 2 days) or **🏗️ Big feature** (significant effort, multiple days).  
Items within each priority tier are ordered by impact.

---

## ✅ Done

- **Core data layer** — Room database with `FeedItem` and `FeedSource` entities, DAOs, and repository
- **RSS / Atom feed parsing** — fetches and parses feeds via OkHttp + Rome library
- **Notification listener** — `NotificationListenerService` captures device notifications as digest items
- **Daily digest job** — WorkManager periodic task fetches all enabled feeds, prunes items older than 30 days, and posts a summary notification
- **Boot persistence** — `BootReceiver` reschedules the digest job after a device reboot
- **Digest screen** — scrollable list of unread items (RSS + notifications) with type icon, pull-to-refresh, and overflow menu
- **Tap to open** — tapping a feed item marks it as read and opens the article in the system browser
- **Settings screen** — add / toggle / delete RSS feed sources, configure daily digest delivery time, shortcut to grant notification access
- **Material Design UI** — custom vector icons, Material Components theme, Snackbars for errors
- **App filter for notification listening** — "Allowed apps" screen lists installed apps with a per-app toggle; `SoMeNotificationService` skips blocked packages stored in SharedPreferences
- **Feed URL validation on add** — `FeedParser.validateFeed()` is called before saving; inline error states (invalid URL, unreachable host, non-feed content) surface via `SettingsViewModel`
- **Multiple daily digest times** — up to 3 configurable delivery times per day; each slot gets its own uniquely-named `PeriodicWorkRequest`; legacy single-time pref is auto-migrated
- **Paging for the digest list** — `FeedItemDao` queries return `PagingSource<Int, FeedItem>`; `DigestRepository` exposes `Flow<PagingData<FeedItem>>` via Paging 3 `Pager`; `DigestViewModel` caches the flow with `cachedIn`; `DigestAdapter` extends `PagingDataAdapter`; `DigestFragment` collects the paging flow with `repeatOnLifecycle` and handles empty state via `loadStateFlow`
- **OPML import / export** — Export all `FeedSource` rows as a valid OPML 2.0 file shared via `Intent.ACTION_SEND`; import feeds from a picked OPML file via `ActivityResultContracts.OpenDocument`, with duplicate detection

---

## Priority 1 — Quality & Reliability

*Must-haves before calling this a professional, shippable product.*

---

### P1-1 — Unit tests 🍎 Low-hanging fruit

**Why:** Core business logic (`FeedParser`, `DigestRepository`, `DigestScheduler`) is completely untested. Regressions will go undetected.  
**What to build:**
- `FeedParserTest` — parse real RSS 2.0 and Atom 1.0 fixture XML files; verify `FeedItem` field mapping and error-path behaviour
- `DigestRepositoryTest` — test `refreshFeeds()` failure-count logic using a fake `FeedParser`
- `DigestSchedulerTest` — verify `calculateInitialDelay()` returns a value in `[0, 24 h)`
- `SettingsViewModelTest` — verify `addSource()` state transitions (Idle → Validating → Success / Invalid)

---

### P1-2 — Database indexes 🍎 Low-hanging fruit

**Why:** `FeedItemDao` queries filter by `isRead` and `sourceId` and sort by `publishedAt` on every observation. As the item count grows, these full-table scans become expensive.  
**What to build:**
- Add `@Index` annotations on `FeedItem.isRead`, `FeedItem.publishedAt`, and `FeedItem.sourceId`
- Pair with a Room schema migration (version bump) so existing installs are not wiped

---

### P1-3 — Database migration strategy 🍎 Low-hanging fruit

**Why:** `AppDatabase` currently uses `fallbackToDestructiveMigration()`. Any schema change silently wipes the user's saved items and sources.  
**What to build:**
- Replace `fallbackToDestructiveMigration()` with explicit `Migration` objects
- Add Room's `exportSchema = true` and commit the generated JSON schema files to source control
- Document the migration convention in a code comment or `CONTRIBUTING.md`

---

### P1-4 — Error retry logic in DigestWorker 🍎 Low-hanging fruit

**Why:** A single transient network error causes `DigestWorker` to return `Result.failure()`. WorkManager will not retry, so users miss their digest silently.  
**What to build:**
- Return `Result.retry()` for transient I/O errors; return `Result.failure()` only for permanent errors (e.g., invalid feed format)
- Respect WorkManager's exponential back-off policy, capped at 1 hour

---

### P1-5 — Accessibility audit 🍎 Low-hanging fruit

**Why:** Icon-only buttons and list items currently have no content descriptions. TalkBack users cannot operate the app.  
**What to build:**
- Add `android:contentDescription` to all icon-only `ImageView`s, `FloatingActionButton`s, and custom drawables
- Enforce minimum 48 dp touch targets on all interactive list-item controls
- Run the Accessibility Scanner report and resolve all critical findings

---

### P1-6 — Lint and code-style enforcement 🍎 Low-hanging fruit

**Why:** There is no automated lint gate. Style drift accumulates quickly as the project grows.  
**What to build:**
- Configure `lintOptions { abortOnError = true }` in `app/build.gradle.kts` with a `lint.xml` baseline for any pre-existing warnings
- Add ktlint (or the Kotlin formatter Gradle plugin) with a check task wired to the CI build

---

### P1-7 — CI/CD pipeline 🍎 Low-hanging fruit

**Why:** There is no automated build or test run on pull requests. Broken code can be merged undetected.  
**What to build:**
- Add a GitHub Actions workflow (`build.yml`) that runs `./gradlew lint test` on every push and PR
- Cache Gradle dependencies to keep build times short

---

### P1-8 — Crash reporting 🍎 Low-hanging fruit

**Why:** Crashes are invisible in production. Without telemetry there is no way to prioritise fixes for real users.  
**What to build:**
- Integrate a lightweight, privacy-friendly crash reporter (Firebase Crashlytics or Sentry)
- Show an opt-in consent dialog on first launch; only activate reporting after explicit consent
- Document the dependency and any privacy implications in the README

---

### P1-9 — ProGuard / R8 rules audit 🍎 Low-hanging fruit

**Why:** The release build enables `minifyEnabled = true` with a boilerplate `proguard-rules.pro`. Rome's JDOM-based parser and OkHttp's reflection APIs may be silently stripped.  
**What to build:**
- Add keep rules for Rome's XML parser internals and any OkHttp extension points used at runtime
- Smoke-test the release APK to verify feed parsing still works after shrinking

---

## Priority 2 — Core UX Improvements

*Features that make the app genuinely useful on a daily basis.*

---

### P2-1 — Last-synced status per feed source 🍎 Low-hanging fruit

**Why:** `FeedSource.lastFetched` is stored in the database but is never displayed. Users cannot tell whether a source is silently failing.  
**What to build:**
- Display a formatted "Last synced: …" subtitle in `item_feed_source.xml`
- Show "Never" when `lastFetched == 0L`
- Show a warning icon when `lastFetched` is more than 24 hours in the past

---

### P2-2 — Show read items (all-items view) 🍎 Low-hanging fruit

**Why:** Once an item is marked as read it disappears permanently. Users have no way to revisit articles.  
**What to build:**
- Add a toggle (chip or overflow menu item) in the Digest screen to switch between "Unread only" and "All items"
- `DigestViewModel` already exposes both `repository.unreadItems` and `repository.allItems`
- Display read items with reduced opacity in `DigestAdapter`

---

### P2-3 — Filter and sort the digest list 🍎 Low-hanging fruit

**Why:** When many sources and notification apps are active the digest list becomes difficult to navigate.  
**What to build:**
- Filter chips at the top of the Digest screen: **All · RSS · Notifications**
- Sort order selector: **Newest first / Oldest first / Source**
- State held in `DigestViewModel` only — no database migration needed

---

### P2-4 — Data retention preference 🍎 Low-hanging fruit

**Why:** The 30-day pruning cutoff is hard-coded in `DigestRepository.pruneOldItems()`. Different users have different storage needs.  
**What to build:**
- Add a "Keep items for" dropdown in Settings: 7 / 14 / 30 / 90 days
- Store the chosen value in SharedPreferences; read it in `pruneOldItems()`

---

### P2-5 — In-app article reader (Custom Tabs) 🍎 Low-hanging fruit

**Why:** Launching the raw system browser breaks the user's flow and loses navigation context.  
**What to build:**
- Add `androidx.browser:browser` dependency
- In `DigestFragment`, launch a `CustomTabsIntent` instead of a bare `Intent.ACTION_VIEW`
- Match the Custom Tab toolbar colour to the app's primary colour

---

### P2-6 — Share item 🍎 Low-hanging fruit

**Why:** Users may want to forward an article to a chat app, notes app, or read-later service.  
**What to build:**
- Long-press action on a digest item (or a share icon in an expanded view)
- Fire a standard `Intent.ACTION_SEND` with the item title and URL

---

### P2-7 — Feed health check 🍎 Low-hanging fruit

**Why:** Saved feed URLs can go stale (domain moves, feed discontinued). Currently there is no user-visible feedback about broken sources.  
**What to build:**
- In `DigestWorker`, track consecutive fetch failures per source (e.g., in SharedPreferences or a new column)
- After 3 consecutive failures, mark the source as unhealthy in `FeedSource`
- Surface a warning badge in the Settings feed list; provide a "Retry now" action

---

### P2-8 — Localization 🍎 Low-hanging fruit

**Why:** All UI strings are hard-coded in English. Extracting them to `strings.xml` is a prerequisite for any translation and good hygiene regardless.  
**What to build:**
- Audit layouts and Kotlin files for hard-coded strings; move everything to `strings.xml`
- Use positional format specifiers (`%1$s`, `%1$02d:%2$02d`) for all format strings so translations can reorder arguments
- Add at least one additional locale (Danish is a natural first choice)

---

### P2-9 — Multiple daily digest times ✅ Done

**What was built:**
- Up to 3 configurable delivery times (morning / noon / evening) via Settings
- Each active time slot gets its own uniquely-named `PeriodicWorkRequest` in WorkManager
- `DigestScheduler` exposes `scheduleAll()`, `cancelAll()`, `loadTimes()`, and `saveTimes()` helpers
- Delivery times are stored as a `Set<String>` in SharedPreferences; legacy `digest_hour` / `digest_minute` keys are auto-migrated on first run
- Settings UI: scrollable list of configured times (tap to edit, delete button); "Add delivery time" button disabled when limit is reached
- `BootReceiver` reschedules all active slots after a device reboot

---

## Priority 3 — Performance

*Optimisations that matter at scale or under constrained conditions.*

---

### P3-1 — Network response caching 🍎 Low-hanging fruit

**Why:** Every background fetch downloads the full feed XML even when nothing has changed. Respecting HTTP cache headers cuts bandwidth and battery usage.  
**What to build:**
- Enable an OkHttp disk cache (e.g., 10 MB) in `FeedParser`
- Send `If-None-Match` / `If-Modified-Since` headers on subsequent fetches to each source
- Treat a `304 Not Modified` response as "no new items" rather than a failure

---

### ~~P3-2 — Paging for the digest list~~ ✅ Done

**Why:** `FeedItemDao.getAllItems()` loads every stored item into memory at once. On a long-running install with many subscriptions this can cause jank or OOM errors.  
**What was built:**
- Migrated `FeedItemDao` queries to return `PagingSource<Int, FeedItem>` (Paging 3 library)
- Replaced `LiveData<List<FeedItem>>` with `Flow<PagingData<FeedItem>>` in `DigestRepository` and `DigestViewModel`
- Updated `DigestAdapter` to extend `PagingDataAdapter`

---

## Priority 4 — Power-User Features

*Additions that make the app stand out for engaged users.*

---

### P4-1 — OPML import / export 🏗️ Big feature ✅ Done

**Why:** OPML is the standard interchange format for RSS subscriptions. Without it, users are locked in and cannot migrate their list from other readers.  
**What was built:**
- **Export:** generates a valid OPML 2.0 XML file from all `FeedSource` rows; shares via `Intent.ACTION_SEND` using `FileProvider` for safe URI sharing
- **Import:** picks a file via `ActivityResultContracts.OpenDocument`; parses `<outline>` elements and bulk-inserts new sources into the database, skipping any URL already present
- New `OPMLManager` object in `feed/` handles OPML XML generation (`exportOpml`) and parsing (`parseOpml`) using Android's built-in `XmlPullParser`
- `OPMLImportState` sealed class in `SettingsViewModel` exposes import progress to the UI
- Import / Export buttons appear in a dedicated card at the bottom of the Settings screen

---

### P4-2 — Starred / bookmarked items 🏗️ Big feature

**Why:** Users may want to save specific articles indefinitely, safe from the pruning schedule.  
**What to build:**
- Add `isStarred` boolean column to `FeedItem` (requires a schema migration)
- Star / unstar action on each digest item
- "Saved" filter in the Digest screen
- Starred items are excluded from `pruneOldItems()`

---

### P4-3 — Search across feed items 🏗️ Big feature

**Why:** With many active sources, finding a previously seen article requires scrolling through the entire list.  
**What to build:**
- `SearchView` in the Digest toolbar
- Room FTS4 virtual table over `FeedItem.title` and `FeedItem.description`
- `DigestViewModel.search(query)` method returning filtered results

---

### P4-4 — Home screen widget 🏗️ Big feature

**Why:** The core promise is "see your digest without opening multiple apps". A widget surfaces unread count (or top headlines) directly on the home screen.  
**What to build:**
- `AppWidgetProvider` with a `RemoteViews` layout showing unread item count and top 3 headlines
- Tap deep-links into the Digest screen
- Widget refreshes via `AppWidgetManager` after each `DigestWorker` run

---

### P4-5 — Instrumented UI tests 🏗️ Big feature

**Why:** Placeholder Espresso test files exist but contain no real tests. UI regressions in the Digest and Settings screens will go undetected.  
**What to build:**
- Espresso test for the Digest screen: verify items render, pull-to-refresh works, mark-all-read clears the list
- Espresso test for the Settings screen: add a feed source, verify it appears in the list, delete it
- Integrate with the CI/CD pipeline (P1-7) to run on an emulator on every PR

---

## 💡 Ideas — Not yet committed

These exist to spark discussion. None are scheduled.

### Reading experience
- **In-app full-text reader** — Fetch the full article body via a Readability-style parser so users never leave the app
- **Estimated read time** — Show "~3 min read" based on word count
- **Offline reading** — Cache article HTML during background fetch for reading without internet
- **Text-to-speech** — Read an article aloud via Android's TTS engine; useful while commuting
- **Font size & typeface preference** — Let users pick a comfortable reading font and size in Settings

### Organisation & discovery
- **Category tags per feed source** — Label feeds (Tech, News, Sport); digest items show the tag colour, and filters snap to tags
- **Duplicate / near-duplicate detection** — Collapse same-story items appearing in multiple sources into a single entry
- **Per-source fetch interval** — Let users tune polling frequency per source instead of a single global schedule
- **Digest history** — Timeline view of past daily digest sessions showing what was delivered each day

### Notifications & focus
- **Quiet hours** — Block background fetches and delivery notifications during user-defined sleep hours
- **Digest summary notification** — Rich expanded notification listing the top 3 headlines inline
- **Unread count badge** on the launcher icon via `NotificationManager` channel badge

### Sharing & export
- **Copy link** — Long-press a digest item to copy its URL to the clipboard
- **Backup & restore** — Export all settings, feed sources, and starred items as a JSON file to device storage or a cloud drive

### Personalisation & appearance
- **Dark / Light / System theme override** — Add an explicit in-app toggle instead of following DayNight automatically
- **Compact list density** — Toggle between comfortable and compact row heights for power users with many subscriptions
- **Colour accent picker** — Small Material You palette for toolbar personalisation

### Platform & ecosystem
- **Wear OS companion** — Mirror unread count and headlines to a paired smartwatch
- **Podcast / video feed support** — Extend `FeedParser` to recognise `<enclosure>` tags and list audio/video episodes alongside text articles
- **Tasker / Shortcuts integration** — Intent-based API so power users can trigger a manual refresh from automation apps

### AI / smart features (longer-term)
- **On-device article summarisation** — Use ML Kit or Gemma Nano to generate a one-sentence summary as a subtitle on each item
- **Interest scoring** — Track taps vs. dismissals; surface higher-scoring items at the top of the digest
- **Smart digest scheduling** — Suggest delivery times based on historical app-open patterns
- **Language detection & filter** — Detect item language and let users hide content in languages they do not read
