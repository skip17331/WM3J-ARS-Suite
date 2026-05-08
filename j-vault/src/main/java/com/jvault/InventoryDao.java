package com.jvault;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.sql.*;
import java.time.Instant;

/**
 * InventoryDao — SQLite-backed CRUD for the shack inventory.
 *
 * <p>Stores three tables in {@code ~/.j-vault/inventory.db}:
 * <ul>
 *   <li>{@code equipment_types} — pre-seeded category list (Radio, Antenna, ...)</li>
 *   <li>{@code inventory_items} — the unified equipment schema</li>
 *   <li>{@code first_call_contacts} — friends / dealers / club leadership</li>
 * </ul>
 *
 * <p>One-time migration: on first open, if {@code ~/.j-vault/inventory.db}
 * does not exist but {@code ~/.j-hub/inventory.db} does, the older
 * file is copied (not moved) into the new location. This lets users
 * keep their data when J-Hub's inventory tab moves out into J-Vault.
 */
public class InventoryDao {

    private static final Logger log = LoggerFactory.getLogger(InventoryDao.class);

    private static final InventoryDao INSTANCE = new InventoryDao();
    public static InventoryDao getInstance() { return INSTANCE; }
    private InventoryDao() {}

    private static Path dbPath() {
        return Path.of(System.getProperty("user.home"), ".j-vault", "inventory.db");
    }

    private static Path legacyDbPath() {
        return Path.of(System.getProperty("user.home"), ".j-hub", "inventory.db");
    }

    private Connection open() throws SQLException {
        Path db = dbPath();
        try {
            Files.createDirectories(db.getParent());
            // First-run migration from j-hub's old data dir.
            Path legacy = legacyDbPath();
            if (!Files.exists(db) && Files.exists(legacy)) {
                Files.copy(legacy, db, StandardCopyOption.COPY_ATTRIBUTES);
                log.info("Migrated inventory DB from {} -> {}", legacy, db);
            }
        } catch (IOException e) {
            throw new SQLException("cannot create j-vault config dir: " + e.getMessage(), e);
        }
        Connection c = DriverManager.getConnection("jdbc:sqlite:" + db);
        try (Statement st = c.createStatement()) {
            st.executeUpdate("PRAGMA foreign_keys = ON");
            st.executeUpdate(
                "CREATE TABLE IF NOT EXISTS equipment_types ("
              + "  id INTEGER PRIMARY KEY AUTOINCREMENT,"
              + "  name TEXT NOT NULL UNIQUE,"
              + "  display_order INTEGER NOT NULL DEFAULT 100"
              + ")");
            st.executeUpdate(
                "CREATE TABLE IF NOT EXISTS inventory_items ("
              + "  id INTEGER PRIMARY KEY AUTOINCREMENT,"
              + "  type_id INTEGER NOT NULL REFERENCES equipment_types(id),"
              + "  manufacturer TEXT,"
              + "  model TEXT,"
              + "  serial_number TEXT,"
              + "  date_acquired TEXT,"
              + "  purchase_price REAL,"
              + "  estimated_value REAL,"
              + "  disposition TEXT NOT NULL DEFAULT 'working',"
              + "  install_status TEXT NOT NULL DEFAULT 'installed',"
              + "  storage_location TEXT,"
              + "  notes TEXT,"
              + "  extra_fields TEXT,"
              + "  created_at TEXT NOT NULL,"
              + "  updated_at TEXT NOT NULL"
              + ")");
            st.executeUpdate(
                "CREATE TABLE IF NOT EXISTS first_call_contacts ("
              + "  id INTEGER PRIMARY KEY AUTOINCREMENT,"
              + "  name TEXT NOT NULL,"
              + "  callsign TEXT,"
              + "  phone TEXT,"
              + "  email TEXT,"
              + "  relationship TEXT,"
              + "  items_wanted TEXT,"
              + "  notes TEXT,"
              + "  priority INTEGER NOT NULL DEFAULT 100,"
              + "  created_at TEXT NOT NULL,"
              + "  updated_at TEXT NOT NULL"
              + ")");
            seedEquipmentTypes(c);
        }
        return c;
    }

