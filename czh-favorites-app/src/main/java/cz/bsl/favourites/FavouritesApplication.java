package cz.bsl.favourites;

import cz.bsl.favourites.config.FavouritesProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication(scanBasePackages = {"cz.bsl.favourites", "cz.bsl.czh.favourites"})
@EnableConfigurationProperties(FavouritesProperties.class)
public class FavouritesApplication {

    public static void main(String[] args) {
        SpringApplication.run(FavouritesApplication.class, args);
    }
}
