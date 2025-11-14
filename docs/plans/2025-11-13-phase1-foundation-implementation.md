# KMP Port Phase 1: Foundation & Shared Code - Implementation Plan

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** Establish the shared foundation (domain layer, database, repositories) for the Kotlin Multiplatform port of VitruvianRedux, compiling for Android, iOS, and Desktop.

**Architecture:** Extract platform-agnostic business logic (domain models, protocol builder, use cases) into commonMain. Migrate Room database to SQLDelight with all 16 schema versions. Implement repositories using SQLDelight with Koin DI.

**Tech Stack:**
- Kotlin Multiplatform 2.0.21
- SQLDelight 2.0.1 (multiplatform database)
- Koin 3.5.0 (multiplatform DI)
- Kotlinx Coroutines 1.8.1
- Kotlinx DateTime 0.5.0

**Source Reference:** `C:\Users\dasbl\AndroidStudioProjects\VitruvianRedux`

**Timeline:** 4 weeks (Weeks 1-4 of overall 16-20 week plan)

---

## Week 1: Project Setup & Domain Layer

### Task 1.1: Configure KMP Module Structure

**Files:**
- Modify: `composeApp/build.gradle.kts`
- Verify: Project builds for all targets

**Step 1: Add kotlin sourceSets configuration**

Open `composeApp/build.gradle.kts` and verify the kotlin multiplatform configuration includes all targets:

```kotlin
kotlin {
    androidTarget {
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_11)
        }
    }

    listOf(
        iosArm64(),
        iosSimulatorArm64()
    ).forEach { iosTarget ->
        iosTarget.binaries.framework {
            baseName = "ComposeApp"
            isStatic = true
        }
    }

    jvm("desktop")

    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation(compose.runtime)
                implementation(compose.foundation)
                implementation(compose.material3)
                implementation(compose.ui)
                implementation(compose.components.resources)
                implementation(compose.components.uiToolingPreview)
                implementation(libs.androidx.lifecycle.viewmodelCompose)
                implementation(libs.androidx.lifecycle.runtimeCompose)

                // Kotlinx dependencies
                implementation(libs.kotlinx.coroutines.core)
                implementation(libs.kotlinx.datetime)
            }
        }

        val androidMain by getting {
            dependencies {
                implementation(compose.preview)
                implementation(libs.androidx.activity.compose)
            }
        }

        val iosMain by creating {
            dependsOn(commonMain)
        }

        val iosArm64Main by getting {
            dependsOn(iosMain)
        }

        val iosSimulatorArm64Main by getting {
            dependsOn(iosMain)
        }

        val desktopMain by getting {
            dependencies {
                implementation(compose.desktop.currentOs)
            }
        }

        val commonTest by getting {
            dependencies {
                implementation(libs.kotlin.test)
                implementation(libs.kotlinx.coroutines.test)
            }
        }
    }
}
```

**Step 2: Verify build configuration**

Run: `./gradlew build`
Expected: Build succeeds for all targets (android, ios, desktop)

**Step 3: Commit**

```bash
git add composeApp/build.gradle.kts
git commit -m "build: configure KMP targets (android, ios, desktop)"
```

---

### Task 1.2: Create Package Structure

**Files:**
- Create: `composeApp/src/commonMain/kotlin/com/vitruvian/domain/model/`
- Create: `composeApp/src/commonMain/kotlin/com/vitruvian/domain/usecase/`
- Create: `composeApp/src/commonMain/kotlin/com/vitruvian/data/repository/`
- Create: `composeApp/src/commonMain/kotlin/com/vitruvian/data/ble/`
- Create: `composeApp/src/commonMain/kotlin/com/vitruvian/util/`
- Create: `composeApp/src/commonMain/kotlin/com/vitruvian/di/`

**Step 1: Create directory structure**

Run:
```bash
mkdir -p composeApp/src/commonMain/kotlin/com/vitruvian/domain/model
mkdir -p composeApp/src/commonMain/kotlin/com/vitruvian/domain/usecase
mkdir -p composeApp/src/commonMain/kotlin/com/vitruvian/data/repository
mkdir -p composeApp/src/commonMain/kotlin/com/vitruvian/data/ble
mkdir -p composeApp/src/commonMain/kotlin/com/vitruvian/util
mkdir -p composeApp/src/commonMain/kotlin/com/vitruvian/di
mkdir -p composeApp/src/commonTest/kotlin/com/vitruvian
```

**Step 2: Verify structure**

Run: `ls -R composeApp/src/commonMain/kotlin/com/vitruvian/`
Expected: All directories created

**Step 3: Commit**

```bash
git add composeApp/src/
git commit -m "chore: create commonMain package structure"
```

---

### Task 1.3: Migrate Domain Models

**Files:**
- Create: `composeApp/src/commonMain/kotlin/com/vitruvian/domain/model/Models.kt`
- Reference: `C:\Users\dasbl\AndroidStudioProjects\VitruvianRedux\app\src\main\java\com\example\vitruvianredux\domain\model\Models.kt`

**Step 1: Copy domain models from VitruvianRedux**

Create `composeApp/src/commonMain/kotlin/com/vitruvian/domain/model/Models.kt`:

```kotlin
package com.vitruvian.domain.model

import kotlinx.datetime.Instant

// Connection states
sealed class ConnectionState {
    data object Disconnected : ConnectionState()
    data object Scanning : ConnectionState()
    data object Connecting : ConnectionState()
    data object Connected : ConnectionState()
    data class Error(val message: String) : ConnectionState()
}

// Workout states
sealed class WorkoutState {
    data object Idle : WorkoutState()
    data object Preparing : WorkoutState()
    data class Active(
        val exerciseId: Long,
        val exerciseName: String,
        val currentSet: Int,
        val totalSets: Int,
        val currentRep: Int = 0,
        val maxLoad: Float = 0f,
        val avgLoad: Float = 0f
    ) : WorkoutState()
    data object Paused : WorkoutState()
    data class Completed(
        val sessionId: Long,
        val totalReps: Int,
        val totalSets: Int,
        val maxLoad: Float,
        val duration: Long
    ) : WorkoutState()
}

// Workout types
sealed class WorkoutType(val displayName: String) {
    data object OldSchool : WorkoutType("Old School")
    data object Pump : WorkoutType("Pump")
    data object TUT : WorkoutType("Time Under Tension")
    data object TUTBeast : WorkoutType("TUT Beast")
    data object Eccentric : WorkoutType("Eccentric")
    data object Echo : WorkoutType("Echo")

    companion object {
        fun fromString(value: String): WorkoutType = when (value) {
            "OldSchool" -> OldSchool
            "Pump" -> Pump
            "TUT" -> TUT
            "TUTBeast" -> TUTBeast
            "Eccentric" -> Eccentric
            "Echo" -> Echo
            else -> OldSchool
        }
    }
}

// Program modes
sealed class ProgramMode(val displayName: String) {
    data object Concentric : ProgramMode("Concentric")
    data object Eccentric : ProgramMode("Eccentric")
    data object ConcentricEccentric : ProgramMode("Concentric + Eccentric")
    data object Isometric : ProgramMode("Isometric")

    companion object {
        fun fromString(value: String): ProgramMode = when (value) {
            "Concentric" -> Concentric
            "Eccentric" -> Eccentric
            "ConcentricEccentric" -> ConcentricEccentric
            "Isometric" -> Isometric
            else -> Concentric
        }
    }
}

// BLE Device
data class BleDevice(
    val address: String,
    val name: String?,
    val rssi: Int = 0
)

// Workout Metric
data class WorkoutMetric(
    val sessionId: Long = 0,
    val timestamp: Long,
    val position: Int,
    val load: Float,
    val ticks: Int,
    val repNumber: Int = 0,
    val setNumber: Int = 0
)

// Workout Parameters
data class WorkoutParameters(
    val workoutType: WorkoutType,
    val programMode: ProgramMode,
    val load: Float,
    val reps: Int,
    val sets: Int = 1,
    val restTime: Int = 60,
    val tempo: String = "2-0-2-0",
    val stopAtTop: Boolean = false
)
```

