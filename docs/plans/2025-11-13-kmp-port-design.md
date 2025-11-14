# Vitruvian Redux → KMP Port - Complete Design Document

**Date**: 2025-11-13
**Project**: VitruvianProjectPhoenixMP
**Approach**: Full Feature Parity (Approach 2)
**Timeline**: 16-20 weeks
**Platforms**: Android, iOS, Desktop (Windows/macOS/Linux)

---

## Executive Summary

This document outlines the complete architecture and implementation plan for porting **VitruvianRedux** (Android-native Kotlin app) to **Kotlin Multiplatform (KMP)** with full feature parity across Android, iOS, and Desktop platforms.

### Key Decisions

- **Platform Priority**: All three platforms equally (Android, iOS, Desktop)
- **Desktop BLE**: Platform-specific native implementations via JNA
- **Database**: SQLDelight (type-safe SQL, KMP-ready)
- **Dependency Injection**: Koin (multiplatform DI framework)
- **Porting Strategy**: Platform-by-platform (Android → iOS → Desktop)
- **Timeline**: 16-20 weeks (no hard deadline, quality-focused)

### Architecture at a Glance

```
┌─────────────────────────────────────────────────────────┐
│                  Compose Multiplatform UI               │
│                    (95% shared code)                    │
├─────────────────────────────────────────────────────────┤
│                  ViewModels (commonMain)                │
│           StateFlow/SharedFlow reactive state           │
├─────────────────────────────────────────────────────────┤
│              Repository Interfaces (common)             │
├──────────────┬──────────────┬──────────────┬────────────┤
│   Android    │     iOS      │    Desktop   │   Shared   │
│   (actual)   │   (actual)   │   (actual)   │  (common)  │
├──────────────┼──────────────┼──────────────┼────────────┤
│ Nordic BLE   │CoreBluetooth │  JNA BLE     │SQLDelight  │
│ Room→SQLDel  │ SQLDelight   │ SQLDelight   │   Koin DI  │
│ Hilt→Koin    │    Koin      │    Koin      │  Protocol  │
│   Services   │  Background  │ System Tray  │   Builder  │
└──────────────┴──────────────┴──────────────┴────────────┘
```

---

## 1. Overall Architecture & Module Structure

### Project Structure

```
VitruvianProjectPhoenixMP/
├── composeApp/                    # Main KMP application module
│   ├── src/
│   │   ├── commonMain/           # Shared code (80-90% of app)
│   │   │   ├── kotlin/com/vitruvian/
│   │   │   │   ├── domain/       # Pure business logic
│   │   │   │   │   ├── model/    # Models, sealed classes
│   │   │   │   │   └── usecase/  # Rep counter, auto-stop
│   │   │   │   ├── data/         # Repository interfaces
│   │   │   │   │   ├── repository/
│   │   │   │   │   └── ble/      # BLE interface (expect)
│   │   │   │   ├── presentation/ # ViewModels, UI state
│   │   │   │   │   ├── screen/   # Composable screens
│   │   │   │   │   ├── components/
│   │   │   │   │   ├── viewmodel/
│   │   │   │   │   ├── navigation/
│   │   │   │   │   └── theme/
│   │   │   │   └── di/           # Koin modules
│   │   │   └── sqldelight/       # .sq schema files
│   │   │       └── com/vitruvian/database/
│   │   │
│   │   ├── androidMain/          # Android implementations
│   │   │   └── kotlin/com/vitruvian/
│   │   │       ├── ble/          # Nordic BLE wrapper (actual)
│   │   │       ├── platform/     # Android platform code
│   │   │       ├── service/      # Foreground service
│   │   │       └── di/           # Android Koin module
│   │   │
│   │   ├── iosMain/              # iOS implementations
│   │   │   └── kotlin/com/vitruvian/
│   │   │       ├── ble/          # CoreBluetooth wrapper
│   │   │       ├── platform/     # iOS platform code
│   │   │       └── di/           # iOS Koin module
│   │   │
│   │   ├── desktopMain/          # Desktop implementations
│   │   │   └── kotlin/com/vitruvian/
│   │   │       ├── ble/          # JNA BLE wrapper
│   │   │       │   ├── windows/  # WinRT Bluetooth
│   │   │       │   ├── macos/    # CoreBluetooth via JNA
│   │   │       │   └── linux/    # BlueZ D-Bus
│   │   │       ├── platform/     # Desktop platform code
│   │   │       └── di/           # Desktop Koin module
│   │   │
│   │   ├── commonTest/           # Shared tests
│   │   ├── androidTest/          # Android tests
│   │   ├── iosTest/              # iOS tests
│   │   └── desktopTest/          # Desktop tests
│   │
│   └── build.gradle.kts          # KMP configuration
│
├── iosApp/                        # iOS app wrapper
│   └── iosApp.swift              # SwiftUI entry point
│
├── .skills/                       # Quadrumvirate AI tools
├── docs/plans/                    # Design documents
└── gradle/                        # Gradle wrapper
```

