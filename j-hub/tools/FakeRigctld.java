/*
 * FakeRigctld — a dependency-free stand-in for the Hamlib rigctld daemon.
 *
 * Purpose
 * -------
 * Lets you exercise j-hub's Hamlib rig backend (and j-digi PTT/CW) with no real
 * radio and no Hamlib install. It speaks the rigctld TCP text protocol the same
 * way a real rigctld does — crucially including the quirk that bare get-commands
 * ("f", "m") return a value with NO trailing "RPRT" line, while '+'-prefixed
 * (extended) commands label every field and DO terminate with "RPRT x".
 *
 * That quirk is exactly what broke j-hub: the old HamlibRigController.sendCommand
 * blocked waiting for an "RPRT" line after "f" that real rigctld never sends.
 * So this tool doubles as a regression fixture:
 *   • Run it in default mode and point j-hub at it. Fixed j-hub (sends "+f")
 *     connects and shows the frequency. Pre-fix j-hub (sends "f") hangs and
 *     loops "rigctld socket opened …" every ~3s — the reported symptom.
 *
 * Build/run (JDK 11+, single-file source launch — no compile step needed):
 *   java tools/FakeRigctld.java [options]
 *
 * Options
 *   --port N      listen port                    (default 4532)
 *   --freq HZ     initial frequency in Hz         (default 14074000)
 *   --mode M      initial mode                    (default USB)
 *   --pb HZ       passband in Hz                  (default 3000)
 *   --sweep       advance freq +100 Hz per read   (so RIG_STATUS keeps updating)
 *   --hang        accept the socket but never reply (simulates "rigctld up,
 *                 radio mute" — j-hub should report a read-timeout diagnostic)
 *   --rprt N      answer every command with "RPRT N" (N!=0 → simulates the rig
 *                 refusing/timeil; e.g. -5 = Hamlib timeout talking to the rig)
 *   --quiet       don't log each command line
 *
 * Test recipes
 *   normal rig:        java tools/FakeRigctld.java --sweep
 *   rig won't answer:  java tools/FakeRigctld.java --hang
 *   rig errors out:    java tools/FakeRigctld.java --rprt -5
 *   rigctld not up:    (just don't run this) → j-hub shows "Can't reach rigctld"
 */
import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

public class FakeRigctld {

    static int     port   = 4532;
    static boolean sweep  = false;
    static boolean hang   = false;
    static boolean quiet  = false;
    static int     rprt   = 0;            // non-zero → reply RPRT <rprt> to everything
    static int     pb     = 3000;
    static final AtomicLong            freq = new AtomicLong(14074000L);
    static final AtomicReference<String> mode = new AtomicReference<>("USB");
    static volatile boolean ptt = false;

    public static void main(String[] args) throws Exception {
        for (int i = 0; i < args.length; i++) {
            switch (args[i]) {
                case "--port":  port = Integer.parseInt(args[++i]); break;
                case "--freq":  freq.set(Long.parseLong(args[++i])); break;
                case "--mode":  mode.set(args[++i]); break;
                case "--pb":    pb   = Integer.parseInt(args[++i]); break;
                case "--rprt":  rprt = Integer.parseInt(args[++i]); break;
                case "--sweep": sweep = true; break;
                case "--hang":  hang  = true; break;
                case "--quiet": quiet = true; break;
                default: System.err.println("unknown option: " + args[i]); System.exit(2);
            }
        }
        ServerSocket server = new ServerSocket();
        server.setReuseAddress(true);
        server.bind(new InetSocketAddress("0.0.0.0", port));
        System.out.printf("FakeRigctld on :%d  freq=%d mode=%s%s%s%s%n",
                port, freq.get(), mode.get(),
                sweep ? " sweep" : "", hang ? " HANG" : "",
                rprt != 0 ? " RPRT=" + rprt : "");
        while (true) {
            Socket s = server.accept();
            Thread t = new Thread(() -> serve(s), "conn-" + s.getPort());
            t.setDaemon(true);
            t.start();
        }
    }

