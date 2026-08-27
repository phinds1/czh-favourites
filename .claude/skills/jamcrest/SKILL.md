---
name: jamcrest
description: How to use Jamcrest for req.js / resp.js JSON matching and how to port JamcrestUtils.java into other projects
allowed-tools: java, bash
---

# Jamcrest

Jamcrest is a Hamcrest-style JSON matching library that lets you assert a JSON
document matches a *definition* written in JavaScript. The definition may mix
literal values (exact match) with **matcher functions** (fuzzy match) and
**template variables** (injected from Java).

- Source & authoritative matcher docs: https://github.com/teknopaul/jamcrest
- Maven artifact: `io.github.teknopaul:jamcrest` (this repo uses `0.1.1`)

This project wraps it in
`winchecker-next-integration/src/test/java/com/igt/czh/winchecker/test/rest/util/JamcrestUtils.java`,
which runs the JS definitions on GraalVM's JS engine (`org.graalvm.polyglot`).

---

## 1. The two file types

Tests keep JSON fixtures next to the test resources as small JavaScript files.
By convention the files live under `src/test/resources/rest/...`.

### `*.req.js` — request templates

The request body sent to the service. It assigns the payload to `t`:

```javascript
t = {
    "serialNumber": serialNumber,   // injected variable
    "firstDrawNumber": 999000,      // literal
    "gameTips": [
        { "gameName": "Keno", "tips": [ /* ... */ ] }
    ]
};
```

- Loaded in Java with `jamcrest.loadJson(path, "serialNumber", "136...")`.
- The engine evaluates the JS, substitutes any variables you passed, and returns
  the resulting JSON string to POST/PUT to the service.
- The same `.req.js` can serve several tests when the differences are expressed
  as variables.

### `*.resp.js` — response definitions (matchers)

The expected response. It assigns the definition to `jsonDefinition` (or `t`):

```javascript
jsonDefinition = {
    "hostVerification": "NOT_APPLICABLE",  // must equal exactly
    "totalPrizeAmount": anyNumber(),       // matcher: any number
    "wagerStatus": anyString(),            // matcher: any string
    "winnings": anyArray(),                // matcher: any array
    "drawNumber": drawId                   // injected variable, exact match
};
```

- Each field is matched positionally/by key against the actual JSON.
- A **literal** must be deeply equal.
- A **matcher function** passes if the actual value satisfies it (use these for
  timestamps, generated ids, amounts that vary, etc.).
- A **variable** is a value injected from Java and then matched exactly.

---

## 2. Matchers

Matchers are plain JS functions available in scope when the definition runs.
The authoritative, up-to-date list is in the README:
https://github.com/teknopaul/jamcrest — check there before inventing a matcher.

Commonly used matchers (confirmed in this codebase and the README):

| Matcher            | Passes when the value is…            |
|--------------------|--------------------------------------|
| `anyString()`      | any JSON string                      |
| `anyNumber()`      | any JSON number                      |
| `anyBoolean()`     | any JSON boolean                     |
| `anyArray()`       | any JSON array (any length/contents) |
| `anyObject()`      | any JSON object                      |
| `any()`            | present with any value               |
| `anyNonNull()`     | present and not null                 |
| `regex(/pattern/)` | a string matching the regexp         |

Guidelines:

- Prefer **literals** for anything deterministic (division names, prize amounts, match counts) so tests actually verify the business logic.
- Use **matchers** only for non-deterministic fields (generated ids, dates, timestamps).
- You can write an inline function as a custom matcher — it receives the actual value and returns a boolean, e.g. `"amount": function(v){ return v > 0; }`.
- Arrays and objects are matched structurally: a literal array must match element by element; use `anyArray()` when the contents are not under test.

---

## 3. Template variables

Both file types can reference free variables that Java supplies as name/value pairs. In `JamcrestUtils` the args are `Object...` alternating `name, value`:

```java
// resp.js references errorCode and description
jamcrest.validateJson(response, "rest/common/error-resp.js", "errorCode", 400, "description", "Bad request");
```

```javascript
// error-resp.js
jsonDefinition = {
  "errorCode": errorCode,       // injected number, exact match
  "description": anyString()
};
```

Reserved names:
- `t` — the value a `.req.js` / template builds.
- `$` / `jsonDefinition` — the definition a `.resp.js` builds.
  The wrapper normalises between `t` and `$` so either assignment works.