### Core Architectural Principles

1. **Expect/Actual Pattern**: Platform-specific code isolated via expect/actual
2. **Repository Pattern**: Interfaces in common, platform implementations delegate to actual classes
3. **Single Source of Truth**: All business logic in commonMain
4. **Compose Multiplatform**: 95% UI shared, platform-specific only where necessary
5. **Reactive Streams**: StateFlow/SharedFlow for all data streams

---

## 2. BLE Abstraction Layer

### The Challenge

BLE is the most complex component - three completely different native APIs:
- **Android**: Nordic BLE Library (wraps BluetoothGatt)
- **iOS**: CoreBluetooth framework
- **Desktop**: JNA to native APIs (WinRT, CoreBluetooth, BlueZ)

### BLE Interface (commonMain)

```kotlin
// commonMain/kotlin/com/vitruvian/data/ble/BleManager.kt
expect class BleManager {
    // Scanning
    fun startScanning(filters: List<ScanFilter> = emptyList())
    fun stopScanning()
    val scanResults: StateFlow<List<BleDevice>>

    // Connection
    suspend fun connect(device: BleDevice): Result<Unit>
    suspend fun disconnect()
    val connectionState: StateFlow<ConnectionState>

    // GATT Operations
    suspend fun discoverServices(): Result<Unit>
    suspend fun enableNotifications(
        serviceUuid: String,
        characteristicUuid: String
    ): Result<Unit>
    suspend fun writeCharacteristic(
        serviceUuid: String,
        characteristicUuid: String,
        data: ByteArray
    ): Result<Unit>

    // Data Streams
    val monitorData: SharedFlow<ByteArray>
    val repNotifications: SharedFlow<Int>
    val propertyData: SharedFlow<ByteArray>

    // Lifecycle
    fun initialize()
    fun cleanup()
}
```

### Platform Implementations

**Android** (`androidMain`):
- Wrap existing Nordic BLE library from VitruvianRedux
- Minimal changes to proven code
- Map callbacks to Kotlin Flows

**iOS** (`iosMain`):
- Implement using CoreBluetooth
- CBCentralManager, CBPeripheral, CBCharacteristic
- Delegate callbacks → Flows

**Desktop** (`desktopMain`):
- **macOS**: JNA → CoreBluetooth (Objective-C bridge)
- **Windows**: JNA → WinRT Bluetooth APIs
- **Linux**: JNA → BlueZ D-Bus API

### Protocol Building (Shared)

```kotlin
// commonMain/kotlin/com/vitruvian/data/ble/ProtocolBuilder.kt
class ProtocolBuilder {
    fun buildInitCommand(): ByteArray { ... }
    fun buildInitPreset(): ByteArray { ... }
    fun buildProgramParams(params: WorkoutParameters): ByteArray { ... }
    fun buildEchoControl(mode: EchoMode): ByteArray { ... }
}

// commonMain/kotlin/com/vitruvian/data/ble/ProtocolParser.kt
object ProtocolParser {
    fun parseMonitorData(bytes: ByteArray): WorkoutMetric { ... }
    fun parseRepNotification(bytes: ByteArray): Int { ... }
}
```

**Key Point**: All binary protocol logic stays in commonMain (pure Kotlin, platform-agnostic)

---

