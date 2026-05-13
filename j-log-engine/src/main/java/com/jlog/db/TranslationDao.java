package com.jlog.db;

import com.jlog.model.Translation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Data access for the QSO phrase translator. Backs both translator
 * windows in J-Log; works fully offline (no external translation APIs).
 *
 * <p>Edits persist immediately — both translator UIs share the same
 * underlying table, so a phrase corrected in the user-selectable window
 * shows the same correction in the DXCC-driven window.
 */
public class TranslationDao {

    private static final Logger log = LoggerFactory.getLogger(TranslationDao.class);
    private static final TranslationDao INSTANCE = new TranslationDao();

    public static TranslationDao getInstance() { return INSTANCE; }
    private TranslationDao() {}

    private Connection conn() {
        return DatabaseManager.getInstance().getTranslationsConnection();
    }

    // ---------------------------------------------------------------
    // Reads
    // ---------------------------------------------------------------

    public List<Translation> getAll() {
        List<Translation> out = new ArrayList<>();
        String sql = "SELECT phrase_id, category, english, spanish, german, portuguese, phonetic " +
                     "FROM translations ORDER BY category, english";
        try (Statement st = conn().createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) out.add(map(rs));
        } catch (SQLException ex) {
            log.warn("Failed to load translations: {}", ex.getMessage());
        }
        return out;
    }

    /**
     * Returns rows whose category matches any of the supplied prefixes
     * (case-insensitive), or all rows if {@code categories} is empty.
     * Used by the DXCC-driven window to bias toward QSO-relevant phrases.
     */
    public List<Translation> getByCategories(List<String> categories) {
        if (categories == null || categories.isEmpty()) return getAll();
        StringBuilder sql = new StringBuilder(
            "SELECT phrase_id, category, english, spanish, german, portuguese, phonetic " +
            "FROM translations WHERE ");
        for (int i = 0; i < categories.size(); i++) {
            if (i > 0) sql.append(" OR ");
            sql.append("LOWER(category)=?");
        }
        sql.append(" ORDER BY category, english");

        List<Translation> out = new ArrayList<>();
        try (PreparedStatement ps = conn().prepareStatement(sql.toString())) {
            for (int i = 0; i < categories.size(); i++) {
                ps.setString(i + 1, categories.get(i).toLowerCase(Locale.ROOT));
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) out.add(map(rs));
            }
        } catch (SQLException ex) {
            log.warn("Failed to load translations by category: {}", ex.getMessage());
        }
        return out;
    }

    // ---------------------------------------------------------------
    // Writes
    // ---------------------------------------------------------------

    /** Insert a new phrase; returns the new row id. */
    public long insert(Translation t) {
        String sql = "INSERT INTO translations(category, english, spanish, german, portuguese, phonetic) " +
                     "VALUES(?,?,?,?,?,?)";
        try (PreparedStatement ps = conn().prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, blankToNull(t.getCategory()));
            ps.setString(2, t.getEnglish());
            ps.setString(3, blankToNull(t.getSpanish()));
            ps.setString(4, blankToNull(t.getGerman()));
            ps.setString(5, blankToNull(t.getPortuguese()));
            ps.setString(6, blankToNull(t.getPhonetic()));
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    long id = keys.getLong(1);
                    t.setId(id);
                    return id;
                }
            }
        } catch (SQLException ex) {
            log.warn("Failed to insert translation: {}", ex.getMessage());
        }
        return -1;
    }

    /**
     * Update a single column in a row. The caller supplies the language
     * code (en/es/de/pt) or "phonetic" / "category" / "english" for the
     * remaining columns. Returns true on success.
     */
    public boolean updateColumn(long id, String column, String newValue) {
        String col = mapColumn(column);
        if (col == null) return false;
        String sql = "UPDATE translations SET " + col + "=? WHERE phrase_id=?";
        try (PreparedStatement ps = conn().prepareStatement(sql)) {
            ps.setString(1, blankToNull(newValue));
            ps.setLong  (2, id);
            return ps.executeUpdate() > 0;
        } catch (SQLException ex) {
            log.warn("Failed to update translation column {}: {}", column, ex.getMessage());
            return false;
        }
    }

    public boolean delete(long id) {
        try (PreparedStatement ps = conn().prepareStatement(
                "DELETE FROM translations WHERE phrase_id=?")) {
            ps.setLong(1, id);
            return ps.executeUpdate() > 0;
        } catch (SQLException ex) {
            log.warn("Failed to delete translation {}: {}", id, ex.getMessage());
            return false;
        }
    }

    // ---------------------------------------------------------------
    // Helpers
    // ---------------------------------------------------------------

    private static Translation map(ResultSet rs) throws SQLException {
        return new Translation(
            rs.getLong("phrase_id"),
            rs.getString("category"),
            rs.getString("english"),
            rs.getString("spanish"),
            rs.getString("german"),
            rs.getString("portuguese"),
            rs.getString("phonetic")
        );
    }

    private static String mapColumn(String code) {
        if (code == null) return null;
        switch (code.toLowerCase(Locale.ROOT)) {
            case "en": case "english":    return "english";
            case "es": case "spanish":    return "spanish";
            case "de": case "german":     return "german";
            case "pt": case "portuguese": return "portuguese";
            case "phonetic":              return "phonetic";
            case "category":              return "category";
            default: return null;
        }
    }

    private static String blankToNull(String s) {
        if (s == null) return null;
        String t = s.trim();
        return t.isEmpty() ? null : t;
    }
}
