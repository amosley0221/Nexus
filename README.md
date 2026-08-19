# Nexus

An Android launcher for Samsung Galaxy Z Fold devices. Niagara-style one-handed
vertical app flow on Home, plus Nova-style swipeable pages where each page is a
purpose-built hub — Games, Media, Music, Budget, Files — with an optional Google
Discover page on the far left.

Built from the `design_handoff_nexus_launcher` package: Kotlin + Jetpack Compose,
a single HOME-category activity, no Launcher3 fork.

## Getting the APK

The Android SDK is not required to get a build — every push builds a debug APK in
CI.

1. Open the [Actions tab](../../actions/workflows/build-apk.yml)
2. Click the most recent green run for your branch
3. Download the **Nexus-debug-apk** artifact and unzip it
4. Copy `Nexus-debug.apk` to the phone and install it (allow installs from your
   file manager when prompted)

To build locally instead, with Android Studio or a configured SDK:

```
./gradlew :app:assembleDebug
# app/build/outputs/apk/debug/app-debug.apk
```

## First run

1. Install, then press Home and pick **Nexus** — or open Nexus and use
   *Settings → Set Nexus as default launcher*.
2. Long-press anywhere to open edit mode (page manager → page editor → widget
   gallery).
3. Two optional permissions unlock parts of the design, both reachable from
   *Nexus Settings → Permissions*:
   - **Usage access** — playtime lines on game cards, and the most-used fallback
     for Home favourites.
   - **Notification access** — the pull-down notification cards, and the
     now-playing card on the Music hub.

## What's wired to real data

| Area | Source |
| --- | --- |
| App list, icons, launching | `LauncherApps` (work profiles and package changes included) |
| Home favourites | Your picks, falling back to most-used until you choose |
| A–Z sections | Derived from the installed list at runtime |
| Games (Installed) | `ApplicationInfo.CATEGORY_GAME` + UsageStats playtime |
| Games (Emulators) | SAF folder grant → extension scan → per-emulator launch intents |
| Music now playing | Active `MediaSession` (needs notification access) |
| Notifications | `NotificationListenerService` |
| Files | Free space from `StatFs`; folder indexing via SAF |
| Budget | Local store, seeded with a starter month |
| Widgets | `AppWidgetHost` — KWGT and any installed app widget |
| App lock | `BiometricPrompt`, fingerprint with device-credential fallback |

## What needs configuring before it shows live data

These render their real UI with an empty state until you add credentials in
*Nexus Settings → Integrations*:

- **Plex** — server URL + token
- **Twitch** — login + OAuth token with `user:read:follows`
- **Claude activity** — relay base URL. Desktop Claude Code hooks POST task
  events to the relay; Nexus polls `GET /tasks` and posts answers to
  `POST /tasks/{id}/reply`. With no relay set, the feed shows sample tasks so the
  screens stay reviewable.

Streaming apps (Netflix, Disney+, Prime) and Apple Music are deep links by
design — neither platform exposes a library API to third-party launchers.

**Google Discover** needs a separately-signed companion APK that speaks the
`LauncherClient` overlay protocol; it cannot ship inside this APK. Until one is
installed the page says so, and it can be hidden entirely in settings.

## Responsive behaviour

No device dimensions are hardcoded anywhere — Fold 7 and Fold 8 both fall out of
the same measurements. `WindowProfile` derives everything from the live window
size and the hinge state reported by Jetpack WindowManager:

- **Cover screen** — single-column favourites, 2-column hub grids
- **Inner screen** — two-column favourites, 3-column hub grids, two-pane hubs
- **Folded landscape** — clock block pinned left, list centre, A–Z right
- **Widget grid** — cell count computed from available space (4×2-class folded,
  6×3-class unfolded)

## Layout

```
app/src/main/java/com/nexus/launcher/
├── NexusLauncherActivity.kt   HOME activity, launcher host actions
├── NexusApp.kt                process-wide singletons
├── domain/                    models
├── data/                      settings + persistence (DataStore)
├── integration/               apps, emulators, media, notifications, widgets, claude
└── ui/
    ├── theme/                 design tokens from the handoff
    ├── layout/                window size class + fold posture
    ├── home/                  clock, favourites, A–Z scrub, overscroll, page jump
    ├── hub/                   Games, Media, Music, Budget, Files, Discover, Claude
    ├── edit/                  page manager, page editor, widget gallery, blank page
    ├── settings/              Nexus settings, app lock, integrations
    └── common/                shared components and the Nexus Blush icon pack
```
