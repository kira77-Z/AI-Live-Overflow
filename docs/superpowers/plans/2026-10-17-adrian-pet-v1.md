# 小黑豹桌宠 v1 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 构建一个可安装的 Android APK，在其他应用上方显示可拖动、会反馈、可接收 Supabase 远程气泡的小黑豹桌宠。

**Architecture:** Android 前台服务拥有透明 WebView 悬浮窗，原生 Kotlin 负责权限、窗口、手势、通知、时间与充电状态，本地 HTML/CSS/JS 负责角色动画和气泡。Supabase Auth 与带 RLS 的 `pet_` 表提供可选远程命令；离线时全部本地互动仍然工作。

**Tech Stack:** Kotlin, Android SDK 26+, AndroidX, XML Views, WebView, DataStore Preferences, Supabase Auth/REST/Realtime, JUnit/Robolectric, GitHub Actions

**Spec:** `DESIGN.md`

## Global Constraints

- 包名固定为 `com.kira.adrianpet`，minSdk 26、compileSdk 36、targetSdk 36，使用 JDK 17。
- 第一版不申请无障碍、使用情况访问、应用列表或截图读取权限。
- WebView 只加载打包的本地资源，不直接持有 Supabase key。
- Android 客户端绝不包含 Supabase `service_role`/secret key。
- 每个公开 `pet_` 表必须启用 RLS，并用 `auth.uid() = user_id` 做行级授权。
- 单击、连戳、拖动必须各自触发气泡；默认气泡时长为 4000ms。
- 普通模式与趴趴模式必须保存不同坐标。
- 第一版无音频，触感反馈可以在设置中关闭。

## Review Focus

- 悬浮窗权限被拒绝或稍后撤销时，服务应安全停止并回到授权页，不得崩溃。
- 屏幕旋转、刘海/导航栏尺寸变化后，保存坐标应被钳制回可见区域。
- 快速“点—拖—点”不能同时被分类成连戳和拖动。
- 远程命令含未知状态、超长文本或恶意转义字符时，只能作为受限文本渲染。
- 网络断开、JWT 过期或 Realtime 断线时，本地桌宠继续工作且同步退化为可恢复状态。

---

### Task 1: 建立可重复构建的 Android 工程

**Files:**
- Create: `settings.gradle.kts`
- Create: `build.gradle.kts`
- Create: `gradle/libs.versions.toml`
- Create: `gradle/wrapper/gradle-wrapper.properties`
- Create: `gradlew`
- Create: `gradlew.bat`
- Create: `app/build.gradle.kts`
- Create: `app/src/main/AndroidManifest.xml`
- Create: `app/src/main/java/com/kira/adrianpet/MainActivity.kt`
- Create: `app/src/main/res/layout/activity_main.xml`
- Create: `.github/workflows/android.yml`
- Create: `.gitignore`

**Interfaces:**
- Produces: installable `app-debug.apk`; application ID `com.kira.adrianpet`; `MainActivity` launcher entry point.

- [ ] **Step 1: Bootstrap the build-only project**

Create the pinned Gradle wrapper, root/module build files and empty source sets so `./gradlew tasks` succeeds before product code is added.

- [ ] **Step 2: Write the failing manifest/config test**

Create `app/src/test/java/com/kira/adrianpet/ProjectConfigTest.kt` asserting the application package, min SDK contract, and that no forbidden permissions (`BIND_ACCESSIBILITY_SERVICE`, `PACKAGE_USAGE_STATS`, `QUERY_ALL_PACKAGES`, media read permissions) appear in the merged manifest fixture.

- [ ] **Step 3: Run the test to verify the app scaffold is missing**

Run: `./gradlew testDebugUnitTest`
Expected: FAIL because the app manifest/activity do not yet exist.

- [ ] **Step 4: Create the minimal app**

Use JDK 17, compileSdk/targetSdk 36, minSdk 26, XML Views, AndroidX AppCompat and Material Components. Add only `INTERNET`, `SYSTEM_ALERT_WINDOW`, `FOREGROUND_SERVICE`, `FOREGROUND_SERVICE_SPECIAL_USE`, `POST_NOTIFICATIONS`, and `VIBRATE` where required by API level. Declare the foreground service subtype property required for `specialUse`.

