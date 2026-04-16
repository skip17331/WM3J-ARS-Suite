# J-Map — Java/JavaFX

A cross-platform desktop amateur radio operator dashboard and world map display.

---

## Features

| Feature | Status |
|---|---|
| Flat world map (equirectangular projection) | ✅ |
| Grayline / day-night terminator (real-time, NOAA SPA algorithm) | ✅ |
| DX spots overlay (mock + DXHeat live) | ✅ |
| Solar & geomagnetic data (SFI, Kp, A-index, sunspot number) | ✅ |
| Sunspot graphic (visual activity chart) | ✅ |
| Propagation data (FOT, MUF, band conditions 80m–6m) | ✅ |
| Aurora overlay (NOAA OVATION mock + live PNG) | ✅ |
| Weather overlay (OpenWeatherMap tile) | ✅ |
| Tropo ducting overlay (Hepburn maps) | ✅ |
| UTC & local time displays | ✅ |
| Great-circle rotor map (lower-right, azimuthal equidistant) | ✅ |
| Green short-path / red long-path bearing lines | ✅ |
| Beam-width arc with configurable degrees | ✅ |
| Arduino HTTP/UDP/WebSocket rotor input | ✅ |
| Web-based Setup Page (mobile-friendly) | ✅ |
| Live settings update without restart | ✅ |
| Persistent JSON settings (~/.j-map/settings.json) | ✅ |
| Mock data mode (no internet required) | ✅ |

---

## Prerequisites

| Tool | Minimum Version |
|---|---|
| Java JDK | 17+ |
| Maven | 3.8+ |
| JavaFX | Bundled via Maven dependencies |

> **Note:** JavaFX is included as a Maven dependency. No separate JavaFX installation is required.

---

## Quick Start

```bash
# 1. Clone or extract the project
cd j-map

# 2. Build
mvn clean package -DskipTests

# 3. Run
mvn javafx:run
```

Or run the fat JAR directly:

```bash
java -jar target/j-map-1.0.0-fat.jar
```

---

## Running Tests

```bash
mvn test
```

Test coverage includes:
- **Solar Position Algorithm** — solstice/equinox declination, subsolar longitude drift, Julian day
- **Grayline / Night Mask** — polar day, polar night, equinox 50% split
- **Great-Circle Math** — distance, initial bearing, long-path arithmetic
- **Sunrise / Sunset** — London summer/winter, equatorial equinox, hemisphere validation
- **Propagation Logic** — band condition classification, MUF/FOT relationship, provider lifecycle
- **Rotor Data** — long-path arithmetic, azimuth normalization, mock provider sweep
- **DX Spots** — frequency-to-band mapping, spot age, band colors, mock provider output
- **Solar Data** — Kp classification, SFI quality, x-ray parsing, mock provider ranges
- **Settings Serialization** — JSON round-trip, null safety, default values

---

## Setup Page

After launch, open a browser to:

```
http://localhost:8080/setup
```

The Setup Page is **only accessible via the web server** — it never appears on the main display.

Use it from any device on your local network:

```
http://<your-machine-ip>:8080/setup
```

### What you can configure

**Operator Identity**
- Callsign, QTH latitude/longitude, Maidenhead grid square, timezone

**Data Sources**
- Toggle between mock data and live data
- NOAA API key (solar/geomagnetic)
- OpenWeatherMap API key (weather overlay)

**Map & Overlays**
- World map, grayline, grayline opacity
- DX spots with band filter and age filter
- Aurora, weather, and tropo ducting overlays

**Rotor Map**
- Enable/disable the great-circle map
- Arduino IP, port, and protocol (HTTP/UDP/WebSocket)
- Beam-width arc and long-path line

**Time Displays**
- Local time, UTC time, timezone

**Solar & Propagation**
- Solar data panel, sunspot graphic, propagation data, band conditions

All settings are saved to `~/.j-map/settings.json` and applied **instantly** to the running display without restart.

---

## Project Structure

