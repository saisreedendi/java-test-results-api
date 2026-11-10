package dev.saisree.results;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Test Results API: a small REST service that receives test runs and serves them as JSON.
 *
 *   POST /api/runs            create a run
 *   GET  /api/runs            list runs (?status=FAILED&build=1.2.0)
 *   GET  /api/runs/{id}       one run
 *   GET  /api/summary         counts per status
 */
public class App {
    private final RunStore store = new RunStore();

    public static void main(String[] args) throws IOException {
        int port = args.length > 0 ? Integer.parseInt(args[0]) : 8080;
        HttpServer server = new App().start(port);
        System.out.println("Test Results API running on http://localhost:" + port);
    }

    public HttpServer start(int port) throws IOException {
        HttpServer server = HttpServer.create(new InetSocketAddress(port), 0);
        server.createContext("/api/runs", this::runs);
        server.createContext("/api/summary", this::summary);
        server.setExecutor(java.util.concurrent.Executors.newFixedThreadPool(8));
        server.start();
        return server;
    }

    private void runs(HttpExchange ex) throws IOException {
        try {
            String path = ex.getRequestURI().getPath();
            String method = ex.getRequestMethod();
            if (path.equals("/api/runs") && method.equals("POST")) {
                String body = new String(ex.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
                TestRun run = store.add(Json.parseFlat(body));
                send(ex, 201, run.toJson());
            } else if (path.equals("/api/runs") && method.equals("GET")) {
                Map<String, String> q = query(ex.getRequestURI().getRawQuery());
                List<TestRun> list = store.list(q.get("status"), q.get("build"));
                send(ex, 200, "[" + list.stream().map(TestRun::toJson).collect(Collectors.joining(",")) + "]");
            } else if (path.startsWith("/api/runs/") && method.equals("GET")) {
                long id = Long.parseLong(path.substring("/api/runs/".length()));
                TestRun run = store.get(id);
                if (run == null) send(ex, 404, error("run " + id + " not found"));
                else send(ex, 200, run.toJson());
            } else {
                send(ex, 405, error("method not allowed"));
            }
        } catch (IllegalArgumentException e) {
            send(ex, 400, error(e.getMessage()));
        } catch (Exception e) {
            send(ex, 500, error("server error"));
        }
    }

    private void summary(HttpExchange ex) throws IOException {
        String json = store.summary().entrySet().stream()
                .map(e -> "\"" + e.getKey() + "\":" + e.getValue())
                .collect(Collectors.joining(",", "{", "}"));
        send(ex, 200, json);
    }

    private static Map<String, String> query(String raw) {
        Map<String, String> out = new HashMap<>();
        if (raw == null) return out;
        for (String pair : raw.split("&")) {
            String[] kv = pair.split("=", 2);
            if (kv.length == 2) {
                out.put(URLDecoder.decode(kv[0], StandardCharsets.UTF_8), URLDecoder.decode(kv[1], StandardCharsets.UTF_8));
            }
        }
        return out;
    }

    private static String error(String msg) {
        return "{\"error\":\"" + Json.escape(msg) + "\"}";
    }

    private static void send(HttpExchange ex, int status, String json) throws IOException {
        byte[] bytes = json.getBytes(StandardCharsets.UTF_8);
        ex.getResponseHeaders().set("Content-Type", "application/json");
        ex.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
        ex.sendResponseHeaders(status, bytes.length);
        try (OutputStream os = ex.getResponseBody()) {
            os.write(bytes);
        }
    }
}
