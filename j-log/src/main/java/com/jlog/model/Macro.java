package com.jlog.model;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents a user-defined macro.
 * A macro contains an ordered list of MacroAction steps.
 */
public class Macro {

    public enum ActionType {
        CIV_COMMAND,      // Raw CI-V hex bytes
        PTT_ON,           // Key transmitter
        PTT_OFF,          // Unkey transmitter
        VOICE_PLAY,       // Play audio file via radio
        CW_TEXT,          // Send CW via CI-V
        INSERT_EXCHANGE,  // Paste canned text into active field
        AUTOFILL_FIELDS,  // Trigger callsign lookup autofill
        DELAY_MS          // Pause N milliseconds
    }

    private long   id;
    private String name;
    private int    fKey;          // 1-12; 0 = no function key binding
    private List<MacroAction> actions = new ArrayList<>();

    public static class MacroAction {
        private ActionType type;
        private String     data;    // Meaning depends on type
        private int        intData; // For delays etc.

        public MacroAction() {}
        public MacroAction(ActionType type, String data) {
            this.type = type;
            this.data = data;
        }
        public MacroAction(ActionType type, int intData) {
            this.type = type;
            this.intData = intData;
        }

        public ActionType getType() { return type; }
        public void setType(ActionType type) { this.type = type; }
        public String getData() { return data; }
        public void setData(String data) { this.data = data; }
        public int getIntData() { return intData; }
        public void setIntData(int intData) { this.intData = intData; }
    }

    // Getters / setters
    public long getId() { return id; }
    public void setId(long id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public int getFKey() { return fKey; }
    public void setFKey(int fKey) { this.fKey = fKey; }

    public List<MacroAction> getActions() { return actions; }
    public void setActions(List<MacroAction> actions) { this.actions = actions; }

    public void addAction(MacroAction action) { this.actions.add(action); }

    @Override
    public String toString() {
        return "Macro{name=" + name + ", fKey=F" + fKey + ", actions=" + actions.size() + "}";
    }
}