    private void seedEquipmentTypes(Connection c) throws SQLException {
        try (Statement st = c.createStatement();
             ResultSet rs = st.executeQuery("SELECT COUNT(*) FROM equipment_types")) {
            if (rs.next() && rs.getInt(1) > 0) return;
        }
        String[][] seed = {
            {"Radio (HF Base)",        "10"},
            {"Radio (HF Mobile)",      "20"},
            {"Radio (VHF/UHF Base)",   "30"},
            {"Radio (VHF/UHF Mobile)", "40"},
            {"Handheld (HT)",          "50"},
            {"Receiver",               "60"},
            {"Amplifier",              "70"},
            {"Antenna (Wire)",         "80"},
            {"Antenna (Vertical)",     "90"},
            {"Antenna (Beam/Yagi)",   "100"},
            {"Antenna (Loop)",        "110"},
            {"Tower / Mast",          "120"},
            {"Rotator",               "130"},
            {"Tuner",                 "140"},
            {"Coax Run",              "150"},
            {"Antenna Switch",        "160"},
            {"Balun / Unun",          "170"},
            {"Power Supply",          "180"},
            {"Battery",               "190"},
            {"Microphone",            "200"},
            {"Headset / Headphones",  "210"},
            {"Audio Interface",       "220"},
            {"Computer",              "230"},
            {"Software License",      "240"},
            {"Test Equipment",        "250"},
            {"Tool",                  "260"},
            {"Book / Reference",      "270"},
            {"Parts / Connectors",    "280"},
            {"Other",                 "999"},
        };
        try (PreparedStatement ps = c.prepareStatement(
                "INSERT OR IGNORE INTO equipment_types(name, display_order) VALUES (?, ?)")) {
            for (String[] row : seed) {
                ps.setString(1, row[0]);
                ps.setInt(2, Integer.parseInt(row[1]));
                ps.addBatch();
            }
            ps.executeBatch();
        }
    }

    // ----- equipment_types ------------------------------------------------

    public JsonArray listTypes() {
        JsonArray out = new JsonArray();
        try (Connection c = open();
             Statement st = c.createStatement();
             ResultSet rs = st.executeQuery(
                 "SELECT id, name, display_order FROM equipment_types "
               + "ORDER BY display_order, name")) {
            while (rs.next()) {
                JsonObject o = new JsonObject();
                o.addProperty("id",            rs.getInt("id"));
                o.addProperty("name",          rs.getString("name"));
                o.addProperty("display_order", rs.getInt("display_order"));
                out.add(o);
            }
        } catch (SQLException e) {
            log.warn("listTypes failed: {}", e.getMessage());
        }
        return out;
    }

