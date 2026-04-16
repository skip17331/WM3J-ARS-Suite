package com.jlog.db;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jlog.model.Macro;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Persists macros as JSON in the config database.
 */
public class MacroDao {

    private static final Logger log = LoggerFactory.getLogger(MacroDao.class);
    private static final MacroDao INSTANCE = new MacroDao();
    private final ObjectMapper mapper = new ObjectMapper();

    public static MacroDao getInstance() { return INSTANCE; }

    private Connection conn() {
        return DatabaseManager.getInstance().getConfigConnection();
    }

    public long insert(Macro macro) throws Exception {
        String json = mapper.writeValueAsString(macro.getActions());
        try (PreparedStatement ps = conn().prepareStatement(
                "INSERT INTO macro(name,fkey,json) VALUES(?,?,?)",
                Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, macro.getName());
            ps.setInt   (2, macro.getFKey());
            ps.setString(3, json);
            ps.executeUpdate();
            ResultSet gk = ps.getGeneratedKeys();
            if (gk.next()) { macro.setId(gk.getLong(1)); return macro.getId(); }
        }
        return -1;
    }

    public void update(Macro macro) throws Exception {
        String json = mapper.writeValueAsString(macro.getActions());
        try (PreparedStatement ps = conn().prepareStatement(
                "UPDATE macro SET name=?,fkey=?,json=? WHERE id=?")) {
            ps.setString(1, macro.getName());
            ps.setInt   (2, macro.getFKey());
            ps.setString(3, json);
            ps.setLong  (4, macro.getId());
            ps.executeUpdate();
        }
    }

    public void delete(long id) throws SQLException {
        try (PreparedStatement ps = conn().prepareStatement("DELETE FROM macro WHERE id=?")) {
            ps.setLong(1, id); ps.executeUpdate();
        }
    }

    public List<Macro> fetchAll() {
        List<Macro> list = new ArrayList<>();
        try (Statement st = conn().createStatement();
             ResultSet rs = st.executeQuery("SELECT * FROM macro ORDER BY fkey,name")) {
            while (rs.next()) {
                Macro m = new Macro();
                m.setId  (rs.getLong  ("id"));
                m.setName(rs.getString("name"));
                m.setFKey(rs.getInt   ("fkey"));
                String json = rs.getString("json");
                List<Macro.MacroAction> actions = mapper.readValue(json,
                    mapper.getTypeFactory().constructCollectionType(List.class, Macro.MacroAction.class));
                m.setActions(actions);
                list.add(m);
            }
        } catch (Exception ex) {
            log.error("Failed to fetch macros", ex);
        }
        return list;
    }

    /** Get macro bound to a specific function key (1-12). */
    public Macro getByFKey(int fkey) {
        return fetchAll().stream()
            .filter(m -> m.getFKey() == fkey)
            .findFirst().orElse(null);
    }
}
