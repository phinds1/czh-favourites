package cz.bsl.favourites.config;

// Grep anchor: favourites

import org.springframework.boot.actuate.autoconfigure.web.ManagementContextConfiguration;
import org.springframework.boot.actuate.autoconfigure.web.ManagementContextType;
import org.springframework.boot.actuate.autoconfigure.web.server.ConditionalOnManagementPort;
import org.springframework.boot.actuate.autoconfigure.web.server.ManagementPortType;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.ViewControllerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.resource.PathResourceResolver;

/**
 * Serves the read-only operator Vision GUI from {@code classpath:/gui/} on the <b>management context
 * only</b> (port 9291). The GUI lives under a custom classpath location (not the default
 * {@code static/}), so it is served explicitly here.
 *
 * <p>This is a {@link ManagementContextConfiguration} of type {@link ManagementContextType#CHILD},
 * registered via the {@code ManagementContextConfiguration.imports} file so Spring Boot only applies
 * it to the management servlet context (9291) — the app context (9290) does not serve the GUI. The
 * management context is where {@code /actuator/prometheus} lives, so the Server and Operations tabs
 * are same-origin there. The Player Lookup tab fetches {@code /admin/favourites/*} from the app port
 * (9290) — reaching that cross-port is a GUI concern (resolved by the nginx reverse-proxy /
 * context-root model), not handled here.
 *
 * <p><b>Context-root awareness:</b> when the GUI is served behind an nginx reverse proxy under a
 * context root (e.g. {@code /favourites-mgmt}), nginx sends an {@code X-Vision-Base} request header
 * and the {@link #visionBaseContextFilter()} rewrites the HTML so its absolute site paths resolve
 * under that root. Direct-on-port (no header) is a no-op. nginx, not the backend, owns the context
 * prefix on the wire — it strips the prefix before proxying, so the servlet paths stay unchanged.
 *
 * <p>{@code GET /} forwards to {@code /gui/index.html} (a view-controller forward, so the resource
 * handler serves the file with the right content type). {@code GET /gui/**} serves the file from the
 * classpath. czh-favourites is not internet-facing; no auth on the GUI.
 */
@ManagementContextConfiguration(ManagementContextType.CHILD)
@ConditionalOnManagementPort(ManagementPortType.DIFFERENT)
public class GuiResourceConfig implements WebMvcConfigurer {

    static final String GUI_CLASSPATH = "classpath:/gui/";

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/gui/**")
                .addResourceLocations(GUI_CLASSPATH)
                .resourceChain(true)
                .addResolver(new PathResourceResolver());
    }

    @Override
    public void addViewControllers(ViewControllerRegistry registry) {
        // Bare root -> the GUI index page (forwarded to the resource handler, served as text/html).
        registry.addViewController("/").setViewName("forward:/gui/index.html");
    }

    /**
     * Rewrites the GUI HTML so its absolute site paths resolve under the nginx context root when the
     * {@code X-Vision-Base} request header is present. Registered only on the management context (where
     * the GUI is served). Direct-on-port (no header) is a no-op.
     */
    @org.springframework.context.annotation.Bean
    FilterRegistrationBean<VisionBaseContextFilter> visionBaseContextFilter() {
        FilterRegistrationBean<VisionBaseContextFilter> reg = new FilterRegistrationBean<>();
        reg.setFilter(new VisionBaseContextFilter());
        // Only the GUI page paths need buffering/rewriting; the filter also short-circuits non-GUI.
        reg.addUrlPatterns("/", "/gui/*");
        reg.setOrder(0);
        return reg;
    }
}