    public JsonObject createType(String name, int displayOrder) {
        try (Connection c = open();
             PreparedStatement ps = c.prepareStatement(
                 "INSERT INTO equipment_types(name, display_order) VALUES (?, ?)",
                 Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, name);
            ps.setInt(2, displayOrder);
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    JsonObject o = new JsonObject();
                    o.addProperty("id",            keys.getInt(1));
                    o.addProperty("name",          name);
                    o.addProperty("display_order", displayOrder);
                    return o;
                }
            }
        } catch (SQLException e) {
            log.warn("createType failed: {}", e.getMessage());
        }
        return null;
    }

    public boolean deleteType(int id) {
        try (Connection c = open();
             PreparedStatement ps = c.prepareStatement(
                 "DELETE FROM equipment_types WHERE id = ?")) {
            ps.setInt(1, id);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            log.warn("deleteType({}) failed: {}", id, e.getMessage());
            return false;
        }
    }

    // ----- inventory_items ------------------------------------------------

    private static final String ITEM_SELECT_COLS =
        "i.id AS i_id, i.type_id, i.manufacturer, i.model, i.serial_number, "
      + "i.date_acquired, i.purchase_price, i.estimated_value, i.disposition, "
      + "i.install_status, i.storage_location, i.notes, i.extra_fields, "
      + "i.created_at, i.updated_at, t.name AS type_name";

    public JsonArray listItems() {
        JsonArray out = new JsonArray();
        try (Connection c = open();
             Statement st = c.createStatement();
             ResultSet rs = st.executeQuery(
                 "SELECT " + ITEM_SELECT_COLS + " "
               + "FROM inventory_items i "
               + "JOIN equipment_types t ON t.id = i.type_id "
               + "ORDER BY t.display_order, i.manufacturer, i.model")) {
            while (rs.next()) out.add(itemRowToJson(rs, true));
        } catch (SQLException e) {
            log.warn("listItems failed: {}", e.getMessage());
        }
        return out;
    }

    public JsonObject getItem(int id) {
        try (Connection c = open();
             PreparedStatement ps = c.prepareStatement(
                 "SELECT " + ITEM_SELECT_COLS + " "
               + "FROM inventory_items i "
               + "JOIN equipment_types t ON t.id = i.type_id "
               + "WHERE i.id = ?")) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return itemRowToJson(rs, true);
            }
        } catch (SQLException e) {
            log.warn("getItem({}) failed: {}", id, e.getMessage());
        }
        return null;
    }

    public JsonObject createItem(JsonObject input) {
        String now = Instant.now().toString();
        try (Connection c = open();
             PreparedStatement ps = c.prepareStatement(
                 "INSERT INTO inventory_items "
               + "(type_id, manufacturer, model, serial_number, date_acquired, "
               + " purchase_price, estimated_value, disposition, install_status, "
               + " storage_location, notes, extra_fields, created_at, updated_at) "
               + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
                 Statement.RETURN_GENERATED_KEYS)) {
            bindItem(ps, input, now, now);
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) return getItem(keys.getInt(1));
            }
        } catch (SQLException e) {
            log.warn("createItem failed: {}", e.getMessage());
        }
        return null;
    }

    public JsonObject updateItem(int id, JsonObject input) {
        String now = Instant.now().toString();
        try (Connection c = open();
             PreparedStatement ps = c.prepareStatement(
                 "UPDATE inventory_items SET "
               + "type_id = ?, manufacturer = ?, model = ?, serial_number = ?, "
               + "date_acquired = ?, purchase_price = ?, estimated_value = ?, "
               + "disposition = ?, install_status = ?, storage_location = ?, "
               + "notes = ?, extra_fields = ?, created_at = COALESCE(created_at, ?), "
               + "updated_at = ? "
               + "WHERE id = ?")) {
            bindItem(ps, input, now, now);
            ps.setInt(15, id);
            if (ps.executeUpdate() == 0) return null;
            return getItem(id);
        } catch (SQLException e) {
            log.warn("updateItem({}) failed: {}", id, e.getMessage());
        }
        return null;
    }

    public boolean deleteItem(int id) {
        try (Connection c = open();
             PreparedStatement ps = c.prepareStatement(
                 "DELETE FROM inventory_items WHERE id = ?")) {
            ps.setInt(1, id);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            log.warn("deleteItem({}) failed: {}", id, e.getMessage());
            return false;
        }
    }

    public String exportCsv() {
        StringBuilder sb = new StringBuilder(8192);
        sb.append("ID,Type,Manufacturer,Model,Serial,Date Acquired,Purchase Price,")
          .append("Estimated Value,Disposition,Install Status,Storage Location,Notes\n");
        try (Connection c = open();
             Statement st = c.createStatement();
             ResultSet rs = st.executeQuery(
                 "SELECT i.id, t.name AS type_name, i.manufacturer, i.model, "
               + "       i.serial_number, i.date_acquired, i.purchase_price, "
               + "       i.estimated_value, i.disposition, i.install_status, "
               + "       i.storage_location, i.notes "
               + "FROM inventory_items i "
               + "JOIN equipment_types t ON t.id = i.type_id "
               + "ORDER BY t.display_order, i.manufacturer, i.model")) {
            while (rs.next()) {
                sb.append(rs.getInt("id")).append(',');
                csv(sb, rs.getString("type_name"));        sb.append(',');
                csv(sb, rs.getString("manufacturer"));     sb.append(',');
                csv(sb, rs.getString("model"));            sb.append(',');
                csv(sb, rs.getString("serial_number"));    sb.append(',');
                csv(sb, rs.getString("date_acquired"));    sb.append(',');
                Object pp = rs.getObject("purchase_price");
                if (pp != null) sb.append(pp);             sb.append(',');
                Object ev = rs.getObject("estimated_value");
                if (ev != null) sb.append(ev);             sb.append(',');
                csv(sb, rs.getString("disposition"));      sb.append(',');
                csv(sb, rs.getString("install_status"));   sb.append(',');
                csv(sb, rs.getString("storage_location")); sb.append(',');
                csv(sb, rs.getString("notes"));            sb.append('\n');
            }
        } catch (SQLException e) {
            log.warn("exportCsv failed: {}", e.getMessage());
        }
        return sb.toString();
    }

    // ----- first_call_contacts -------------------------------------------

    public JsonArray listContacts() {
        JsonArray out = new JsonArray();
        try (Connection c = open();
             Statement st = c.createStatement();
             ResultSet rs = st.executeQuery(
                 "SELECT id, name, callsign, phone, email, relationship, "
               + "       items_wanted, notes, priority, created_at, updated_at "
               + "FROM first_call_contacts ORDER BY priority, name")) {
            while (rs.next()) out.add(contactRowToJson(rs));
        } catch (SQLException e) {
            log.warn("listContacts failed: {}", e.getMessage());
        }
        return out;
    }

    public JsonObject getContact(int id) {
        try (Connection c = open();
             PreparedStatement ps = c.prepareStatement(
                 "SELECT id, name, callsign, phone, email, relationship, "
               + "       items_wanted, notes, priority, created_at, updated_at "
               + "FROM first_call_contacts WHERE id = ?")) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return contactRowToJson(rs);
            }
        } catch (SQLException e) {
            log.warn("getContact({}) failed: {}", id, e.getMessage());
        }
        return null;
    }

    public JsonObject createContact(JsonObject input) {
        String now = Instant.now().toString();
        try (Connection c = open();
             PreparedStatement ps = c.prepareStatement(
                 "INSERT INTO first_call_contacts "
               + "(name, callsign, phone, email, relationship, items_wanted, "
               + " notes, priority, created_at, updated_at) "
               + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
                 Statement.RETURN_GENERATED_KEYS)) {
            bindContact(ps, input, now, now);
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) return getContact(keys.getInt(1));
            }
        } catch (SQLException e) {
            log.warn("createContact failed: {}", e.getMessage());
        }
        return null;
    }

    public JsonObject updateContact(int id, JsonObject input) {
        String now = Instant.now().toString();
        try (Connection c = open();
             PreparedStatement ps = c.prepareStatement(
                 "UPDATE first_call_contacts SET "
               + "name = ?, callsign = ?, phone = ?, email = ?, relationship = ?, "
               + "items_wanted = ?, notes = ?, priority = ?, "
               + "created_at = COALESCE(created_at, ?), updated_at = ? "
               + "WHERE id = ?")) {
            bindContact(ps, input, now, now);
            ps.setInt(11, id);
            if (ps.executeUpdate() == 0) return null;
            return getContact(id);
        } catch (SQLException e) {
            log.warn("updateContact({}) failed: {}", id, e.getMessage());
        }
        return null;
    }

    public boolean deleteContact(int id) {
        try (Connection c = open();
             PreparedStatement ps = c.prepareStatement(
                 "DELETE FROM first_call_contacts WHERE id = ?")) {
            ps.setInt(1, id);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            log.warn("deleteContact({}) failed: {}", id, e.getMessage());
            return false;
        }
    }

    // ----- helpers --------------------------------------------------------

    private static JsonObject itemRowToJson(ResultSet rs, boolean joined) throws SQLException {
        JsonObject o = new JsonObject();
        o.addProperty("id",               rs.getInt(joined ? "i_id" : "id"));
        o.addProperty("type_id",          rs.getInt("type_id"));
        if (joined) o.addProperty("type_name", rs.getString("type_name"));
        o.addProperty("manufacturer",     rs.getString("manufacturer"));
        o.addProperty("model",            rs.getString("model"));
        o.addProperty("serial_number",    rs.getString("serial_number"));
        o.addProperty("date_acquired",    rs.getString("date_acquired"));
        Object pp = rs.getObject("purchase_price");
        if (pp != null) o.addProperty("purchase_price", ((Number) pp).doubleValue());
        Object ev = rs.getObject("estimated_value");
        if (ev != null) o.addProperty("estimated_value", ((Number) ev).doubleValue());
        o.addProperty("disposition",      rs.getString("disposition"));
        o.addProperty("install_status",   rs.getString("install_status"));
        o.addProperty("storage_location", rs.getString("storage_location"));
        o.addProperty("notes",            rs.getString("notes"));
        o.addProperty("extra_fields",     rs.getString("extra_fields"));
        o.addProperty("created_at",       rs.getString("created_at"));
        o.addProperty("updated_at",       rs.getString("updated_at"));
        return o;
    }

    private static JsonObject contactRowToJson(ResultSet rs) throws SQLException {
        JsonObject o = new JsonObject();
        o.addProperty("id",            rs.getInt("id"));
        o.addProperty("name",          rs.getString("name"));
        o.addProperty("callsign",      rs.getString("callsign"));
        o.addProperty("phone",         rs.getString("phone"));
        o.addProperty("email",         rs.getString("email"));
        o.addProperty("relationship",  rs.getString("relationship"));
        o.addProperty("items_wanted",  rs.getString("items_wanted"));
        o.addProperty("notes",         rs.getString("notes"));
        o.addProperty("priority",      rs.getInt("priority"));
        o.addProperty("created_at",    rs.getString("created_at"));
        o.addProperty("updated_at",    rs.getString("updated_at"));
        return o;
    }

    private static void bindItem(PreparedStatement ps, JsonObject in, String createdAt, String updatedAt)
            throws SQLException {
        ps.setInt   (1,  in.has("type_id") ? in.get("type_id").getAsInt() : 1);
        setStr (ps, 2,  in, "manufacturer");
        setStr (ps, 3,  in, "model");
        setStr (ps, 4,  in, "serial_number");
        setStr (ps, 5,  in, "date_acquired");
        setReal(ps, 6,  in, "purchase_price");
        setReal(ps, 7,  in, "estimated_value");
        setStr (ps, 8,  in, "disposition",    "working");
        setStr (ps, 9,  in, "install_status", "installed");
        setStr (ps, 10, in, "storage_location");
        setStr (ps, 11, in, "notes");
        setStr (ps, 12, in, "extra_fields");
        ps.setString(13, createdAt);
        ps.setString(14, updatedAt);
    }

    private static void bindContact(PreparedStatement ps, JsonObject in, String createdAt, String updatedAt)
            throws SQLException {
        setStr (ps, 1,  in, "name");
        setStr (ps, 2,  in, "callsign");
        setStr (ps, 3,  in, "phone");
        setStr (ps, 4,  in, "email");
        setStr (ps, 5,  in, "relationship");
        setStr (ps, 6,  in, "items_wanted");
        setStr (ps, 7,  in, "notes");
        ps.setInt(8, in.has("priority") && !in.get("priority").isJsonNull()
                ? in.get("priority").getAsInt() : 100);
        ps.setString(9,  createdAt);
        ps.setString(10, updatedAt);
    }

    private static void setStr(PreparedStatement ps, int idx, JsonObject in, String key) throws SQLException {
        setStr(ps, idx, in, key, null);
    }

    private static void setStr(PreparedStatement ps, int idx, JsonObject in, String key, String defaultIfMissing)
            throws SQLException {
        if (in.has(key) && !in.get(key).isJsonNull()) {
            String v = in.get(key).getAsString();
            ps.setString(idx, v.isEmpty() ? null : v);
        } else if (defaultIfMissing != null) {
            ps.setString(idx, defaultIfMissing);
        } else {
            ps.setNull(idx, Types.VARCHAR);
        }
    }

    private static void setReal(PreparedStatement ps, int idx, JsonObject in, String key) throws SQLException {
        if (in.has(key) && !in.get(key).isJsonNull()) {
            String s = in.get(key).getAsString();
            if (s.isEmpty()) { ps.setNull(idx, Types.REAL); return; }
            try {
                ps.setDouble(idx, Double.parseDouble(s));
            } catch (NumberFormatException e) {
                ps.setNull(idx, Types.REAL);
            }
        } else {
            ps.setNull(idx, Types.REAL);
        }
    }

    private static void csv(StringBuilder sb, String v) {
        if (v == null) return;
        boolean needsQuote = v.indexOf(',') >= 0 || v.indexOf('"') >= 0
                          || v.indexOf('\n') >= 0 || v.indexOf('\r') >= 0;
        if (needsQuote) sb.append('"').append(v.replace("\"", "\"\"")).append('"');
        else            sb.append(v);
    }
}