## 3. Database Layer - SQLDelight Migration

### Migration from Room to SQLDelight

**Current State (VitruvianRedux)**:
- Room database (Android-only)
- 10 entities, 16 migration versions
- Complex relationships, foreign keys

**Target State (KMP)**:
- SQLDelight (all platforms)
- Same 10 entities, same 16 migrations
- Type-safe queries, compile-time verification

### SQLDelight Structure

```
commonMain/sqldelight/com/vitruvian/database/
├── WorkoutSession.sq
├── WorkoutMetric.sq
├── Exercise.sq
├── ExerciseVideo.sq
├── Routine.sq
├── RoutineExercise.sq
├── PersonalRecord.sq
├── WeeklyProgram.sq
├── ProgramDay.sq
├── ConnectionLog.sq
└── migrations/
    ├── 1.sqm
    ├── 2.sqm
    └── ... (up to 16.sqm)
```

### Example Schema

```sql
-- WorkoutSession.sq
CREATE TABLE WorkoutSession (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    routineId INTEGER,
    exerciseId INTEGER NOT NULL,
    workoutType TEXT NOT NULL,
    programMode TEXT NOT NULL,
    startTimestamp INTEGER NOT NULL,
    endTimestamp INTEGER,
    totalReps INTEGER DEFAULT 0,
    totalSets INTEGER DEFAULT 0,
    maxLoad REAL DEFAULT 0.0,
    avgLoad REAL DEFAULT 0.0,
    isCompleted INTEGER AS Boolean DEFAULT 0,
    notes TEXT,
    FOREIGN KEY (routineId) REFERENCES Routine(id) ON DELETE SET NULL,
    FOREIGN KEY (exerciseId) REFERENCES Exercise(id) ON DELETE CASCADE
);

-- Queries
insertWorkoutSession:
INSERT INTO WorkoutSession(routineId, exerciseId, workoutType, programMode, startTimestamp)
VALUES (?, ?, ?, ?, ?);

getWorkoutSessionById:
SELECT * FROM WorkoutSession WHERE id = ?;

getAllWorkoutSessions:
SELECT * FROM WorkoutSession ORDER BY startTimestamp DESC;
```

### Platform Drivers

**Android**: AndroidSqliteDriver
**iOS**: NativeSqliteDriver
**Desktop**: JdbcSqliteDriver

All use expect/actual pattern for `DatabaseDriverFactory`.

---

## 4. Dependency Injection - Koin Migration

### Migration from Hilt to Koin

| Hilt (Android-only) | Koin (Multiplatform) |
|---------------------|----------------------|
| `@HiltAndroidApp` | `startKoin { }` |
| `@AndroidEntryPoint` | `koinViewModel<T>()` |
| `@Provides` | `single { }` / `factory { }` |
| `@Singleton` | `single { }` |
| Compile-time DI | Runtime DI |

### Koin Module Structure

```kotlin
// commonMain/kotlin/com/vitruvian/di/CommonModule.kt
val commonModule = module {
    // Database
    single { get<DatabaseDriverFactory>().createDriver() }
    single { VitruvianDatabase(driver = get()) }

    // Repositories
    single<WorkoutRepository> { WorkoutRepositoryImpl(get()) }
    single<ExerciseRepository> { ExerciseRepositoryImpl(get()) }
    single<BleRepository> { BleRepositoryImpl(get()) }

    // ViewModels
    viewModel { MainViewModel(get(), get(), get(), get()) }
    viewModel { ExerciseLibraryViewModel(get()) }

    // Utilities
    single { ProtocolBuilder() }
}

// androidMain/kotlin/com/vitruvian/di/AndroidModule.kt
val androidModule = module {
    single<DatabaseDriverFactory> { DatabaseDriverFactory(androidContext()) }
    single<BleManager> { BleManager(androidContext()) }
}
```

### Initialization

**Android**: In `Application.onCreate()`
**iOS**: In Swift app init, call `initKoin()`
**Desktop**: In `main()` before window creation

---

## 5. UI Layer - Compose Multiplatform

### Shared UI (95% commonMain)

