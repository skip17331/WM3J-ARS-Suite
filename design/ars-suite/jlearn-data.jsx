/* J-Learn — reference / learning library data. */

const JLEARN_CATS = [
  { id: 'lic', name: 'Licensing & exams', n: 6 },
  { id: 'prop', name: 'Propagation', n: 5 },
  { id: 'ant', name: 'Antennas', n: 8 },
  { id: 'ops', name: 'Operating practice', n: 7 },
  { id: 'rules', name: 'Rules & regs', n: 4 },
  { id: 'elec', name: 'Electronics', n: 9 },
];

const JLEARN_DECKS = [
  { id: 1, cat: 'lic', title: 'Extra Class — Subelement E3', sub: 'Radio wave propagation', cards: 42, done: 31, due: 8, kind: 'Flashcards' },
  { id: 2, cat: 'prop', title: 'Sporadic-E & the gray line', sub: 'When & where to work DX', cards: 18, done: 18, due: 0, kind: 'Guide' },
  { id: 3, cat: 'ant', title: 'Feedlines & SWR', sub: 'Loss, matching, baluns', cards: 26, done: 12, due: 14, kind: 'Flashcards' },
  { id: 4, cat: 'ops', title: 'CW operating procedures', sub: 'Q-codes, abbreviations, etiquette', cards: 60, done: 47, due: 6, kind: 'Flashcards' },
  { id: 5, cat: 'rules', title: 'Part 97 essentials', sub: 'Band plans, power, identification', cards: 30, done: 22, due: 4, kind: 'Reference' },
  { id: 6, cat: 'elec', title: 'Smith chart basics', sub: 'Impedance matching visually', cards: 15, done: 3, due: 12, kind: 'Guide' },
  { id: 7, cat: 'ops', title: 'Contest exchange formats', sub: 'CQ WW, WPX, SS, Field Day', cards: 22, done: 22, due: 0, kind: 'Reference' },
  { id: 8, cat: 'ant', title: 'Yagi design fundamentals', sub: 'Boom, element spacing, gain', cards: 34, done: 9, due: 19, kind: 'Flashcards' },
];

const JLEARN_REF = [
  { t: 'Q-code quick reference', tag: 'QRZ · QSB · QRM · QSY…' },
  { t: 'RST signal reporting', tag: 'Readability · Strength · Tone' },
  { t: 'Band plan — HF (Region 2)', tag: '160m – 10m segments' },
  { t: 'Phonetic alphabet', tag: 'Alfa · Bravo · Charlie…' },
  { t: 'Grid square locator math', tag: 'Maidenhead system' },
  { t: 'Common CW abbreviations', tag: 'GM · OM · HW · CUL · 73' },
];

// reading pane sample (Q-codes)
const JLEARN_ARTICLE = {
  title: 'Q-code quick reference',
  cat: 'Operating practice',
  read: 4,
  body: [
    ['p', 'Q-codes are three-letter signals beginning with Q, used to convey common phrases quickly — essential on CW where every character counts. Each works as both a statement and a question.'],
    ['h', 'Most-used on the air'],
    ['kv', 'QRZ', 'Who is calling me?'],
    ['kv', 'QSB', 'Your signal is fading.'],
    ['kv', 'QRM', 'I have interference (man-made).'],
    ['kv', 'QRN', 'I have static / noise (natural).'],
    ['kv', 'QSY', 'Change frequency to ___.'],
    ['kv', 'QTH', 'My location is ___.'],
    ['kv', 'QSL', 'I acknowledge receipt.'],
    ['kv', 'QRP', 'Reduce power / low power.'],
    ['h', 'As a question vs. statement'],
    ['p', 'Append a question mark on CW or raise inflection on phone to ask: “QRL?” means “Is this frequency in use?” while “QRL” states “The frequency is in use.” Always send QRL? before calling CQ on a clear-sounding frequency.'],
  ],
};

Object.assign(window, { JLEARN_CATS, JLEARN_DECKS, JLEARN_REF, JLEARN_ARTICLE });
