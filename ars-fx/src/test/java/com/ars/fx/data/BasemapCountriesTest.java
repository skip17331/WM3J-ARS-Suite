package com.ars.fx.data;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/** J-Map basemap, punch list 5-7: country shapes and colours, and azimuthal shapes with no antipode wedge. */
class BasemapCountriesTest {
    static final double MAX = 140;

    @Test void countryShapesLoad() {
        List<CountryShapes.Country> all = CountryShapes.all();
        assertTrue(all.size() > 200, "countries: " + all.size());
        for (CountryShapes.Country c : all) assertTrue(c.colour() >= 1 && c.colour() <= 7, c.code() + " " + c.colour());
        assertEquals("ARG", CountryShapes.at(-34.6, -58.4).code(), "Buenos Aires");
        assertEquals("USA", CountryShapes.at(39.95, -75.17).code(), "Philadelphia");
        assertEquals("DEU", CountryShapes.at(52.52, 13.40).code(), "Berlin");
        assertEquals("ZAF", CountryShapes.at(-26.2, 28.05).code(), "Johannesburg");
        assertEquals("LSO", CountryShapes.at(-29.3, 27.5).code(), "Maseru: Lesotho is a hole in South Africa, not part of it");
        assertNull(CountryShapes.at(40, -40), "mid-Atlantic");
        assertTrue(CountryShapes.at(-80, 0).ice());
    }

    @Test void neighboursNeverShareAColour() {
        // pairs that share a land border
        String[][] pairs = {{"USA","CAN"},{"USA","MEX"},{"FRA","DEU"},{"DEU","POL"},{"ARG","CHL"},{"BRA","ARG"},{"CHN","RUS"},{"IND","PAK"},{"ZAF","LSO"},{"ESP","PRT"}};
        java.util.Map<String, Integer> col = new java.util.HashMap<>();
        for (CountryShapes.Country c : CountryShapes.all()) col.put(c.code(), c.colour());
        for (String[] p : pairs) assertNotEquals(col.get(p[0]), col.get(p[1]), p[0] + "/" + p[1]);
    }

    /** A box of land, as a closed ring in lon / lat. */
    static double[] box(double lon0, double lat0, double lon1, double lat1) {
        return new double[]{lon0, lat0, lon1, lat0, lon1, lat1, lon0, lat1, lon0, lat0};
    }
    static double[] at(double qLat, double qLon, double brgDeg, double distDeg) {
        double b = Math.toRadians(brgDeg);
        return new double[]{Math.sin(b) * distDeg, -Math.cos(b) * distDeg};
    }

    @Test void landRoundTheAntipodeIsFilledOnTheRightSide() {
        // station on the equator at 60 W; antipode 0, 120 E. A continent from 70 E to 170 E covers the antipode: from the station it
        // starts 130 degrees away to the east, so a narrow strip of it is on the visible disk and the rest is beyond the edge.
        double qLat = 0, qLon = -60;
        List<double[]> p = AzimuthalPath.polygon(List.of(box(70, -50, 170, 50)), qLat, qLon, MAX, false);
        assertFalse(AzimuthalPath.filled(p, 0, 0), "the station is at sea");
        double[] east132 = at(qLat, qLon, 90, 132), east100 = at(qLat, qLon, 90, 100), west132 = at(qLat, qLon, 270, 132), north60 = at(qLat, qLon, 0, 60);
        assertTrue(AzimuthalPath.filled(p, east132[0], east132[1]), "132 degrees east is 72 E: on the continent");
        assertFalse(AzimuthalPath.filled(p, east100[0], east100[1]), "100 degrees east is 40 E: sea");
        assertTrue(AzimuthalPath.filled(p, west132[0], west132[1]), "132 degrees west is 168 E: on the continent too");
        assertFalse(AzimuthalPath.filled(p, north60[0], north60[1]), "no wedge across the middle");
    }

    @Test void smallIslandAtTheAntipodeDrawsNothingVisible() {
        List<double[]> p = AzimuthalPath.polygon(List.of(box(115, -5, 125, 5)), 0, -60, MAX, false);
        for (int brg = 0; brg < 360; brg += 15)
            for (int d = 0; d <= 134; d += 20) {
                double[] xy = at(0, -60, brg, d);
                assertFalse(AzimuthalPath.filled(p, xy[0], xy[1]), "nothing at " + brg + " / " + d);
            }
    }

    @Test void theStationsOwnContinentAndAnOrdinaryOneStillFill() {
        double qLat = 40, qLon = -75;                                  // eastern US
        List<double[]> home = AzimuthalPath.polygon(List.of(box(-125, 25, -65, 50)), qLat, qLon, MAX, true);
        assertTrue(AzimuthalPath.filled(home, 0, 0));
        double[] west20 = at(qLat, qLon, 270, 20);
        assertTrue(AzimuthalPath.filled(home, west20[0], west20[1]));
        List<double[]> europe = AzimuthalPath.polygon(List.of(box(-10, 36, 30, 60)), qLat, qLon, MAX, false);
        double[] berlin = AzimuthalPath.point(52.5, 13.4, qLat, qLon);
        assertTrue(AzimuthalPath.filled(europe, berlin[0], berlin[1]));
        assertFalse(AzimuthalPath.filled(europe, 0, 0));
        assertEquals(1, europe.size(), "no extra edge circle for an ordinary shape");
    }

    @Test void realAsiaFromBuenosAires() {
        // the case from the original note: a station in Argentina, land at its antipode (China)
        double qLat = -34.6, qLon = -58.4;
        CountryShapes.Country china = CountryShapes.all().stream().filter(c -> c.code().equals("CHN")).findFirst().orElseThrow();
        CountryShapes.Polygon main = china.polygons()[0];
        for (CountryShapes.Polygon q : china.polygons()) if (q.rings()[0].points() > main.rings()[0].points()) main = q;
        List<double[]> rings = new ArrayList<>();
        for (VectorBasemap.Ring r : main.rings()) rings.add(r.ll());
        List<double[]> p = AzimuthalPath.polygon(rings, qLat, qLon, MAX, false);
        for (int brg = 0; brg < 360; brg += 5)
            for (int d = 0; d <= 120; d += 10) {
                double[] xy = at(qLat, qLon, brg, d);
                assertFalse(AzimuthalPath.filled(p, xy[0], xy[1]), "China is over 150 degrees away, yet filled at " + brg + " / " + d);
            }
    }
}