All screens, components, navigation in commonMain:
- EnhancedMainScreen.kt
- ActiveWorkoutScreen.kt
- ExerciseLibraryScreen.kt
- AnalyticsScreen.kt
- ProgramBuilderScreen.kt
- ... (15+ screens total)

### Platform-Specific UI (5%)

Only for:
- Permission requests
- File pickers
- Platform-specific notifications

Use expect/actual:
```kotlin
expect @Composable fun PlatformSpecificPermissions()
expect @Composable fun PlatformFilePicker(onFileSelected: (String) -> Unit)
```

### Material 3 Theme (Shared)

```kotlin
// commonMain/kotlin/com/vitruvian/presentation/theme/Theme.kt
@Composable
fun VitruvianTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) darkColorScheme(...) else lightColorScheme(...)
    MaterialTheme(colorScheme, typography, content)
}
```

### Charts

Use **Vico** (already in VitruvianRedux, KMP-ready):
```kotlin
implementation("com.patrykandpatrick.vico:compose:2.0.0-alpha.28")
implementation("com.patrykandpatrick.vico:compose-m3:2.0.0-alpha.28")
```

---

## 6. Platform-Specific Features

### Android

**Foreground Service**:
- Keeps BLE connection alive during workouts
- Wake lock for screen on
- Notification channel for workout status

**Permissions**:
- BLUETOOTH_SCAN, BLUETOOTH_CONNECT
- ACCESS_FINE_LOCATION
- POST_NOTIFICATIONS (Android 13+)

**AndroidManifest.xml**:
```xml
<uses-permission android:name="android.permission.BLUETOOTH_SCAN" />
<uses-permission android:name="android.permission.BLUETOOTH_CONNECT" />
<uses-permission android:name="android.permission.ACCESS_FINE_LOCATION" />
<uses-permission android:name="android.permission.FOREGROUND_SERVICE" />
<uses-permission android:name="android.permission.WAKE_LOCK" />
```

### iOS

**Background Modes** (Info.plist):
```xml
<key>UIBackgroundModes</key>
<array>
    <string>bluetooth-central</string>
</array>

<key>NSBluetoothAlwaysUsageDescription</key>
<string>Vitruvian needs Bluetooth to connect to your fitness equipment.</string>
```

**Notifications**: UNUserNotificationCenter
**Wake Lock**: `UIApplication.sharedApplication.idleTimerDisabled = true`

### Desktop

**BLE**: Platform detection → macOS/Windows/Linux implementations
**Notifications**: System tray notifications
**Storage**: User home directory (`~/.vitruvian/`)
**No background service needed** (app always foreground)

---

## 7. Desktop BLE Implementation (JNA Strategy)

### Architecture

```
desktopMain/kotlin/com/vitruvian/ble/
├── BleManager.kt                  # Facade (platform detection)
├── windows/WinRTBleManager.kt    # Windows via WinRT
├── macos/CoreBluetoothManager.kt # macOS via CoreBluetooth
└── linux/BlueZManager.kt         # Linux via BlueZ D-Bus
```

### Platform Detection

```kotlin
actual class BleManager {
    private val platformManager: PlatformBleManager = when {
        isWindows() -> WinRTBleManager()
        isMacOS() -> CoreBluetoothManager()
        isLinux() -> BlueZManager()
        else -> throw UnsupportedOperationException("BLE not supported")
    }

    // Delegate all operations to platform-specific manager
}
```

### Implementation Complexity

- **macOS**: Moderate (JNA → Objective-C CoreBluetooth)
- **Windows**: High (JNA → WinRT Bluetooth APIs)
- **Linux**: Moderate (D-Bus to BlueZ)

### Phased Approach (Pragmatic)

**Phase 1 (v1.0)**: macOS only
**Phase 2 (v1.1)**: Add Windows support
**Phase 3 (v1.2)**: Add Linux support

**Alternative**: Use TinyB library (Java BLE, limited platform support)

---

## 8. Testing Strategy

### Test Structure

```
commonTest/       # Shared tests (business logic, ViewModels)
androidTest/      # Android-specific (BLE, database migrations)
iosTest/          # iOS-specific (CoreBluetooth)
desktopTest/      # Desktop-specific (JNA BLE)
```

