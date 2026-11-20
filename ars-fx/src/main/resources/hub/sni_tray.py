#!/usr/bin/env python3
"""ARS Suite Hub: a KDE / freedesktop StatusNotifierItem (the tray icon Plasma draws natively).

Java's own tray icon goes through an XEmbed proxy that never delivers clicks on KDE Wayland. This helper registers a proper
StatusNotifierItem instead. A click, a middle click or a right click on the icon prints QUIT on stdout; the hub reads that and
shuts down. It prints READY once the icon is registered, and exits when the hub (its parent) goes away.

usage: sni_tray.py ICON_DIR ICON_NAME TOOLTIP
"""
import os, sys

try:
    import dbus, dbus.service, dbus.mainloop.glib
    from gi.repository import GLib
except Exception as e:
    print("UNAVAILABLE " + str(e), flush=True)
    sys.exit(2)

ICON_DIR, ICON_NAME, TIP = sys.argv[1], sys.argv[2], sys.argv[3]
IFACE = "org.kde.StatusNotifierItem"


class Item(dbus.service.Object):
    def __init__(self, bus):
        dbus.service.Object.__init__(self, bus, "/StatusNotifierItem")
        self.props = {
            "Category": "ApplicationStatus", "Id": "ars-suite-hub", "Title": "ARS Suite Hub", "Status": "Active",
            "WindowId": dbus.UInt32(0), "IconName": ICON_NAME, "IconThemePath": ICON_DIR, "ItemIsMenu": False,
            "ToolTip": dbus.Struct(("", dbus.Array([], signature="(iiay)"), "ARS Suite Hub", TIP), signature=None),
        }

    def _quit(self):
        print("QUIT", flush=True)

    @dbus.service.method(IFACE, in_signature="ii")
    def Activate(self, x, y): self._quit()

    @dbus.service.method(IFACE, in_signature="ii")
    def SecondaryActivate(self, x, y): self._quit()

    @dbus.service.method(IFACE, in_signature="ii")
    def ContextMenu(self, x, y): self._quit()

    @dbus.service.method(IFACE, in_signature="is")
    def Scroll(self, delta, orientation): pass

    @dbus.service.method(dbus.PROPERTIES_IFACE, in_signature="ss", out_signature="v")
    def Get(self, iface, name): return self.props[name]

    @dbus.service.method(dbus.PROPERTIES_IFACE, in_signature="s", out_signature="a{sv}")
    def GetAll(self, iface): return self.props


def main():
    dbus.mainloop.glib.DBusGMainLoop(set_as_default=True)
    bus = dbus.SessionBus()
    name = "org.kde.StatusNotifierItem-%d-1" % os.getpid()
    bus_name = dbus.service.BusName(name, bus)
    item = Item(bus)
    watcher = bus.get_object("org.kde.StatusNotifierWatcher", "/StatusNotifierWatcher")
    watcher.RegisterStatusNotifierItem(name, dbus_interface="org.kde.StatusNotifierWatcher")
    print("READY", flush=True)
    parent = os.getppid()

    def watch():
        if os.getppid() != parent:           # the hub is gone: so are we
            loop.quit()
            return False
        return True
    GLib.timeout_add_seconds(2, watch)
    loop = GLib.MainLoop()
    loop.run()


try:
    main()
except Exception as e:
    print("UNAVAILABLE " + str(e), flush=True)
    sys.exit(2)
