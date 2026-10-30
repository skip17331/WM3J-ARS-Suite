#!/usr/bin/env python3
"""
Morse Trainer Wireless Keyer (Raspberry Pi Zero W)
==================================================

Sends key events to the Morse Trainer desktop app over UDP/Wi-Fi.

Supported modes:
    straight    : single straight key on KEY_DIT_PIN (default GPIO 17)
    paddle_a    : iambic paddle, mode A (no element memory after release)
    paddle_b    : iambic paddle, mode B (one trailing opposite element)

Wiring (BCM numbering):
    Straight key:  tip -> GPIO 17, sleeve -> GND
    Paddle:        dit -> GPIO 17, dah -> GPIO 27, common -> GND

Requirements:
    sudo apt install python3-gpiozero

Run:
    python3 keyer.py --mode straight --host 192.168.1.10 --port 51234
    python3 keyer.py --mode paddle_a --wpm 22 --host 192.168.1.10
    python3 keyer.py --mode paddle_b --sidetone   # piezo on GPIO 18

Wire protocol (UTF-8, newline-terminated UDP packets):
    Straight key:  DOWN|<millis>|<batteryPct>
                   UP|<millis>|<batteryPct>
    Paddle:        ELEM|DIT|<millis>|<batteryPct>
                   ELEM|DAH|<millis>|<batteryPct>
    Heartbeat:     PING|<millis>|<batteryPct>     (every 5s)
"""

import argparse
import socket
import sys
import time
from threading import Thread, Event

try:
    from gpiozero import Button
except ImportError:
    print("gpiozero not installed. Run: sudo apt install python3-gpiozero", file=sys.stderr)
    sys.exit(1)

# Optional sidetone via PWM on GPIO 18
try:
    from gpiozero import TonalBuzzer
    from gpiozero.tones import Tone
    HAS_BUZZER = True
except ImportError:
    HAS_BUZZER = False


# ---------------------------------------------------------------------------
# Configuration defaults
# ---------------------------------------------------------------------------
DEFAULT_HOST     = "192.168.1.10"
DEFAULT_PORT     = 51234
DEFAULT_DIT_PIN  = 17
DEFAULT_DAH_PIN  = 27
SIDETONE_PIN     = 18
DEFAULT_WPM      = 18
HEARTBEAT_SEC    = 5.0


# ---------------------------------------------------------------------------
# Battery telemetry stub
# ---------------------------------------------------------------------------
def battery_pct() -> int:
    """Return battery percentage 0..100, or -1 if no monitor wired up.

    Replace this with a real implementation if you add an INA219, MAX17048,
    or similar fuel-gauge IC. Reading via I2C is straightforward; see the
    project README for an example.
    """
    return -1


# ---------------------------------------------------------------------------
# UDP transport
# ---------------------------------------------------------------------------
class UdpSender:
    def __init__(self, host: str, port: int):
        self.addr = (host, port)
        self.sock = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)
        # Allow sending broadcasts if user passes a .255 address
        self.sock.setsockopt(socket.SOL_SOCKET, socket.SO_BROADCAST, 1)

    def send(self, payload: str) -> None:
        try:
            self.sock.sendto(payload.encode("utf-8"), self.addr)
        except OSError as e:
            print(f"[udp] send failed: {e}", file=sys.stderr)

    def down(self):
        self.send(f"DOWN|{int(time.time() * 1000)}|{battery_pct()}\n")

    def up(self):
        self.send(f"UP|{int(time.time() * 1000)}|{battery_pct()}\n")

    def elem(self, kind: str):  # 'DIT' or 'DAH'
        self.send(f"ELEM|{kind}|{int(time.time() * 1000)}|{battery_pct()}\n")

    def ping(self):
        self.send(f"PING|{int(time.time() * 1000)}|{battery_pct()}\n")


# ---------------------------------------------------------------------------
# Sidetone
# ---------------------------------------------------------------------------
class Sidetone:
    def __init__(self, enabled: bool, freq_hz: int = 650):
        self.enabled = enabled and HAS_BUZZER
        self.buzzer = None
        if self.enabled:
            try:
                self.buzzer = TonalBuzzer(SIDETONE_PIN)
                self.tone = Tone(freq_hz)
            except Exception as e:
                print(f"[sidetone] disabled: {e}", file=sys.stderr)
                self.enabled = False

    def on(self):
        if self.enabled and self.buzzer:
            try: self.buzzer.play(self.tone)
            except Exception: pass

    def off(self):
        if self.enabled and self.buzzer:
            try: self.buzzer.stop()
            except Exception: pass


# ---------------------------------------------------------------------------
# Straight key
# ---------------------------------------------------------------------------
def run_straight(args, udp: UdpSender, sidetone: Sidetone, stop: Event):
    key = Button(args.dit_pin, pull_up=True, bounce_time=0.003)
    def pressed():
        sidetone.on()
        udp.down()
    def released():
        sidetone.off()
        udp.up()
    key.when_pressed = pressed
    key.when_released = released
    print(f"[straight] listening on GPIO {args.dit_pin}")
    while not stop.is_set():
        time.sleep(0.1)


