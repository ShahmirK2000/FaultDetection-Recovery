package src;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;

/**
 * Minimal checkpoint store for passive redundancy.
 * File: checkpoint.json
 * Format: {"lastSeqId":N,"lastDecision":"SAFE|BRAKE"}
 */
public class BackupState {
    private static final String FILE = "checkpoint.json";

    public static class State {
        public long lastSeqId;
        public String lastDecision; // "SAFE" or "BRAKE"
        @Override public String toString() {
            return "State{lastSeqId=" + lastSeqId + ", lastDecision=" + lastDecision + "}";
        }
    }

    /** Update only the sequence id (e.g., from Camera when a new frame/event arrives). */
    public static synchronized void updateSeq(long seqId) {
        State s = load();
        s.lastSeqId = seqId;
        save(s);
    }

    /** Update only the decision (e.g., from CollisionDetector/CarController). */
    public static synchronized void updateDecision(String decision) {
        if (decision == null) return;
        State s = load();
        s.lastDecision = decision;
        save(s);
    }

    /** Load last checkpoint, return SAFE/0 if none exists. */
    public static synchronized State load() {
        State s = new State();
        s.lastSeqId = 0L;
        s.lastDecision = "SAFE";

        File f = new File(FILE);
        if (!f.exists()) return s;

        try (BufferedReader br = new BufferedReader(new FileReader(f))) {
            String json = br.readLine();
            if (json == null) return s;
            json = json.trim();
            if (json.startsWith("{") && json.endsWith("}")) {
                json = json.substring(1, json.length()-1);
                for (String p : json.split(",")) {
                    String[] kv = p.split(":", 2);
                    if (kv.length < 2) continue;
                    String key = kv[0].trim().replace("\"", "");
                    String val = kv[1].trim();
                    if ("lastSeqId".equals(key)) s.lastSeqId = Long.parseLong(val);
                    if ("lastDecision".equals(key)) s.lastDecision = val.replace("\"", "");
                }
            }
        } catch (Exception e) {
            System.err.println("[BackupState] load failed: " + e.getMessage());
        }
        return s;
    }

    private static void save(State s) {
        try (BufferedWriter bw = new BufferedWriter(new FileWriter(FILE, false))) {
            bw.write("{\"lastSeqId\":" + s.lastSeqId + ",\"lastDecision\":\"" + s.lastDecision + "\"}");
        } catch (Exception e) {
            System.err.println("[BackupState] save failed: " + e.getMessage());
        }
    }
}
