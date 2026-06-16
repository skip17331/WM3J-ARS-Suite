package com.ars.fx.data;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * J-Vault station-inventory + estate store, backed by {@code ~/.j-vault/inventory.db}
 * — the same SQLite schema the standalone J-Vault app uses (equipment_types,
 * inventory_items, first_call_contacts), so the two share data.
 */
public final class JVaultDb {
    private JVaultDb() {}

    public record Cat(int id, String name) {}
    public record Item(int id, int typeId, String category, String manufacturer, String model, String serial,
                       String dateAcquired, double purchasePrice, double estimatedValue,
                       String disposition, String installStatus, String location, String notes) {
        public String title() { return (nz(manufacturer) + " " + nz(model)).trim(); }
    }
    public record Contact(int id, String name, String callsign, String relationship, String itemsWanted) {}
    public record Totals(int count, double value, int installed) {}

    public static final String[] DISPOSITIONS = {"working", "needs repair", "for sale", "sold", "retired", "spare"};
    public static final String[] INSTALL = {"installed", "stored", "spare", "loaned"};

    private static final String[][] SEED = {
            {"Radio (HF Base)","10"},{"Radio (HF Mobile)","20"},{"Radio (VHF/UHF Base)","30"},{"Radio (VHF/UHF Mobile)","40"},
            {"Handheld (HT)","50"},{"Receiver","60"},{"Amplifier","70"},{"Antenna (Wire)","80"},{"Antenna (Vertical)","90"},
            {"Antenna (Beam/Yagi)","100"},{"Antenna (Loop)","110"},{"Tower / Mast","120"},{"Rotator","130"},{"Tuner","140"},
            {"Coax Run","150"},{"Antenna Switch","160"},{"Balun / Unun","170"},{"Power Supply","180"},{"Battery","190"},
            {"Microphone","200"},{"Headset / Headphones","210"},{"Audio Interface","220"},{"Computer","230"},
            {"Software License","240"},{"Test Equipment","250"},{"Tool","260"},{"Book / Reference","270"},
            {"Parts / Connectors","280"},{"Other","999"},
    };

    private static Path dbPath() { return Paths.get(System.getProperty("user.home"), ".j-vault", "inventory.db"); }

    private static Connection open() throws Exception {
        Path p = dbPath();
        Files.createDirectories(p.getParent());
        Connection c = DriverManager.getConnection("jdbc:sqlite:" + p + "?busyTimeout=5000");
        try (Statement st = c.createStatement()) {
            st.execute("PRAGMA journal_mode=WAL");
            st.execute("CREATE TABLE IF NOT EXISTS equipment_types(id INTEGER PRIMARY KEY AUTOINCREMENT,name TEXT NOT NULL UNIQUE,display_order INTEGER NOT NULL DEFAULT 100)");
            st.execute("CREATE TABLE IF NOT EXISTS inventory_items(id INTEGER PRIMARY KEY AUTOINCREMENT,type_id INTEGER NOT NULL REFERENCES equipment_types(id),"
                    + "manufacturer TEXT,model TEXT,serial_number TEXT,date_acquired TEXT,purchase_price REAL,estimated_value REAL,"
                    + "disposition TEXT NOT NULL DEFAULT 'working',install_status TEXT NOT NULL DEFAULT 'installed',storage_location TEXT,"
                    + "notes TEXT,extra_fields TEXT,created_at TEXT NOT NULL,updated_at TEXT NOT NULL)");
            st.execute("CREATE TABLE IF NOT EXISTS first_call_contacts(id INTEGER PRIMARY KEY AUTOINCREMENT,name TEXT NOT NULL,callsign TEXT,phone TEXT,email TEXT,"
                    + "relationship TEXT,items_wanted TEXT,notes TEXT,priority INTEGER NOT NULL DEFAULT 100,created_at TEXT NOT NULL,updated_at TEXT NOT NULL)");
            try (ResultSet rs = st.executeQuery("SELECT COUNT(*) FROM equipment_types")) {
                if (rs.next() && rs.getInt(1) == 0) {
                    try (PreparedStatement ps = c.prepareStatement("INSERT OR IGNORE INTO equipment_types(name,display_order) VALUES(?,?)")) {
                        for (String[] s : SEED) { ps.setString(1, s[0]); ps.setInt(2, Integer.parseInt(s[1])); ps.addBatch(); }
                        ps.executeBatch();
                    }
                }
            }
        }
        return c;
    }

