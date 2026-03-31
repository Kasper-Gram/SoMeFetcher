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

- **Unread count badge** on the launcher icon (using `ShortcutBadger` or `NotificationManager` badge)
- **Search** across all feed item titles and descriptions
- **Dark / Light theme override** — currently auto via DayNight, add an explicit preference
- **Per-source fetch interval** — some feeds update hourly, others weekly; let the user tune this
- **Notification grouping** — group multiple notifications from the same app into a single digest entry
