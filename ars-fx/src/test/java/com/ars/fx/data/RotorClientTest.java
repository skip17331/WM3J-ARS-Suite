package com.ars.fx.data;

import org.junit.jupiter.api.Test;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Locale;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/** What the rotor client sends to rotctld, checked against a stand-in rotctld that records every command and answers "RPRT 0". */
class RotorClientTest {
    /** A one-purpose rotctld: accepts connections, remembers each line it is sent, replies RPRT 0. */
    private static final class FakeRotctld implements AutoCloseable {
        final ServerSocket server;
        final BlockingQueue<String> got = new LinkedBlockingQueue<>();
        FakeRotctld() throws IOException {
            server = new ServerSocket(0, 5, InetAddress.getLoopbackAddress());
            Thread t = new Thread(this::serve, "fake-rotctld"); t.setDaemon(true); t.start();
        }
        int port() { return server.getLocalPort(); }
        private void serve() {
            try { while (true) { Socket s = server.accept(); Thread c = new Thread(() -> talk(s), "fake-rotctld-conn"); c.setDaemon(true); c.start(); } }
            catch (IOException closed) { /* shut down */ }
        }
        private void talk(Socket s) {
            try (s; BufferedReader r = new BufferedReader(new InputStreamReader(s.getInputStream(), StandardCharsets.US_ASCII)); OutputStream o = s.getOutputStream()) {
                String line;
                while ((line = r.readLine()) != null) { got.add(line); o.write("RPRT 0\n".getBytes(StandardCharsets.US_ASCII)); o.flush(); }
            } catch (IOException ignored) { /* client went away */ }
        }
        String next() throws InterruptedException { return got.poll(3, TimeUnit.SECONDS); }
        @Override public void close() throws IOException { server.close(); }
    }

    private interface Body { void run(FakeRotctld fake, RotorClient rotor) throws Exception; }

    /** Runs {@code body} with the rotor pointed at a stand-in rotctld, in a throwaway settings folder, with the elevation setting as given. */
    private static void withRotor(boolean elevationRotor, Body body) throws Exception {
        String oldHome = System.getProperty("user.home"), oldPort = System.getProperty("rotor.port"), oldHost = System.getProperty("rotor.host");
        Locale oldLocale = Locale.getDefault();
        System.setProperty("user.home", Files.createTempDirectory("rotor-test").toString());            // not the user's real config
        HubConfig.reload();
        HubConfig.setBool("rotor.hasElevation", elevationRotor);
        try (FakeRotctld fake = new FakeRotctld()) {
            System.setProperty("rotor.host", "127.0.0.1"); System.setProperty("rotor.port", String.valueOf(fake.port()));
            RotorClient rotor = RotorClient.getInstance();
            rotor.reconnect();
            rotor.applyRemote(RotorClient.State.OFFLINE);
            body.run(fake, rotor);
        } finally {
            Locale.setDefault(oldLocale);
            restore("rotor.port", oldPort); restore("rotor.host", oldHost); System.setProperty("user.home", oldHome);
            HubConfig.reload();
            RotorClient.getInstance().reconnect();
        }
    }
    private static void restore(String key, String old) { if (old == null) System.clearProperty(key); else System.setProperty(key, old); }

    @Test
    void anElevationRotorIsSentTheAzimuthAndTheElevation() throws Exception {
        withRotor(true, (fake, rotor) -> {
            rotor.trackTo(123.4, 45.6);
            assertEquals("P 123.4 45.6", fake.next());
        });
    }

    @Test
    void anAzimuthOnlyRotorIsNeverSentAnElevation() throws Exception {
        withRotor(false, (fake, rotor) -> {
            rotor.trackTo(123.4, 45.6);                                   // the satellite is at 45.6 degrees; this rotor is not asked
            assertEquals("P 123.4 0.0", fake.next());                     // the elevation it was already at (nothing read yet: 0)
        });
    }

    @Test
    void elevationIsHeldBetweenTheHorizonAndStraightUp() throws Exception {
        withRotor(true, (fake, rotor) -> {
            rotor.trackTo(10, 95);
            assertEquals("P 10.0 90.0", fake.next());
            rotor.trackTo(20, -5);
            assertEquals("P 20.0 0.0", fake.next());
        });
    }

    @Test
    void theDeadBandAppliesToBothAxes() throws Exception {
        withRotor(true, (fake, rotor) -> {
            rotor.applyRemote(new RotorClient.State(true, 100, 30));       // the rotor reports it is at az 100, el 30
            rotor.trackTo(101, 31);                                        // within the 2 degree dead-band on both
            assertNull(fake.got.poll(600, TimeUnit.MILLISECONDS), "no move for a change inside the dead-band");
            rotor.trackTo(110, 30);                                        // azimuth has moved on
            assertEquals("P 110.0 30.0", fake.next());
            rotor.trackTo(100, 40);                                        // elevation alone has moved on
            assertEquals("P 100.0 40.0", fake.next());
        });
    }

    @Test
    void commandsUseADecimalPointWhateverTheSystemLocale() throws Exception {
        withRotor(true, (fake, rotor) -> {
            Locale.setDefault(Locale.GERMANY);                              // decimal comma
            rotor.trackTo(123.4, 45.6);
            assertEquals("P 123.4 45.6", fake.next());
            rotor.moveTo(200.5);                                            // the azimuth-only move too
            assertEquals("P 200.5 0.0", fake.next());
        });
    }
}
