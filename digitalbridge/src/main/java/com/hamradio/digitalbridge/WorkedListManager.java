package com.hamradio.digitalbridge;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * WorkedListManager — thread-safe set of worked callsigns.
 *
 * Populated from j-hub's state cache on connection (WORKED_LIST message).
 * Updated whenever a WSJTX_QSO_LOGGED event is received.
 *
 * workedStatus values mirror j-hub Spot.workedStatus convention:
 *   "worked"  — callsign is in the worked list
 *   "needed"  — list is populated but callsign is absent
 *   "unknown" — list has never been received from hub
 */
public class WorkedListManager {

    private static final Logger log = LoggerFactory.getLogger(WorkedListManager.class);

    private static final WorkedListManager INSTANCE = new WorkedListManager();
    public  static WorkedListManager getInstance() { return INSTANCE; }
    private WorkedListManager() {}

    private final Set<String> worked = Collections.synchronizedSet(new HashSet<>());
    private volatile boolean listReceived = false; // true once hub has sent us the list

    /** Replace the entire list (from hub WORKED_LIST replay). */
    public void setWorkedList(Iterable<String> callsigns) {
        worked.clear();
        if (callsigns != null) {
            for (String cs : callsigns) {
                if (cs != null && !cs.isBlank()) worked.add(cs.trim().toUpperCase());
            }
        }
        listReceived = true;
        log.info("Worked list loaded: {} callsigns", worked.size());
    }

    /** Add a single callsign (called after QSO logged). */
    public void markWorked(String callsign) {
        if (callsign == null || callsign.isBlank()) return;
        worked.add(callsign.trim().toUpperCase());
        listReceived = true;
        log.debug("Marked worked: {}", callsign.trim().toUpperCase());
    }

    /** Evaluate the worked status of a callsign. */
    public String getWorkedStatus(String callsign) {
        if (callsign == null || callsign.isBlank()) return "unknown";
        if (!listReceived) return "unknown";
        return worked.contains(callsign.trim().toUpperCase()) ? "worked" : "needed";
    }

    public boolean isWorked(String callsign) {
        if (callsign == null || callsign.isBlank()) return false;
        return worked.contains(callsign.trim().toUpperCase());
    }

    public int size()    { return worked.size(); }
    public void clear()  { worked.clear(); listReceived = false; }
}
