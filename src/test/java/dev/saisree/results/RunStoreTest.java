package dev.saisree.results;

import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/** Plain-Java tests (no framework needed): run with scripts shown in the README. */
public class RunStoreTest {
    static int failures = 0;

    public static void main(String[] args) throws Exception {
        addsAndReadsRun();
        rejectsBadStatus();
        rejectsMissingName();
        filtersByStatus();
        handlesParallelWrites();
        System.out.println(failures == 0 ? "ALL TESTS PASSED" : failures + " TEST(S) FAILED");
        if (failures > 0) System.exit(1);
    }

    static void addsAndReadsRun() {
        RunStore s = new RunStore();
        TestRun r = s.add(Map.of("name", "login", "build", "1.0", "status", "passed", "durationMs", "120"));
        check("adds run", r.id() == 1 && s.get(1).status().equals("PASSED"));
    }

    static void rejectsBadStatus() {
        try {
            new RunStore().add(Map.of("name", "x", "build", "1", "status", "WEIRD"));
            check("rejects bad status", false);
        } catch (IllegalArgumentException e) {
            check("rejects bad status", e.getMessage().contains("status"));
        }
    }

    static void rejectsMissingName() {
        try {
            new RunStore().add(Map.of("build", "1", "status", "PASSED"));
            check("rejects missing name", false);
        } catch (IllegalArgumentException e) {
            check("rejects missing name", e.getMessage().equals("name is required"));
        }
    }

    static void filtersByStatus() {
        RunStore s = new RunStore();
        s.add(Map.of("name", "a", "build", "1", "status", "PASSED"));
        s.add(Map.of("name", "b", "build", "1", "status", "FAILED"));
        check("filters by status", s.list("FAILED", null).size() == 1 && s.summary().get("PASSED") == 1);
    }

    static void handlesParallelWrites() throws Exception {
        RunStore s = new RunStore();
        ExecutorService pool = Executors.newFixedThreadPool(8);
        for (int i = 0; i < 500; i++) {
            pool.submit(() -> s.add(Map.of("name", "t", "build", "1", "status", "PASSED")));
        }
        pool.shutdown();
        pool.awaitTermination(10, TimeUnit.SECONDS);
        check("parallel writes keep every run", s.list(null, null).size() == 500);
    }

    static void check(String name, boolean ok) {
        System.out.println((ok ? "PASS  " : "FAIL  ") + name);
        if (!ok) failures++;
    }
}
