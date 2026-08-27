package cz.bsl.favourites.config;

// Grep anchor: favourites

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Serves the operator Vision GUI static files from {@code classpath:/gui/} at {@code /gui/**}.
 */
@Configuration
public class GuiWebConfig implements WebMvcConfigurer {

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/gui/**")
                .addResourceLocations("classpath:/gui/");
    }
}
