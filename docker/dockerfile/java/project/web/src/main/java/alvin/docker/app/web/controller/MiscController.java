package alvin.docker.app.web.controller;


import alvin.docker.app.common.ApplicationInfo;
import alvin.docker.app.web.model.Version;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.inject.Inject;

@Controller
@RequestMapping("/d")
public class MiscController {

    private final ApplicationInfo applicationInfo;

    @Inject
    public MiscController(ApplicationInfo applicationInfo) {
        this.applicationInfo = applicationInfo;
    }

    @ResponseBody
    @GetMapping(path = "/version")
    Version index() {
        return new Version(applicationInfo.getVersion(), applicationInfo.getZone());
    }
}
