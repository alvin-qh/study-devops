package alvin.docker.core.http.model;

import lombok.Data;

@Data
public class ApplicationInfo {
    private final CrossDomain crossDomain = new CrossDomain();
    private String version;
    private String zone;

    @Data
    public static class CrossDomain {
        private boolean enabled;
        private String allowOrigin;
        private String allowHeaders;
        private String allowMethods;
        private int maxAge;
    }
}
