// J-Learn UI i18n — chrome strings only.
//
// Chapter content stays in English (the ~200 markdown sections are a
// content-team translation effort, not a code task). This bundle covers
// the navigation chrome — sidebar search, top-bar controls, placeholder
// text — so the surrounding shell follows the operator's chosen
// language even when the body content doesn't.
//
// Machine-translated; review recommended by native speakers. Operators
// can override their language with ?lang=fr / de / es / it / pt /
// en in the URL, or via the in-page selector once one is wired up.

const JLEARN_I18N = {
  en: {
    'brand.sub':       'Amateur Radio Reference Library',
    'text.size':       'Text size:',
    'theme':           'Theme:',
    'theme.dark':      'Dark',
    'theme.light':     'Light',
    'btn.reset':       'Reset',
    'btn.issue':       '🐛 Issue',
    'search.placeholder': 'Filter chapters…',
    'placeholder':     'Pick a chapter on the left to start reading. J-Learn remembers your last-opened section automatically. You can also link directly:',
    'advanced.toggle': 'Advanced',
  },
  de: {
    'brand.sub':       'Amateurfunk-Nachschlagewerk',
    'text.size':       'Textgröße:',
    'theme':           'Thema:',
    'theme.dark':      'Dunkel',
    'theme.light':     'Hell',
    'btn.reset':       'Zurücksetzen',
    'btn.issue':       '🐛 Problem',
    'search.placeholder': 'Kapitel filtern…',
    'placeholder':     'Wählen Sie links ein Kapitel zum Lesen aus. J-Learn merkt sich automatisch den zuletzt geöffneten Abschnitt. Sie können auch direkt verlinken:',
    'advanced.toggle': 'Erweitert',
  },
  es: {
    'brand.sub':       'Biblioteca de referencia de radioafición',
    'text.size':       'Tamaño del texto:',
    'theme':           'Tema:',
    'theme.dark':      'Oscuro',
    'theme.light':     'Claro',
    'btn.reset':       'Restablecer',
    'btn.issue':       '🐛 Reportar',
    'search.placeholder': 'Filtrar capítulos…',
    'placeholder':     'Elige un capítulo a la izquierda para comenzar a leer. J-Learn recuerda automáticamente la última sección abierta. También puedes enlazar directamente:',
    'advanced.toggle': 'Avanzado',
  },
  fr: {
    'brand.sub':       'Bibliothèque de référence radioamateur',
    'text.size':       'Taille du texte :',
    'theme':           'Thème :',
    'theme.dark':      'Sombre',
    'theme.light':     'Clair',
    'btn.reset':       'Réinitialiser',
    'btn.issue':       '🐛 Signaler',
    'search.placeholder': 'Filtrer les chapitres…',
    'placeholder':     'Choisissez un chapitre à gauche pour commencer la lecture. J-Learn se souvient automatiquement de la dernière section ouverte. Vous pouvez aussi établir un lien direct :',
    'advanced.toggle': 'Avancé',
  },
  it: {
    'brand.sub':       'Libreria di riferimento per radioamatori',
    'text.size':       'Dimensione del testo:',
    'theme':           'Tema:',
    'theme.dark':      'Scuro',
    'theme.light':     'Chiaro',
    'btn.reset':       'Reimposta',
    'btn.issue':       '🐛 Segnala',
    'search.placeholder': 'Filtra capitoli…',
    'placeholder':     'Scegli un capitolo a sinistra per iniziare a leggere. J-Learn ricorda automaticamente l\'ultima sezione aperta. Puoi anche collegarti direttamente:',
    'advanced.toggle': 'Avanzato',
  },
  pt: {
    'brand.sub':       'Biblioteca de referência para radioamadores',
    'text.size':       'Tamanho do texto:',
    'theme':           'Tema:',
    'theme.dark':      'Escuro',
    'theme.light':     'Claro',
    'btn.reset':       'Redefinir',
    'btn.issue':       '🐛 Relatar',
    'search.placeholder': 'Filtrar capítulos…',
    'placeholder':     'Escolha um capítulo à esquerda para começar a ler. O J-Learn lembra automaticamente da última seção aberta. Você também pode criar um link direto:',
    'advanced.toggle': 'Avançado',
  }
};

/**
 * Detect operator language. Priority:
 *   1. ?lang=xx in the URL (lets j-hub iframe override per session)
 *   2. localStorage 'jlearn.ui.lang'
 *   3. navigator.language top-level subtag
 *   4. fall back to English
 */
function jlearnDetectLanguage() {
  try {
    const url = new URLSearchParams(window.location.search);
    const q = url.get('lang');
    if (q && JLEARN_I18N[q]) return q;
  } catch (e) { /* ignore */ }
  try {
    const saved = localStorage.getItem('jlearn.ui.lang');
    if (saved && JLEARN_I18N[saved]) return saved;
  } catch (e) { /* ignore */ }
  const nav = (navigator.language || 'en').slice(0, 2).toLowerCase();
  return JLEARN_I18N[nav] ? nav : 'en';
}

function jlearnT(key, lang) {
  const bundle = JLEARN_I18N[lang] || JLEARN_I18N.en;
  return bundle[key] || JLEARN_I18N.en[key] || key;
}

/** Apply translated strings to known DOM nodes. Idempotent. */
function applyJLearnI18n(lang) {
  lang = lang || jlearnDetectLanguage();
  try { localStorage.setItem('jlearn.ui.lang', lang); } catch (e) { /* ignore */ }
  document.documentElement.lang = lang;

  const sub = document.querySelector('.brand-sub');
  if (sub) sub.textContent = jlearnT('brand.sub', lang);

  document.querySelectorAll('label.text-size').forEach(el => {
    el.childNodes[0].nodeValue = jlearnT('text.size', lang) + ' ';
  });
  document.querySelectorAll('label.theme-pick').forEach(el => {
    el.childNodes[0].nodeValue = jlearnT('theme', lang) + ' ';
  });
  const themeSel = document.getElementById('jl-theme');
  if (themeSel) {
    [...themeSel.options].forEach(o => {
      if (o.value === 'mocha') o.textContent = jlearnT('theme.dark',  lang);
      if (o.value === 'latte') o.textContent = jlearnT('theme.light', lang);
    });
  }
  const search = document.getElementById('jl-search');
  if (search) search.placeholder = jlearnT('search.placeholder', lang);

  const placeholder = document.querySelector('.placeholder');
  if (placeholder) {
    placeholder.firstChild.nodeValue = jlearnT('placeholder', lang) + ' ';
  }

  // Buttons rendered with emoji + text — preserve emoji, swap label.
  document.querySelectorAll('.btn-ghost').forEach(btn => {
    const txt = btn.textContent.trim();
    if (txt === 'Reset' || txt === jlearnT('btn.reset', 'de') ||
        txt === jlearnT('btn.reset', 'es') || txt === jlearnT('btn.reset', 'fr') ||
        txt === jlearnT('btn.reset', 'it') || txt === jlearnT('btn.reset', 'pt')) {
      btn.textContent = jlearnT('btn.reset', lang);
    } else if (txt.includes('🐛')) {
      btn.textContent = jlearnT('btn.issue', lang);
    }
  });
}

// Auto-apply on load — defers until DOMContentLoaded since this file is
// included after the body in index.html.
if (document.readyState === 'loading') {
  document.addEventListener('DOMContentLoaded', () => applyJLearnI18n());
} else {
  applyJLearnI18n();
}
