# okDriver — Android Vehicle Safety Platform

## 1. Setup & Build

1. Open the project root in Android Studio (Koala or newer recommended).
2. Let Gradle sync — the project uses Kotlin KSP (not KAPT) for Room/Hilt annotation processing.
3. Minimum requirements: Android SDK 34+ target, a device/emulator running API 26+.
4. Grant runtime permissions on first launch via the in-app Permissions screen:
   `ACCESS_FINE_LOCATION`, `RECORD_AUDIO`, `POST_NOTIFICATIONS` (API 33+).
5. Build & run the `app` module. The Foreground Service ("Safety Monitoring") starts from the
   Home screen toggle once permissions are granted.

No API keys or backend configuration are required — every external dependency (vehicle
telemetry, driver monitoring, emergency API, responder network) is simulated locally, per the
assignment's brief for a self-contained prototype.

## 2. Architecture Overview

**Pattern: MVVM + Clean Architecture (Repository pattern), Hilt for DI.**

Chosen because the assignment explicitly asks the safety engine (sensing → anomaly detection →
state machine → escalation) to be swappable for real hardware/AI later without rewriting
consumers. Clean layering achieves that directly:

```
sensors/            Raw & simulated data sources (Accelerometer, Gyroscope, GPS,
                     Battery, Network, DMS simulator, Vehicle telemetry simulator)
        │  Flow<Sample>
        ▼
data/repository/    Normalizes & combines sensor streams (SensorRepository),
                     persists domain data via Room (DAOs/Entities), exposes
                     everything as Flow/suspend functions behind interfaces
        │
        ▼
domain/engine/       Pure Kotlin business logic — zero Android imports:
                     BaselineManager, AnomalyEngine/AnomalyManager,
                     AnomalyRules (rules layer), IncidentStateMachine,
                     EmergencyVerificationOrchestrator,
                     ResponderMobilizationManager, IncidentPayloadBuilder,
                     EmergencyOrchestratorCoordinator
domain/community/    Community responder simulation & mobilization
audio/               TTS (EmergencyTtsManager) + STT (VoiceVerificationService,
                     Real/Simulated + Router) — conversational AI verification
        │
        ▼
service/             SafetyMonitoringService (Foreground Service) — thin glue:
                     collects the combined sensor snapshot, feeds it to the
                     domain engines, and reacts to state-machine transitions
                     by launching orchestrators via the Coordinator
        │
        ▼
ui/                  Fragments + Hilt ViewModels (MVVM), one per screen,
                     observing repositories/engines via StateFlow,
                     zero business logic in Fragments
```

**Why this separation matters for the assignment's goals:** every engine
(`AnomalyEngine`, `IncidentStateMachine`, `BaselineManager`, etc.) is plain Kotlin, testable with
JUnit alone, and has no dependency on Android framework classes. Swapping the DMS simulator for a
real CameraX-based CV model, or the vehicle telemetry simulator for a real OBD-II BLE adapter,
requires implementing the same interface — nothing downstream changes.

**Coroutines & Flow:** used throughout for sensor streams, Room observation, and orchestration.
`SafetyMonitoringService` owns a `CoroutineScope` that survives backgrounding (see §3), and all
long-running orchestration (voice verification retries, responder mobilization radius expansion)
runs as cancellable coroutine jobs tracked by `EmergencyOrchestratorCoordinator`, so a driver's
manual "cancel" action can cleanly interrupt an in-progress verification or mobilization loop.

## 3. Battery & Background Decisions (Section 14)

- **Foreground Service** (`SafetyMonitoringService`) with `foregroundServiceType="location|dataSync"`
  hosts all continuous sensor collection and anomaly evaluation, so monitoring survives app
  minimization and screen-off. A persistent notification ("Safety Monitoring Active") is shown at
  all times per Android's foreground service requirements.
- **Configurable sampling rate** — Settings exposes Normal (1 Hz) vs Battery Saver (0.2 Hz)
  modes, persisted via DataStore (`SamplingRateRepository`), consumed reactively by the sensor
  collection loop.
- **No wake locks are held.** The Foreground Service protects the process from being killed, but
  deliberately does **not** acquire a `PARTIAL_WAKE_LOCK` to keep the CPU awake through Doze. This
  is an intentional battery-preservation tradeoff for a prototype: at 1 Hz/0.2 Hz sampling,
  occasional Doze-induced gaps in background sampling are an acceptable cost. A production
  version would evaluate `setExactAndAllowWhileIdle` alarms or a foreground-service-bound wake
  lock only if missed samples during Doze proved unacceptable in real-world testing.
- **Service & battery status are shown in-app** on the Home screen (monitoring toggle +
  live battery percentage/charging state via `BatteryStatusManager`).

## 4. Offline-First Design (Section 15)

The entire safety workflow — sensor processing, baseline computation, anomaly detection, the
state machine, and incident logging — runs on-device against Room and never depends on network
availability. Voice verification automatically falls back to a local simulated responder
(`VoiceVerificationRouter`) when offline, when `RECORD_AUDIO` isn't granted, or when on-device
recognition is unavailable. A `SyncQueueEntity`/`SyncWorker` (WorkManager, network-constrained)
demonstrates queuing of incidents/anomalies for later synchronization to a backend — since no
real backend exists for this prototype, `SyncWorker` logs and marks items synced rather than
performing a real network call (see `LIMITATIONS.md`). The Home screen shows live Online/Offline
status via `NetworkStatusManager`.

## 5. Further Documentation
- `ARCHITECTURE.md` — diagram + DI wiring
- `DB_SCHEMA.md` — Room schema
- `STATE_MACHINE.md` — emergency state machine transition table
- `TEST_CONTROLS_GUIDE.md` — how to trigger every simulated event
- `TEST_SCENARIOS.md` — Section 18 mandatory scenario results
- `LIMITATIONS.md` — known gaps & production improvements
- `DEMO_VIDEO_SCRIPT.md` — screen-by-screen recording script