- [ ] **Step 5: Add CI build verification**

Workflow command: `./gradlew testDebugUnitTest lintDebug assembleDebug --stacktrace`
Upload: `app/build/outputs/apk/debug/app-debug.apk`

- [ ] **Step 6: Run the checks**

Run: `./gradlew testDebugUnitTest lintDebug assembleDebug`
Expected: all tasks PASS and debug APK exists.

- [ ] **Step 7: Commit**

```bash
git add .gitignore .github gradle gradlew gradlew.bat settings.gradle.kts build.gradle.kts app
git commit -m "build: scaffold Android pet app"
```

### Task 2: 实现本地桌宠渲染器

**Files:**
- Create: `app/src/main/assets/pet/index.html`
- Create: `app/src/main/assets/pet/pet.css`
- Create: `app/src/main/assets/pet/pet.js`
- Create: `app/src/main/assets/pet/images/pet_base.png`
- Create: `app/src/main/assets/pet/images/pet_blink.png`
- Create: `app/src/main/assets/pet/images/pet_perch.png`
- Create: `app/src/test/java/com/kira/adrianpet/render/RenderEventTest.kt`
- Create: `app/src/main/java/com/kira/adrianpet/render/RenderEvent.kt`
- Create: `app/src/main/java/com/kira/adrianpet/render/PetRenderer.kt`

**Interfaces:**
- Produces: `PetState` enum; `RenderEvent(state: PetState, bubble: String?, durationMs: Long)`; `PetRenderer.render(event: RenderEvent)`.

- [ ] **Step 1: Write failing serialization and validation tests**

Assert all seven spec states serialize to a JSON object, bubble text is capped at 36 characters, default duration is 4000ms, and quote/newline characters remain data rather than executable JavaScript.

- [ ] **Step 2: Run the focused test and verify failure**

Run: `./gradlew testDebugUnitTest --tests '*RenderEventTest'`
Expected: FAIL because render types do not exist.

- [ ] **Step 3: Prepare and verify the three production assets**

Create the blink and perch variants from the approved thick-line transparent base art, visually inspect them at 180dp, and copy the three optimized PNGs into the asset paths above. The face, hair, ears, body markings and palette must remain consistent across states.

- [ ] **Step 4: Implement the typed render contract**

Create `enum class PetState { IDLE, BLINK, TAP, RAPID_TAP, DRAG, SLEEP, PERCH }` and a serializer that passes one JSON string to a fixed `window.petEngine.render(...)` entry point.

- [ ] **Step 5: Implement transparent local WebView content**

The JS API must be exactly `window.petEngine.render(event)` and use `textContent`, never `innerHTML`, for bubble text. CSS must animate only transform/opacity and support reduced motion.

- [ ] **Step 6: Run renderer tests**

Run: `./gradlew testDebugUnitTest --tests '*RenderEventTest'`
Expected: PASS.

- [ ] **Step 7: Commit**

```bash
git add app/src/main/assets app/src/main/java/com/kira/adrianpet/render app/src/test/java/com/kira/adrianpet/render
git commit -m "feat: add local pet renderer"
```

### Task 3: 实现悬浮窗服务、通知与位置存储

**Files:**
- Create: `app/src/main/java/com/kira/adrianpet/overlay/OverlayService.kt`
- Create: `app/src/main/java/com/kira/adrianpet/overlay/OverlayController.kt`
- Create: `app/src/main/java/com/kira/adrianpet/overlay/OverlayPosition.kt`
- Create: `app/src/main/java/com/kira/adrianpet/overlay/OverlayNotification.kt`
- Create: `app/src/main/java/com/kira/adrianpet/data/PetPreferences.kt`
- Create: `app/src/test/java/com/kira/adrianpet/overlay/OverlayPositionTest.kt`
- Modify: `app/src/main/AndroidManifest.xml`

**Interfaces:**
- Consumes: `PetRenderer.render(RenderEvent)` from Task 2.
- Produces: `OverlayController.show()`, `hide()`, `moveTo(x, y)`, `setPerchMode(enabled)`; `OverlayPosition.clamp(bounds, size)`; DataStore keys for `normal_x/y`, `perch_x/y`, `perch_enabled`.

