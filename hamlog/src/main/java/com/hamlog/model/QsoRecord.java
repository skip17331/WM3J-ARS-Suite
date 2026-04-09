package com.hamlog.model;

import java.time.LocalDateTime;

/**
 * Represents a single QSO (contact) in the log.
 * Used for both Normal Log and Contest Log.
 */
public class QsoRecord {

    private long   id;
    private String callsign;
    private LocalDateTime dateTimeUtc;
    private String band;
    private String mode;
    private String frequency;
    private int    powerWatts;
    private String rstSent;
    private String rstReceived;
    private String country;
    private String operatorName;
    private String state;
    private String county;
    private String notes;
    private boolean qslSent;
    private boolean qslReceived;

    // Contest-specific fields
    private String contestId;
    private String operator;        // current op (contest multi-op)
    private String serialSent;
    private String serialReceived;
    private String exchange;        // full exchange string
    private String contestField1;
    private String contestField2;
    private String contestField3;
    private String contestField4;
    private String contestField5;
    private int    points;
    private boolean isDupe;

    // ---- Constructors ----
    public QsoRecord() {}

    // ---- Getters / Setters ----
    public long getId() { return id; }
    public void setId(long id) { this.id = id; }

    public String getCallsign() { return callsign; }
    public void setCallsign(String callsign) { this.callsign = callsign != null ? callsign.toUpperCase().trim() : ""; }

    public LocalDateTime getDateTimeUtc() { return dateTimeUtc; }
    public void setDateTimeUtc(LocalDateTime dt) { this.dateTimeUtc = dt; }

    public String getBand() { return band; }
    public void setBand(String band) { this.band = band; }

    public String getMode() { return mode; }
    public void setMode(String mode) { this.mode = mode; }

    public String getFrequency() { return frequency; }
    public void setFrequency(String frequency) { this.frequency = frequency; }

    public int getPowerWatts() { return powerWatts; }
    public void setPowerWatts(int powerWatts) { this.powerWatts = powerWatts; }

    public String getRstSent() { return rstSent; }
    public void setRstSent(String rstSent) { this.rstSent = rstSent; }

    public String getRstReceived() { return rstReceived; }
    public void setRstReceived(String rstReceived) { this.rstReceived = rstReceived; }

    public String getCountry() { return country; }
    public void setCountry(String country) { this.country = country; }

    public String getOperatorName() { return operatorName; }
    public void setOperatorName(String name) { this.operatorName = name; }

    public String getState() { return state; }
    public void setState(String state) { this.state = state; }

    public String getCounty() { return county; }
    public void setCounty(String county) { this.county = county; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }

    public boolean isQslSent() { return qslSent; }
    public void setQslSent(boolean qslSent) { this.qslSent = qslSent; }

    public boolean isQslReceived() { return qslReceived; }
    public void setQslReceived(boolean qslReceived) { this.qslReceived = qslReceived; }

    public String getContestId() { return contestId; }
    public void setContestId(String contestId) { this.contestId = contestId; }

    public String getOperator() { return operator; }
    public void setOperator(String operator) { this.operator = operator; }

    public String getSerialSent() { return serialSent; }
    public void setSerialSent(String serialSent) { this.serialSent = serialSent; }

    public String getSerialReceived() { return serialReceived; }
    public void setSerialReceived(String serialReceived) { this.serialReceived = serialReceived; }

    public String getExchange() { return exchange; }
    public void setExchange(String exchange) { this.exchange = exchange; }

    public String getContestField1() { return contestField1; }
    public void setContestField1(String v) { this.contestField1 = v; }

    public String getContestField2() { return contestField2; }
    public void setContestField2(String v) { this.contestField2 = v; }

    public String getContestField3() { return contestField3; }
    public void setContestField3(String v) { this.contestField3 = v; }

    public String getContestField4() { return contestField4; }
    public void setContestField4(String v) { this.contestField4 = v; }

    public String getContestField5() { return contestField5; }
    public void setContestField5(String v) { this.contestField5 = v; }

    public int getPoints() { return points; }
    public void setPoints(int points) { this.points = points; }

    public boolean isDupe() { return isDupe; }
    public void setDupe(boolean dupe) { isDupe = dupe; }

    @Override
    public String toString() {
        return "QsoRecord{id=" + id + ", call=" + callsign +
               ", dt=" + dateTimeUtc + ", band=" + band + ", mode=" + mode + "}";
    }
}
