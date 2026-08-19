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
4. The artifact holds two APKs — copy both to the phone and install them
   (allow installs from your file manager when prompted):
   - `Nexus-debug.apk` — the launcher
   - `NexusCompanion-debug.apk` — the Discover feed bridge (optional)

   Install both from the **same** build. The bridge between them is
   signature-checked, so a mismatched pair will not connect.

To build locally instead, with Android Studio or a configured SDK:

```
./gradlew :app:assembleDebug
# app/build/outputs/apk/debug/app-debug.apk
```

## Signing

Every build — local or CI — is signed with the committed key at
`keystore/nexus.jks`, so a new APK installs straight over the last one.

This matters more than it looks. Android refuses to update an app whose
signature changed, and the Android debug key is *auto-generated per machine*: an
ephemeral CI runner makes a fresh one on every run. Left on the default, each
build would carry a different signature and every install would fail until you
uninstalled first. The companion's overlay bridge also only accepts a launcher
signed with its own key, so the two APKs must match each other as well.

The committed key is a self-signed development key, not a release key. It is in
the repository on purpose so builds are reproducible and updatable — but the
repository is public, so treat it as public: anyone can sign an APK that claims
to be `com.nexus.launcher`, including one the companion would accept. To sign
with a key of your own instead, set these before building and nothing else
changes:

```
NEXUS_KEYSTORE_PATH      path to your keystore, relative to the repo root
NEXUS_KEYSTORE_PASSWORD
NEXUS_KEY_ALIAS
NEXUS_KEY_PASSWORD
```

Switching keys means one final uninstall-and-reinstall, since the signature
changes that once.

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

### Google Discover

The `:companion` module builds `NexusCompanion-debug.apk`, which implements the
`LauncherClient` overlay protocol — the same approach Lawnchair's Lawnfeed and
the other open-source companions take. It binds the Google app's
`com.android.launcher3.WINDOW_OVERLAY` service and re-exports it to Nexus over a
signature-guarded AIDL bridge, so the launcher can hand over its window token and
drive the feed's scroll from the pager.

It lives in a second APK because the Google app decides whether to serve the
overlay from the **calling package** — a launcher that binds the service directly
is refused.

**The honest caveat:** Google only serves that overlay to companion packages it
recognises, and a self-signed build is not one of them. Expect the Google app to
return no binding; the companion reports exactly that on its status screen and
the Discover page shows the reason rather than a blank feed. Every piece on the
Nexus side is wired and will light up if the Google app accepts the package, but
that acceptance is Google's to give — it is not something the code can force.
The page can be hidden entirely in *Nexus Settings → Apps*.

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
shared/aidl/com/nexus/companion/   the launcher <-> companion interface,
                                   compiled into both APKs

companion/src/main/
├── aidl/com/google/android/libraries/launcherclient/
│                               client-side declaration of the Google app's
│                               overlay interface (names are part of the
│                               binder contract — do not rename)
└── java/com/nexus/companion/
    ├── GoogleOverlayClient.kt  binds com.android.launcher3.WINDOW_OVERLAY
    ├── NexusOverlayService.kt  re-exports it to Nexus, caller-verified
    └── CompanionActivity.kt    status and setup screen

app/src/main/java/com/nexus/launcher/
├── NexusLauncherActivity.kt   HOME activity, launcher host actions
├── NexusApp.kt                process-wide singletons
├── domain/                    models
├── data/                      settings + persistence (DataStore)
├── integration/               apps, emulators, media, notifications, widgets, claude
├── integration/discover/      launcher side of the Discover bridge
└── ui/
    ├── theme/                 design tokens from the handoff
    ├── layout/                window size class + fold posture
    ├── home/                  clock, favourites, A–Z scrub, overscroll, page jump
    ├── hub/                   Games, Media, Music, Budget, Files, Discover, Claude
    ├── edit/                  page manager, page editor, widget gallery, blank page
    ├── settings/              Nexus settings, app lock, integrations
    └── common/                shared components and the Nexus Blush icon pack
```