**Step 2: Verify compilation**

Run: `./gradlew :composeApp:compileCommonMainKotlinMetadata`
Expected: Builds successfully

**Step 3: Commit**

```bash
git add composeApp/src/commonMain/kotlin/com/vitruvian/domain/model/Models.kt
git commit -m "feat(domain): add core domain models (ConnectionState, WorkoutState, etc.)"
```

---

### Task 1.4: Migrate Exercise Model

**Files:**
- Create: `composeApp/src/commonMain/kotlin/com/vitruvian/domain/model/Exercise.kt`
- Reference: `C:\Users\dasbl\AndroidStudioProjects\VitruvianRedux\app\src\main\java\com\example\vitruvianredux\domain\model\Exercise.kt`

**Step 1: Create Exercise model**

Create `composeApp/src/commonMain/kotlin/com/vitruvian/domain/model/Exercise.kt`:

```kotlin
package com.vitruvian.domain.model

data class Exercise(
    val id: Long = 0,
    val name: String,
    val category: String = "",
    val muscleGroup: String = "",
    val equipment: String = "Vitruvian",
    val difficulty: String = "Intermediate",
    val description: String = "",
    val instructions: String = "",
    val videoUrl: String? = null,
    val thumbnailUrl: String? = null,
    val isCustom: Boolean = false,
    val isFavorite: Boolean = false,
    val createdAt: Long = 0
)

data class ExerciseVideo(
    val id: Long = 0,
    val exerciseId: Long,
    val url: String,
    val title: String,
    val duration: Int = 0,
    val thumbnailUrl: String? = null
)

// Exercise categories
object ExerciseCategory {
    const val UPPER_BODY = "Upper Body"
    const val LOWER_BODY = "Lower Body"
    const val CORE = "Core"
    const val FULL_BODY = "Full Body"
    const val CARDIO = "Cardio"

    val ALL = listOf(UPPER_BODY, LOWER_BODY, CORE, FULL_BODY, CARDIO)
}

// Muscle groups
object MuscleGroup {
    const val CHEST = "Chest"
    const val BACK = "Back"
    const val SHOULDERS = "Shoulders"
    const val ARMS = "Arms"
    const val LEGS = "Legs"
    const val GLUTES = "Glutes"
    const val ABS = "Abs"

    val ALL = listOf(CHEST, BACK, SHOULDERS, ARMS, LEGS, GLUTES, ABS)
}
```

**Step 2: Verify compilation**

Run: `./gradlew :composeApp:compileCommonMainKotlinMetadata`
Expected: Builds successfully

**Step 3: Commit**

```bash
git add composeApp/src/commonMain/kotlin/com/vitruvian/domain/model/Exercise.kt
git commit -m "feat(domain): add Exercise model and categories"
```

---

### Task 1.5: Migrate Routine Models

**Files:**
- Create: `composeApp/src/commonMain/kotlin/com/vitruvian/domain/model/Routine.kt`

**Step 1: Create Routine models**

Create `composeApp/src/commonMain/kotlin/com/vitruvian/domain/model/Routine.kt`:

```kotlin
package com.vitruvian.domain.model

data class Routine(
    val id: Long = 0,
    val name: String,
    val description: String = "",
    val exercises: List<RoutineExercise> = emptyList(),
    val isCustom: Boolean = true,
    val isFavorite: Boolean = false,
    val createdAt: Long = 0,
    val lastUsed: Long = 0
)

data class RoutineExercise(
    val id: Long = 0,
    val routineId: Long,
    val exerciseId: Long,
    val exerciseName: String,
    val orderIndex: Int,
    val sets: Int = 3,
    val reps: Int = 10,
    val load: Float = 50f,
    val restTime: Int = 60,
    val notes: String = ""
)

data class WeeklyProgram(
    val id: Long = 0,
    val name: String,
    val description: String = "",
    val isActive: Boolean = false,
    val createdAt: Long = 0
)

data class ProgramDay(
    val id: Long = 0,
    val programId: Long,
    val dayOfWeek: Int, // 1-7 (Monday-Sunday)
    val routineId: Long,
    val routineName: String = ""
)
```

**Step 2: Verify compilation**

Run: `./gradlew :composeApp:compileCommonMainKotlinMetadata`
Expected: Builds successfully

**Step 3: Commit**

```bash
git add composeApp/src/commonMain/kotlin/com/vitruvian/domain/model/Routine.kt
git commit -m "feat(domain): add Routine and WeeklyProgram models"
```

---

### Task 1.6: Migrate ProtocolBuilder

**Files:**
- Create: `composeApp/src/commonMain/kotlin/com/vitruvian/util/ProtocolBuilder.kt`
- Reference: `C:\Users\dasbl\AndroidStudioProjects\VitruvianRedux\app\src\main\java\com\example\vitruvianredux\util\ProtocolBuilder.kt`

**Step 1: Create ProtocolBuilder (pure Kotlin, no Android dependencies)**

Create `composeApp/src/commonMain/kotlin/com/vitruvian/util/ProtocolBuilder.kt`:

```kotlin
package com.vitruvian.util

import com.vitruvian.domain.model.WorkoutParameters
import com.vitruvian.domain.model.WorkoutType
import com.vitruvian.domain.model.ProgramMode
import kotlin.experimental.or

class ProtocolBuilder {

    fun buildInitCommand(): ByteArray {
        return byteArrayOf(
            0x01.toByte(), // Command ID: Init
            0x00,
            0x00,
            0x00
        )
    }

    fun buildInitPreset(): ByteArray {
        val frame = ByteArray(34)
        frame[0] = 0x02.toByte() // Command ID: Init Preset
        // Rest filled with zeros (default preset)
        return frame
    }

    fun buildProgramParams(params: WorkoutParameters): ByteArray {
        val frame = ByteArray(96)

        // Command ID
        frame[0] = 0x03.toByte() // Program params

        // Workout type (byte 1)
        frame[1] = when (params.workoutType) {
            is WorkoutType.OldSchool -> 0x00
            is WorkoutType.Pump -> 0x01
            is WorkoutType.TUT -> 0x02
            is WorkoutType.TUTBeast -> 0x03
            is WorkoutType.Eccentric -> 0x04
            is WorkoutType.Echo -> 0x05
        }.toByte()

        // Program mode (byte 2)
        frame[2] = when (params.programMode) {
            is ProgramMode.Concentric -> 0x00
            is ProgramMode.Eccentric -> 0x01
            is ProgramMode.ConcentricEccentric -> 0x02
            is ProgramMode.Isometric -> 0x03
        }.toByte()

        // Load (bytes 8-11, little-endian float)
        writeLittleEndianFloat(frame, 8, params.load)

        // Reps (bytes 12-13, little-endian short)
        writeLittleEndianShort(frame, 12, params.reps.toShort())

        // Sets (byte 14)
        frame[14] = params.sets.toByte()

        // Rest time (bytes 16-17, little-endian short, in seconds)
        writeLittleEndianShort(frame, 16, params.restTime.toShort())

        // Stop at top (byte 20)
        frame[20] = if (params.stopAtTop) 0x01 else 0x00

        return frame
    }

    fun buildEchoControl(mode: EchoMode): ByteArray {
        val frame = ByteArray(32)
        frame[0] = 0x04.toByte() // Echo control command

        frame[1] = when (mode) {
            EchoMode.OFF -> 0x00
            EchoMode.MIRROR -> 0x01
            EchoMode.CONTRAST -> 0x02
        }.toByte()

        return frame
    }

    private fun writeLittleEndianFloat(buffer: ByteArray, offset: Int, value: Float) {
        val bits = value.toBits()
        buffer[offset] = (bits and 0xFF).toByte()
        buffer[offset + 1] = ((bits shr 8) and 0xFF).toByte()
        buffer[offset + 2] = ((bits shr 16) and 0xFF).toByte()
        buffer[offset + 3] = ((bits shr 24) and 0xFF).toByte()
    }

    private fun writeLittleEndianShort(buffer: ByteArray, offset: Int, value: Short) {
        buffer[offset] = (value.toInt() and 0xFF).toByte()
        buffer[offset + 1] = ((value.toInt() shr 8) and 0xFF).toByte()
    }
}

enum class EchoMode {
    OFF,
    MIRROR,
    CONTRAST
}
```

