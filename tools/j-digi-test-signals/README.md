# J-Digi test signals

Generated WAV test signals for the J-Digi decoders, with the scripts that make and play them.

- `generate_<mode>_test_suite.py` — writes `<mode>_test_suite/*.wav`. Run from this folder (output paths are relative).
- `run_cw_test.sh`, `run_psk31_test.sh` — play a suite through a PulseAudio null sink (`jdigi_loop`) so J-Digi can
  capture it from "Monitor of JDigiLoop". `./run_cw_test.sh list` shows the variants.
- `rtty-*.wav` — loose RTTY samples.
- `j-digi/src/test/.../Ax25ModeTest` decodes `ax25_test_suite/*.wav` from here.
