package com.hamradio.jhub.callsign;

import com.hamradio.jhub.ConfigManager;
import com.hamradio.jhub.model.JHubConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.*;
import java.time.Instant;

/**
 * Local SQLite callsign database provider.
 *
 * Tried first in auto mode — local lookups are instant and work offline.
 * Populate the database via:
 *   POST /api/callsign/db/import/fcc  — FCC ULS amateur radio data (free)
 *   POST /api/callsign/db/import/csv  — generic CSV (HamCall, Buckmaster, custom)
 *
 * Configure the database path in j-hub.json → callsignLookup.localDbPath.
 */
public class LocalDbProvider implements CallsignProvider {

    private static final Logger log = LoggerFactory.getLogger(LocalDbProvider.class);

    @Override public String name() { return "local"; }

    @Override
    public boolean isAvailable() {
        String path = dbPath();
        return path != null && !path.isBlank() && Files.exists(Path.of(path));
    }

    @Override
    public CallsignResult lookup(String callsign) throws Exception {
        String path = dbPath();
        try (Connection conn = DriverManager.getConnection("jdbc:sqlite:" + path);
             PreparedStatement ps = conn.prepareStatement(
                 "SELECT * FROM callsigns WHERE callsign = ? LIMIT 1")) {

            ps.setString(1, callsign.toUpperCase());
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next())
                    return CallsignResult.notFound(callsign, "Not in local database");

                CallsignResult r = new CallsignResult();
                r.callsign       = callsign.toUpperCase();
                r.found          = true;
                r.source         = "local";
                r.name           = rs.getString("name");
                r.firstName      = rs.getString("first_name");
                r.lastName       = rs.getString("last_name");
                r.addr1          = rs.getString("addr1");
                r.city           = rs.getString("city");
                r.state          = rs.getString("state");
                r.zip            = rs.getString("zip");
                r.country        = rs.getString("country");
                r.grid           = rs.getString("grid");
                r.licenseClass   = rs.getString("license_class");
                r.licenseExpires = rs.getString("license_expires");
                r.email          = rs.getString("email");
                r.lat            = rs.getDouble("lat");
                r.lon            = rs.getDouble("lon");
                r.fetchedAt      = Instant.now().toString();

                String city  = nvl(r.city);
                String state = nvl(r.state);
                r.addr2 = (city + " " + state).trim();

                return r;
            }
        }
    }

    /** Returns record count, or -1 if the DB doesn't exist or has no table yet. */
    public long recordCount() {
        String path = dbPath();
        if (path == null || path.isBlank()) return -1;
        try (Connection conn = DriverManager.getConnection("jdbc:sqlite:" + path);
             Statement  st   = conn.createStatement();
             ResultSet  rs   = st.executeQuery("SELECT COUNT(*) FROM callsigns")) {
            return rs.next() ? rs.getLong(1) : 0;
        } catch (Exception e) {
            return -1;
        }
    }

    public static String dbPath() {
        JHubConfig.CallsignSection s = ConfigManager.getInstance().getConfig().callsignLookup;
        return (s != null) ? s.localDbPath : null;
    }

    private static String nvl(String v) { return v != null ? v : ""; }
}
