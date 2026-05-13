package com.jlog.model;

/**
 * One row in the QSO phrase translator. English is mandatory; the
 * other-language columns may be null/blank when the operator hasn't
 * filled them in yet.
 *
 * <p>Two viewers consume these rows:
 * <ul>
 *   <li>The user-selectable window — operator picks which target
 *       columns are visible.</li>
 *   <li>The DXCC-driven window — the active callsign's DXCC entity
 *       drives the visible columns via {@code DxccLanguageMap}.</li>
 * </ul>
 */
public class Translation {

    private long   id;
    private String category;
    private String english;
    private String spanish;
    private String german;
    private String portuguese;
    private String phonetic;

    public Translation() {}

    public Translation(long id, String category, String english, String spanish,
                       String german, String portuguese, String phonetic) {
        this.id         = id;
        this.category   = category;
        this.english    = english;
        this.spanish    = spanish;
        this.german     = german;
        this.portuguese = portuguese;
        this.phonetic   = phonetic;
    }

    public long   getId()         { return id; }
    public String getCategory()   { return category; }
    public String getEnglish()    { return english; }
    public String getSpanish()    { return spanish; }
    public String getGerman()     { return german; }
    public String getPortuguese() { return portuguese; }
    public String getPhonetic()   { return phonetic; }

    public void setId(long id)               { this.id = id; }
    public void setCategory(String s)        { this.category = s; }
    public void setEnglish(String s)         { this.english = s; }
    public void setSpanish(String s)         { this.spanish = s; }
    public void setGerman(String s)          { this.german = s; }
    public void setPortuguese(String s)      { this.portuguese = s; }
    public void setPhonetic(String s)        { this.phonetic = s; }

    /**
     * Generic accessor used by the JavaFX TableView columns so a single
     * column definition can swap between languages.
     */
    public String forLanguage(String code) {
        if (code == null) return english;
        switch (code) {
            case "en": return english;
            case "es": return spanish;
            case "de": return german;
            case "pt": return portuguese;
            default:   return null;
        }
    }
}