**Step 2: Verify compilation**

Run: `./gradlew :composeApp:compileCommonMainKotlinMetadata`
Expected: Builds successfully

**Step 3: Commit**

```bash
git add composeApp/src/commonMain/kotlin/com/vitruvian/util/ProtocolBuilder.kt
git commit -m "feat(util): add ProtocolBuilder for BLE command frames"
```

---

### Task 1.7: Test ProtocolBuilder

**Files:**
- Create: `composeApp/src/commonTest/kotlin/com/vitruvian/util/ProtocolBuilderTest.kt`

**Step 1: Write tests for ProtocolBuilder**

Create `composeApp/src/commonTest/kotlin/com/vitruvian/util/ProtocolBuilderTest.kt`:

```kotlin
package com.vitruvian.util

import com.vitruvian.domain.model.WorkoutParameters
import com.vitruvian.domain.model.WorkoutType
import com.vitruvian.domain.model.ProgramMode
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ProtocolBuilderTest {

    private val protocolBuilder = ProtocolBuilder()

    @Test
    fun `buildInitCommand creates 4-byte frame with correct command ID`() {
        val frame = protocolBuilder.buildInitCommand()

        assertEquals(4, frame.size)
        assertEquals(0x01.toByte(), frame[0])
    }

    @Test
    fun `buildInitPreset creates 34-byte frame`() {
        val frame = protocolBuilder.buildInitPreset()

        assertEquals(34, frame.size)
        assertEquals(0x02.toByte(), frame[0])
    }

    @Test
    fun `buildProgramParams creates 96-byte frame`() {
        val params = WorkoutParameters(
            workoutType = WorkoutType.OldSchool,
            programMode = ProgramMode.Concentric,
            load = 50.0f,
            reps = 10,
            sets = 3,
            restTime = 60
        )

        val frame = protocolBuilder.buildProgramParams(params)

        assertEquals(96, frame.size)
        assertEquals(0x03.toByte(), frame[0])
    }

    @Test
    fun `buildProgramParams encodes workout type correctly`() {
        val params = WorkoutParameters(
            workoutType = WorkoutType.Pump,
            programMode = ProgramMode.Concentric,
            load = 50.0f,
            reps = 10
        )

        val frame = protocolBuilder.buildProgramParams(params)

        assertEquals(0x01.toByte(), frame[1]) // Pump = 0x01
    }

    @Test
    fun `buildProgramParams encodes load as little-endian float`() {
        val params = WorkoutParameters(
            workoutType = WorkoutType.OldSchool,
            programMode = ProgramMode.Concentric,
            load = 25.5f,
            reps = 10
        )

        val frame = protocolBuilder.buildProgramParams(params)

        // Verify load at bytes 8-11 (little-endian)
        val loadBits = (frame[8].toInt() and 0xFF) or
                       ((frame[9].toInt() and 0xFF) shl 8) or
                       ((frame[10].toInt() and 0xFF) shl 16) or
                       ((frame[11].toInt() and 0xFF) shl 24)

        val loadValue = Float.fromBits(loadBits)
        assertEquals(25.5f, loadValue, 0.01f)
    }

    @Test
    fun `buildEchoControl creates 32-byte frame`() {
        val frame = protocolBuilder.buildEchoControl(EchoMode.MIRROR)

        assertEquals(32, frame.size)
        assertEquals(0x04.toByte(), frame[0])
        assertEquals(0x01.toByte(), frame[1]) // MIRROR = 0x01
    }
}
```

**Step 2: Run tests**

Run: `./gradlew :composeApp:testDebugUnitTest`
Expected: All tests PASS

**Step 3: Commit**

```bash
git add composeApp/src/commonTest/kotlin/com/vitruvian/util/ProtocolBuilderTest.kt
git commit -m "test(util): add ProtocolBuilder tests"
```

---

## Week 2: SQLDelight Setup & Core Schema

### Task 2.1: Add SQLDelight Dependencies

**Files:**
- Modify: `gradle/libs.versions.toml`
- Modify: `composeApp/build.gradle.kts`

**Step 1: Add SQLDelight version to version catalog**

Edit `gradle/libs.versions.toml`, add to `[versions]` section:

```toml
sqldelight = "2.0.1"
```

Add to `[libraries]` section:

```toml
sqldelight-runtime = { module = "app.cash.sqldelight:runtime", version.ref = "sqldelight" }
sqldelight-coroutines = { module = "app.cash.sqldelight:coroutines-extensions", version.ref = "sqldelight" }
sqldelight-android-driver = { module = "app.cash.sqldelight:android-driver", version.ref = "sqldelight" }
sqldelight-native-driver = { module = "app.cash.sqldelight:native-driver", version.ref = "sqldelight" }
sqldelight-sqlite-driver = { module = "app.cash.sqldelight:sqlite-driver", version.ref = "sqldelight" }
```

Add to `[plugins]` section:

```toml
sqldelight = { id = "app.cash.sqldelight", version.ref = "sqldelight" }
```

**Step 2: Add SQLDelight plugin to build.gradle.kts**

Edit `composeApp/build.gradle.kts`, add to `plugins` block:

```kotlin
plugins {
    // ... existing plugins
    alias(libs.plugins.sqldelight)
}
```

Add to sourceSets:

```kotlin
sourceSets {
    val commonMain by getting {
        dependencies {
            // ... existing dependencies
            implementation(libs.sqldelight.runtime)
            implementation(libs.sqldelight.coroutines)
        }
    }

    val androidMain by getting {
        dependencies {
            // ... existing dependencies
            implementation(libs.sqldelight.android.driver)
        }
    }

    val iosMain by creating {
        dependencies {
            implementation(libs.sqldelight.native.driver)
        }
    }

    val desktopMain by getting {
        dependencies {
            implementation(libs.sqldelight.sqlite.driver)
        }
    }
}
```

Add SQLDelight configuration at the end of the file:

```kotlin
sqldelight {
    databases {
        create("VitruvianDatabase") {
            packageName.set("com.vitruvian.database")
            srcDirs.setFrom("src/commonMain/sqldelight")
        }
    }
}
```

**Step 3: Sync Gradle**

Run: `./gradlew --refresh-dependencies`
Expected: Dependencies downloaded successfully

**Step 4: Commit**

```bash
git add gradle/libs.versions.toml composeApp/build.gradle.kts
git commit -m "build: add SQLDelight dependencies and configuration"
```

---

### Task 2.2: Create Database Directory Structure

**Files:**
- Create: `composeApp/src/commonMain/sqldelight/com/vitruvian/database/`

**Step 1: Create sqldelight directory**

