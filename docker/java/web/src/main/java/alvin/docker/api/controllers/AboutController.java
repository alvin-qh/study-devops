package alvin.docker.api.controllers;

import alvin.docker.api.models.Version;
import alvin.docker.common.ApplicationInfo;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import javax.inject.Inject;

@RestController
@RequestMapping("/api/about")
public class AboutController {

    private final ApplicationInfo applicationInfo;

    @Inject
    public AboutController(ApplicationInfo applicationInfo) {
        this.applicationInfo = applicationInfo;
    }

    @GetMapping
    @ResponseBody
    Version version() {
        return new Version(applicationInfo.getVersion(), applicationInfo.getZone());
    }
}
