package com.jlog.model;

/**
 * Station and operator configuration (stored in SQLite config table).
 */
public class StationInfo {

    private String callsign;
    private String operatorName;
    private String gridSquare;
    private double latitude;
    private double longitude;
    private String qth;
    private String state;
    private String county;
    private String country;
    private String radioModel;
    private String antenna;
    private int    defaultPower;
    private String qrzUsername;
    private String qrzPassword;

    // Getters/Setters
    public String getCallsign() { return callsign; }
    public void setCallsign(String callsign) { this.callsign = callsign; }

    public String getOperatorName() { return operatorName; }
    public void setOperatorName(String operatorName) { this.operatorName = operatorName; }

    public String getGridSquare() { return gridSquare; }
    public void setGridSquare(String gridSquare) { this.gridSquare = gridSquare; }

    public double getLatitude() { return latitude; }
    public void setLatitude(double latitude) { this.latitude = latitude; }

    public double getLongitude() { return longitude; }
    public void setLongitude(double longitude) { this.longitude = longitude; }

    public String getQth() { return qth; }
    public void setQth(String qth) { this.qth = qth; }

    public String getState() { return state; }
    public void setState(String state) { this.state = state; }

    public String getCounty() { return county; }
    public void setCounty(String county) { this.county = county; }

    public String getCountry() { return country; }
    public void setCountry(String country) { this.country = country; }

    public String getRadioModel() { return radioModel; }
    public void setRadioModel(String radioModel) { this.radioModel = radioModel; }

    public String getAntenna() { return antenna; }
    public void setAntenna(String antenna) { this.antenna = antenna; }

    public int getDefaultPower() { return defaultPower; }
    public void setDefaultPower(int defaultPower) { this.defaultPower = defaultPower; }

    public String getQrzUsername() { return qrzUsername; }
    public void setQrzUsername(String qrzUsername) { this.qrzUsername = qrzUsername; }

    public String getQrzPassword() { return qrzPassword; }
    public void setQrzPassword(String qrzPassword) { this.qrzPassword = qrzPassword; }
}
