package com.smartinsure.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "smartinsure")
public class SmartInsureProperties {

    private final Jwt jwt = new Jwt();
    private final MlService mlService = new MlService();
    private final Storage storage = new Storage();

    @Getter
    @Setter
    public static class Jwt {
        private String secret;
        private long expirationMs;
    }

    @Getter
    @Setter
    public static class MlService {
        private String baseUrl = "http://localhost:8090";
        private boolean enabled = true;
    }

    @Getter
    @Setter
    public static class Storage {
        private String rootPath;
    }
}
