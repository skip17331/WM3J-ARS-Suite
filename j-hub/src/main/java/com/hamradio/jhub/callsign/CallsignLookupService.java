package com.hamradio.jhub.callsign;

import com.google.gson.Gson;
import com.hamradio.jhub.ConfigManager;
import com.hamradio.jhub.model.JHubConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Orchestrates callsign lookups across multiple providers with in-memory caching.
 *
 * Provider priority in "auto" mode: QRZ → HamQTH → HamDB → Callook.
 * First provider that returns a hit wins; unauthenticated providers are always tried last.
 *
 * Cache TTLs: 24 h for found results, 1 h for not-found.
 */
public class CallsignLookupService {

    private static final Logger log = LoggerFactory.getLogger(CallsignLookupService.class);

    private static final long HIT_TTL_SEC  = 24 * 3600L;
    private static final long MISS_TTL_SEC =       3600L;

    private final LocalDbProvider localDb = new LocalDbProvider();
    private final QrzXmlProvider  qrz     = new QrzXmlProvider();
    private final HamQthProvider  hamqth  = new HamQthProvider();
    private final HamDbProvider   hamdb   = new HamDbProvider();
    private final CallookProvider callook = new CallookProvider();

    private static final class CacheEntry {
        final CallsignResult result;
        final Instant        cachedAt;
        CacheEntry(CallsignResult result, Instant cachedAt) {
            this.result   = result;
            this.cachedAt = cachedAt;
        }
    }
    private final ConcurrentHashMap<String, CacheEntry> cache = new ConcurrentHashMap<>();

    private static final CallsignLookupService INSTANCE = new CallsignLookupService();
    public static CallsignLookupService getInstance() { return INSTANCE; }
    private CallsignLookupService() {}

    /**
     * Looks up a callsign, returning a cached result when still fresh.
     * Never throws — errors are encoded in the returned result's {@code error} field.
     */
    public CallsignResult lookup(String rawCallsign) {
        if (rawCallsign == null || rawCallsign.isBlank())
            return CallsignResult.notFound("", "Empty callsign");

        String callsign = rawCallsign.trim().toUpperCase();

        CacheEntry entry = cache.get(callsign);
        if (entry != null) {
            long age = Instant.now().getEpochSecond() - entry.cachedAt.getEpochSecond();
            long ttl = entry.result.found ? HIT_TTL_SEC : MISS_TTL_SEC;
            if (age < ttl) {
                log.debug("Cache hit for {} (age {}s)", callsign, age);
                return entry.result;
            }
            cache.remove(callsign);
        }

        CallsignResult result = doLookup(callsign);
        cache.put(callsign, new CacheEntry(result, Instant.now()));
        return result;
    }

    /** Forces a fresh lookup on the next call for this callsign. */
    public void invalidate(String callsign) {
        cache.remove(callsign.trim().toUpperCase());
    }

    /** Returns a JSON array of all cached results (for diagnostics). */
    public String getCacheJson() {
        List<CallsignResult> results = new ArrayList<>();
        for (CacheEntry e : cache.values()) results.add(e.result);
        return new Gson().toJson(results);
    }

    // ── Private ────────────────────────────────────────────────────────────────

    private CallsignResult doLookup(String callsign) {
        JHubConfig.CallsignSection s = ConfigManager.getInstance().getConfig().callsignLookup;
        if (s != null && !s.enabled)
            return CallsignResult.notFound(callsign, "Callsign lookup disabled");

        String pref = s != null ? s.provider : "auto";

        for (CallsignProvider provider : buildChain(pref)) {
            try {
                log.debug("Trying {} for {}", provider.name(), callsign);
                CallsignResult r = provider.lookup(callsign);
                if (r.found) {
                    log.debug("Found {} via {}", callsign, provider.name());
                    return r;
                }
            } catch (Exception e) {
                log.debug("{} lookup failed for {}: {}", provider.name(), callsign, e.getMessage());
            }
        }

        log.debug("{} not found in any provider", callsign);
        return CallsignResult.notFound(callsign, "Not found");
    }

    private List<CallsignProvider> buildChain(String pref) {
        String p = (pref == null) ? "auto" : pref.toLowerCase();
        switch (p) {
            case "local":   return List.of(localDb);
            case "qrz":     return List.of(qrz);
            case "hamqth":  return List.of(hamqth);
            case "hamdb":   return List.of(hamdb);
            case "callook": return List.of(callook);
            default: {
                List<CallsignProvider> chain = new ArrayList<>();
                if (localDb.isAvailable()) chain.add(localDb);  // local first — fast, offline
                if (qrz.isAvailable())     chain.add(qrz);
                if (hamqth.isAvailable())  chain.add(hamqth);
                chain.add(hamdb);
                chain.add(callook);
                return chain;
            }
        }
    }
}
