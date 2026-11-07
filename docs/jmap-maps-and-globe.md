# J-Map: the maps review and the 3D globe

Written 2026-11-06 for the two TODO items "Maps: look at the J-Map options and see whether something better exists" and "3D globe as a map
option". The globe was tried, not just thought about: a throwaway program drew a textured globe with markers and a lifted great-circle arc in
JavaFX 21 on Windows (`docs/images/jmap-globe-probe.png`).

> **Update 2026-11-06:** the vector basemap below is built. Both views now draw Natural Earth 1:50 m land and borders (`VectorBasemap`, `land50.bin.gz`, `borders50.bin.gz`); the two pictures in the table (and `AzimuthalBasemap`) are gone, `bluemarble.jpg` stays for J-Sat. The flat map zooms with the wheel and pans by dragging.

## What J-Map drew before

| View | Basemap | Notes |
|---|---|---|
| Flat | `bluemarble.jpg`, 2048 x 1024 (263 KB), equirectangular | Spots, beam, rings, gray line and overlays are drawn on a Canvas over it. |
| Azimuthal | `gcm-gray.jpg`, 768 x 768 (42 KB), azimuthal-equidistant, edge = antipode | Drawn clipped to the disk; spots and the beam are projected by the same projection. |

## A bug the review found, and fixed

The azimuthal picture is **centred on about 40.0 N, 77.0 W** (the US east coast), but the map on screen is centred on the *operator's* station.
Used as it was, the continents were in the wrong place against the spots, the beam and the rings for anyone away from there. Checked numerically:
the picture's land and sea match the satellite image 95% of the time at that centre, and 71% at a Los Angeles centre. It is now turned to the
station's own position at start-up (`AzimuthalBasemap`, tested against the satellite image for stations in seven places).

## Options for something better

| Option | Gain | Cost / catch | Verdict |
|---|---|---|---|
| **Vector maps** (Natural Earth, public domain) drawn on the Canvas | Crisp at any size and zoom, any projection (Mercator, Robinson, Winkel Tripel, azimuthal about any point, orthographic) from one data file, country borders, no resampling | A data file of about 0.1 MB (110 m) to 1-2 MB (50 m), a projection and polygon-drawing layer, labels. Medium. | **Best next step.** Removes the picture-resampling and gives zoom. |
| Higher-resolution raster (NASA Blue Marble 5400 x 2700, public domain) | Sharper flat map | 2-4 MB in the jar, more memory, still blurry when zoomed | Small win; only if vector is not done. |
| Slippy tiles (OpenStreetMap and others) | Streets, zoom | Needs the internet, which a field station may not have; OSM's tile policy forbids heavy use without your own server and most others need a key; attribution; a tile cache | Not as the default. |
| JavaFX WebView + Leaflet / MapLibre | Familiar, many layers | WebView needs the `javafx.web` natives (large), a JavaScript bridge, online tiles | No. |
| 3D globe | Looks good, intuitive | See below | Optional extra view, after vector maps. |

## The 3D globe: possible, with traps

**Possible: yes.** `Platform.isSupported(ConditionalFeature.SCENE3D)` is true here. A `Sphere` with a `PhongMaterial` diffuse map takes the satellite
image; stations are small spheres, the beam and great-circle paths are thin cylinders lifted off the surface; a snapshot of 66 nodes took about
4-5 ms after the first (about 110-280 ms), and building the scene about 190 ms. Rotation by dragging is a `Rotate` transform, zoom a camera move.

**Traps found, with the fix for each:**

1. **The texture is laid on the wrong latitudes.** JavaFX's `Sphere` puts its rings evenly in *height*, so row `v` of the texture is drawn at
   `sin(latitude) = 1 - 2v`, not at an even step of angle. An ordinary world map comes out squashed toward the equator (Britain at 52 N is drawn at
   35 N). Fix: pre-warp the picture vertically once, so row `v` shows latitude `asin(1 - 2v)`. With that, London's marker sits on south-east England
   and Cape Town's on the tip of Africa (see the picture).
2. **Marker positions.** With the camera looking along +z, put latitude `la`, longitude `lo` at
   `x = r cos(la) sin(lo)`, `y = -r sin(la)`, `z = -r cos(la) cos(lo)`; longitude 0 faces the camera. Longitude then agrees with the texture.
3. **Lines.** The 3D renderer has no thick-line primitive: use short cylinders (6 sides is enough) joined along the great circle, or a mesh strip.
4. **Labels** cannot be 3D text without effort: project each marker to the screen (`localToScene`) and draw ordinary 2D labels over the scene.
5. **Day and night** is a second texture (the night mask the azimuthal map already computes, remade each minute) blended over the first, or a light
   placed at the sub-solar point.
6. **Picking** (clicking a spot) uses the 3D `PickResult`; the Canvas hit-testing in `JMapView` does not carry over.
7. **Environments.** Software rendering (a virtual machine, some Raspberry Pi desktops) can be slow; keep it an opt-in view and fall back to the flat
   map when `SCENE3D` is unsupported.

**Cost:** a basic globe with the station, spots, the beam, great-circle paths, drag and zoom, and a day / night terminator is about 3 to 5 days.
It is also a third drawing path to keep in step with the flat and azimuthal views (about 1,000 lines of Canvas code in `JMapView` would not be shared).

**Value:** mostly looks. For working the bands the azimuthal map is better (beam headings and distances read straight off it), so this is the last of
the three to build.

## Recommended order

1. Done: centre the azimuthal map on the station.
2. Vector basemap (Natural Earth) with zoom and pan, and country borders: medium, the best gain.
3. 3D globe as an optional view, using the recipe above: medium to large.
