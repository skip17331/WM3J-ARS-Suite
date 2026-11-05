# Legal notices — WM3J ARS Suite

Plain-language notices for people using or contributing to the suite. This is not legal advice.

## License

The WM3J ARS Suite is free software, released under the **GNU General Public License, version 3** — see [LICENSE](LICENSE)
(also at <https://www.gnu.org/licenses/gpl-3.0.html>). You may use, study, change and share it under those terms.

A copy of this licence, the Code of Conduct and these notes comes with the app (open them from the bottom of the Info
page) and is kept in `~/.j-hub/legal/`. When an update is downloaded, the app checks the copies in the new release and
replaces the local ones if they are newer.

The program is distributed in the hope that it will be useful, but **without any warranty**; without even the implied
warranty of merchantability or fitness for a particular purpose. See the GPL for details.

## Your station, your responsibility

The suite can key a transmitter (CAT, PTT, CW and digital keying), turn an antenna rotator and switch an amplifier. The
licensed operator remains responsible for everything that goes out over the air: frequencies, power, modes, station
identification, third-party rules, and compliance with the regulations that apply where you operate. The software is a
logging and station-control aid, not a substitute for the control operator, and it is not intended for safety-of-life use.

## Third-party content and trademarks

The **Info** page shows dates and headlines that come from other people's websites:

- DXpedition dates from the NG3K *Announced DX Operations* list (as an iCalendar feed).
- Headlines from the news feeds listed under **Info ▸ Feeds**, and from any feeds you add yourself.
- Contest and hamfest dates from calendar links that *you* supply.

That content belongs to its owners. The suite shows only the headline (or event title), the source's name and a link back
to the source's own site; it does not copy articles. Follow each site's own terms when you use their content. The suite
ships no built-in access to the WA7BNM Contest Calendar; if you add a link to it, its terms of use apply to you.

The app includes the **Contest Country Files** (`cty.dat`) by Jim Reisert AD1C, <https://www.country-files.com>, used under the
MIT licence (its notice is kept with the file in the app). They give the DXCC entity and the CQ / ITU zone for a callsign.

Names and marks such as ARRL, RSGB, AMSAT, CQ, QRZ, LoTW, eQSL, Club Log, WSJT-X, Hamlib and others belong to their
respective owners. Their appearance here only identifies the service; it does not mean the owner is connected with, or
endorses, this project.

## Privacy

The suite has no accounts and the author does not run a server that collects your data. It talks to the internet only for
the features you use, and to the services you set up:

- **Update check** — once a day it asks GitHub for the latest release, anonymously. Turn it off in J-Hub ▸ Station ▸ Updates.
- **Info page** — it downloads the feeds you have switched on. Those sites can see your IP address and a User-Agent of
  the form `WM3J-ARS-Suite/<version>`. Feeds are kept on disk for six hours so each is fetched rarely.
- **DX cluster, reverse beacon, callsign lookups, log uploads** (LoTW, eQSL, QRZ, Club Log) — only the ones you configure,
  with the logins you enter.

Your log, settings and caches stay on your computer in your home folder (`~/.j-log`, `~/.j-hub` and similar). Logins you
enter are stored there too, so protect that folder as you would any file holding passwords.

## Add, change or remove a source

If you own a site that appears in the Info feeds and would like it **removed**, or you would like a source **added**,
write to **wm3jmike@gmail.com**. Every feed can also be switched off by each user under **Info ▸ Feeds**.

## Code of Conduct

Everyone taking part in this project is expected to follow the [Code of Conduct](CodeOfConduct.md).
