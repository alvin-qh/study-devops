package alvin.docker.app.api.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import lombok.Getter;

@Getter
public class Version {
    private String version;
    private String zone;

    @JsonCreator
    public Version(String version, String zone) {
        this.version = version;
        this.zone = zone;
    }
}