Run:
```bash
mkdir -p composeApp/src/commonMain/sqldelight/com/vitruvian/database
mkdir -p composeApp/src/commonMain/sqldelight/com/vitruvian/database/migrations
```

**Step 2: Verify structure**

Run: `ls -R composeApp/src/commonMain/sqldelight/`
Expected: Directory structure created

**Step 3: Commit**

```bash
git add composeApp/src/commonMain/sqldelight/
git commit -m "chore: create SQLDelight schema directory"
```

---

### Task 2.3: Create WorkoutSession Schema

**Files:**
- Create: `composeApp/src/commonMain/sqldelight/com/vitruvian/database/WorkoutSession.sq`

**Step 1: Create WorkoutSession table and queries**

Create `composeApp/src/commonMain/sqldelight/com/vitruvian/database/WorkoutSession.sq`:

```sql
-- WorkoutSession table definition
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
    FOREIGN KEY (exerciseId) REFERENCES Exercise(id) ON DELETE CASCADE
);

CREATE INDEX idx_session_exercise ON WorkoutSession(exerciseId);
CREATE INDEX idx_session_timestamp ON WorkoutSession(startTimestamp);

-- Queries
insertWorkoutSession:
INSERT INTO WorkoutSession(
    routineId,
    exerciseId,
    workoutType,
    programMode,
    startTimestamp,
    endTimestamp,
    totalReps,
    totalSets,
    maxLoad,
    avgLoad,
    isCompleted,
    notes
) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?);

getWorkoutSessionById:
SELECT * FROM WorkoutSession WHERE id = ?;

getAllWorkoutSessions:
SELECT * FROM WorkoutSession ORDER BY startTimestamp DESC;

getWorkoutSessionsByDateRange:
SELECT * FROM WorkoutSession
WHERE startTimestamp >= ? AND startTimestamp <= ?
ORDER BY startTimestamp DESC;

getRecentWorkouts:
SELECT * FROM WorkoutSession
ORDER BY startTimestamp DESC
LIMIT ?;

updateWorkoutSession:
UPDATE WorkoutSession
SET endTimestamp = ?,
    totalReps = ?,
    totalSets = ?,
    maxLoad = ?,
    avgLoad = ?,
    isCompleted = ?,
    notes = ?
WHERE id = ?;

deleteWorkoutSession:
DELETE FROM WorkoutSession WHERE id = ?;

lastInsertRowId:
SELECT last_insert_rowid();
```

**Step 2: Generate SQLDelight code**

Run: `./gradlew :composeApp:generateCommonMainVitruvianDatabaseInterface`
Expected: Kotlin code generated in `build/generated/sqldelight/`

**Step 3: Verify generated code**

Run: `./gradlew :composeApp:compileCommonMainKotlinMetadata`
Expected: Builds successfully

**Step 4: Commit**

```bash
git add composeApp/src/commonMain/sqldelight/com/vitruvian/database/WorkoutSession.sq
git commit -m "feat(database): add WorkoutSession schema and queries"
```

---

### Task 2.4: Create WorkoutMetric Schema

**Files:**
- Create: `composeApp/src/commonMain/sqldelight/com/vitruvian/database/WorkoutMetric.sq`

**Step 1: Create WorkoutMetric table (time-series data)**

Create `composeApp/src/commonMain/sqldelight/com/vitruvian/database/WorkoutMetric.sq`:

```sql
-- WorkoutMetric table (high-volume time-series data)
CREATE TABLE WorkoutMetric (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    sessionId INTEGER NOT NULL,
    timestamp INTEGER NOT NULL,
    position INTEGER NOT NULL,
    load REAL NOT NULL,
    ticks INTEGER NOT NULL,
    repNumber INTEGER DEFAULT 0,
    setNumber INTEGER DEFAULT 0,
    FOREIGN KEY (sessionId) REFERENCES WorkoutSession(id) ON DELETE CASCADE
);

CREATE INDEX idx_metric_session ON WorkoutMetric(sessionId);
CREATE INDEX idx_metric_timestamp ON WorkoutMetric(sessionId, timestamp);

-- Queries
insertMetric:
INSERT INTO WorkoutMetric(
    sessionId,
    timestamp,
    position,
    load,
    ticks,
    repNumber,
    setNumber
) VALUES (?, ?, ?, ?, ?, ?, ?);

getMetricsForSession:
SELECT * FROM WorkoutMetric
WHERE sessionId = ?
ORDER BY timestamp ASC;

getMetricsBetweenTimestamps:
SELECT * FROM WorkoutMetric
WHERE sessionId = ? AND timestamp >= ? AND timestamp <= ?
ORDER BY timestamp ASC;

deleteMetricsForSession:
DELETE FROM WorkoutMetric WHERE sessionId = ?;

getMetricCount:
SELECT COUNT(*) FROM WorkoutMetric WHERE sessionId = ?;
```

**Step 2: Generate and verify**

Run: `./gradlew :composeApp:generateCommonMainVitruvianDatabaseInterface`
Expected: Code generated

Run: `./gradlew :composeApp:compileCommonMainKotlinMetadata`
Expected: Builds successfully

**Step 3: Commit**

```bash
git add composeApp/src/commonMain/sqldelight/com/vitruvian/database/WorkoutMetric.sq
git commit -m "feat(database): add WorkoutMetric schema for time-series data"
```

---

### Task 2.5: Create Exercise Schema

**Files:**
- Create: `composeApp/src/commonMain/sqldelight/com/vitruvian/database/Exercise.sq`

**Step 1: Create Exercise table**

Create `composeApp/src/commonMain/sqldelight/com/vitruvian/database/Exercise.sq`:

```sql
-- Exercise table
CREATE TABLE Exercise (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    name TEXT NOT NULL,
    category TEXT NOT NULL,
    muscleGroup TEXT NOT NULL,
    equipment TEXT DEFAULT 'Vitruvian',
    difficulty TEXT DEFAULT 'Intermediate',
    description TEXT,
    instructions TEXT,
    videoUrl TEXT,
    thumbnailUrl TEXT,
    isCustom INTEGER AS Boolean DEFAULT 0,
    isFavorite INTEGER AS Boolean DEFAULT 0,
    createdAt INTEGER NOT NULL
);

CREATE INDEX idx_exercise_category ON Exercise(category);
CREATE INDEX idx_exercise_muscle ON Exercise(muscleGroup);
CREATE INDEX idx_exercise_favorite ON Exercise(isFavorite);

-- Queries
insertExercise:
INSERT INTO Exercise(
    name,
    category,
    muscleGroup,
    equipment,
    difficulty,
    description,
    instructions,
    videoUrl,
    thumbnailUrl,
    isCustom,
    isFavorite,
    createdAt
) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?);

getExerciseById:
SELECT * FROM Exercise WHERE id = ?;

getAllExercises:
SELECT * FROM Exercise ORDER BY name ASC;

getExercisesByCategory:
SELECT * FROM Exercise WHERE category = ? ORDER BY name ASC;

getExercisesByMuscleGroup:
SELECT * FROM Exercise WHERE muscleGroup = ? ORDER BY name ASC;

getFavoriteExercises:
SELECT * FROM Exercise WHERE isFavorite = 1 ORDER BY name ASC;

getCustomExercises:
SELECT * FROM Exercise WHERE isCustom = 1 ORDER BY createdAt DESC;

searchExercises:
SELECT * FROM Exercise WHERE name LIKE '%' || ? || '%' ORDER BY name ASC;

updateExercise:
UPDATE Exercise
SET name = ?,
    category = ?,
    muscleGroup = ?,
    equipment = ?,
    difficulty = ?,
    description = ?,
    instructions = ?,
    videoUrl = ?,
    thumbnailUrl = ?,
    isFavorite = ?
WHERE id = ?;

toggleFavorite:
UPDATE Exercise SET isFavorite = ? WHERE id = ?;

deleteExercise:
DELETE FROM Exercise WHERE id = ?;

lastInsertRowId:
SELECT last_insert_rowid();
```

