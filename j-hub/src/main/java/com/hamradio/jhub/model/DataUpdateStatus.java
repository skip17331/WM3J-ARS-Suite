package com.hamradio.jhub.model;

/**
 * DataUpdateStatus — broadcast as type "DATA_UPDATE_STATUS" whenever the
 * {@link com.hamradio.jhub.DataUpdateService} completes (or fails) a fetch
 * cycle. Apps can subscribe to refresh their UI indicators.
 */
public class DataUpdateStatus {

    public String type = "DATA_UPDATE_STATUS";

    /** Per-file status. NaN/empty values mean "no update has run yet". */
    public FileStatus cty = new FileStatus();
    public FileStatus scp = new FileStatus();

    public DataUpdateStatus() {}

    public static class FileStatus {
        public String  url            = "";
        public String  localPath      = "";
        public long    sizeBytes      = 0L;
        /** ISO-8601 UTC of the last successful update; empty if never. */
        public String  lastUpdated    = "";
        /** ISO-8601 UTC of the last attempt (success or failure); empty if never. */
        public String  lastAttempted  = "";
        /** Most recent error message, or "" on success. */
        public String  lastError      = "";
        /** HTTP response details for diagnostics — kept across attempts. */
        public int     lastHttpStatus = 0;
        public boolean validated      = false;
        public FileStatus() {}
    }
}
