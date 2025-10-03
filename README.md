
# FaultDetection-Recovery - Assignment for SWEN755

A lightweight **fault detection & recovery** demo using a **primary–backup** architecture:
- Heartbeats from the primary to a monitor (`CarController`)
- **Fail detection** via heartbeat timeout
- **Passive backup** that waits for a **TAKEOVER** signal
- **Checkpointing** (`checkpoint.json`) of sequence ID and last decision (`SAFE|BRAKE`) for seamless resume

---

## 🗂️ Components