```
src/main/java/com/wm3j/jmap/
├── app/
│   ├── JMapApp.java              ← Entry point (JavaFX Application)
│   └── ServiceRegistry.java      ← DI container, provider wiring
├── service/
│   ├── DataProvider.java         ← Base provider interface
│   ├── DataProviderException.java
│   ├── AbstractDataProvider.java ← Caching base class
│   ├── astronomy/
│   │   ├── SolarPositionService.java    ← NOAA SPA algorithm
│   │   ├── SolarPosition.java
│   │   ├── GraylineService.java         ← Night mask computation
│   │   ├── NightMask.java
│   │   └── SunriseSunsetService.java
│   ├── solar/
│   │   ├── SolarData.java
│   │   ├── SolarDataProvider.java
│   │   ├── NoaaSolarDataProvider.java   ← NOAA SWPC live
│   │   └── MockSolarDataProvider.java
│   ├── propagation/
│   │   ├── PropagationData.java
│   │   ├── PropagationDataProvider.java
│   │   ├── HamQslPropagationProvider.java ← HamQSL XML API
│   │   └── MockPropagationProvider.java
│   ├── aurora/
│   │   ├── AuroraOverlay.java
│   │   ├── AuroraProvider.java
│   │   ├── NoaaOvationProvider.java     ← NOAA OVATION PNG
│   │   └── MockAuroraProvider.java
│   ├── weather/
│   │   ├── WeatherOverlay.java
│   │   ├── WeatherProvider.java
│   │   ├── OpenWeatherMapProvider.java
│   │   └── MockWeatherProvider.java
│   ├── tropo/
│   │   ├── TropoOverlay.java
│   │   ├── TropoProvider.java
│   │   ├── HepburnTropoProvider.java
│   │   └── MockTropoProvider.java
│   ├── dx/
│   │   ├── DxSpot.java
│   │   ├── DxSpotProvider.java
│   │   ├── HttpDxSpotProvider.java      ← DXHeat JSON API
│   │   └── MockDxSpotProvider.java
│   ├── rotor/
│   │   ├── RotorData.java
│   │   ├── RotorProvider.java
│   │   ├── ArduinoRotorHttpProvider.java
│   │   └── MockRotorProvider.java
│   └── config/
│       ├── Settings.java                ← All configuration
│       └── SettingsLoader.java          ← JSON persistence
├── ui/
│   ├── main/
│   │   ├── MainWindow.java              ← JavaFX Stage manager
│   │   └── DashboardLayout.java         ← Layout orchestrator
│   ├── overlays/
│   │   └── WorldMapCanvas.java          ← Map + grayline + DX + aurora
│   ├── panels/
│   │   ├── TimePanel.java               ← UTC + local time
│   │   ├── SolarDataPanel.java          ← SFI/Kp/A/SSN + sunspot graphic
│   │   ├── PropagationPanel.java        ← FOT/MUF/LUF
│   │   ├── BandConditionsPanel.java     ← Color-coded band conditions
│   │   └── SetupHintBar.java            ← Bottom bar with setup URL
│   └── rotor/
│       └── RotorMapPane.java            ← Azimuthal equidistant map
└── web/
    ├── SetupWebServer.java              ← NanoHTTPD embedded server
    └── SetupPageHtml.java               ← Mobile-friendly Setup Page HTML

src/test/java/com/wm3j/jmap/
├── astronomy/
│   ├── SolarPositionServiceTest.java
│   ├── GreatCircleMathTest.java
│   ├── SunriseSunsetServiceTest.java
│   └── SolarDataTest.java
├── propagation/
│   └── PropagationLogicTest.java
├── rotor/
│   └── RotorDataTest.java
└── dx/
    └── DxSpotTest.java
```

---

## Data Sources

| Data | Provider | API/URL |
|---|---|---|
| Solar (SFI, Kp, A) | NOAA SWPC | `services.swpc.noaa.gov` |
| Propagation (MUF, bands) | HamQSL / N0NBH | `hamqsl.com/solarxml.php` |
| Aurora overlay | NOAA OVATION | `services.swpc.noaa.gov/images/` |
| DX spots | DXHeat API | `dxheat.com/dxc/` |
| Weather | OpenWeatherMap | `tile.openweathermap.org` |
| Tropo ducting | Hepburn | `dxinfocentre.com` |
| Solar position / grayline | Internal | NOAA SPA algorithm |
| Rotor control | Arduino | HTTP / UDP / WebSocket |

---

## Arduino Rotor Interface

The rotor controller expects your Arduino to serve an HTTP endpoint:

```
GET http://<arduino-ip>:<port>/rotor
```

**JSON response format (recommended):**
```json
{
  "azimuth": 135.5,
  "elevation": 0.0,
  "moving": false
}
```

**Plain text format (simple):**
```
135.5
```

Example Arduino sketch stub:

```cpp
#include <Ethernet.h>
#include <EthernetServer.h>

EthernetServer server(80);

void loop() {
  EthernetClient client = server.available();
  if (client) {
    // ... read request headers ...
    float azimuth = readRotorSensor(); // your sensor code
    client.println("HTTP/1.1 200 OK");
    client.println("Content-Type: application/json");
    client.println();
    client.print("{\"azimuth\":");
    client.print(azimuth, 1);
    client.println(",\"elevation\":0.0,\"moving\":false}");
    client.stop();
  }
}
```

---

## World Map Image

For the best visual result, place an equirectangular world map image at:

```
src/main/resources/images/world_map.jpg
```

Recommended free sources:
- Natural Earth: `naturalearthdata.com` (high-resolution, public domain)
- NASA Blue Marble: `visibleearth.nasa.gov`

The application renders a fallback blue-ocean background if no image is found.

---

## Keyboard Shortcuts

| Key | Action |
|---|---|
| `F` or `F11` | Toggle full screen |
| `Ctrl+S` (Setup Page) | Save settings |

---

## Configuration File

Settings are persisted at:

```
~/.j-map/settings.json
```

You can edit this file directly, or use the Setup Page at `http://localhost:8080/setup`.

---

## License

MIT License — free for amateur radio use.

73 de WM3J
