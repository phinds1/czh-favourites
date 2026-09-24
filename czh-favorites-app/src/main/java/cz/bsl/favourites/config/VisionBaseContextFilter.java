package cz.bsl.favourites.config;

// Grep anchor: favourites

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.WriteListener;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpServletResponseWrapper;
import org.springframework.core.io.ClassPathResource;
import org.springframework.util.StreamUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;

/**
 * Makes the Vision GUI context-root aware when served behind an nginx reverse proxy. nginx routes
 * by the first URL segment (e.g. {@code /favourites-mgmt/...}) and tells the backend which context the
 * request arrived under by sending an {@value #VISION_BASE_HEADER} <b>request</b> header. This filter
 * reads that header and rewrites the served GUI HTML so its absolute site paths resolve under the
 * context root:
 * <ul>
 *   <li>fills {@code <meta name="vision-base" content="">} with the base (the GUI JS reads this at
 *       load time to prefix its {@code /api} and {@code /actuator} fetch URLs);</li>
 *   <li>prefixes the HTML asset references {@code ="/gui/..."} with the base so the browser fetches
 *       {@code /favourites-mgmt/gui/...} through the proxy rather than a root {@code /gui/} that nginx
 *       does not serve.</li>
 * </ul>
 *
 * <p>When the header is absent (the GUI loaded direct on its own port at {@code /gui/}) the base is
 * empty and the response passes through untouched. Only {@code text/html} responses are rewritten;
 * JS/CSS/actuator pass through byte-for-byte.
 *
 * <p><b>Root path:</b> {@code GET /} normally forwards to {@code /gui/index.html} via a view
 * controller. A servlet {@code forward} commits the response before this filter can rewrite it, so
 * when a context root is present the filter serves the index page directly from the classpath
 * (rewritten) instead of forwarding. With no context root (direct on port) the forward runs as
 * before.
 *
 * <p>nginx, not the backend, owns the context-root prefix on the wire: it strips
 * {@code /favourites-mgmt} before proxying, so the backend keeps serving {@code /gui/}, {@code /api/},
 * {@code /actuator/} at the servlet root. This filter only adjusts the GUI's own HTML output so the
 * browser reissues asset + fetch URLs under the context root.
 */
public class VisionBaseContextFilter extends OncePerRequestFilter {

    /** The request header nginx sends carrying the GUI's context root (e.g. {@code /favourites-mgmt}). */
    public static final String VISION_BASE_HEADER = "X-Vision-Base";

    /** The empty-content meta placeholder written into {@code index.html}; filled with the base. */
    static final String META_EMPTY = "<meta name=\"vision-base\" content=\"\">";

    private static final String INDEX_CLASSPATH = "/gui/index.html";

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {
        String base = normalizeBase(request.getHeader(VISION_BASE_HEADER));
        if (base.isEmpty()) {
            filterChain.doFilter(request, response);
            return;
        }
        String path = request.getRequestURI();
        if (isRootPath(path)) {
            serveIndexRewritten(response, base);
            return;
        }
        if (!isGuiAssetPath(path)) {
            filterChain.doFilter(request, response);
            return;
        }
        BufferingResponseWrapper wrapped = new BufferingResponseWrapper(response);
        filterChain.doFilter(request, wrapped);
        byte[] body = wrapped.getBytes();
        String contentType = wrapped.getContentType();
        byte[] out = (body.length > 0 && isHtml(contentType))
                ? rewrite(new String(body, StandardCharsets.UTF_8), base).getBytes(StandardCharsets.UTF_8)
                : body;
        response.setContentLength(out.length);
        response.getOutputStream().write(out);
    }

    private static void serveIndexRewritten(HttpServletResponse response, String base) throws IOException {
        String html;
        try (InputStream in = new ClassPathResource(INDEX_CLASSPATH).getInputStream()) {
            html = StreamUtils.copyToString(in, StandardCharsets.UTF_8);
        }
        byte[] out = rewrite(html, base).getBytes(StandardCharsets.UTF_8);
        response.setContentType("text/html;charset=UTF-8");
        response.setContentLength(out.length);
        response.getOutputStream().write(out);
    }

    /** Strip trailing slash(es); {@code "/"} and empty → {@code ""} (no base, root context). */
    static String normalizeBase(String raw) {
        if (raw == null) {
            return "";
        }
        String base = raw.trim();
        while (base.length() > 1 && base.endsWith("/")) {
            base = base.substring(0, base.length() - 1);
        }
        return base.equals("/") ? "" : base;
    }

    private static boolean isRootPath(String path) {
        return "/".equals(path);
    }

    private static boolean isGuiAssetPath(String path) {
        return path.startsWith("/gui/");
    }

    private static boolean isHtml(String contentType) {
        return contentType != null && contentType.toLowerCase().contains("text/html");
    }

    /**
     * Rewrite the GUI HTML for a context root: fill the {@code vision-base} meta content and prefix
     * the local {@code /gui/} asset references with the base. CDN {@code https://} URLs are untouched
     * (they do not match {@code ="/gui/}).
     */
    static String rewrite(String html, String base) {
        String filled = html.replace(META_EMPTY,
                "<meta name=\"vision-base\" content=\"" + base + "\">");
        return filled.replace("=\"/gui/", "=\"" + base + "/gui/");
    }

    /**
     * Buffers the response body so the filter can rewrite it after the handler has written. Only used
     * for {@code /gui/**} resource responses (no forward involved); the root path is served directly.
     */
    static final class BufferingResponseWrapper extends HttpServletResponseWrapper {
        private final ByteArrayOutputStream buffer = new ByteArrayOutputStream(8192);
        private ServletOutputStream outputStream;
        private PrintWriter writer;
        private String contentType;

        BufferingResponseWrapper(HttpServletResponse response) {
            super(response);
        }

        @Override
        public ServletOutputStream getOutputStream() {
            if (outputStream == null) {
                outputStream = new ServletOutputStream() {
                    @Override
                    public void write(int b) {
                        buffer.write(b);
                    }

                    @Override
                    public boolean isReady() {
                        return true;
                    }

                    @Override
                    public void setWriteListener(WriteListener writeListener) {
                        // Not used — this is a buffering sink, never async.
                    }
                };
            }
            return outputStream;
        }

        @Override
        public PrintWriter getWriter() throws UnsupportedEncodingException {
            if (writer == null) {
                writer = new PrintWriter(new OutputStreamWriter(buffer, StandardCharsets.UTF_8));
            }
            return writer;
        }

        @Override
        public void setContentType(String type) {
            this.contentType = type;
            super.setContentType(type);
        }

        @Override
        public void setContentLength(int len) {
            // Suppress: the handler writes the original length before we rewrite. The filter sets the
            // final length after rewriting, so drop the intermediate value (it would commit a wrong
            // Content-Length header on the real response before the body is written).
        }

        @Override
        public void setContentLengthLong(long len) {
            // As above — the rewritten body length is set by the filter after buffering.
        }

        @Override
        public void setHeader(String name, String value) {
            if (isContentLength(name)) {
                return;
            }
            super.setHeader(name, value);
        }

        @Override
        public void addHeader(String name, String value) {
            if (isContentLength(name)) {
                return;
            }
            super.addHeader(name, value);
        }

        private static boolean isContentLength(String name) {
            return name != null && name.equalsIgnoreCase("content-length");
        }

        @Override
        public String getContentType() {
            return contentType != null ? contentType : super.getContentType();
        }

        byte[] getBytes() {
            if (writer != null) {
                writer.flush();
            }
            return buffer.toByteArray();
        }
    }
}
