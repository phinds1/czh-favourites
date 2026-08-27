package cz.bsl.czh.favourites.test;

// Grep anchor: favourites

import io.github.teknopaul.jamcrest.Jamcrest;
import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.Value;
import org.slf4j.Logger;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.slf4j.LoggerFactory.getLogger;

/**
 * JSON validation and template utilities backed by jamcrest and GraalVM JS.
 *
 * <p>Ported verbatim from {@code czh-money} ({@code cz.bsl.money.test}) into
 * {@code czh-favourites} test infrastructure.
 *
 * <h2>Usage</h2>
 * <pre>
 *   // Validate a response against a .resp.js definition
 *   jamcrest.validateJson(responseBody, "favourites/list-wagers.resp.js");
 *
 *   // Load a request body from a .req.js template
 *   String body = jamcrest.loadJson("favourites/create-wager.req.js", "playerId", 42L);
 *
 *   // Read a value from the last validated JSON
 *   int id = (int) jamcrest.access("$.id");
 * </pre>
 *
 * <p>Fixture files live under {@code src/test/resources/}.
 * <p>Call {@link #reset()} between tests (e.g. in {@code @AfterEach}) to prevent variable leakage.
 */
public class JamcrestUtils {

    private static final Logger LOG = getLogger(JamcrestUtils.class);
    static final String RES_PREFIX = "src/test/resources/";

    private final Jamcrest jamcrest = new Jamcrest();
    private Context ctx;
    private String lastJson;

    public JamcrestUtils() {
        initContext();
        // notNull() is not in jamcrest-matchers.js but is widely used in resp.js files;
        // inject it as an alias for notNullValue() which IS in the matchers.
        jamcrest.putGlobalsFromJs("({ notNull: notNullValue })");
    }

    private void initContext() {
        ctx = Context.newBuilder("js").allowAllAccess(false).build();
    }

    /**
     * Validate {@code json} against the definition in {@code path} (relative to
     * {@code src/test/resources/}). If the file is absent or empty, logs a
     * suggested definition so you can bootstrap the {@code .resp.js} file.
     *
     * @param args alternating name/value pairs injected as JS variables
     */
    public void validateJson(String json, String path, Object... args) {
        String fullPath = RES_PREFIX + path;
        File f = new File(fullPath);
        if (!f.exists() || f.length() == 0) {
            suggestJsonDefinition(json, path);
            throw new AssertionError("Missing .resp.js fixture: " + fullPath + " — see log for suggested definition");
        }
        try {
            runValidation(json, Files.readString(Path.of(fullPath)), args);
        } catch (IOException e) {
            throw new RuntimeException("Failed to read: " + fullPath, e);
        }
    }

    /** Match {@code json} against the definition from the last {@link #validateJson} call. */
    public void validateExactMatch(String json) {
        if (lastJson == null) throw new IllegalStateException("No previous JSON to match against");
        runValidation(json, "jsonDefinition = (" + lastJson + ");");
    }

    /**
     * Evaluate a {@code .req.js} template with injected variables and return the
     * resulting JSON string.
     *
     * @param args alternating name/value pairs injected as JS variables
     */
    public String loadJson(String path, Object... args) {
        try {
            return evaluateTemplate(Files.readString(Path.of(RES_PREFIX + path)), args);
        } catch (IOException e) {
            throw new RuntimeException("Failed to read template: " + RES_PREFIX + path, e);
        }
    }

    /** Run arbitrary JS against the current template context and re-serialise. */
    public String mutateTemplate(String jsCode) {
        ctx.eval("js", jsCode);
        String result = ctx.eval("js", "JSON.stringify($)").asString();
        ctx.eval("js", "t = $;");
        return result;
    }

    /** Return the raw JSON string from the last successful {@link #validateJson} call. */
    public String access() {
        if (lastJson == null) throw new IllegalStateException("No JSON has been validated yet");
        return lastJson;
    }

    /**
     * Evaluate a JS path expression (e.g. {@code "$.fieldName"}) against the
     * last validated JSON and return a Java value.
     */
    public Object access(String jsPath) {
        if (lastJson == null) throw new IllegalStateException("No JSON has been validated yet");
        ctx.eval("js", "$ = JSON.parse(" + jsLiteral(lastJson) + ");");
        return toJavaValue(ctx.eval("js", jsPath));
    }

    /** Like {@link #access(String)} but returns the result as a JSON string. */
    public String accessAsJson(String jsPath) {
        if (lastJson == null) throw new IllegalStateException("No JSON has been validated yet");
        ctx.eval("js", "$ = JSON.parse(" + jsLiteral(lastJson) + ");");
        return ctx.eval("js", "JSON.stringify(" + jsPath + ")").asString();
    }

    /** Reset the JS context and clear cached JSON. Call between tests. */
    public void reset() {
        ctx.close();
        lastJson = null;
        initContext();
    }

    private void runValidation(String json, String matcherContent, Object... args) {
        Jamcrest.Result r = jamcrest.compare(json, matcherContent, true, args);
        if (!r.match()) {
            LOG.error("Validation failed: {}", r);
            throw new AssertionError("JSON validation failed: " + r);
        }
        lastJson = json;
    }

    private String evaluateTemplate(String jsCode, Object... args) {
        if (args.length > 0) {
            StringBuilder vars = new StringBuilder();
            for (int i = 0; i < args.length; i += 2) {
                String name = (String) args[i];
                Object val  = args[i + 1];
                String jsVal = val instanceof String s ? jsLiteral(s) : String.valueOf(val);
                vars.append("var ").append(name).append(" = ").append(jsVal).append(";\n");
            }
            ctx.eval("js", vars.toString());
        }
        ctx.eval("js", "t = null; $ = null;");
        ctx.eval("js", wrapTemplateForEval(jsCode));
        ctx.eval("js", "if ($ === null && t !== null) { $ = t; } else if (t === null && $ !== null) { t = $; }");
        return ctx.eval("js", "JSON.stringify($)").asString();
    }

    private static String wrapTemplateForEval(String jsCode) {
        String trimmed = jsCode.trim();
        if (!trimmed.contains("t =") && !trimmed.contains("t=")) {
            return "t = (" + trimmed + ");";
        }
        return jsCode;
    }

    private static Object toJavaValue(Value v) {
        if (v.isBoolean()) return v.asBoolean();
        if (v.isNumber()) {
            if (v.fitsInInt())  return v.asInt();
            if (v.fitsInLong()) return v.asLong();
            return v.asDouble();
        }
        if (v.isString()) return v.asString();
        if (v.isNull())   return null;
        return v.toString();
    }

    static String jsLiteral(String s) {
        StringBuilder sb = new StringBuilder("\"");
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            switch (c) {
                case '\\' -> sb.append("\\\\");
                case '"'  -> sb.append("\\\"");
                case '\n' -> sb.append("\\n");
                case '\r' -> sb.append("\\r");
                case '\0' -> sb.append("\\u0000");
                default   -> sb.append(c);
            }
        }
        sb.append('"');
        return sb.toString();
    }

    private void suggestJsonDefinition(String json, String path) {
        try {
            String pretty = json.startsWith("{")
                    ? new org.json.JSONObject(json).toString(4)
                    : new org.json.JSONArray(json).toString(4);
            LOG.warn("Missing fixture: {}\n\nSuggested jsonDefinition:\n\njsonDefinition = {};\n", path, pretty);
        } catch (Exception ex) {
            LOG.warn("Missing fixture: {}\n\nRaw JSON:\n\n{}\n", path, json);
        }
    }
}
