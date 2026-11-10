# Java Test Results API

A small REST service written in plain Java (no frameworks) that receives automated test runs and serves them as JSON. A React dashboard reads from it: [react-test-dashboard](https://github.com/saisreedendi/react-test-dashboard).

## What it does
- `POST /api/runs` - save a test run (fields are validated, bad input returns `400` with the reason)
- `GET /api/runs` - list runs, newest first (`?status=FAILED&build=1.2.0`)
- `GET /api/runs/{id}` - one run (`404` if missing)
- `GET /api/summary` - counts of passed, failed and skipped

## How it is built
- Java 17+ with the built-in `HttpServer` and a fixed thread pool
- `ConcurrentHashMap` and `AtomicLong` so many test machines can report at the same time
- Input validation with clear error messages
- Plain-Java tests, including a test with 500 parallel writes

## Run it
```bash
mkdir out && javac -d out $(find src -name '*.java')
java -cp out dev.saisree.results.RunStoreTest     # tests
java -cp out dev.saisree.results.App              # starts on port 8080
```

Try it:
```bash
curl -X POST localhost:8080/api/runs -d '{"name":"login test","build":"1.2.0","status":"failed","durationMs":340}'
curl "localhost:8080/api/runs?status=FAILED"
curl localhost:8080/api/summary
```