# ---------------------------------------------------------------------------
# Iambic keyer (Python implementation, mirrors the Arduino firmware)
# ---------------------------------------------------------------------------
class IambicKeyer:
    def __init__(self, args, udp: UdpSender, sidetone: Sidetone, mode_b: bool):
        self.dit_btn = Button(args.dit_pin, pull_up=True, bounce_time=0.002)
        self.dah_btn = Button(args.dah_pin, pull_up=True, bounce_time=0.002)
        self.udp = udp
        self.sidetone = sidetone
        self.mode_b = mode_b
        self.dit_ms = 1200 // args.wpm
        self.dah_ms = 3 * self.dit_ms
        self.gap_ms = self.dit_ms
        self.last_sent = 0     # 1=dit 2=dah
        self.dit_mem = False
        self.dah_mem = False
        print(f"[iambic {'B' if mode_b else 'A'}] WPM={args.wpm} dit={self.dit_ms}ms "
              f"dit_pin={args.dit_pin} dah_pin={args.dah_pin}")

    def _wait_with_memory(self, ms: int):
        """Sleep ms milliseconds while watching paddles for memory updates."""
        end = time.monotonic() + (ms / 1000.0)
        while time.monotonic() < end:
            if self.dit_btn.is_pressed: self.dit_mem = True
            if self.dah_btn.is_pressed: self.dah_mem = True
            time.sleep(0.001)

    def _send_element(self, dah: bool):
        ms = self.dah_ms if dah else self.dit_ms
        self.sidetone.on()
        self.udp.elem("DAH" if dah else "DIT")
        self._wait_with_memory(ms)
        self.sidetone.off()
        self.last_sent = 2 if dah else 1
        if dah: self.dah_mem = False
        else:   self.dit_mem = False
        # inter-element gap
        gap_end = time.monotonic() + (self.gap_ms / 1000.0)
        while time.monotonic() < gap_end:
            if self.dit_btn.is_pressed: self.dit_mem = True
            if self.dah_btn.is_pressed: self.dah_mem = True
            time.sleep(0.001)

    def run(self, stop: Event):
        while not stop.is_set():
            dit_p = self.dit_btn.is_pressed
            dah_p = self.dah_btn.is_pressed
            # Idle scan
            if not (dit_p or dah_p or self.dit_mem or self.dah_mem):
                time.sleep(0.002)
                continue

            # Decide what to send next
            sending_dah = False
            if (dit_p or self.dit_mem) and (dah_p or self.dah_mem):
                # squeeze: alternate
                sending_dah = (self.last_sent != 2)
            elif dit_p or self.dit_mem:
                sending_dah = False
            elif dah_p or self.dah_mem:
                sending_dah = True
            else:
                continue

            self._send_element(sending_dah)

            # Mode A/B decision after element + gap
            opp_pressed = self.dah_btn.is_pressed if not sending_dah else self.dit_btn.is_pressed
            opp_mem     = self.dah_mem if not sending_dah else self.dit_mem
            mode_b_trail = (self.mode_b
                            and not self.dit_btn.is_pressed
                            and not self.dah_btn.is_pressed
                            and opp_mem)
            if not self.mode_b and not self.dit_btn.is_pressed: self.dit_mem = False
            if not self.mode_b and not self.dah_btn.is_pressed: self.dah_mem = False

            if opp_pressed or mode_b_trail:
                # opposite element will be picked up on next loop iteration
                # via the priority logic; force memory so it happens
                if sending_dah: self.dit_mem = True
                else:           self.dah_mem = True


def run_paddle(args, udp: UdpSender, sidetone: Sidetone, stop: Event, mode_b: bool):
    keyer = IambicKeyer(args, udp, sidetone, mode_b)
    keyer.run(stop)


# ---------------------------------------------------------------------------
# Heartbeat
# ---------------------------------------------------------------------------
def heartbeat_loop(udp: UdpSender, stop: Event):
    while not stop.is_set():
        udp.ping()
        stop.wait(HEARTBEAT_SEC)


# ---------------------------------------------------------------------------
# Main
# ---------------------------------------------------------------------------
def parse_args():
    p = argparse.ArgumentParser(description="Morse Trainer wireless keyer")
    p.add_argument("--mode", choices=["straight", "paddle_a", "paddle_b"],
                   default="straight", help="input mode")
    p.add_argument("--host", default=DEFAULT_HOST,
                   help="host (laptop) IP address running the desktop trainer")
    p.add_argument("--port", type=int, default=DEFAULT_PORT,
                   help="UDP port to send to")
    p.add_argument("--dit-pin", type=int, default=DEFAULT_DIT_PIN,
                   help="GPIO pin (BCM) for dit / straight key")
    p.add_argument("--dah-pin", type=int, default=DEFAULT_DAH_PIN,
                   help="GPIO pin (BCM) for dah paddle")
    p.add_argument("--wpm", type=int, default=DEFAULT_WPM,
                   help="iambic keyer speed in words per minute")
    p.add_argument("--sidetone", action="store_true",
                   help="enable on-board piezo sidetone (TonalBuzzer on GPIO 18)")
    return p.parse_args()


def main():
    args = parse_args()
    udp = UdpSender(args.host, args.port)
    sidetone = Sidetone(args.sidetone)
    stop = Event()

    Thread(target=heartbeat_loop, args=(udp, stop), daemon=True).start()

    print(f"[main] mode={args.mode} -> {args.host}:{args.port}  "
          f"(Ctrl-C to stop)")
    try:
        if args.mode == "straight":
            run_straight(args, udp, sidetone, stop)
        elif args.mode == "paddle_a":
            run_paddle(args, udp, sidetone, stop, mode_b=False)
        elif args.mode == "paddle_b":
            run_paddle(args, udp, sidetone, stop, mode_b=True)
    except KeyboardInterrupt:
        pass
    finally:
        stop.set()
        sidetone.off()
        print("[main] shutting down")


if __name__ == "__main__":
    main()