**Step 2: Generate and verify**

Run: `./gradlew :composeApp:generateCommonMainVitruvianDatabaseInterface`
Run: `./gradlew :composeApp:compileCommonMainKotlinMetadata`
Expected: Builds successfully

**Step 3: Commit**

```bash
git add composeApp/src/commonMain/sqldelight/com/vitruvian/database/Exercise.sq
git commit -m "feat(database): add Exercise schema and queries"
```

---

### Task 2.6: Create Routine Schema

**Files:**
- Create: `composeApp/src/commonMain/sqldelight/com/vitruvian/database/Routine.sq`
- Create: `composeApp/src/commonMain/sqldelight/com/vitruvian/database/RoutineExercise.sq`

**Step 1: Create Routine table**

Create `composeApp/src/commonMain/sqldelight/com/vitruvian/database/Routine.sq`:

```sql
-- Routine table
CREATE TABLE Routine (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    name TEXT NOT NULL,
    description TEXT,
    isCustom INTEGER AS Boolean DEFAULT 1,
    isFavorite INTEGER AS Boolean DEFAULT 0,
    createdAt INTEGER NOT NULL,
    lastUsed INTEGER DEFAULT 0
);

CREATE INDEX idx_routine_favorite ON Routine(isFavorite);
CREATE INDEX idx_routine_lastused ON Routine(lastUsed);

-- Queries
insertRoutine:
INSERT INTO Routine(name, description, isCustom, isFavorite, createdAt, lastUsed)
VALUES (?, ?, ?, ?, ?, ?);

getRoutineById:
SELECT * FROM Routine WHERE id = ?;

getAllRoutines:
SELECT * FROM Routine ORDER BY lastUsed DESC, name ASC;

getFavoriteRoutines:
SELECT * FROM Routine WHERE isFavorite = 1 ORDER BY name ASC;

updateRoutine:
UPDATE Routine
SET name = ?, description = ?, isFavorite = ?
WHERE id = ?;

updateLastUsed:
UPDATE Routine SET lastUsed = ? WHERE id = ?;

deleteRoutine:
DELETE FROM Routine WHERE id = ?;

lastInsertRowId:
SELECT last_insert_rowid();
```

**Step 2: Create RoutineExercise junction table**

Create `composeApp/src/commonMain/sqldelight/com/vitruvian/database/RoutineExercise.sq`:

```sql
-- RoutineExercise junction table
CREATE TABLE RoutineExercise (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    routineId INTEGER NOT NULL,
    exerciseId INTEGER NOT NULL,
    orderIndex INTEGER NOT NULL,
    sets INTEGER DEFAULT 3,
    reps INTEGER DEFAULT 10,
    load REAL DEFAULT 50.0,
    restTime INTEGER DEFAULT 60,
    notes TEXT,
    FOREIGN KEY (routineId) REFERENCES Routine(id) ON DELETE CASCADE,
    FOREIGN KEY (exerciseId) REFERENCES Exercise(id) ON DELETE CASCADE
);

CREATE INDEX idx_routine_exercise ON RoutineExercise(routineId);
CREATE INDEX idx_exercise_routine ON RoutineExercise(exerciseId);

-- Queries
insertRoutineExercise:
INSERT INTO RoutineExercise(
    routineId, exerciseId, orderIndex, sets, reps, load, restTime, notes
) VALUES (?, ?, ?, ?, ?, ?, ?, ?);

getExercisesForRoutine:
SELECT
    re.id,
    re.routineId,
    re.exerciseId,
    e.name AS exerciseName,
    re.orderIndex,
    re.sets,
    re.reps,
    re.load,
    re.restTime,
    re.notes
FROM RoutineExercise re
JOIN Exercise e ON re.exerciseId = e.id
WHERE re.routineId = ?
ORDER BY re.orderIndex ASC;

updateRoutineExercise:
UPDATE RoutineExercise
SET sets = ?, reps = ?, load = ?, restTime = ?, notes = ?
WHERE id = ?;

deleteRoutineExercise:
DELETE FROM RoutineExercise WHERE id = ?;

deleteAllExercisesForRoutine:
DELETE FROM RoutineExercise WHERE routineId = ?;

lastInsertRowId:
SELECT last_insert_rowid();
```

**Step 3: Generate and verify**

Run: `./gradlew :composeApp:generateCommonMainVitruvianDatabaseInterface`
Run: `./gradlew :composeApp:compileCommonMainKotlinMetadata`
Expected: Builds successfully

**Step 4: Commit**

```bash
git add composeApp/src/commonMain/sqldelight/com/vitruvian/database/Routine.sq
git add composeApp/src/commonMain/sqldelight/com/vitruvian/database/RoutineExercise.sq
git commit -m "feat(database): add Routine and RoutineExercise schemas"
```

---

## Week 3: Complete SQLDelight Migration

### Task 3.1: Create Remaining Entity Schemas

**Files:**
- Create: `composeApp/src/commonMain/sqldelight/com/vitruvian/database/ExerciseVideo.sq`
- Create: `composeApp/src/commonMain/sqldelight/com/vitruvian/database/PersonalRecord.sq`
- Create: `composeApp/src/commonMain/sqldelight/com/vitruvian/database/WeeklyProgram.sq`
- Create: `composeApp/src/commonMain/sqldelight/com/vitruvian/database/ProgramDay.sq`
- Create: `composeApp/src/commonMain/sqldelight/com/vitruvian/database/ConnectionLog.sq`

**Step 1: Create ExerciseVideo schema**

Create `composeApp/src/commonMain/sqldelight/com/vitruvian/database/ExerciseVideo.sq`:

```sql
CREATE TABLE ExerciseVideo (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    exerciseId INTEGER NOT NULL,
    url TEXT NOT NULL,
    title TEXT NOT NULL,
    duration INTEGER DEFAULT 0,
    thumbnailUrl TEXT,
    FOREIGN KEY (exerciseId) REFERENCES Exercise(id) ON DELETE CASCADE
);

CREATE INDEX idx_video_exercise ON ExerciseVideo(exerciseId);

-- Queries
insertExerciseVideo:
INSERT INTO ExerciseVideo(exerciseId, url, title, duration, thumbnailUrl)
VALUES (?, ?, ?, ?, ?);

getVideosForExercise:
SELECT * FROM ExerciseVideo WHERE exerciseId = ?;

deleteExerciseVideo:
DELETE FROM ExerciseVideo WHERE id = ?;
```

**Step 2: Create PersonalRecord schema**

Create `composeApp/src/commonMain/sqldelight/com/vitruvian/database/PersonalRecord.sq`:

```sql
CREATE TABLE PersonalRecord (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    exerciseId INTEGER NOT NULL,
    workoutType TEXT NOT NULL,
    maxLoad REAL NOT NULL,
    reps INTEGER NOT NULL,
    achievedAt INTEGER NOT NULL,
    sessionId INTEGER,
    FOREIGN KEY (exerciseId) REFERENCES Exercise(id) ON DELETE CASCADE
);

CREATE INDEX idx_pr_exercise ON PersonalRecord(exerciseId);
CREATE INDEX idx_pr_date ON PersonalRecord(achievedAt);

-- Queries
insertPersonalRecord:
INSERT INTO PersonalRecord(exerciseId, workoutType, maxLoad, reps, achievedAt, sessionId)
VALUES (?, ?, ?, ?, ?, ?);

getPRsForExercise:
SELECT * FROM PersonalRecord WHERE exerciseId = ? ORDER BY achievedAt DESC;

getPRForExerciseAndType:
SELECT * FROM PersonalRecord
WHERE exerciseId = ? AND workoutType = ?
ORDER BY maxLoad DESC, achievedAt DESC
LIMIT 1;

getAllPRs:
SELECT
    pr.*,
    e.name AS exerciseName
FROM PersonalRecord pr
JOIN Exercise e ON pr.exerciseId = e.id
ORDER BY pr.achievedAt DESC;

deletePersonalRecord:
DELETE FROM PersonalRecord WHERE id = ?;
```