    public static boolean available() { try (Connection c = open()) { return true; } catch (Throwable t) { return false; } }

    public static List<Cat> categories() {
        List<Cat> out = new ArrayList<>();
        try (Connection c = open(); Statement st = c.createStatement();
             ResultSet rs = st.executeQuery("SELECT id,name FROM equipment_types ORDER BY display_order,name")) {
            while (rs.next()) out.add(new Cat(rs.getInt(1), rs.getString(2)));
        } catch (Throwable t) { /* empty */ }
        return out;
    }

    private static final String ITEM_SQL =
            "SELECT i.id,i.type_id,t.name,i.manufacturer,i.model,i.serial_number,i.date_acquired,i.purchase_price,"
          + "i.estimated_value,i.disposition,i.install_status,i.storage_location,i.notes "
          + "FROM inventory_items i JOIN equipment_types t ON t.id=i.type_id ORDER BY t.display_order,i.manufacturer,i.model";

    /** All items, optionally filtered by a search string (call/mfr/model/serial/notes) and category name. */
    public static List<Item> items(String query, String category) {
        List<Item> out = new ArrayList<>();
        String q = query == null ? "" : query.trim().toLowerCase();
        try (Connection c = open(); Statement st = c.createStatement(); ResultSet rs = st.executeQuery(ITEM_SQL)) {
            while (rs.next()) {
                Item it = row(rs);
                if (category != null && !category.isBlank() && !"All".equals(category) && !category.equals(it.category())) continue;
                if (!q.isEmpty() && !(low(it.title()).contains(q) || low(it.serial()).contains(q)
                        || low(it.notes()).contains(q) || low(it.category()).contains(q))) continue;
                out.add(it);
            }
        } catch (Throwable t) { /* empty */ }
        return out;
    }

