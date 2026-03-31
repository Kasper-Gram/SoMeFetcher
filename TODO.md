# SoMeFetcher — Feature TODO

This file tracks what has been built and what is planned next.  
Items are ordered by usefulness inside each section.

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

---

## 🔜 Planned

### 1 — App filter for notification listening
**Why:** `SoMeNotificationService` currently captures notifications from every installed app.  
Users need to choose which apps are included (e.g., WhatsApp, Gmail) and which are ignored.  
**What to build:**
- Add a `notifiedApps` table (or a SharedPreferences set) of allowed package names
- Add an "Allowed apps" screen in Settings that lists installed apps with a toggle switch per app
- `SoMeNotificationService.shouldIgnorePackage()` checks the allowlist

---

### 2 — Feed URL validation on add
**Why:** The "Add Feed Source" dialog currently accepts any text as a URL. Invalid URLs
cause silent failures in `FeedParser` and confuse users.  
**What to build:**
- In `SettingsViewModel.addSource()`, call `FeedParser.fetchFeed()` before saving
- Show a loading indicator in the dialog while validating
- Show an inline error (TextInputLayout error text) if the URL is not a valid RSS/Atom feed

---

### 3 — Last-synced status per feed source
**Why:** `FeedSource.lastFetched` is already stored in the database but never shown in the UI.  
**What to build:**
- Display a formatted "Last synced: …" subtitle in `item_feed_source.xml`
- Show "Never" for sources that have `lastFetched == 0L`
- Optionally show a warning icon when `lastFetched` is more than 24 hours ago

---

### 4 — Show read items (all-items view)
**Why:** Once an item is marked as read it disappears entirely. Users have no way to revisit articles.  
**What to build:**
- Add a toggle (chip or menu item) in the Digest screen to switch between "Unread" and "All"
- `DigestViewModel` exposes both `repository.unreadItems` and `repository.allItems`
- Read items displayed with reduced opacity or a "read" visual treatment in `DigestAdapter`

---

### 5 — Filter and sort the digest list
**Why:** When many sources and apps are active the digest list becomes unwieldy.  
**What to build:**
- Filter chips at the top of the Digest screen: **All · RSS · Notifications**
- Optional sort order: **Newest first / Oldest first / Source**
- State held in `DigestViewModel` (no database migration needed)

---

### 6 — Multiple daily digest times
**Why:** The original goal is "display at set times of day". Currently only one delivery time is supported.  
**What to build:**
- Allow up to 3 delivery times (e.g., morning, noon, evening) in Settings
- Store times in SharedPreferences as a list
- `DigestScheduler` enqueues one `PeriodicWorkRequest` per active time slot, using unique work names

---

### 7 — In-app article reader (Custom Tabs)
**Why:** Opening raw browser breaks flow and loses the user. Chrome Custom Tabs keep the user inside the app and respect the system theme.  
**What to build:**
- Add `androidx.browser:browser` dependency
- In `DigestFragment`, launch a `CustomTabsIntent` instead of a bare `Intent.ACTION_VIEW`
- Set toolbar color to match the app's primary color

---

### 8 — Share item
**Why:** Users may want to send an article to another app (chat, notes, read-later).  
**What to build:**
- Long-press or swipe action on a digest item
- Fire a standard `Intent.ACTION_SEND` with the item title and URL

---

### 9 — OPML import / export
**Why:** OPML is the standard interchange format for RSS feed lists, letting users move their subscriptions between apps.  
**What to build:**
- Export: generate an OPML XML file from all `FeedSource` rows and share it via `Intent.ACTION_SEND`
- Import: pick a file via `ActivityResultContracts.OpenDocument`, parse `<outline>` elements, insert into the database

---

### 10 — Home screen widget
**Why:** The core promise is "see your digest without opening multiple apps". A widget surfaces unread count (or top headlines) directly on the home screen.  
**What to build:**
- `AppWidgetProvider` subclass with a `RemoteViews` layout showing unread item count
- Tap opens the app at the Digest screen
- Widget updates via `AppWidgetManager` whenever the database changes (observe via WorkManager or a BroadcastReceiver)

---

### 11 — Unit tests
**Why:** There are only placeholder test files. Key logic is completely untested.  
**What to build:**
- `FeedParserTest` — test RSS and Atom parsing with fixture XML files, verify `FeedItem` fields
- `DigestRepositoryTest` — test `refreshFeeds()` failure-count logic using a fake `FeedParser`
- `DigestSchedulerTest` — verify `calculateInitialDelay()` returns a value in [0, 24h)

---

### 12 — Data retention preference
**Why:** The 30-day pruning cutoff is hard-coded in `DigestRepository.pruneOldItems()`.  
**What to build:**
- Add a "Keep items for" dropdown in Settings (7 / 14 / 30 / 90 days)
- Store the value in SharedPreferences; read it in `pruneOldItems()`

