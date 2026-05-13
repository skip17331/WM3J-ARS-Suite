/* J-Vault i18n — six-language UI chrome.
 *
 * Detection priority:
 *   1. ?lang=xx in the URL
 *   2. localStorage.jvault_lang
 *   3. navigator.language
 *   4. fallback "en"
 *
 * Body content (equipment names, user notes, estate PDF copy) is operator-
 * written and remains in whatever language the operator typed it in.
 */
(function () {
  const JVAULT_I18N = {
    en: {
      "doc.title": "J-Vault — Shack Inventory & Estate Handoff",
      "brand.sub": "Shack Inventory · Estate Handoff",
      "station.name.ph": "Operator name",
      "station.call.ph": "Callsign",
      "prefs.textsize": "Text size:",
      "prefs.theme": "Theme:",
      "prefs.theme.dark": "Dark",
      "prefs.theme.light": "Light",
      "prefs.reset": "Reset",
      "prefs.report": "🐛 Report Issue",
      "help.title": "📋 Getting started",
      "help.hide": "Hide",
      "help.show": "Show",
      "card.equipment": "Equipment",
      "search.ph": "Filter (manufacturer, model, serial, notes)…",
      "filter.types.all": "All Types",
      "filter.disp.all": "All Dispositions",
      "filter.disp.working": "Working",
      "filter.disp.repairable": "Repairable",
      "filter.disp.notrepairable": "Not Repairable",
      "filter.install.both": "Installed + Storage",
      "filter.install.installed": "Installed only",
      "filter.install.storage": "Storage only",
      "lang.label": "Language:"
    },
    es: {
      "doc.title": "J-Vault — Inventario de estación y traspaso patrimonial",
      "brand.sub": "Inventario · Traspaso patrimonial",
      "station.name.ph": "Nombre del operador",
      "station.call.ph": "Indicativo",
      "prefs.textsize": "Tamaño del texto:",
      "prefs.theme": "Tema:",
      "prefs.theme.dark": "Oscuro",
      "prefs.theme.light": "Claro",
      "prefs.reset": "Restablecer",
      "prefs.report": "🐛 Reportar problema",
      "help.title": "📋 Empezar",
      "help.hide": "Ocultar",
      "help.show": "Mostrar",
      "card.equipment": "Equipos",
      "search.ph": "Filtrar (fabricante, modelo, serie, notas)…",
      "filter.types.all": "Todos los tipos",
      "filter.disp.all": "Todas las disposiciones",
      "filter.disp.working": "Funcional",
      "filter.disp.repairable": "Reparable",
      "filter.disp.notrepairable": "No reparable",
      "filter.install.both": "Instalado + Almacén",
      "filter.install.installed": "Solo instalado",
      "filter.install.storage": "Solo almacén",
      "lang.label": "Idioma:"
    },
    de: {
      "doc.title": "J-Vault — Shack-Inventar & Nachlass-Übergabe",
      "brand.sub": "Inventar · Nachlass-Übergabe",
      "station.name.ph": "Name des Operators",
      "station.call.ph": "Rufzeichen",
      "prefs.textsize": "Textgröße:",
      "prefs.theme": "Design:",
      "prefs.theme.dark": "Dunkel",
      "prefs.theme.light": "Hell",
      "prefs.reset": "Zurücksetzen",
      "prefs.report": "🐛 Problem melden",
      "help.title": "📋 Erste Schritte",
      "help.hide": "Ausblenden",
      "help.show": "Anzeigen",
      "card.equipment": "Geräte",
      "search.ph": "Filter (Hersteller, Modell, Seriennr., Notizen)…",
      "filter.types.all": "Alle Typen",
      "filter.disp.all": "Alle Zustände",
      "filter.disp.working": "Funktionsfähig",
      "filter.disp.repairable": "Reparierbar",
      "filter.disp.notrepairable": "Nicht reparierbar",
      "filter.install.both": "Installiert + Lager",
      "filter.install.installed": "Nur installiert",
      "filter.install.storage": "Nur Lager",
      "lang.label": "Sprache:"
    },
    fr: {
      "doc.title": "J-Vault — Inventaire de station & transmission successorale",
      "brand.sub": "Inventaire · Transmission successorale",
      "station.name.ph": "Nom de l'opérateur",
      "station.call.ph": "Indicatif",
      "prefs.textsize": "Taille du texte :",
      "prefs.theme": "Thème :",
      "prefs.theme.dark": "Sombre",
      "prefs.theme.light": "Clair",
      "prefs.reset": "Réinitialiser",
      "prefs.report": "🐛 Signaler un problème",
      "help.title": "📋 Démarrage",
      "help.hide": "Masquer",
      "help.show": "Afficher",
      "card.equipment": "Équipement",
      "search.ph": "Filtrer (fabricant, modèle, série, notes)…",
      "filter.types.all": "Tous types",
      "filter.disp.all": "Toutes dispositions",
      "filter.disp.working": "Fonctionne",
      "filter.disp.repairable": "Réparable",
      "filter.disp.notrepairable": "Non réparable",
      "filter.install.both": "Installé + Stock",
      "filter.install.installed": "Installé uniquement",
      "filter.install.storage": "Stock uniquement",
      "lang.label": "Langue :"
    },
    it: {
      "doc.title": "J-Vault — Inventario della stazione e passaggio testamentario",
      "brand.sub": "Inventario · Passaggio testamentario",
      "station.name.ph": "Nome dell'operatore",
      "station.call.ph": "Nominativo",
      "prefs.textsize": "Dimensione testo:",
      "prefs.theme": "Tema:",
      "prefs.theme.dark": "Scuro",
      "prefs.theme.light": "Chiaro",
      "prefs.reset": "Ripristina",
      "prefs.report": "🐛 Segnala problema",
      "help.title": "📋 Per iniziare",
      "help.hide": "Nascondi",
      "help.show": "Mostra",
      "card.equipment": "Apparati",
      "search.ph": "Filtra (produttore, modello, seriale, note)…",
      "filter.types.all": "Tutti i tipi",
      "filter.disp.all": "Tutte le condizioni",
      "filter.disp.working": "Funzionante",
      "filter.disp.repairable": "Riparabile",
      "filter.disp.notrepairable": "Non riparabile",
      "filter.install.both": "Installato + Magazzino",
      "filter.install.installed": "Solo installato",
      "filter.install.storage": "Solo magazzino",
      "lang.label": "Lingua:"
    },
    pt: {
      "doc.title": "J-Vault — Inventário da estação & transferência sucessória",
      "brand.sub": "Inventário · Transferência sucessória",
      "station.name.ph": "Nome do operador",
      "station.call.ph": "Indicativo",
      "prefs.textsize": "Tamanho do texto:",
      "prefs.theme": "Tema:",
      "prefs.theme.dark": "Escuro",
      "prefs.theme.light": "Claro",
      "prefs.reset": "Redefinir",
      "prefs.report": "🐛 Relatar problema",
      "help.title": "📋 Primeiros passos",
      "help.hide": "Ocultar",
      "help.show": "Mostrar",
      "card.equipment": "Equipamentos",
      "search.ph": "Filtrar (fabricante, modelo, série, notas)…",
      "filter.types.all": "Todos os tipos",
      "filter.disp.all": "Todas as condições",
      "filter.disp.working": "Funcional",
      "filter.disp.repairable": "Reparável",
      "filter.disp.notrepairable": "Não reparável",
      "filter.install.both": "Instalado + Estoque",
      "filter.install.installed": "Somente instalado",
      "filter.install.storage": "Somente estoque",
      "lang.label": "Idioma:"
    }
  };

  function jvaultDetectLanguage() {
    const url = new URLSearchParams(window.location.search).get("lang");
    if (url && JVAULT_I18N[url]) return url;
    try {
      const saved = localStorage.getItem("jvault_lang");
      if (saved && JVAULT_I18N[saved]) return saved;
    } catch (_) {}
    const nav = (navigator.language || "en").slice(0, 2);
    return JVAULT_I18N[nav] ? nav : "en";
  }

  function applyJVaultI18n(lang) {
    lang = JVAULT_I18N[lang] ? lang : "en";
    try { localStorage.setItem("jvault_lang", lang); } catch (_) {}
    const t = JVAULT_I18N[lang];

    // <title>
    document.title = t["doc.title"];

    // Brand subtitle
    const hdrSub = document.querySelector(".hdr-sub");
    if (hdrSub) hdrSub.textContent = t["brand.sub"];

    // Station-info placeholders
    const stName = document.getElementById("station-name");
    if (stName) stName.placeholder = t["station.name.ph"];
    const stCall = document.getElementById("station-callsign");
    if (stCall) stCall.placeholder = t["station.call.ph"];

    // Header preference labels (the first text node of each <label>)
    const labels = document.querySelectorAll(".hdr-prefs > label");
    if (labels[0]) labels[0].firstChild && (labels[0].firstChild.textContent = "\n        " + t["prefs.textsize"] + "\n        ");
    if (labels[1]) labels[1].firstChild && (labels[1].firstChild.textContent = "\n        " + t["prefs.theme"] + "\n        ");

    const themeSel = document.getElementById("jv-theme");
    if (themeSel && themeSel.options.length >= 2) {
      themeSel.options[0].textContent = t["prefs.theme.dark"];
      themeSel.options[1].textContent = t["prefs.theme.light"];
    }

    document.querySelectorAll(".reset-btn").forEach(btn => {
      if (btn.textContent.trim() === "Reset") btn.textContent = t["prefs.reset"];
      else if (btn.textContent.trim() === "🐛 Report Issue") btn.textContent = t["prefs.report"];
    });

    // Inventory help panel header
    const helpHdr = document.querySelector("#inv-help .title");
    if (helpHdr) helpHdr.textContent = t["help.title"];
    const helpToggle = document.getElementById("inv-help-toggle");
    if (helpToggle) {
      const hidden = helpToggle.textContent.trim() === "Show" || helpToggle.textContent.trim() === t["help.show"];
      helpToggle.textContent = hidden ? t["help.show"] : t["help.hide"];
    }

    // Equipment card title (first card-title)
    const eqTitle = document.querySelector(".card .card-title");
    if (eqTitle) eqTitle.textContent = t["card.equipment"];

    const search = document.getElementById("inv-search");
    if (search) search.placeholder = t["search.ph"];

    const typeFilter = document.getElementById("inv-type-filter");
    if (typeFilter && typeFilter.options.length) typeFilter.options[0].textContent = t["filter.types.all"];

    const dispFilter = document.getElementById("inv-disposition-filter");
    if (dispFilter && dispFilter.options.length >= 4) {
      dispFilter.options[0].textContent = t["filter.disp.all"];
      dispFilter.options[1].textContent = t["filter.disp.working"];
      dispFilter.options[2].textContent = t["filter.disp.repairable"];
      dispFilter.options[3].textContent = t["filter.disp.notrepairable"];
    }

    const installFilter = document.getElementById("inv-install-filter");
    if (installFilter && installFilter.options.length >= 3) {
      installFilter.options[0].textContent = t["filter.install.both"];
      installFilter.options[1].textContent = t["filter.install.installed"];
      installFilter.options[2].textContent = t["filter.install.storage"];
    }
  }

  function ensureLanguageSelector() {
    if (document.getElementById("jv-lang")) return;
    const prefs = document.querySelector(".hdr-prefs");
    if (!prefs) return;
    const wrap = document.createElement("label");
    wrap.title = "UI language";
    wrap.style.marginRight = "8px";
    wrap.appendChild(document.createTextNode((JVAULT_I18N[jvaultDetectLanguage()] || JVAULT_I18N.en)["lang.label"] + " "));
    const sel = document.createElement("select");
    sel.id = "jv-lang";
    for (const code of ["en","es","de","fr","it","pt"]) {
      const opt = document.createElement("option");
      opt.value = code;
      opt.textContent = code;
      sel.appendChild(opt);
    }
    sel.value = jvaultDetectLanguage();
    sel.addEventListener("change", () => applyJVaultI18n(sel.value));
    wrap.appendChild(sel);
    prefs.insertBefore(wrap, prefs.firstChild);
  }

  document.addEventListener("DOMContentLoaded", () => {
    ensureLanguageSelector();
    applyJVaultI18n(jvaultDetectLanguage());
  });

  window.jvaultDetectLanguage = jvaultDetectLanguage;
  window.applyJVaultI18n = applyJVaultI18n;
})();
