package com.jlog.service;

import com.jlog.db.DatabaseManager;

import java.io.FileOutputStream;
import java.nio.file.*;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Business logic for the Database Tools popup.
 * Manages named log databases stored in ~/.j-log/.
 */
public class DatabaseToolsService {

    private static final Path   LOG_DIR       = Path.of(System.getProperty("user.home"), ".j-log");
    private static final String ACTIVE_DB_KEY = "active.log.db";
    private static final String DEFAULT_DB    = "j-log.db";

    private static final DatabaseToolsService INSTANCE = new DatabaseToolsService();

    private DatabaseToolsService() {}

    public static DatabaseToolsService getInstance() {
        return INSTANCE;
    }

    /** Lists all log databases (*.db files, excluding contest.db and config.db). */
    public List<String> listLogDatabases() throws Exception {
        List<String> dbs = Files.list(LOG_DIR)
            .map(p -> p.getFileName().toString())
            .filter(n -> n.endsWith(".db") && !n.equals("contest.db") && !n.equals("config.db"))
            .sorted()
            .collect(Collectors.toList());
        if (!dbs.contains(DEFAULT_DB)) dbs.add(0, DEFAULT_DB);
        return dbs;
    }

    /**
     * Creates a new empty log database file.
     * Name is sanitised: only alphanumerics, hyphens and underscores are kept.
     */
    public String createDatabase(String rawName) throws Exception {
        String safe = rawName.trim().replaceAll("[^a-zA-Z0-9_\\-]", "_");
        if (safe.isBlank()) throw new IllegalArgumentException("Invalid database name.");
        if (!safe.endsWith(".db")) safe += ".db";
        Path target = LOG_DIR.resolve(safe);
        if (Files.exists(target)) throw new IllegalStateException("Database already exists: " + safe);
        Files.createDirectories(LOG_DIR);
        new FileOutputStream(target.toFile()).close();
        return safe;
    }

    /** Deletes a log database. The default j-log.db cannot be deleted. */
    public void deleteDatabase(String name) throws Exception {
        if (DEFAULT_DB.equals(name)) throw new IllegalArgumentException("Cannot delete the default database.");
        Files.deleteIfExists(LOG_DIR.resolve(name));
    }

    /** Returns the currently configured active log database name. */
    public String getActiveDatabase() {
        return DatabaseManager.getInstance().getConfig(ACTIVE_DB_KEY, DEFAULT_DB);
    }

    /**
     * Persists the active database selection.
     * The change takes effect on the next application restart
     * (QsoDao holds an open connection to the previous file).
     */
    public void setActiveDatabase(String name) {
        DatabaseManager.getInstance().setConfig(ACTIVE_DB_KEY, name);
    }
}