### Unit Tests (commonTest)

- ProtocolBuilder: Binary frame generation
- ProtocolParser: Monitor data parsing
- RepCounterFromMachine: Rep detection logic
- WorkoutRepository: CRUD operations
- MainViewModel: State transitions

### Platform Tests

**Android**:
- BLE scanning/connection with real device
- Database migration 1→16 validation
- Foreground service behavior

**iOS**:
- CoreBluetooth scanning/connection
- Background mode BLE persistence
- Permission flows

**Desktop**:
- JNA BLE operations (per platform)
- File storage in user home
- System tray notifications

### Integration Tests

- End-to-end workout flow
- BLE data stream → database persistence
- PR detection and celebration
- Auto-stop functionality

### Manual Testing Checklist

- [ ] Device scanning on all platforms
- [ ] BLE connection reliability
- [ ] All 6 workout modes functional
- [ ] Rep counting accuracy (±1 rep)
- [ ] Auto-stop detection
- [ ] Just Lift mode auto-start
- [ ] PR detection and notifications
- [ ] Analytics charts display correctly
- [ ] Database persistence across app restarts

---

## 9. Implementation Roadmap

### Timeline: 16-20 Weeks

#### PHASE 1: Foundation (Weeks 1-4)

**Week 1: Project Setup & Domain Layer**
- Create KMP module structure
- Move domain models to commonMain
- Move ProtocolBuilder to commonMain
- Set up Koin DI skeleton
- **Deliverable**: Domain layer compiling for all targets

**Week 2: SQLDelight Core Schema**
- Add SQLDelight plugin
- Create core entities (WorkoutSession, WorkoutMetric, Exercise, Routine)
- Implement DatabaseDriverFactory (expect/actual)
- **Deliverable**: Core database working on Android

**Week 3: Complete SQLDelight Migration**
- Create all 10 entity schemas
- Port all 16 migration versions
- Write comprehensive queries
- **Deliverable**: Complete database with migrations

**Week 4: Repository Layer & Testing**
- Implement all repositories
- Write unit tests (ProtocolBuilder, repositories)
- Validate CRUD operations
- **Deliverable**: Fully tested repository layer

#### PHASE 2: Android (Weeks 5-8)

**Week 5: Android BLE Wrapper**
- Create BleManager interface (commonMain)
- Implement Android actual (Nordic BLE)
- Map callbacks to Flows
- **Deliverable**: Android BLE scanning/connecting

**Week 6: Android BLE GATT**
- Implement read/write/notify
- Port monitor/property polling
- Test with real device
- **Deliverable**: Full Android BLE functionality

**Week 7: Android Platform Services**
- WorkoutForegroundService
- NotificationService, PermissionManager
- AndroidManifest configuration
- **Deliverable**: Android platform integration

**Week 8: Android UI & ViewModels**
- Implement ViewModels in commonMain
- Create core screens
- End-to-end workout flow
- **Deliverable**: Android feature parity

#### PHASE 3: iOS (Weeks 9-13)

**Week 9: iOS BLE Foundation**
- iOS actual for BleManager
- CBCentralManager implementation
- Scanning and connection
- **Deliverable**: iOS BLE scanning

**Week 10: iOS BLE GATT**
- Service/characteristic discovery
- Read/write/notify operations
- Test on real iPhone
- **Deliverable**: iOS BLE fully functional

**Week 11: iOS Platform Services**
- Info.plist configuration
- NotificationService (UNNotificationCenter)
- Background mode testing
- **Deliverable**: iOS platform integration

**Week 12: iOS UI Integration**
- SwiftUI → Compose bridge
- Test all screens
- Navigation flow
- **Deliverable**: iOS UI working

**Week 13: iOS Testing & Polish**
- End-to-end workout testing
- All 6 workout modes
- Performance optimization
- **Deliverable**: iOS feature parity

#### PHASE 4: Desktop (Weeks 14-18)

**Week 14: Desktop BLE - macOS Foundation**
- Set up JNA dependencies
- Objective-C runtime bridge
- CBCentralManager via JNA
- **Deliverable**: macOS BLE scanning

