
# FaultDetection-Recovery - Assignment for SWEN755

A lightweight **fault detection & recovery** demo using a **primary–backup** architecture:
- Heartbeats from the primary to a monitor (`CarController`)
- **Fail detection** via heartbeat timeout
- **Passive backup** that waits for a **TAKEOVER** signal
- **Checkpointing** (`checkpoint.json`) of sequence ID and last decision (`SAFE|BRAKE`) for seamless resume

---

## Components


### BackupState.java
- Persists `{ "lastSeqId": N, "lastDecision": "SAFE|BRAKE" }` to `checkpoint.json`
- `updateSeq`, `updateDecision`, `load()`; thread-safe (`synchronized`)

### Camera.java
- `isObjectDetected()` with configurable detection probability
- Increments a sequence ID each tick → `BackupState.updateSeq(...)`
- Simulates a crash (~2% per call)

### CarController.java
- TCP server (default port **6355**) receiving `"HEARTBEAT"`
- If **>5s** gap → signals backup via control port **7003** with `"TAKEOVER"`
- Accepts next client after triggering takeover

### CollisionDetector.java
- Sends periodic `"HEARTBEAT"` to `CarController`
- Random **HEALTHY / DEGRADED** state; counts consecutive degradations
- Uses `Camera` to decide `SAFE` or `BRAKE` → `BackupState.updateDecision(...)`
- **Primary mode:** connects to monitor and runs  
- **Backup mode:** waits on port **7003**; on `"TAKEOVER"` → restores checkpoint → connects → runs

---

## Requirements

- Java 8+ (JDK)
- No external libs

---

## Build

```bash
# Unix/macOS
javac -d out src/*.java

# Windows (PowerShell or CMD)
javac -d out src\*.java
