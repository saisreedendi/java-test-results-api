package dev.saisree.results;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/** Thread-safe in-memory store, safe when many test machines report at the same time. */
public class RunStore {
    private final Map<Long, TestRun> runs = new ConcurrentHashMap<>();
    private final AtomicLong nextId = new AtomicLong(1);

    /** Validates the fields and saves the run. Throws IllegalArgumentException with a clear reason. */
    public TestRun add(Map<String, String> fields) {
        String name = require(fields, "name");
        String build = require(fields, "build");
        String status = require(fields, "status").toUpperCase();
        if (!TestRun.STATUSES.contains(status)) {
            throw new IllegalArgumentException("status must be one of " + TestRun.STATUSES);
        }
        long duration;
        try {
            duration = Long.parseLong(fields.getOrDefault("durationMs", "0"));
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("durationMs must be a whole number");
        }
        if (duration < 0) {
            throw new IllegalArgumentException("durationMs cannot be negative");
        }
        TestRun run = new TestRun(nextId.getAndIncrement(), name, build, status, duration, Instant.now());
        runs.put(run.id(), run);
        return run;
    }

    public TestRun get(long id) {
        return runs.get(id);
    }

    /** Newest first; optional filters by status and build. */
    public List<TestRun> list(String status, String build) {
        List<TestRun> out = new ArrayList<>();
        for (TestRun r : runs.values()) {
            if (status != null && !r.status().equalsIgnoreCase(status)) continue;
            if (build != null && !r.build().equals(build)) continue;
            out.add(r);
        }
        out.sort(Comparator.comparingLong(TestRun::id).reversed());
        return out;
    }

    /** Counts per status, used for the summary endpoint. */
    public Map<String, Long> summary() {
        Map<String, Long> counts = new java.util.TreeMap<>();
        for (String s : TestRun.STATUSES) counts.put(s, 0L);
        for (TestRun r : runs.values()) counts.merge(r.status(), 1L, Long::sum);
        return counts;
    }

    private static String require(Map<String, String> f, String key) {
        String v = f.get(key);
        if (v == null || v.isBlank()) {
            throw new IllegalArgumentException(key + " is required");
        }
        return v.trim();
    }
}
