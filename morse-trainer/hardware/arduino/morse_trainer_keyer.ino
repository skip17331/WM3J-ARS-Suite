/*
 * Morse Trainer Keyer Firmware
 * ----------------------------
 * Targets: Arduino Pro Micro, Leonardo, Uno, Nano (anything with Serial).
 * For HID keyboard mode see morse_trainer_hid.ino instead.
 *
 * Supports three input modes (set INPUT_MODE below):
 *   MODE_STRAIGHT  : single key, one contact between KEY_DIT_PIN and GND
 *   MODE_PADDLE_A  : iambic paddle, mode A (no element memory after release)
 *   MODE_PADDLE_B  : iambic paddle, mode B (one extra opposite element after release)
 *
 * Wiring:
 *   Straight key:  tip -> KEY_DIT_PIN, sleeve -> GND
 *   Paddle:        dit -> KEY_DIT_PIN, dah -> KEY_DAH_PIN, common -> GND
 *   (INPUT_PULLUP is enabled, so no external resistors needed.)
 *   Optional sidetone: piezo between SIDETONE_PIN and GND (with 1k resistor).
 *
 * Serial protocol @ 115200 baud, line-terminated:
 *   Straight key:
 *     DOWN <millis>
 *     UP   <millis>
 *   Paddle (iambic decoded):
 *     ELEM DIT <millis>
 *     ELEM DAH <millis>
 *   Diagnostic / heartbeat:
 *     READY <mode> <wpm>
 *     PING  <millis>
 *
 * Host commands (newline-terminated):
 *   WPM <n>          : set keyer WPM (5..50)
 *   MODE STRAIGHT    : switch to straight-key mode
 *   MODE PADDLE_A    : switch to iambic mode A
 *   MODE PADDLE_B    : switch to iambic mode B
 *   SIDETONE ON|OFF  : enable/disable on-board piezo
 *   PING             : reply with PING <millis>
 */

#include <Arduino.h>

// ---- Configuration ----------------------------------------------------------
#define MODE_STRAIGHT  0
#define MODE_PADDLE_A  1
#define MODE_PADDLE_B  2

uint8_t inputMode = MODE_STRAIGHT;

const uint8_t KEY_DIT_PIN  = 2;
const uint8_t KEY_DAH_PIN  = 3;
const uint8_t SIDETONE_PIN = 9;
const uint8_t LED_PIN      = LED_BUILTIN;

uint8_t  keyerWpm     = 18;
uint16_t sidetoneHz   = 650;
bool     sidetoneOn   = true;

const uint16_t DEBOUNCE_MS = 3;

// ---- Derived state ----------------------------------------------------------
uint16_t ditMs;
uint16_t dahMs;
uint16_t gapMs;

void recomputeTiming() {
  ditMs = 1200 / keyerWpm;
  dahMs = 3 * ditMs;
  gapMs = ditMs;
}

// ---- Straight-key state -----------------------------------------------------
bool     skLastStable    = false;
bool     skLastRaw       = false;
uint32_t skLastChangeMs  = 0;

// ---- Paddle state -----------------------------------------------------------
bool     ditPressed      = false;
bool     dahPressed      = false;
bool     ditMemory       = false;
bool     dahMemory       = false;
uint8_t  lastElementSent = 0;

inline bool readDit() { return digitalRead(KEY_DIT_PIN) == LOW; }
inline bool readDah() { return digitalRead(KEY_DAH_PIN) == LOW; }

void sidetoneStart() {
  if (sidetoneOn && SIDETONE_PIN != 0) tone(SIDETONE_PIN, sidetoneHz);
  digitalWrite(LED_PIN, HIGH);
}
void sidetoneStop() {
  if (SIDETONE_PIN != 0) noTone(SIDETONE_PIN);
  digitalWrite(LED_PIN, LOW);
}

void emitDown(uint32_t t) { Serial.print(F("DOWN "));   Serial.println(t); }
void emitUp  (uint32_t t) { Serial.print(F("UP "));     Serial.println(t); }
void emitElem(bool dah, uint32_t t) {
  Serial.print(F("ELEM "));
  Serial.print(dah ? F("DAH ") : F("DIT "));
  Serial.println(t);
}

void setup() {
  pinMode(KEY_DIT_PIN, INPUT_PULLUP);
  pinMode(KEY_DAH_PIN, INPUT_PULLUP);
  pinMode(LED_PIN, OUTPUT);
  digitalWrite(LED_PIN, LOW);
  Serial.begin(115200);
  uint32_t t0 = millis();
  while (!Serial && (millis() - t0) < 1500) { /* wait */ }
  recomputeTiming();
  Serial.print(F("READY "));
  Serial.print(inputMode);
  Serial.print(' ');
  Serial.println(keyerWpm);
}