- [ ] **Step 1: Write failing coordinate tests**

Assert negative/off-screen coordinates clamp inside current safe bounds, portrait coordinates remain usable after landscape conversion, and normal/perch keys do not overwrite each other.

- [ ] **Step 2: Run the focused test and verify failure**

Run: `./gradlew testDebugUnitTest --tests '*OverlayPositionTest'`
Expected: FAIL because overlay classes do not exist.

- [ ] **Step 3: Implement the foreground overlay lifecycle**

Create a 180dp by 240dp transparent, non-focusable `TYPE_APPLICATION_OVERLAY` WebView. Set transparent background before `loadUrl`, destroy the WebView on service teardown, and stop safely when overlay permission is missing.

- [ ] **Step 4: Implement persistent notification actions**

Actions: `ACTION_TOGGLE_VISIBILITY`, `ACTION_TOGGLE_PERCH`, `ACTION_OPEN_SETTINGS`. Use a low-importance silent channel.

- [ ] **Step 5: Implement position persistence and edge snapping**

On drag end, snap horizontally to the nearest safe edge and save the active mode's coordinate pair. Re-clamp whenever display metrics change.

- [ ] **Step 6: Run overlay tests and build**

Run: `./gradlew testDebugUnitTest --tests '*OverlayPositionTest' assembleDebug`
Expected: PASS and APK builds.

- [ ] **Step 7: Commit**

```bash
git add app/src/main/AndroidManifest.xml app/src/main/java/com/kira/adrianpet/overlay app/src/main/java/com/kira/adrianpet/data app/src/test/java/com/kira/adrianpet/overlay
git commit -m "feat: add persistent overlay service"
```

### Task 4: 实现手势、气泡与本地行为

**Files:**
- Create: `app/src/main/java/com/kira/adrianpet/gesture/GestureClassifier.kt`
- Create: `app/src/main/java/com/kira/adrianpet/behavior/PetBehaviorEngine.kt`
- Create: `app/src/main/java/com/kira/adrianpet/behavior/BubbleCatalog.kt`
- Create: `app/src/main/java/com/kira/adrianpet/behavior/ChargingMonitor.kt`
- Create: `app/src/test/java/com/kira/adrianpet/gesture/GestureClassifierTest.kt`
- Create: `app/src/test/java/com/kira/adrianpet/behavior/PetBehaviorEngineTest.kt`
- Modify: `app/src/main/java/com/kira/adrianpet/overlay/OverlayController.kt`

**Interfaces:**
- Produces: `GestureClassifier.onTouch(sample: TouchSample): List<GestureResult>`; `GestureResult.Tap`, `RapidTap`, `DragMove`, `DragEnd`; `PetBehaviorEngine.onGesture(result): RenderEvent`; `onChargingChanged(isCharging): RenderEvent?`; `onIdleTimeout(): RenderEvent`.

- [ ] **Step 1: Write failing gesture sequence tests**

Cover single tap, three taps within 2000ms, movement beyond Android touch slop, drag end, cancelled touch, and the review-focus “tap—drag—tap” sequence.

- [ ] **Step 2: Write failing behavior tests**

Assert tap/rapid tap/drag end each returns a non-empty bubble with 4000ms duration, five minutes returns `SLEEP`, and charging events are de-duplicated.

- [ ] **Step 3: Run focused tests and verify failure**

Run: `./gradlew testDebugUnitTest --tests '*GestureClassifierTest' --tests '*PetBehaviorEngineTest'`
Expected: FAIL because gesture/behavior classes do not exist.

- [ ] **Step 4: Implement gesture classification using raw coordinates**

Use `ViewConfiguration.scaledTouchSlop`; reset tap streak on drag/cancel; do not classify a drag as a tap.

- [ ] **Step 5: Implement local behavior and haptics**

Use deterministic injected clock/random providers in tests. Trigger `VibrationEffect.EFFECT_TICK` where available with a short fallback vibration on API 26; respect the preference toggle.

- [ ] **Step 6: Connect charging and idle events**

