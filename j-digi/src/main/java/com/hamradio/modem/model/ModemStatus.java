package com.hamradio.modem.model;

import com.hamradio.modem.tx.TxState;

public class ModemStatus {

    private boolean hubConnected;
    private boolean audioRunning;

    private ModeType mode = ModeType.RTTY;

    private String hubUrl = "";
    private long rigFrequencyHz;
    private String rigMode = "";

    private double rms;
    private double peakFrequencyHz;
    private double snr;

    // RTTY diagnostics
    private boolean rttyReverse;
    private double rttyMarkPower;
    private double rttySpacePower;
    private double rttyDominance;

    // TX status
    private boolean transmitting;
    private ModeType txMode = ModeType.RTTY;
    private TxState txState = TxState.IDLE;
    private String txStatusText = "Idle";
    private String txTextPreview = "";

    public boolean isHubConnected() {
        return hubConnected;
    }

    public void setHubConnected(boolean hubConnected) {
        this.hubConnected = hubConnected;
    }

    public boolean isAudioRunning() {
        return audioRunning;
    }

    public void setAudioRunning(boolean audioRunning) {
        this.audioRunning = audioRunning;
    }

    public ModeType getMode() {
        return mode;
    }

    public void setMode(ModeType mode) {
        this.mode = mode;
    }

    public String getHubUrl() {
        return hubUrl;
    }

    public void setHubUrl(String hubUrl) {
        this.hubUrl = hubUrl;
    }

    public long getRigFrequencyHz() {
        return rigFrequencyHz;
    }

    public void setRigFrequencyHz(long rigFrequencyHz) {
        this.rigFrequencyHz = rigFrequencyHz;
    }

    public String getRigMode() {
        return rigMode;
    }

    public void setRigMode(String rigMode) {
        this.rigMode = rigMode;
    }

    public double getRms() {
        return rms;
    }

    public void setRms(double rms) {
        this.rms = rms;
    }

    public double getPeakFrequencyHz() {
        return peakFrequencyHz;
    }

    public void setPeakFrequencyHz(double peakFrequencyHz) {
        this.peakFrequencyHz = peakFrequencyHz;
    }

    public double getSnr() {
        return snr;
    }

    public void setSnr(double snr) {
        this.snr = snr;
    }

    public boolean isRttyReverse() {
        return rttyReverse;
    }

    public void setRttyReverse(boolean rttyReverse) {
        this.rttyReverse = rttyReverse;
    }

    public double getRttyMarkPower() {
        return rttyMarkPower;
    }

    public void setRttyMarkPower(double rttyMarkPower) {
        this.rttyMarkPower = rttyMarkPower;
    }

    public double getRttySpacePower() {
        return rttySpacePower;
    }

    public void setRttySpacePower(double rttySpacePower) {
        this.rttySpacePower = rttySpacePower;
    }

    public double getRttyDominance() {
        return rttyDominance;
    }

    public void setRttyDominance(double rttyDominance) {
        this.rttyDominance = rttyDominance;
    }

    public boolean isTransmitting() {
        return transmitting;
    }

    public void setTransmitting(boolean transmitting) {
        this.transmitting = transmitting;
    }

    public ModeType getTxMode() {
        return txMode;
    }

    public void setTxMode(ModeType txMode) {
        this.txMode = txMode;
    }

    public TxState getTxState() {
        return txState;
    }

    public void setTxState(TxState txState) {
        this.txState = txState;
    }

    public String getTxStatusText() {
        return txStatusText;
    }

    public void setTxStatusText(String txStatusText) {
        this.txStatusText = txStatusText;
    }

    public String getTxTextPreview() {
        return txTextPreview;
    }

    public void setTxTextPreview(String txTextPreview) {
        this.txTextPreview = txTextPreview;
    }
}