**Step 3: Create WeeklyProgram and ProgramDay schemas**

Create `composeApp/src/commonMain/sqldelight/com/vitruvian/database/WeeklyProgram.sq`:

```sql
CREATE TABLE WeeklyProgram (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    name TEXT NOT NULL,
    description TEXT,
    isActive INTEGER AS Boolean DEFAULT 0,
    createdAt INTEGER NOT NULL
);

-- Queries
insertWeeklyProgram:
INSERT INTO WeeklyProgram(name, description, isActive, createdAt)
VALUES (?, ?, ?, ?);

getWeeklyProgramById:
SELECT * FROM WeeklyProgram WHERE id = ?;

getAllWeeklyPrograms:
SELECT * FROM WeeklyProgram ORDER BY createdAt DESC;

getActiveProgram:
SELECT * FROM WeeklyProgram WHERE isActive = 1 LIMIT 1;

updateWeeklyProgram:
UPDATE WeeklyProgram SET name = ?, description = ?, isActive = ? WHERE id = ?;

deactivateAllPrograms:
UPDATE WeeklyProgram SET isActive = 0;

deleteWeeklyProgram:
DELETE FROM WeeklyProgram WHERE id = ?;
```

Create `composeApp/src/commonMain/sqldelight/com/vitruvian/database/ProgramDay.sq`:

```sql
CREATE TABLE ProgramDay (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    programId INTEGER NOT NULL,
    dayOfWeek INTEGER NOT NULL,
    routineId INTEGER NOT NULL,
    FOREIGN KEY (programId) REFERENCES WeeklyProgram(id) ON DELETE CASCADE,
    FOREIGN KEY (routineId) REFERENCES Routine(id) ON DELETE CASCADE
);

CREATE INDEX idx_program_day ON ProgramDay(programId, dayOfWeek);

-- Queries
insertProgramDay:
INSERT INTO ProgramDay(programId, dayOfWeek, routineId)
VALUES (?, ?, ?);

getDaysForProgram:
SELECT
    pd.*,
    r.name AS routineName
FROM ProgramDay pd
JOIN Routine r ON pd.routineId = r.id
WHERE pd.programId = ?
ORDER BY pd.dayOfWeek ASC;

getRoutineForDay:
SELECT * FROM ProgramDay WHERE programId = ? AND dayOfWeek = ?;

deleteProgramDay:
DELETE FROM ProgramDay WHERE id = ?;

deleteAllDaysForProgram:
DELETE FROM ProgramDay WHERE programId = ?;
```

**Step 4: Create ConnectionLog schema (for debugging)**

Create `composeApp/src/commonMain/sqldelight/com/vitruvian/database/ConnectionLog.sq`:

```sql
CREATE TABLE ConnectionLog (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    timestamp INTEGER NOT NULL,
    eventType TEXT NOT NULL,
    deviceAddress TEXT,
    deviceName TEXT,
    rssi INTEGER,
    message TEXT,
    details TEXT
);

CREATE INDEX idx_log_timestamp ON ConnectionLog(timestamp);
CREATE INDEX idx_log_type ON ConnectionLog(eventType);

-- Queries
insertConnectionLog:
INSERT INTO ConnectionLog(timestamp, eventType, deviceAddress, deviceName, rssi, message, details)
VALUES (?, ?, ?, ?, ?, ?, ?);

getAllLogs:
SELECT * FROM ConnectionLog ORDER BY timestamp DESC LIMIT ?;

getLogsByType:
SELECT * FROM ConnectionLog WHERE eventType = ? ORDER BY timestamp DESC LIMIT ?;

getLogsForDevice:
SELECT * FROM ConnectionLog WHERE deviceAddress = ? ORDER BY timestamp DESC;

clearOldLogs:
DELETE FROM ConnectionLog WHERE timestamp < ?;

deleteAllLogs:
DELETE FROM ConnectionLog;
```

**Step 5: Generate and verify all schemas**

Run: `./gradlew :composeApp:generateCommonMainVitruvianDatabaseInterface`
Run: `./gradlew :composeApp:compileCommonMainKotlinMetadata`
Expected: All schemas compile successfully

**Step 6: Commit**

```bash
git add composeApp/src/commonMain/sqldelight/com/vitruvian/database/*.sq
git commit -m "feat(database): add remaining entity schemas (ExerciseVideo, PersonalRecord, WeeklyProgram, ProgramDay, ConnectionLog)"
```

---

### Task 3.2: Create Database Driver Factory (Expect/Actual)

**Files:**
- Create: `composeApp/src/commonMain/kotlin/com/vitruvian/data/local/DatabaseDriverFactory.kt`
- Create: `composeApp/src/androidMain/kotlin/com/vitruvian/data/local/DatabaseDriverFactory.kt`
- Create: `composeApp/src/iosMain/kotlin/com/vitruvian/data/local/DatabaseDriverFactory.kt`
- Create: `composeApp/src/desktopMain/kotlin/com/vitruvian/data/local/DatabaseDriverFactory.kt`

**Step 1: Create expect interface**

Create `composeApp/src/commonMain/kotlin/com/vitruvian/data/local/DatabaseDriverFactory.kt`:

```kotlin
package com.vitruvian.data.local

import app.cash.sqldelight.db.SqlDriver

expect class DatabaseDriverFactory {
    fun createDriver(): SqlDriver
}
```

**Step 2: Create Android actual**

Create `composeApp/src/androidMain/kotlin/com/vitruvian/data/local/DatabaseDriverFactory.kt`:

```kotlin
package com.vitruvian.data.local

import android.content.Context
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.android.AndroidSqliteDriver
import com.vitruvian.database.VitruvianDatabase

actual class DatabaseDriverFactory(private val context: Context) {
    actual fun createDriver(): SqlDriver {
        return AndroidSqliteDriver(
            schema = VitruvianDatabase.Schema,
            context = context,
            name = "vitruvian.db"
        )
    }
}
```

**Step 3: Create iOS actual**

Create `composeApp/src/iosMain/kotlin/com/vitruvian/data/local/DatabaseDriverFactory.kt`:

```kotlin
package com.vitruvian.data.local

import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.native.NativeSqliteDriver
import com.vitruvian.database.VitruvianDatabase

actual class DatabaseDriverFactory {
    actual fun createDriver(): SqlDriver {
        return NativeSqliteDriver(
            schema = VitruvianDatabase.Schema,
            name = "vitruvian.db"
        )
    }
}
```

**Step 4: Create Desktop actual**

Create `composeApp/src/desktopMain/kotlin/com/vitruvian/data/local/DatabaseDriverFactory.kt`:

```kotlin
package com.vitruvian.data.local

import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import com.vitruvian.database.VitruvianDatabase
import java.io.File

actual class DatabaseDriverFactory {
    actual fun createDriver(): SqlDriver {
        val databasePath = getDatabasePath()
        val driver = JdbcSqliteDriver("jdbc:sqlite:$databasePath")
        VitruvianDatabase.Schema.create(driver)
        return driver
    }

    private fun getDatabasePath(): String {
        val userHome = System.getProperty("user.home")
        val appDir = File(userHome, ".vitruvian")
        appDir.mkdirs()
        return File(appDir, "vitruvian.db").absolutePath
    }
}
```