Register charging receiver only while the service is alive. Schedule the five-minute idle transition and cancel/reschedule it on touch.

- [ ] **Step 7: Run behavior tests**

Run: `./gradlew testDebugUnitTest --tests '*GestureClassifierTest' --tests '*PetBehaviorEngineTest'`
Expected: PASS.

- [ ] **Step 8: Commit**

```bash
git add app/src/main/java/com/kira/adrianpet/gesture app/src/main/java/com/kira/adrianpet/behavior app/src/main/java/com/kira/adrianpet/overlay app/src/test/java/com/kira/adrianpet/gesture app/src/test/java/com/kira/adrianpet/behavior
git commit -m "feat: add pet gestures and local behavior"
```

### Task 5: 完成首次启动、授权和控制页

**Files:**
- Create: `app/src/main/java/com/kira/adrianpet/ui/MainViewModel.kt`
- Create: `app/src/main/java/com/kira/adrianpet/ui/PermissionCoordinator.kt`
- Create: `app/src/main/res/layout/view_welcome.xml`
- Create: `app/src/main/res/layout/view_settings.xml`
- Create: `app/src/test/java/com/kira/adrianpet/ui/PermissionCoordinatorTest.kt`
- Modify: `app/src/main/java/com/kira/adrianpet/MainActivity.kt`
- Modify: `app/src/main/res/layout/activity_main.xml`

**Interfaces:**
- Consumes: Overlay service actions from Task 3; preference toggles from Tasks 3–4.
- Produces: `PermissionCoordinator.nextStep(state): PermissionStep`; controls for service, perch mode, bubbles, haptics and recalibration.

- [ ] **Step 1: Write failing permission-state tests**

Cover overlay denied, notification denied on API 33+, notification not required on API 26–32, permission revoked while service exists, and all permissions granted.

- [ ] **Step 2: Run the test and verify failure**

Run: `./gradlew testDebugUnitTest --tests '*PermissionCoordinatorTest'`
Expected: FAIL because coordinator does not exist.

- [ ] **Step 3: Implement the onboarding state machine**

Request only overlay and version-gated notification permissions. Show explicit recovery UI instead of repeatedly launching settings.

- [ ] **Step 4: Implement settings controls**

Wire all switches and recalibration actions to DataStore and service intents. Service off must remove the overlay but preserve saved coordinates.

- [ ] **Step 5: Run UI unit tests and lint**

Run: `./gradlew testDebugUnitTest --tests '*PermissionCoordinatorTest' lintDebug`
Expected: PASS.

- [ ] **Step 6: Commit**

```bash
git add app/src/main/java/com/kira/adrianpet/MainActivity.kt app/src/main/java/com/kira/adrianpet/ui app/src/main/res/layout app/src/test/java/com/kira/adrianpet/ui
git commit -m "feat: add onboarding and pet controls"
```

### Task 6: 加入 Supabase 登录、RLS 和远程气泡

**Files:**
- Create: `supabase/migrations/<generated>_create_pet_tables.sql`
- Create: `app/src/main/java/com/kira/adrianpet/data/auth/AuthRepository.kt`
- Create: `app/src/main/java/com/kira/adrianpet/data/remote/PetCommand.kt`
- Create: `app/src/main/java/com/kira/adrianpet/data/remote/PetRemoteRepository.kt`
- Create: `app/src/main/res/layout/view_login.xml`
- Create: `app/src/test/java/com/kira/adrianpet/data/auth/AuthRepositoryTest.kt`
- Create: `app/src/test/java/com/kira/adrianpet/data/remote/PetCommandTest.kt`
- Create: `app/src/test/java/com/kira/adrianpet/data/remote/PetRemoteRepositoryTest.kt`
- Modify: `app/src/main/java/com/kira/adrianpet/ui/MainViewModel.kt`
- Modify: `app/build.gradle.kts`

**Interfaces:**
- Produces: `AuthRepository.signIn(email, password)`, `signOut()`, `session`; `PetRemoteRepository.observeCommands(userId): Flow<PetCommand>`; `recordEvent(event)`; `acknowledge(commandId)`.

- [ ] **Step 1: Verify current Supabase docs and pin dependencies**

