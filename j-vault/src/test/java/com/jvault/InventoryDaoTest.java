package com.jvault;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Exercises the InventoryDao singleton end-to-end against a temp-dir
 * SQLite database. The DAO reads {@code System.getProperty("user.home")}
 * inside {@code dbPath()}, so we redirect user.home in {@code @BeforeAll}.
 */
class InventoryDaoTest {

    @BeforeAll
    static void redirectHome(@TempDir Path tmpHome) {
        System.setProperty("user.home", tmpHome.toString());
    }

    @BeforeEach
    void wipeTables() throws SQLException {
        Path db = Path.of(System.getProperty("user.home"), ".j-vault", "inventory.db");
        if (!Files.exists(db)) {
            // First call creates schema + seeds defaults.
            InventoryDao.getInstance().listTypes();
            return;
        }
        try (Connection c = DriverManager.getConnection("jdbc:sqlite:" + db);
             Statement st = c.createStatement()) {
            st.executeUpdate("DELETE FROM inventory_items");
            st.executeUpdate("DELETE FROM first_call_contacts");
            // Leave equipment_types seed in place — items FK it.
        }
    }

    @Test
    void seedTypesArePopulatedOnFirstOpen() {
        JsonArray types = InventoryDao.getInstance().listTypes();
        assertTrue(types.size() >= 25,
            "expected the ~29 seeded equipment types, got " + types.size());
        boolean hasRadio = false;
        for (var t : types) {
            if ("Radio (HF Base)".equals(t.getAsJsonObject().get("name").getAsString())) {
                hasRadio = true; break;
            }
        }
        assertTrue(hasRadio, "seed must include 'Radio (HF Base)'");
    }

    @Test
    void createTypeReturnsRowWithGeneratedId() {
        JsonObject created = InventoryDao.getInstance()
            .createType("Custom Test Type", 555);
        assertNotNull(created);
        assertTrue(created.get("id").getAsInt() > 0);
        assertEquals("Custom Test Type", created.get("name").getAsString());
        assertEquals(555, created.get("display_order").getAsInt());
    }

    @Test
    void createDuplicateTypeReturnsNull() {
        InventoryDao.getInstance().createType("DupTest", 600);
        JsonObject second = InventoryDao.getInstance().createType("DupTest", 700);
        assertNull(second, "UNIQUE constraint should reject duplicate name");
    }

    @Test
    void deleteTypeReturnsTrueOnHit() {
        JsonObject t = InventoryDao.getInstance().createType("EphemeralType", 800);
        int id = t.get("id").getAsInt();
        assertTrue(InventoryDao.getInstance().deleteType(id));
        assertFalse(InventoryDao.getInstance().deleteType(id),
            "second delete returns false (no row)");
    }

    @Test
    void itemCrudRoundTrip() {
        int typeId = firstTypeId();

        JsonObject in = new JsonObject();
        in.addProperty("type_id",          typeId);
        in.addProperty("manufacturer",     "Yaesu");
        in.addProperty("model",            "FT-991A");
        in.addProperty("serial_number",    "ABC123");
        in.addProperty("purchase_price",   "999.00");
        in.addProperty("estimated_value",  "750.00");
        in.addProperty("disposition",      "working");
        in.addProperty("install_status",   "installed");

        JsonObject created = InventoryDao.getInstance().createItem(in);
        assertNotNull(created);
        int id = created.get("id").getAsInt();
        assertEquals("Yaesu",    created.get("manufacturer").getAsString());
        assertEquals(999.00,     created.get("purchase_price").getAsDouble(), 1e-9);
        assertNotNull(created.get("type_name").getAsString(),
            "join with equipment_types must populate type_name");

        JsonObject fetched = InventoryDao.getInstance().getItem(id);
        assertEquals("FT-991A", fetched.get("model").getAsString());

        JsonObject upd = new JsonObject();
        upd.addProperty("type_id",        typeId);
        upd.addProperty("manufacturer",   "Yaesu");
        upd.addProperty("model",          "FT-710");           // changed
        upd.addProperty("estimated_value", "650.00");          // changed
        upd.addProperty("disposition",    "working");
        upd.addProperty("install_status", "installed");
        JsonObject updated = InventoryDao.getInstance().updateItem(id, upd);
        assertNotNull(updated);
        assertEquals("FT-710", updated.get("model").getAsString());
        assertEquals(650.00, updated.get("estimated_value").getAsDouble(), 1e-9);

        assertTrue(InventoryDao.getInstance().deleteItem(id));
        assertNull(InventoryDao.getInstance().getItem(id));
    }