    public static Item item(int id) {
        try (Connection c = open(); PreparedStatement ps = c.prepareStatement(
                "SELECT i.id,i.type_id,t.name,i.manufacturer,i.model,i.serial_number,i.date_acquired,i.purchase_price,"
              + "i.estimated_value,i.disposition,i.install_status,i.storage_location,i.notes "
              + "FROM inventory_items i JOIN equipment_types t ON t.id=i.type_id WHERE i.id=?")) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) { if (rs.next()) return row(rs); }
        } catch (Throwable t) { /* none */ }
        return null;
    }
    private static Item row(ResultSet rs) throws SQLException {
        return new Item(rs.getInt(1), rs.getInt(2), rs.getString(3), rs.getString(4), rs.getString(5), rs.getString(6),
                rs.getString(7), rs.getDouble(8), rs.getDouble(9), rs.getString(10), rs.getString(11), rs.getString(12), rs.getString(13));
    }

    public static int addItem(int typeId, String mfr, String model, String serial, String dateAcq, double purchase,
                              double value, String disposition, String install, String location, String notes) {
        String now = LocalDateTime.now().toString();
        String sql = "INSERT INTO inventory_items(type_id,manufacturer,model,serial_number,date_acquired,purchase_price,"
                + "estimated_value,disposition,install_status,storage_location,notes,created_at,updated_at) VALUES(?,?,?,?,?,?,?,?,?,?,?,?,?)";
        try (Connection c = open(); PreparedStatement ps = c.prepareStatement(sql)) {
            bind(ps, typeId, mfr, model, serial, dateAcq, purchase, value, disposition, install, location, notes);
            ps.setString(12, now); ps.setString(13, now);
            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) { return rs.next() ? rs.getInt(1) : -1; }
        } catch (Throwable t) { return -1; }
    }
    public static boolean updateItem(int id, int typeId, String mfr, String model, String serial, String dateAcq,
                                     double purchase, double value, String disposition, String install, String location, String notes) {
        String sql = "UPDATE inventory_items SET type_id=?,manufacturer=?,model=?,serial_number=?,date_acquired=?,purchase_price=?,"
                + "estimated_value=?,disposition=?,install_status=?,storage_location=?,notes=?,updated_at=? WHERE id=?";
        try (Connection c = open(); PreparedStatement ps = c.prepareStatement(sql)) {
            bind(ps, typeId, mfr, model, serial, dateAcq, purchase, value, disposition, install, location, notes);
            ps.setString(12, LocalDateTime.now().toString()); ps.setInt(13, id);
            return ps.executeUpdate() > 0;
        } catch (Throwable t) { return false; }
    }
    private static void bind(PreparedStatement ps, int typeId, String mfr, String model, String serial, String dateAcq,
                             double purchase, double value, String disposition, String install, String location, String notes) throws SQLException {
        ps.setInt(1, typeId); ps.setString(2, nzn(mfr)); ps.setString(3, nzn(model)); ps.setString(4, nzn(serial));
        ps.setString(5, nzn(dateAcq)); ps.setDouble(6, purchase); ps.setDouble(7, value);
        ps.setString(8, disposition == null || disposition.isBlank() ? "working" : disposition);
        ps.setString(9, install == null || install.isBlank() ? "installed" : install);
        ps.setString(10, nzn(location)); ps.setString(11, nzn(notes));
    }
    public static boolean deleteItem(int id) {
        try (Connection c = open(); PreparedStatement ps = c.prepareStatement("DELETE FROM inventory_items WHERE id=?")) {
            ps.setInt(1, id); return ps.executeUpdate() > 0;
        } catch (Throwable t) { return false; }
    }

    public static Totals totals() {
        try (Connection c = open(); Statement st = c.createStatement();
             ResultSet rs = st.executeQuery("SELECT COUNT(*),COALESCE(SUM(estimated_value),0),"
                     + "COALESCE(SUM(CASE WHEN install_status='installed' THEN 1 ELSE 0 END),0) FROM inventory_items")) {
            if (rs.next()) return new Totals(rs.getInt(1), rs.getDouble(2), rs.getInt(3));
        } catch (Throwable t) { /* zero */ }
        return new Totals(0, 0, 0);
    }

    public static List<Contact> contacts() {
        List<Contact> out = new ArrayList<>();
        try (Connection c = open(); Statement st = c.createStatement();
             ResultSet rs = st.executeQuery("SELECT id,name,callsign,relationship,items_wanted FROM first_call_contacts ORDER BY priority,name")) {
            while (rs.next()) out.add(new Contact(rs.getInt(1), rs.getString(2), rs.getString(3), rs.getString(4), rs.getString(5)));
        } catch (Throwable t) { /* empty */ }
        return out;
    }
    public static boolean addContact(String name, String callsign, String relationship, String itemsWanted) {
        String now = LocalDateTime.now().toString();
        try (Connection c = open(); PreparedStatement ps = c.prepareStatement(
                "INSERT INTO first_call_contacts(name,callsign,relationship,items_wanted,created_at,updated_at) VALUES(?,?,?,?,?,?)")) {
            ps.setString(1, nzn(name)); ps.setString(2, nzn(callsign)); ps.setString(3, nzn(relationship));
            ps.setString(4, nzn(itemsWanted)); ps.setString(5, now); ps.setString(6, now);
            return ps.executeUpdate() > 0;
        } catch (Throwable t) { return false; }
    }
    public static boolean deleteContact(int id) {
        try (Connection c = open(); PreparedStatement ps = c.prepareStatement("DELETE FROM first_call_contacts WHERE id=?")) {
            ps.setInt(1, id); return ps.executeUpdate() > 0;
        } catch (Throwable t) { return false; }
    }

    /** Inventory CSV (estate report) for the J-Vault DB. */
    public static String exportCsv() {
        StringBuilder sb = new StringBuilder("Category,Manufacturer,Model,Serial,Acquired,Purchase,Value,Disposition,Install,Location,Notes\n");
        for (Item i : items(null, null)) {
            sb.append(String.join(",", csv(i.category()), csv(i.manufacturer()), csv(i.model()), csv(i.serial()),
                    csv(i.dateAcquired()), money(i.purchasePrice()), money(i.estimatedValue()),
                    csv(i.disposition()), csv(i.installStatus()), csv(i.location()), csv(i.notes()))).append("\n");
        }
        return sb.toString();
    }

    private static String low(String s) { return s == null ? "" : s.toLowerCase(); }
    private static String nz(String s) { return s == null ? "" : s; }
    private static String nzn(String s) { return (s == null || s.isBlank()) ? null : s.trim(); }
    private static String money(double v) { return v == 0 ? "" : String.valueOf(v); }
    private static String csv(String s) {
        if (s == null) return "";
        return (s.contains(",") || s.contains("\"") || s.contains("\n")) ? "\"" + s.replace("\"", "\"\"") + "\"" : s;
    }
}