**Week 15: Desktop BLE - macOS GATT**
- CBPeripheral via JNA
- GATT operations
- Test with real device
- **Deliverable**: macOS BLE fully functional

**Week 16: Desktop BLE - Windows (Optional)**
- WinRT Bluetooth via JNA OR defer to v1.1
- **Deliverable**: Windows foundation or deferred

**Week 17: Desktop Platform & UI**
- Desktop file storage
- System tray notifications
- Compose Desktop UI
- **Deliverable**: Desktop app running

**Week 18: Desktop Testing & Polish**
- End-to-end testing on macOS
- Fix desktop-specific bugs
- UI optimization for larger screens
- **Deliverable**: Desktop (macOS) feature parity

#### PHASE 5: Release (Weeks 19-20)

**Week 19: Cross-Platform Testing**
- All screens on all platforms
- All workout modes
- Database migrations
- Performance testing
- **Deliverable**: All critical bugs fixed

**Week 20: Documentation & Release**
- User documentation
- Release notes
- CI/CD setup (Android/iOS/Desktop)
- **Deliverable**: v1.0 ready for release

---

## 10. Key Milestones

| Week | Milestone | Validation |
|------|-----------|------------|
| 4 | Foundation complete | All repositories tested, database migrated |
| 8 | Android parity | Full workout matches VitruvianRedux |
| 13 | iOS parity | Full workout matches Android |
| 18 | Desktop parity | Full workout on macOS matches mobile |
| 20 | v1.0 Release | All platforms shipped |

---

## 11. Risk Mitigation

### High-Risk Items

**1. Desktop BLE Complexity** (Weeks 14-16)
- **Risk**: JNA implementation harder than expected
- **Mitigation**: Start macOS only, defer Windows/Linux
- **Fallback**: Desktop analytics-only mode

**2. SQLDelight Migration** (Weeks 2-3)
- **Risk**: Migration errors cause data loss
- **Mitigation**: Extensive testing with migration validator
- **Fallback**: Start fresh schema

**3. iOS BLE Background** (Week 11)
- **Risk**: iOS terminates BLE in background
- **Mitigation**: Proper background mode config, keep-alive pings
- **Fallback**: Require foreground during workouts

**4. Performance** (Week 19)
- **Risk**: 100ms polling drains battery
- **Mitigation**: Profile and optimize
- **Fallback**: Reduce to 200ms

---

## 12. Success Criteria

### v1.0 Release Requirements

✅ **Feature Parity**:
- All 6 workout modes on all platforms
- All 15+ screens on all platforms
- BLE reliable on all platforms (macOS for desktop)
- Database persistence working
- Rep counting accurate (±1 rep)
- PR detection and notifications

✅ **Quality**:
- No critical bugs
- <5% crash rate
- BLE connection success >90%
- Unit test coverage >70% (shared code)
- Manual testing 100% passed

✅ **Performance**:
- App launch <3 seconds
- BLE connection <5 seconds
- UI responsive (60fps) during workouts
- Battery drain <10%/hour

---

## 13. Post-v1.0 Roadmap

**v1.1 - Desktop Expansion**:
- Windows BLE (WinRT via JNA)
- Linux BLE (BlueZ via D-Bus)

**v1.2 - Cloud Sync**:
- Cloud backup
- Cross-device sync
- Web dashboard

**v1.3 - Advanced Features**:
- Video coaching
- Social features
- ML predictions

---

## Conclusion

This design provides a comprehensive blueprint for porting VitruvianRedux to Kotlin Multiplatform with **full feature parity** across Android, iOS, and Desktop platforms.

**Key Strengths**:
- Clean architecture with well-defined layers
- Realistic timeline with phased approach
- Risk mitigation strategies
- Clear success criteria

**Next Steps**:
1. Review and approve this design
2. Create detailed implementation plan with bite-sized tasks
3. Begin Week 1: Project setup and domain layer migration

**Estimated Effort**: 16-20 weeks for full parity across all 3 platforms.

---

**Document Version**: 1.0
**Last Updated**: 2025-11-13
**Author**: Claude Code (with Quadrumvirate framework)