void runStraightKey() {
  bool raw = readDit();
  uint32_t now = millis();
  if (raw != skLastRaw) {
    skLastRaw = raw;
    skLastChangeMs = now;
  }
  if ((now - skLastChangeMs) >= DEBOUNCE_MS && raw != skLastStable) {
    skLastStable = raw;
    if (raw) { sidetoneStart(); emitDown(now); }
    else     { sidetoneStop();  emitUp(now); }
  }
}

enum KeyerPhase { IDLE, ELEMENT_ON, INTER_GAP };
KeyerPhase keyerPhase = IDLE;
uint32_t   phaseEndMs = 0;
bool       sendingDah = false;

void scanPaddles() {
  bool nowDit = readDit();
  bool nowDah = readDah();
  if (keyerPhase != IDLE) {
    if (nowDit) ditMemory = true;
    if (nowDah) dahMemory = true;
  }
  ditPressed = nowDit;
  dahPressed = nowDah;
}

void startElement(bool dah) {
  sendingDah = dah;
  uint32_t now = millis();
  sidetoneStart();
  emitElem(dah, now);
  phaseEndMs = now + (dah ? dahMs : ditMs);
  keyerPhase = ELEMENT_ON;
  lastElementSent = dah ? 2 : 1;
  if (dah) dahMemory = false; else ditMemory = false;
}

void runIambic(bool modeB) {
  scanPaddles();
  uint32_t now = millis();
  switch (keyerPhase) {
    case IDLE: {
      bool wantDit = ditPressed || ditMemory;
      bool wantDah = dahPressed || dahMemory;
      if (wantDit && wantDah)      startElement(lastElementSent != 2);
      else if (wantDit)            startElement(false);
      else if (wantDah)            startElement(true);
      break;
    }
    case ELEMENT_ON: {
      if ((int32_t)(now - phaseEndMs) >= 0) {
        sidetoneStop();
        phaseEndMs = now + gapMs;
        keyerPhase = INTER_GAP;
      }
      break;
    }
    case INTER_GAP: {
      if ((int32_t)(now - phaseEndMs) >= 0) {
        bool oppositePressed = sendingDah ? ditPressed : dahPressed;
        bool oppositeMem     = sendingDah ? ditMemory  : dahMemory;
        bool samePressed     = sendingDah ? dahPressed : ditPressed;
        bool sameMem         = sendingDah ? dahMemory  : ditMemory;

        bool modeBTrail = (modeB && !ditPressed && !dahPressed && oppositeMem);

        if (!modeB && !ditPressed) ditMemory = false;
        if (!modeB && !dahPressed) dahMemory = false;

        if (oppositePressed || modeBTrail)   startElement(!sendingDah);
        else if (samePressed || sameMem)     startElement(sendingDah);
        else                                 keyerPhase = IDLE;
      }
      break;
    }
  }
}

String cmdBuf;

void handleCommand(String& s) {
  s.trim();
  if (s.length() == 0) return;
  if (s.startsWith("WPM ")) {
    int v = s.substring(4).toInt();
    if (v >= 5 && v <= 50) {
      keyerWpm = (uint8_t)v;
      recomputeTiming();
      Serial.print(F("OK WPM ")); Serial.println(keyerWpm);
    } else {
      Serial.println(F("ERR WPM range 5..50"));
    }
  } else if (s == "MODE STRAIGHT") {
    inputMode = MODE_STRAIGHT;
    keyerPhase = IDLE; sidetoneStop();
    Serial.println(F("OK MODE STRAIGHT"));
  } else if (s == "MODE PADDLE_A") {
    inputMode = MODE_PADDLE_A;
    keyerPhase = IDLE; sidetoneStop();
    Serial.println(F("OK MODE PADDLE_A"));
  } else if (s == "MODE PADDLE_B") {
    inputMode = MODE_PADDLE_B;
    keyerPhase = IDLE; sidetoneStop();
    Serial.println(F("OK MODE PADDLE_B"));
  } else if (s == "SIDETONE ON")  { sidetoneOn = true;  Serial.println(F("OK SIDETONE ON")); }
    else if (s == "SIDETONE OFF") { sidetoneOn = false; sidetoneStop(); Serial.println(F("OK SIDETONE OFF")); }
    else if (s == "PING") { Serial.print(F("PING ")); Serial.println(millis()); }
    else { Serial.print(F("ERR UNKNOWN ")); Serial.println(s); }
}

void pollSerial() {
  while (Serial.available()) {
    char c = (char)Serial.read();
    if (c == '\n' || c == '\r') {
      if (cmdBuf.length()) { handleCommand(cmdBuf); cmdBuf = ""; }
    } else if (cmdBuf.length() < 64) {
      cmdBuf += c;
    }
  }
}

void loop() {
  pollSerial();
  switch (inputMode) {
    case MODE_STRAIGHT: runStraightKey(); break;
    case MODE_PADDLE_A: runIambic(false); break;
    case MODE_PADDLE_B: runIambic(true);  break;
  }
}