    static void serve(Socket s) {
        String peer = s.getRemoteSocketAddress().toString();
        if (!quiet) System.out.println("+ connect " + peer);
        try (Socket sock = s) {
            BufferedReader in = new BufferedReader(
                    new InputStreamReader(sock.getInputStream(), StandardCharsets.US_ASCII));
            OutputStream out = sock.getOutputStream();
            String line;
            while ((line = in.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty()) continue;
                if (!quiet) System.out.println("  " + peer + " > " + line);
                if (hang) continue;                       // read & ignore: never reply
                boolean ext = line.startsWith("+");
                String cmd  = ext ? line.substring(1).trim() : line;
                String reply = handle(cmd, ext);
                if (reply == null) break;                  // quit
                out.write(reply.getBytes(StandardCharsets.US_ASCII));
                out.flush();
            }
        } catch (IOException ignored) {
        } finally {
            if (!quiet) System.out.println("- close   " + peer);
        }
    }

    /** Returns the wire reply, or null to close the connection (quit). */
    static String handle(String cmd, boolean ext) {
        String[] tok = cmd.split("\\s+");
        String op = tok[0];

        if (op.equals("q") || op.equals("Q")) return null;

        // Forced-error mode: every command fails like a rig that won't talk.
        if (rprt != 0) {
            return ext ? longName(op) + ":\nRPRT " + rprt + "\n"
                       : "RPRT " + rprt + "\n";
        }

        switch (op) {
            case "f": case "\\get_freq": {
                long f = sweep ? freq.addAndGet(100) : freq.get();
                return ext ? "get_freq:\nFrequency: " + f + "\nRPRT 0\n"
                           : f + "\n";
            }
            case "m": case "\\get_mode": {
                String md = mode.get();
                return ext ? "get_mode:\nMode: " + md + "\nPassband: " + pb + "\nRPRT 0\n"
                           : md + "\n" + pb + "\n";
            }
            case "t": case "\\get_ptt":
                return ext ? "get_ptt:\nPTT: " + (ptt ? 1 : 0) + "\nRPRT 0\n"
                           : (ptt ? 1 : 0) + "\n";
            case "v": case "\\get_vfo":
                return ext ? "get_vfo:\nVFO: VFOA\nRPRT 0\n" : "VFOA\n";

            case "F": case "\\set_freq":
                if (tok.length > 1) try { freq.set((long) Double.parseDouble(tok[1])); }
                                    catch (NumberFormatException ignored) {}
                return setReply(ext, "set_freq", cmd);
            case "M": case "\\set_mode":
                if (tok.length > 1) mode.set(tok[1]);
                return setReply(ext, "set_mode", cmd);
            case "T": case "\\set_ptt":
                ptt = tok.length > 1 && tok[1].equals("1");
                return setReply(ext, "set_ptt", cmd);
            case "L": case "\\set_level":           // e.g. L KEYSPD 22
                return setReply(ext, "set_level", cmd);
            case "b": case "\\send_morse":          // CW out — accepted, no real keying
                return setReply(ext, "send_morse", cmd);
            case "\\stop_morse":
                return setReply(ext, "stop_morse", cmd);
            case "\\chk_vfo":
                return ext ? "ChkVFO: 0\n" : "CHKVFO 0\n";

            default:
                // Unknown command → Hamlib RIG_ENIMPL (-11)
                return ext ? longName(op) + ":\nRPRT -11\n" : "RPRT -11\n";
        }
    }

    /** Set-command reply: extended echoes "<name>: <args>" + RPRT 0; plain is just RPRT 0. */
    static String setReply(boolean ext, String name, String cmd) {
        if (!ext) return "RPRT 0\n";
        int sp = cmd.indexOf(' ');
        String args = sp < 0 ? "" : cmd.substring(sp + 1).trim();
        return name + ": " + args + "\nRPRT 0\n";
    }

    static String longName(String op) {
        switch (op) {
            case "f": return "get_freq";   case "m": return "get_mode";
            case "F": return "set_freq";   case "M": return "set_mode";
            case "t": return "get_ptt";    case "T": return "set_ptt";
            case "L": return "set_level";  case "b": return "send_morse";
            case "v": return "get_vfo";
            default:  return op.startsWith("\\") ? op.substring(1) : op;
        }
    }
}
