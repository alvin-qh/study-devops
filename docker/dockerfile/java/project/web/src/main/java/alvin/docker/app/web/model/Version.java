package alvin.docker.app.web.model;

import lombok.Getter;

@Getter
public class Version {
    private String version;
    private String zone;

    public Version(String version, String zone) {
        this.version = version;
        this.zone = zone;
    }
}
