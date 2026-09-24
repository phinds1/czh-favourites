package cz.bsl.czh.favourites.test.it;

// Grep anchor: favourites

import cz.bsl.czh.favourites.test.AbstractRestTest;
import cz.bsl.favourites.config.VisionBaseContextFilter;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.web.server.LocalManagementPort;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.DefaultUriBuilderFactory;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Guards the context-root awareness of the Vision GUI behind the nginx reverse proxy
 * (the {@link VisionBaseContextFilter}). Extends {@link AbstractRestTest} so the real app boots
 * end-to-end against HSQLDB on a random app port, with the actuator on the management port injected
 * via {@link LocalManagementPort}. Named {@code *Test} (not {@code *IT}) so surefire runs it in the
 * same phase as the rest of the favourites integration suite — there is no failsafe phase here.
 *
 * <p>Asserts the filter's two behaviours on the management port:
 * <ul>
 *   <li><b>No {@code X-Vision-Base} header</b> (direct on port) — HTML passes through unchanged:
 *       the {@code vision-base} meta stays empty, asset paths stay root {@code /gui/}.</li>
 *   <li><b>With {@code X-Vision-Base: /favourites-mgmt}</b> — the filter rewrites the HTML: the
 *       {@code vision-base} meta is filled with the base, and the {@code /gui/} asset references
 *       are prefixed with the base. The root path is served directly (rewritten) rather than
 *       forwarded.</li>
 * </ul>
 * Both paths must be served as {@code text/html} with no {@code X-Frame-Options} (frame-loadable).
 */
class VisionBaseTest extends AbstractRestTest {

    @LocalManagementPort
    protected int managementPort;

    private static final String BASE = "/favourites-mgmt";

    private RestTemplate managementRest() {
        RestTemplate t = new RestTemplate();
        t.setUriTemplateHandler(new DefaultUriBuilderFactory("http://localhost:" + managementPort));
        return t;
    }

    private RestTemplate managementRest(String visionBase) {
        RestTemplate t = managementRest();
        t.getInterceptors().add((req, body, exec) -> {
            req.getHeaders().set(VisionBaseContextFilter.VISION_BASE_HEADER, visionBase);
            return exec.execute(req, body);
        });
        return t;
    }

    @Test
    void noBaseHeader_indexHtml_passesThroughUnchanged() {
        ResponseEntity<String> r = managementRest().exchange("/gui/index.html",
                HttpMethod.GET, new HttpEntity<>(new HttpHeaders()), String.class);
        assertEquals(200, r.getStatusCode().value(), "GET /gui/index.html (no base): " + r.getStatusCode());
        assertTrue(r.getHeaders().getContentType().toString().startsWith("text/html"),
                "index.html is text/html, got: " + r.getHeaders().getContentType());
        // No base → meta stays empty, asset paths stay root /gui/.
        assertTrue(r.getBody().contains("<meta name=\"vision-base\" content=\"\">"),
                "vision-base meta must be empty when no X-Vision-Base header: " + r.getBody());
        assertTrue(r.getBody().contains("=\"/gui/js/api.js\""),
                "asset paths must stay root /gui/ when no base: " + r.getBody());
    }

    @Test
    void noBaseHeader_rootPath_forwardsToGuiIndex() {
        ResponseEntity<String> r = managementRest().exchange("/",
                HttpMethod.GET, new HttpEntity<>(new HttpHeaders()), String.class);
        assertEquals(200, r.getStatusCode().value(), "GET / (no base): " + r.getStatusCode());
        assertTrue(r.getBody().contains("czh-favourites Vision"),
                "the GUI index page must be served at / (forwarded): " + r.getBody());
    }

    @Test
    void withBaseHeader_indexHtml_metaAndAssetsRewritten() {
        ResponseEntity<String> r = managementRest(BASE).exchange("/gui/index.html",
                HttpMethod.GET, new HttpEntity<>(new HttpHeaders()), String.class);
        assertEquals(200, r.getStatusCode().value(), "GET /gui/index.html (base): " + r.getStatusCode());
        assertTrue(r.getBody().contains("<meta name=\"vision-base\" content=\"" + BASE + "\">"),
                "vision-base meta must be filled with the base: " + r.getBody());
        assertTrue(r.getBody().contains("=\"" + BASE + "/gui/js/api.js\""),
                "asset paths must be prefixed with the base: " + r.getBody());
        // The empty meta placeholder must be gone (replaced, not duplicated).
        assertFalse(r.getBody().contains("<meta name=\"vision-base\" content=\"\">"),
                "the empty meta placeholder must be replaced, not duplicated: " + r.getBody());
    }

    @Test
    void withBaseHeader_rootPath_servedDirectlyRewritten() {
        // GET / with a base would forward (and commit before rewriting); the filter serves the
        // index directly from the classpath (rewritten) instead.
        ResponseEntity<String> r = managementRest(BASE).exchange("/",
                HttpMethod.GET, new HttpEntity<>(new HttpHeaders()), String.class);
        assertEquals(200, r.getStatusCode().value(), "GET / (base): " + r.getStatusCode());
        assertTrue(r.getHeaders().getContentType().toString().startsWith("text/html"),
                "direct-serve index is text/html, got: " + r.getHeaders().getContentType());
        assertTrue(r.getBody().contains("<meta name=\"vision-base\" content=\"" + BASE + "\">"),
                "the direct-served root index must be rewritten with the base: " + r.getBody());
    }

    @Test
    void gui_isFrameLoadable_noXFrameOptions() {
        ResponseEntity<String> r = managementRest().exchange("/gui/index.html",
                HttpMethod.GET, new HttpEntity<>(new HttpHeaders()), String.class);
        assertEquals(200, r.getStatusCode().value());
        assertTrue(r.getHeaders().getFirst("X-Frame-Options") == null,
                "the GUI must be iframe-loadable — no X-Frame-Options: " + r.getHeaders());
        String csp = r.getHeaders().getFirst("Content-Security-Policy");
        assertTrue(csp == null || !csp.contains("frame-ancestors"),
                "no CSP frame-ancestors: " + r.getHeaders());
    }

    @Test
    void withBaseHeader_jsAsset_passesThroughByteForByte() {
        // JS is not HTML — the filter must NOT rewrite it (resolveUrl reads the meta at runtime).
        ResponseEntity<String> r = managementRest(BASE).exchange("/gui/js/api.js",
                HttpMethod.GET, new HttpEntity<>(new HttpHeaders()), String.class);
        assertEquals(200, r.getStatusCode().value(), "GET /gui/js/api.js (base): " + r.getStatusCode());
        assertTrue(r.getBody().contains("resolveUrl"),
                "api.js must be served (contains resolveUrl): " + r.getBody().substring(0, 80));
        // The base is NOT injected into JS — the asset path is unchanged.
        assertFalse(r.getBody().contains(BASE + "/gui/"),
                "JS must not be rewritten with the base (only HTML is): " + r.getBody().substring(0, 200));
    }

    @Test
    void withBaseHeader_actuatorPassesThroughUnchanged() {
        // Actuator is not /gui/** — the filter short-circuits it (no rewrite, no buffering).
        ResponseEntity<String> r = managementRest(BASE).exchange("/actuator/health",
                HttpMethod.GET, new HttpEntity<>(new HttpHeaders()), String.class);
        assertEquals(200, r.getStatusCode().value(), "GET /actuator/health (base): " + r.getStatusCode());
        assertTrue(r.getBody().contains("\"status\":\"UP\""),
                "actuator health must pass through unchanged: " + r.getBody());
    }
}