    @Test
    void numericFieldsAcceptEmptyStringAsNull() {
        int typeId = firstTypeId();
        JsonObject in = new JsonObject();
        in.addProperty("type_id",          typeId);
        in.addProperty("manufacturer",     "Test");
        in.addProperty("model",            "EmptyPrice");
        in.addProperty("purchase_price",   "");   // empty → NULL
        in.addProperty("estimated_value",  "");   // empty → NULL

        JsonObject created = InventoryDao.getInstance().createItem(in);
        assertNotNull(created);
        assertFalse(created.has("purchase_price"),
            "empty price string should land as NULL, not 0.0");
        assertFalse(created.has("estimated_value"));
    }

    @Test
    void contactCrudRoundTrip() {
        JsonObject in = new JsonObject();
        in.addProperty("name",         "Alice K1ABC");
        in.addProperty("callsign",     "K1ABC");
        in.addProperty("phone",        "555-1234");
        in.addProperty("email",        "alice@example.com");
        in.addProperty("relationship", "Club president");
        in.addProperty("priority",     50);

        JsonObject created = InventoryDao.getInstance().createContact(in);
        assertNotNull(created);
        int id = created.get("id").getAsInt();
        assertEquals(50, created.get("priority").getAsInt());

        JsonObject upd = new JsonObject();
        upd.addProperty("name",         "Alice K1ABC");
        upd.addProperty("callsign",     "K1ABC");
        upd.addProperty("phone",        "555-9999");
        upd.addProperty("priority",     10);
        JsonObject updated = InventoryDao.getInstance().updateContact(id, upd);
        assertEquals("555-9999", updated.get("phone").getAsString());
        assertEquals(10, updated.get("priority").getAsInt());

        assertTrue(InventoryDao.getInstance().deleteContact(id));
        assertFalse(InventoryDao.getInstance().deleteContact(id));
    }

    @Test
    void contactDefaultPriorityIs100WhenMissing() {
        JsonObject in = new JsonObject();
        in.addProperty("name", "No-priority");
        JsonObject created = InventoryDao.getInstance().createContact(in);
        assertEquals(100, created.get("priority").getAsInt());
    }

    @Test
    void exportCsvIncludesHeaderAndCreatedItems() {
        int typeId = firstTypeId();
        JsonObject in = new JsonObject();
        in.addProperty("type_id",      typeId);
        in.addProperty("manufacturer", "Kenwood");
        in.addProperty("model",        "TS-590SG");
        in.addProperty("notes",        "needs new fan, ordered\nspare on shelf");
        InventoryDao.getInstance().createItem(in);

        String csv = InventoryDao.getInstance().exportCsv();
        String[] lines = csv.split("\n");
        assertTrue(lines[0].startsWith("ID,Type,Manufacturer,Model"),
            "first row must be the header");
        assertTrue(csv.contains("Kenwood"),  "row must include item manufacturer");
        assertTrue(csv.contains("TS-590SG"), "row must include item model");
        assertTrue(csv.contains("\"needs new fan, ordered\nspare on shelf\""),
            "notes field with comma + newline must be quoted and escaped");
    }

    private static int firstTypeId() {
        JsonArray types = InventoryDao.getInstance().listTypes();
        return types.get(0).getAsJsonObject().get("id").getAsInt();
    }
}