---

## 4. Typical test flow

```java
// 1. build the request from a template, injecting variables
String reqJson = jamcrest.loadJson("rest/inquiry/inquiry-PUT-....req.js");

// 2. call the running service (rest client / MockMvc / TestRestTemplate)
String respJson = put("/api/winchecker/inquiry", reqJson);

// 3. validate the response against a definition
jamcrest.validateJson(respJson, "rest/inquiry/inquiry-PUT-....resp.js");

// 4. pull values out of the last validated JSON for follow-up assertions
String id = (String) jamcrest.access("$.id");
```

**Bootstrapping `.resp.js`:** run the test *without* the `.resp.js` present.
`validateJson` detects the missing/empty file and logs a suggested definition
(pretty-printed JSON) via `suggestJsonDefinition`. Copy that into the new
`.resp.js`, then replace non-deterministic fields with matchers and verify the
deterministic values are actually correct before committing.

---

## 5. Porting JamcrestUtils.java to another project

`JamcrestUtils` is a thin, self-contained wrapper — copy it and adjust the
package. Its responsibilities and the API to preserve:

**Construction / lifecycle**
- Builds a GraalVM JS `Context` (`Context.newBuilder("js").allowAllAccess(false)`).
- `reset()` closes and rebuilds the context and clears `lastJson` — call between
  tests so variables/state don't leak. In Spring it is a `@Component @Lazy` bean.
- `RES_PREFIX = "src/test/resources/"` — the base dir for relative paths.

**Core API to keep**

| Method                                                 | Purpose                                                                         |
|--------------------------------------------------------|---------------------------------------------------------------------------------|
| `loadJson(path, args...)`                              | Evaluate a `.req.js` template with injected vars → JSON string                  |
| `validateJson(json, path, args...)`                    | Match `json` against a `.resp.js` definition; auto-suggests the file if missing |
| `validateExactMatch(json)`                             | Match `json` against the previously validated JSON                              |
| `validateError(resp, code[, desc])`                    | Convenience matchers for the common error envelope                              |
| `access()` / `access(jsPath)` / `accessAsJson(jsPath)` | Read values (via `$.path`) out of the last validated JSON                       |
| `mutateTemplate(jsCode)`                               | Run JS against the current template and re-serialise                            |
| `reset()`                                              | Reset engine + state                                                            |

**Internals worth understanding**
- `runValidation` delegates to `Jamcrest.compare(json, definition, true, args)` and throws `AssertionError` on `!match`.
- `evaluateTemplate` declares `var name = value;` for each arg, resets `t`/`$`, evaluates the file, then reconciles `t` and `$`.
- `wrapTemplateForEval` wraps a bare object/array (no `t =`) as `t = (...)`.
- `jsLiteral` safely quotes a Java string into a JS string literal (escapes `\ " \n \r \0`); use it whenever passing strings into `ctx.eval`.
- `toJavaValue` converts a GraalVM `Value` back to a Java `Boolean/Integer/Long/Double/String/null`.

**Dependencies for the new project (Maven)**

```xml
<dependency>
    <groupId>io.github.teknopaul</groupId>
    <artifactId>jamcrest</artifactId>
    <version>0.1.1</version>
    <scope>test</scope>
</dependency>
<dependency>
    <groupId>org.graalvm.polyglot</groupId>
    <artifactId>polyglot</artifactId>
    <scope>test</scope>
</dependency>
<!-- plus the graal js language pack, e.g. org.graalvm.polyglot:js -->
```

Also needs `org.json` for the `suggestJsonDefinition` pretty-printer and slf4j
for logging. Drop the Spring `@Component @Lazy` annotations if the target
project is not Spring-based and just `new JamcrestUtils()` in a test base class.

---

## 6. Gotchas

- These are **JavaScript** files, not JSON — comments and trailing expressions are allowed, and matcher calls are real function calls.
- Keep matcher use minimal; over-using `any*()` hides regressions.
- Call `reset()` between tests to avoid variable leakage in the shared context.
- Paths passed to `validateJson`/`loadJson` are relative to `src/test/resources/`.
- Always check https://github.com/teknopaul/jamcrest for the current matcher set and semantics rather than guessing.

