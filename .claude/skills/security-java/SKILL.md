---
name: security-java
description: review Java code for NPEs, unvalidated input, and DDoS/resource-exhaustion risks; output recommendations, not fixes
allowed-tools: bash
---

Run a Java-specific security and crash-bug review on user-specified files (or, with none given, on
the Java that changed since the last review). This is the post-execution security pass for a Java 25
/ Spring Boot 4 microservice built off this template. It specialises the generic `/security` skill
with the three failure classes most likely to survive a clean-room port: null-pointer crashes,
unvalidated input, and resource exhaustion (DoS).

This skill produces a **report of recommendations only.** It does not edit code or configuration.
Recommendations are applied separately, test-first, by the refactoring/fix phase — never by this
skill.

## Scope of review

For each file under review, look for:

### NPE / null-dereference risks
- Method returns, `Map`/`List` lookups, `Optional` misuse, `RestTemplate`/HTTP response bodies,
  JSON-parsed fields, and DB `RowMapper` reads that are dereferenced without a null check.
- `@RequestBody` / deserialised POJOs whose fields may be absent (Jackson leaves them null) and are
  then dereferenced as if always present.
- `@PathVariable`/`@RequestParam` that are declared as boxed types and dereferenced directly.
- Auto-unboxing of `Integer`/`Long`/`Boolean` from DB columns or JSON that may be null.
- Chained access (`a.getB().getC()`) where any link can be null, especially on external WS / client
  responses (SP3-style XML clients) and DAO reads.
- Spring beans, `@Value`-injected config, and `@ConfigurationProperties` fields assumed non-null
  before they are guaranteed bound.

### Unvalidated input issues
- HTTP request bodies, path variables, query params, and headers that flow into SQL, XML string
  building, file paths, external service calls, or cache keys without validation.
- Input that controls SQL — flag SQL injection and dynamic-SQL construction (the DAO layer is
  `JdbcTemplate`; parameterised queries are the norm, so any string concatenation into SQL is a
  finding).
- Input that controls XML/WS request bodies built by string manipulation (the clients build SOAP
  envelopes as strings) — flag XML/SOAP injection and WS-Security header construction from
  untrusted input.
- Input that controls cache keys or stored JSON (`PAYLOAD`) — flag cache poisoning and
  store-manipulation paths.
- Public API methods where a caller could reasonably pass bad input and not understand the
  consequence — flag as a contract/validation gap even if the current callers are safe.
- Size/length limits: unbounded collections, arrays, or strings accepted from HTTP and held in
  memory or written to the `VARCHAR(30000)` payload.

### DDoS / resource-exhaustion risks
- User-controlled input that can occupy the CPU for too long: unbounded loops, recursion depth
  driven by input, regex on user input, expensive JSON/XML parsing of oversized bodies, sorts or
  scans over user-controlled result sizes.
- User-controlled input that can exhaust memory or DB connections: unbounded result sets, missing
  pagination/limits on list endpoints, `fetchAll`-style DAO reads without a bound.
- User-controlled input that can fan out external calls: a single request that triggers many WS /
  client calls (e.g. per-item loops calling the Aristocrat clients) with no cap.
- Missing timeouts / no circuit breaker on outbound HTTP (XML clients) — a slow upstream can tie up
  request threads and exhaust the Tomcat thread pool.
- Sleeps, waits, or blocking driven by user input.
- Endpoints callable by external schedulers (admin/cleanup/reconcile) that do unbounded work in one
  request — flag the need for batching or a row cap.

## Assumptions (inherited from `/security`)

- We can safely assume input is valid HTTP in this project, but the service should be resilient to
  malformed HTTP anyway; treat such issues as **low severity**.
- Spring Boot is tested and trusted — do not report framework internals as findings.
- Authentication is pre-authenticated via HTTP headers (per the migration input); do not report the
  absence of in-process auth, but **do** flag code that trusts a header without documenting which
  header is required, and code that uses an untrusted header value as an identity/authority input
  without validation.

## Output format

Produce a markdown report. For each finding:

- **Severity** — `critical` / `high` / `medium` / `low`, with the low band reserved for
  malformed-HTTP resilience and trusted-framework issues per the assumptions above.
- **File:line** — the exact location.
- **Issue** — one sentence: the failure class (NPE / unvalidated input / DDoS) and the concrete
  input or state that triggers it.
- **Recommendation** — what to change. **Describe the fix, do not apply it.** Phrase as a
  recommendation the fix phase can pick up test-first.

End with a short summary: counts by severity, and any input-validation or DoS concern that is
systemic (repeats across endpoints) rather than per-file.

## Rules

- **Do not make any changes to code or configuration.** Output is a report.
- **Do not use GSD** (getting-shit-done) — this is analysis, not implementation.
- If a file under review cannot be read or does not exist, report that and skip it; do not guess its
  contents.
- Distinguish a real finding from a theoretical one: a finding must name the concrete input or state
  that reaches the defect. If you cannot construct one, mark it `low` or drop it.
