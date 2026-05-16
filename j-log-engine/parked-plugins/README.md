# Parked contest plugins

These contest-plugin JSONs are **intentionally not built or shipped**. This
directory is outside `src/main/resources`, so nothing here is packaged into the
j-log-engine jar, and the entries were removed from
`PluginLoader.java`'s bundled list. They are kept (git-tracked) so we can
circle back and validate/conform them against official rules later.

Parked 2026-05-16 — "niche + VHF+/EME + RAC" bucket (16):

| Plugin | Why parked |
|---|---|
| `ap_sprint_cw`, `ap_sprint_ssb` | Asia-Pacific Sprint — niche/low participation |
| `baltic` | Baltic Contest — small regional |
| `nz_zl_sprint` | NZ ZL Sprint — niche |
| `vk_qrp` | VK QRP — niche |
| `trans_tasman_lb` | Trans-Tasman Low Band — niche |
| `rsgb_80m_cw`, `rsgb_80m_ssb`, `rsgb_80m_digi` | RSGB 80m Club Sprint — niche |
| `cq_ironman` | Aggregate challenge, not a discrete contest |
| `arrl_222_up`, `arrl_10ghz_up`, `arrl_eme` | VHF+/microwave/EME specialist — very low participation |
| `cq_ww_vhf` | CQ WW VHF — VHF specialist |
| `rac_canada_day`, `rac_winter` | RAC (Canadian national) — parked at user request |

## To un-park a plugin

1. `git mv j-log-engine/parked-plugins/<name>.json j-log-engine/src/main/resources/com/jlog/plugins/<name>.json`
2. Add `"/com/jlog/plugins/<name>.json"` back to the `BUNDLED` list in
   `j-log-engine/src/main/java/com/jlog/plugin/PluginLoader.java`
3. Validate it against official rules, then rebuild
   (`mvn clean install` in j-log-engine, then `mvn clean package` in j-log).
