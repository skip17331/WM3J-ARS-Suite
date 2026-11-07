#!/usr/bin/env python3
"""Mouse and keyboard for the ARS Suite window on a real X display (python-xlib XTEST; there is no xdotool here).

All coordinates are SCREEN PIXELS, i.e. the same numbers as in a screenshot file taken with gnome-screenshot.

  driver.py click X Y            one left click at X, Y
  driver.py dbl X Y              double click
  driver.py drag X1 Y1 X2 Y2     press at 1, move to 2 in ten steps, release
  driver.py wheel X Y N          N wheel notches at X, Y (N > 0 up / zoom in, N < 0 down)
  driver.py type TEXT            type letters, digits, space, - and / (needs a focused text field)
  driver.py key NAME [N]         press a named key N times, e.g. key BackSpace 8, key End, key Return
"""
import sys, time
from Xlib import X, XK, display
from Xlib.ext import xtest

d = display.Display()

def move(x, y):
    xtest.fake_input(d, X.MotionNotify, x=int(x), y=int(y)); d.sync(); time.sleep(0.08)

def button(b, down):
    xtest.fake_input(d, X.ButtonPress if down else X.ButtonRelease, b); d.sync(); time.sleep(0.08)

def keycode(name):
    return d.keysym_to_keycode(XK.string_to_keysym(name))

def tap(name):
    kc = keycode(name); xtest.fake_input(d, X.KeyPress, kc); xtest.fake_input(d, X.KeyRelease, kc); d.sync(); time.sleep(0.04)

def main(argv):
    if len(argv) < 2: print(__doc__); return 2
    cmd, a = argv[1], argv[2:]
    if cmd == "click":
        move(a[0], a[1]); button(1, True); button(1, False)
    elif cmd == "dbl":
        move(a[0], a[1])
        for _ in range(2): button(1, True); button(1, False)
    elif cmd == "drag":
        x1, y1, x2, y2 = [float(v) for v in a[:4]]
        move(x1, y1); button(1, True)
        for i in range(1, 11): move(x1 + (x2 - x1) * i / 10, y1 + (y2 - y1) * i / 10)
        button(1, False)
    elif cmd == "wheel":
        move(a[0], a[1]); n = int(a[2]); b = 4 if n > 0 else 5
        for _ in range(abs(n)): button(b, True); button(b, False)
    elif cmd == "type":
        for ch in a[0]:
            name = {" ": "space", "-": "minus", "/": "slash"}.get(ch, ch)
            if ch.isupper(): xtest.fake_input(d, X.KeyPress, keycode("Shift_L"))
            tap(name)
            if ch.isupper(): xtest.fake_input(d, X.KeyRelease, keycode("Shift_L"))
            d.sync()
    elif cmd == "key":
        for _ in range(int(a[1]) if len(a) > 1 else 1): tap(a[0])
    else:
        print(__doc__); return 2
    return 0

if __name__ == "__main__":
    sys.exit(main(sys.argv))
