package dev.saisree.results;

import java.time.Instant;

/** One automated test run reported by a test machine. */
public record TestRun(long id, String name, String build, String status, long durationMs, Instant createdAt) {

    public static final java.util.Set<String> STATUSES = java.util.Set.of("PASSED", "FAILED", "SKIPPED");

    public String toJson() {
        return "{\"id\":" + id
                + ",\"name\":\"" + Json.escape(name) + "\""
                + ",\"build\":\"" + Json.escape(build) + "\""
                + ",\"status\":\"" + status + "\""
                + ",\"durationMs\":" + durationMs
                + ",\"createdAt\":\"" + createdAt + "\"}";
    }
}
