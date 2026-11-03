package cz.bsl.favorites;

import cz.bsl.favorites.config.FavoritesProperties;
import cz.bsl.mscommon.pid.PidFile;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication(scanBasePackages = {"cz.bsl.favorites", "cz.bsl.czh.favorites"})
@EnableConfigurationProperties(FavoritesProperties.class)
public class FavoritesApplication {

    public static void main(String[] args) {
        PidFile.createPid("/var/run/czh", "favorites.pid");
        SpringApplication.run(FavoritesApplication.class, args);
    }
}
