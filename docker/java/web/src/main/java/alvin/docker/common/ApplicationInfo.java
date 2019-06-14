package alvin.docker.common;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "application")
public class ApplicationInfo {
    @Getter
    @Setter
    private String version;

    @Getter
    @Setter
    private String zone;

    @Getter
    private final CrossDomain crossDomain = new CrossDomain();

    @Getter
    @Setter
    public static class CrossDomain {
        private boolean enabled;
        private String allowOrigin;
        private String allowHeaders;
        private String allowMethods;
        private int maxAge;
    }
}