**Step 5: Verify compilation for all targets**

Run: `./gradlew :composeApp:compileKotlinAndroid`
Run: `./gradlew :composeApp:compileKotlinIosArm64`
Run: `./gradlew :composeApp:compileKotlinDesktop`
Expected: All compile successfully

**Step 6: Commit**

```bash
git add composeApp/src/commonMain/kotlin/com/vitruvian/data/local/DatabaseDriverFactory.kt
git add composeApp/src/androidMain/kotlin/com/vitruvian/data/local/DatabaseDriverFactory.kt
git add composeApp/src/iosMain/kotlin/com/vitruvian/data/local/DatabaseDriverFactory.kt
git add composeApp/src/desktopMain/kotlin/com/vitruvian/data/local/DatabaseDriverFactory.kt
git commit -m "feat(database): add DatabaseDriverFactory expect/actual for all platforms"
```

---

## Week 4: Repository Layer & Testing

### Task 4.1: Create Repository Interfaces

**Files:**
- Create: `composeApp/src/commonMain/kotlin/com/vitruvian/data/repository/WorkoutRepository.kt`
- Create: `composeApp/src/commonMain/kotlin/com/vitruvian/data/repository/ExerciseRepository.kt`

**Step 1: Create WorkoutRepository interface**

Create `composeApp/src/commonMain/kotlin/com/vitruvian/data/repository/WorkoutRepository.kt`:

```kotlin
package com.vitruvian.data.repository

import com.vitruvian.database.WorkoutSession
import com.vitruvian.database.WorkoutMetric
import kotlinx.coroutines.flow.Flow

interface WorkoutRepository {
    suspend fun insertWorkoutSession(
        routineId: Long?,
        exerciseId: Long,
        workoutType: String,
        programMode: String,
        startTimestamp: Long
    ): Long

    suspend fun updateWorkoutSession(
        id: Long,
        endTimestamp: Long,
        totalReps: Long,
        totalSets: Long,
        maxLoad: Double,
        avgLoad: Double,
        isCompleted: Boolean,
        notes: String?
    )

    suspend fun insertWorkoutMetric(
        sessionId: Long,
        timestamp: Long,
        position: Long,
        load: Double,
        ticks: Long,
        repNumber: Long,
        setNumber: Long
    )

    fun getWorkoutSessionById(id: Long): Flow<WorkoutSession?>
    fun getAllWorkoutSessions(): Flow<List<WorkoutSession>>
    fun getRecentWorkouts(limit: Long): Flow<List<WorkoutSession>>
    fun getMetricsForSession(sessionId: Long): Flow<List<WorkoutMetric>>

    suspend fun deleteWorkoutSession(id: Long)
}
```

**Step 2: Create ExerciseRepository interface**

Create `composeApp/src/commonMain/kotlin/com/vitruvian/data/repository/ExerciseRepository.kt`:

```kotlin
package com.vitruvian.data.repository

import com.vitruvian.database.Exercise
import kotlinx.coroutines.flow.Flow

interface ExerciseRepository {
    suspend fun insertExercise(
        name: String,
        category: String,
        muscleGroup: String,
        equipment: String,
        difficulty: String,
        description: String?,
        instructions: String?,
        videoUrl: String?,
        thumbnailUrl: String?,
        isCustom: Boolean,
        isFavorite: Boolean,
        createdAt: Long
    ): Long

    fun getExerciseById(id: Long): Flow<Exercise?>
    fun getAllExercises(): Flow<List<Exercise>>
    fun getExercisesByCategory(category: String): Flow<List<Exercise>>
    fun getFavoriteExercises(): Flow<List<Exercise>>
    fun searchExercises(query: String): Flow<List<Exercise>>

    suspend fun toggleFavorite(id: Long, isFavorite: Boolean)
    suspend fun deleteExercise(id: Long)
}
```

**Step 3: Verify compilation**

Run: `./gradlew :composeApp:compileCommonMainKotlinMetadata`
Expected: Builds successfully

**Step 4: Commit**

```bash
git add composeApp/src/commonMain/kotlin/com/vitruvian/data/repository/*.kt
git commit -m "feat(repository): add WorkoutRepository and ExerciseRepository interfaces"
```

---

### Task 4.2: Implement WorkoutRepository

**Files:**
- Create: `composeApp/src/commonMain/kotlin/com/vitruvian/data/repository/WorkoutRepositoryImpl.kt`

**Step 1: Write failing test first**

Create `composeApp/src/commonTest/kotlin/com/vitruvian/data/repository/WorkoutRepositoryTest.kt`:

```kotlin
package com.vitruvian.data.repository

import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import com.vitruvian.database.VitruvianDatabase
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class WorkoutRepositoryTest {

    private lateinit var database: VitruvianDatabase
    private lateinit var repository: WorkoutRepository

    @BeforeTest
    fun setup() {
        val driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        VitruvianDatabase.Schema.create(driver)
        database = VitruvianDatabase(driver)
        repository = WorkoutRepositoryImpl(database)
    }

    @AfterTest
    fun tearDown() {
        database.close()
    }

    @Test
    fun `insertWorkoutSession returns valid ID`() = runTest {
        val id = repository.insertWorkoutSession(
            routineId = null,
            exerciseId = 1,
            workoutType = "OldSchool",
            programMode = "Concentric",
            startTimestamp = 1000L
        )

        assertTrue(id > 0)
    }

    @Test
    fun `getWorkoutSessionById returns inserted session`() = runTest {
        val insertedId = repository.insertWorkoutSession(
            routineId = null,
            exerciseId = 1,
            workoutType = "OldSchool",
            programMode = "Concentric",
            startTimestamp = 1000L
        )

        val session = repository.getWorkoutSessionById(insertedId).first()

        assertNotNull(session)
        assertEquals(insertedId, session.id)
        assertEquals("OldSchool", session.workoutType)
    }

    @Test
    fun `getRecentWorkouts returns sessions ordered by timestamp`() = runTest {
        repository.insertWorkoutSession(null, 1, "OldSchool", "Concentric", 1000L)
        repository.insertWorkoutSession(null, 1, "Pump", "Concentric", 3000L)
        repository.insertWorkoutSession(null, 1, "TUT", "Concentric", 2000L)

        val recent = repository.getRecentWorkouts(10).first()

        assertEquals(3, recent.size)
        assertEquals(3000L, recent[0].startTimestamp) // Most recent
        assertEquals(2000L, recent[1].startTimestamp)
        assertEquals(1000L, recent[2].startTimestamp)
    }
}
```

**Step 2: Run test to verify it fails**

Run: `./gradlew :composeApp:testDebugUnitTest --tests WorkoutRepositoryTest`
Expected: FAIL - WorkoutRepositoryImpl not defined

**Step 3: Implement WorkoutRepositoryImpl**

Create `composeApp/src/commonMain/kotlin/com/vitruvian/data/repository/WorkoutRepositoryImpl.kt`:

```kotlin
package com.vitruvian.data.repository

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import app.cash.sqldelight.coroutines.mapToOneOrNull
import com.vitruvian.database.VitruvianDatabase
import com.vitruvian.database.WorkoutSession
import com.vitruvian.database.WorkoutMetric
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

class WorkoutRepositoryImpl(
    private val database: VitruvianDatabase
) : WorkoutRepository {

    override suspend fun insertWorkoutSession(
        routineId: Long?,
        exerciseId: Long,
        workoutType: String,
        programMode: String,
        startTimestamp: Long
    ): Long = withContext(Dispatchers.IO) {
        database.workoutSessionQueries.insertWorkoutSession(
            routineId = routineId,
            exerciseId = exerciseId,
            workoutType = workoutType,
            programMode = programMode,
            startTimestamp = startTimestamp,
            endTimestamp = null,
            totalReps = 0,
            totalSets = 0,
            maxLoad = 0.0,
            avgLoad = 0.0,
            isCompleted = false,
            notes = null
        )
        database.workoutSessionQueries.lastInsertRowId().executeAsOne()
    }

    override suspend fun updateWorkoutSession(
        id: Long,
        endTimestamp: Long,
        totalReps: Long,
        totalSets: Long,
        maxLoad: Double,
        avgLoad: Double,
        isCompleted: Boolean,
        notes: String?
    ) = withContext(Dispatchers.IO) {
        database.workoutSessionQueries.updateWorkoutSession(
            endTimestamp = endTimestamp,
            totalReps = totalReps,
            totalSets = totalSets,
            maxLoad = maxLoad,
            avgLoad = avgLoad,
            isCompleted = isCompleted,
            notes = notes,
            id = id
        )
    }

    override suspend fun insertWorkoutMetric(
        sessionId: Long,
        timestamp: Long,
        position: Long,
        load: Double,
        ticks: Long,
        repNumber: Long,
        setNumber: Long
    ) = withContext(Dispatchers.IO) {
        database.workoutMetricQueries.insertMetric(
            sessionId = sessionId,
            timestamp = timestamp,
            position = position,
            load = load,
            ticks = ticks,
            repNumber = repNumber,
            setNumber = setNumber
        )
    }

    override fun getWorkoutSessionById(id: Long): Flow<WorkoutSession?> {
        return database.workoutSessionQueries
            .getWorkoutSessionById(id)
            .asFlow()
            .mapToOneOrNull(Dispatchers.IO)
    }

    override fun getAllWorkoutSessions(): Flow<List<WorkoutSession>> {
        return database.workoutSessionQueries
            .getAllWorkoutSessions()
            .asFlow()
            .mapToList(Dispatchers.IO)
    }

    override fun getRecentWorkouts(limit: Long): Flow<List<WorkoutSession>> {
        return database.workoutSessionQueries
            .getRecentWorkouts(limit)
            .asFlow()
            .mapToList(Dispatchers.IO)
    }

    override fun getMetricsForSession(sessionId: Long): Flow<List<WorkoutMetric>> {
        return database.workoutMetricQueries
            .getMetricsForSession(sessionId)
            .asFlow()
            .mapToList(Dispatchers.IO)
    }

    override suspend fun deleteWorkoutSession(id: Long) = withContext(Dispatchers.IO) {
        database.workoutSessionQueries.deleteWorkoutSession(id)
    }
}
```

**Step 4: Run tests to verify they pass**

Run: `./gradlew :composeApp:testDebugUnitTest --tests WorkoutRepositoryTest`
Expected: All tests PASS

**Step 5: Commit**

```bash
git add composeApp/src/commonMain/kotlin/com/vitruvian/data/repository/WorkoutRepositoryImpl.kt
git add composeApp/src/commonTest/kotlin/com/vitruvian/data/repository/WorkoutRepositoryTest.kt
git commit -m "feat(repository): implement WorkoutRepository with tests"
```

---

### Task 4.3: Add Koin DI Module

**Files:**
- Modify: `gradle/libs.versions.toml`
- Modify: `composeApp/build.gradle.kts`
- Create: `composeApp/src/commonMain/kotlin/com/vitruvian/di/CommonModule.kt`

**Step 1: Add Koin to version catalog**

Edit `gradle/libs.versions.toml`, add to `[versions]`:

```toml
koin = "3.5.0"
```

Add to `[libraries]`:

```toml
koin-core = { module = "io.insert-koin:koin-core", version.ref = "koin" }
koin-test = { module = "io.insert-koin:koin-test", version.ref = "koin" }
```

**Step 2: Add Koin to build.gradle.kts**

Edit `composeApp/build.gradle.kts`, add to commonMain dependencies:

```kotlin
sourceSets {
    val commonMain by getting {
        dependencies {
            // ... existing
            implementation(libs.koin.core)
        }
    }

    val commonTest by getting {
        dependencies {
            // ... existing
            implementation(libs.koin.test)
        }
    }
}
```

**Step 3: Create Koin module**

Create `composeApp/src/commonMain/kotlin/com/vitruvian/di/CommonModule.kt`:

```kotlin
package com.vitruvian.di

import com.vitruvian.data.local.DatabaseDriverFactory
import com.vitruvian.data.repository.WorkoutRepository
import com.vitruvian.data.repository.WorkoutRepositoryImpl
import com.vitruvian.database.VitruvianDatabase
import com.vitruvian.util.ProtocolBuilder
import org.koin.dsl.module

val commonModule = module {
    // Database
    single { get<DatabaseDriverFactory>().createDriver() }
    single { VitruvianDatabase(driver = get()) }

    // Repositories
    single<WorkoutRepository> { WorkoutRepositoryImpl(database = get()) }

    // Utilities
    single { ProtocolBuilder() }
}
```

**Step 4: Sync and verify**

Run: `./gradlew --refresh-dependencies`
Run: `./gradlew :composeApp:compileCommonMainKotlinMetadata`
Expected: Builds successfully

**Step 5: Commit**

```bash
git add gradle/libs.versions.toml composeApp/build.gradle.kts
git add composeApp/src/commonMain/kotlin/com/vitruvian/di/CommonModule.kt
git commit -m "feat(di): add Koin DI framework and commonModule"
```

---

## Phase 1 Complete

**Deliverables:**
- ✅ KMP module structure configured for Android, iOS, Desktop
- ✅ Domain models migrated to commonMain
- ✅ ProtocolBuilder migrated with tests
- ✅ SQLDelight database with all 10 entities
- ✅ Database drivers for all platforms (expect/actual)
- ✅ Repository interfaces and implementations
- ✅ Koin DI integration
- ✅ Unit tests for core functionality

**Verification:**

Run full test suite:
```bash
./gradlew :composeApp:testDebugUnitTest
```
Expected: All tests PASS

Build for all targets:
```bash
./gradlew :composeApp:compileKotlinAndroid
./gradlew :composeApp:compileKotlinIosArm64
./gradlew :composeApp:compileKotlinDesktop
```
Expected: All compile successfully

**Next Phase:** Week 5-8 - Android BLE implementation and platform services

---

## Notes for Engineer

**Key Concepts:**
- **expect/actual**: Kotlin Multiplatform's mechanism for platform-specific code
- **SQLDelight**: Type-safe SQL database library that generates Kotlin APIs from .sq files
- **Koin**: Lightweight dependency injection framework for Kotlin Multiplatform
- **Flow**: Kotlin's reactive stream API for asynchronous data

**Common Issues:**
- If SQLDelight generation fails, try `./gradlew clean` first
- iOS compilation requires macOS with Xcode installed
- Desktop uses JDBC driver (works on all platforms)
- All timestamps are in milliseconds (Unix epoch)

**Testing:**
- Use `JdbcSqliteDriver.IN_MEMORY` for tests (fast, isolated)
- `runTest` is required for suspending test functions
- Clean up resources in `@AfterTest` to prevent leaks

**References:**
- SQLDelight docs: https://cashapp.github.io/sqldelight/
- Koin docs: https://insert-koin.io/
- KMP docs: https://kotlinlang.org/docs/multiplatform.html

---

**Plan Status**: Phase 1 (Foundation) - Ready for Execution
**Estimated Time**: 4 weeks (160 hours)
**Prerequisites**: Kotlin knowledge, familiarity with coroutines, SQL basics
