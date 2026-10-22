# App Detection

Detect which app is in the foreground. Your pet can react differently based on what the user is doing.

## How It Works

Android's `UsageStatsManager` lets you query recent app usage events. By polling every few seconds, you can determine the current foreground app.

## Permission

Requires `PACKAGE_USAGE_STATS` — user must grant this in Settings > Apps > Special Access > Usage Access.

## Current Implementation

`ForegroundAppReader` reads recent `UsageEvents` every 3 seconds. `AppSwitchGate` requires two
matching samples, de-duplicates the current app, and applies a 12-second global cooldown before
`ForegroundAppBubbleCatalog` chooses a local line. Unknown apps do not produce a bubble.

## Ideas for App Reactions

- Social media: pet holds a sign ("come back")
- Shopping: pet puts on accessories
- Camera: pet poses
- Games: pet cheers or falls asleep
- Late night browsing: pet yawns, taps watch

## Backend Boundary

App switches are intentionally not sent through `PetRemoteRepository` and are not stored locally.
The current version uses them only to select an on-device speech bubble.

## Privacy Note

If you're publishing this for others, be mindful about what you log. App usage data is sensitive. In a personal project built for one person, this is a feature. In a distributed app, it would need consent flows.