Read the current Supabase changelog and official Kotlin/Auth/Realtime docs. Record pinned versions in `libs.versions.toml` and commit the lockfiles produced by the selected dependency tooling.

- [ ] **Step 2: Write failing command validation tests**

Assert only `bubble` type and known `PetState` values pass, text is nonblank and capped at 36 characters, duration is clamped to a safe range, and unknown JSON fields are ignored.

- [ ] **Step 3: Write failing authentication tests**

Assert successful email/password login persists the session, invalid credentials surface a user-readable error, sign-out clears the session, and no password is written to preferences or logs.

- [ ] **Step 4: Write failing offline/re-auth repository tests**

With a fake transport, assert network failure does not affect local renderer, expired session moves sync to signed-out/recoverable state, duplicate command IDs are not rendered twice, and reconnect resumes observation.

- [ ] **Step 5: Run focused tests and verify failure**

Run: `./gradlew testDebugUnitTest --tests '*AuthRepositoryTest' --tests '*PetCommandTest' --tests '*PetRemoteRepositoryTest'`
Expected: FAIL because remote types do not exist.

- [ ] **Step 6: Create and verify the Supabase schema**

Generate the migration with Supabase CLI, create `pet_devices`, `pet_events`, `pet_commands`, enable RLS, and add owner policies using `(select auth.uid()) = user_id`. Test as two different users that cross-user reads/writes fail. Run Supabase database/security advisors before finalizing the migration.

- [ ] **Step 7: Implement Auth and remote repositories**

Read build-time `SUPABASE_URL` and `SUPABASE_PUBLISHABLE_KEY`; fail closed when absent. Keep credentials/session in the approved Supabase client storage, never assets or source.

- [ ] **Step 8: Add the login UI and connect validated commands to the renderer**

Require the existing Supabase email/password account on first launch, restore a valid session on later launches, render each command once, acknowledge it for the signed-in owner, and keep local behavior active while sync is unavailable.

- [ ] **Step 9: Run tests and build without secrets**

Run: `./gradlew testDebugUnitTest lintDebug assembleDebug`
Expected: PASS; app shows cloud sync as unconfigured rather than crashing.

- [ ] **Step 10: Commit**

```bash
git add supabase app/src/main/java/com/kira/adrianpet/data app/src/main/java/com/kira/adrianpet/ui app/src/main/res/layout/view_login.xml app/src/test/java/com/kira/adrianpet/data app/build.gradle.kts gradle
git commit -m "feat: add secure pet cloud sync"
```

### Task 7: 端到端验收与 APK 交付

**Files:**
- Create: `docs/TEST-CHECKLIST.md`
- Modify: `README.md`
- Modify: `.github/workflows/android.yml`

**Interfaces:**
- Consumes: completed APK and all interfaces from Tasks 1–6.
- Produces: reproducible build instructions, manual Android test matrix, CI APK artifact.

- [ ] **Step 1: Write the device checklist from the spec acceptance criteria**

Include fresh install, permission deny/retry/revoke, service process restart, drag at all four corners, rotation, notification actions, normal/perch coordinate restoration, keyboard-open visual calibration, offline/online transitions and Supabase cross-user isolation.

- [ ] **Step 2: Run the complete automated suite**

Run: `./gradlew clean testDebugUnitTest lintDebug assembleDebug --stacktrace`
Expected: PASS with no lint errors and APK generated.

- [ ] **Step 3: Inspect APK permissions and contents**

Run Android build tools to confirm the merged permissions match the design and search the unpacked APK for forbidden secret names/values.
Expected: only declared v1 permissions; no service role, password, JWT or keystore.

- [ ] **Step 4: Execute the manual checklist on the target Android 16 phone**

Record pass/fail and device-specific permission labels in `docs/TEST-CHECKLIST.md`; fix every release-blocking failure and rerun the affected test.

- [ ] **Step 5: Verify CI from a clean commit**

Expected: GitHub Actions test/lint/build job succeeds and publishes `app-debug.apk` as an artifact.

- [ ] **Step 6: Commit**

```bash
git add README.md docs/TEST-CHECKLIST.md .github/workflows/android.yml
git commit -m "docs: add build and device verification"
```
