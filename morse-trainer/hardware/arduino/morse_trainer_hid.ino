/*
 * Morse Trainer HID Keyer Firmware
 * --------------------------------
 * Targets: Arduino Pro Micro / Leonardo / SparkFun Pro Micro (ATmega32U4),
 *          or any board with USB HID support (e.g. RP2040 with TinyUSB).
 *          DOES NOT WORK on Uno/Nano (they use a separate USB-serial chip
 *          and cannot present as a HID keyboard).
 *
 * Behavior: emulates a USB keyboard. Key press = Space down, release = Space up.
 * Auto-repeat is suppressed by holding the key down for the entire press
 * duration (the host OS only generates one keydown event per physical press
 * because we never release+press repeatedly).
 *
 * Use this firmware if you want zero application configuration: the trainer
 * (or any other Morse software that accepts spacebar input) will see your
 * key as if it were a regular USB keyboard. No serial port selection needed.
 *
 * For paddle / iambic support with HID, the firmware generates a sequence
 * of timed Space presses matching the dit and dah durations. Set INPUT_MODE
 * accordingly.
 *
 * Wiring is identical to morse_trainer_keyer.ino.
 */

#include <Keyboard.h>

#define MODE_STRAIGHT  0
#define MODE_PADDLE_A  1
#define MODE_PADDLE_B  2

uint8_t inputMode = MODE_STRAIGHT;

const uint8_t KEY_DIT_PIN  = 2;
const uint8_t KEY_DAH_PIN  = 3;
const uint8_t LED_PIN      = LED_BUILTIN;

uint8_t keyerWpm = 18;
uint16_t ditMs, dahMs, gapMs;
const uint16_t DEBOUNCE_MS = 3;

void recomputeTiming() {
  ditMs = 1200 / keyerWpm;
  dahMs = 3 * ditMs;
  gapMs = ditMs;
}

bool     skLastStable = false, skLastRaw = false;
uint32_t skLastChangeMs = 0;

inline bool readDit() { return digitalRead(KEY_DIT_PIN) == LOW; }
inline bool readDah() { return digitalRead(KEY_DAH_PIN) == LOW; }

void hidDown() { Keyboard.press(' '); digitalWrite(LED_PIN, HIGH); }
void hidUp()   { Keyboard.release(' '); digitalWrite(LED_PIN, LOW); }

void setup() {
  pinMode(KEY_DIT_PIN, INPUT_PULLUP);
  pinMode(KEY_DAH_PIN, INPUT_PULLUP);
  pinMode(LED_PIN, OUTPUT);
  digitalWrite(LED_PIN, LOW);
  Keyboard.begin();
  recomputeTiming();
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
    if (raw) hidDown(); else hidUp();
  }
}

// For iambic over HID, we emit each element as a timed Space press.
// Element memory and squeeze logic mirrors the serial firmware.
enum KeyerPhase { IDLE, ELEMENT_ON, INTER_GAP };
KeyerPhase keyerPhase = IDLE;
uint32_t   phaseEndMs = 0;
bool       sendingDah = false;
bool       ditPressed = false, dahPressed = false;
bool       ditMemory  = false, dahMemory  = false;
uint8_t    lastElementSent = 0;

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
  hidDown();
  phaseEndMs = millis() + (dah ? dahMs : ditMs);
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
      if      (wantDit && wantDah) startElement(lastElementSent != 2);
      else if (wantDit)            startElement(false);
      else if (wantDah)            startElement(true);
      break;
    }
    case ELEMENT_ON:
      if ((int32_t)(now - phaseEndMs) >= 0) {
        hidUp();
        phaseEndMs = now + gapMs;
        keyerPhase = INTER_GAP;
      }
      break;
    case INTER_GAP:
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

void loop() {
  switch (inputMode) {
    case MODE_STRAIGHT: runStraightKey(); break;
    case MODE_PADDLE_A: runIambic(false); break;
    case MODE_PADDLE_B: runIambic(true);  break;
  }
}