---

## 💡 Ideas (not yet prioritised)

Ideas are grouped by theme. None are committed to yet — they exist to spark discussion and future planning.

---

### Reading experience

- **Starred / bookmarked items** — Let users save articles with a star tap; a separate "Saved" tab shows them indefinitely, immune to the pruning schedule
- **In-app full-text reader** — Fetch the full article body (via Readability / Mercury parser API) so the user never leaves the app
- **Estimated read time** — Show "~3 min read" next to each RSS item based on word count
- **Offline reading** — Cache article HTML/text during the background fetch so items can be read without internet
- **Focus mode** — A distraction-free reader view that hides the app chrome and shows only the article
- **Font size & typeface preference** — Let users pick a comfortable reading font and size in Settings
- **Text-to-speech** — Read an article aloud via Android's TTS engine; useful while commuting

---

### Organisation & discovery

- **Category tags per feed source** — Users label feeds (e.g., Tech, News, Sport); digest items show the tag colour, and filters snap to tags
- **Duplicate / near-duplicate detection** — When the same story appears from multiple sources, collapse them into a single item with a "N sources" indicator
- **Per-source fetch interval** — Some feeds update hourly, others weekly; let users tune polling frequency per source instead of a global schedule
- **Trending badge** — Mark items that appear across multiple enabled sources as "Trending"
- **Search** across all feed item titles, descriptions, and source names with a SearchView in the Digest toolbar
- **Digest history** — A calendar or timeline view of past daily digest sessions, showing what was delivered on each day

---

### Notifications & focus

- **Unread count badge** on the launcher icon via `NotificationManager` channel badge
- **Quiet hours** — Block background fetches and delivery notifications during user-defined sleep hours
- **Notification grouping** — Collapse multiple notifications from the same source app into a single digest entry using Android's notification grouping API
- **Digest summary notification** — Replace the current simple notification with a rich expanded notification listing the top 3 headlines inline
- **Custom notification sound per source type** — Different ringtones for RSS items vs. app notifications

---

### Sharing & export

- **Share to read-later apps** — Pre-built quick-share targets for Pocket, Instapaper, and generic Intent.ACTION_SEND so users can save articles in one tap
- **Copy link** — Long-press a digest item to copy its URL to the clipboard
- **OPML import** — Pick an OPML file from storage; parse `<outline>` entries and bulk-add them as `FeedSource` rows
- **OPML export** — Generate and share an OPML file from all saved feed sources, compatible with any RSS reader
- **Backup & restore** — Export all settings, feed sources, and starred items as a JSON file to device storage or a cloud drive

---

### Personalisation & appearance

- **Dark / Light / System theme override** — Currently follows DayNight automatically; add an explicit in-app toggle
- **Colour accent picker** — Let users choose from a small Material You palette to personalise the toolbar colour
- **Compact list density** — A toggle between "comfortable" (default) and "compact" row heights for power users with many subscriptions
- **Custom app icon** — Offer a set of alternate launcher icons (e.g., monochrome, outlined) via Android's `<activity-alias>` mechanism

---

### Platform & ecosystem

- **Home screen widget** — A resizable widget showing unread item count or the top 3 headlines; taps deep-link into the Digest screen
- **Wear OS companion** — Mirror the unread count and a short headline list to a paired smartwatch
- **Android Auto / Car integration** — Read article titles aloud via Android Auto's `CarAppService` while driving
- **Podcast / video feed support** — Extend `FeedParser` to recognise `<enclosure>` tags and list audio/video episodes alongside text articles
- **Tasker / Shortcuts integration** — Expose an intent-based API so power users can trigger a manual refresh or open a specific feed from automation apps

---

### Quality & reliability

- **Accessibility audit** — Full TalkBack labelling, minimum 48 dp touch targets, and content descriptions on all icons
- **Crash reporting** — Integrate a lightweight, privacy-friendly crash reporter (e.g., Firebase Crashlytics or Sentry) with opt-in consent dialog on first launch
- **Feed health check** — Periodically verify that saved feed URLs still return valid content; surface a warning in Settings for broken sources
- **Localization** — Extract all hard-coded English strings into `strings.xml` and add at least one additional locale (e.g., Danish, given the project owner)
- **Instrumented UI tests** — Espresso tests for the Digest and Settings screens to catch regressions on real device/emulator

---

### AI / smart features (longer-term)

- **On-device article summarisation** — Use ML Kit or a small local model (e.g., Gemma Nano) to generate a one-sentence summary displayed as a subtitle on each item
- **Interest scoring** — Track which articles the user taps vs. dismisses; surface higher-scoring items at the top of the digest
- **Smart digest scheduling** — Automatically suggest delivery times based on when the user has historically opened the app
- **Language detection & filter** — Detect the language of incoming items and let users hide content in languages they don't read
