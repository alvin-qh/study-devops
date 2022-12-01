package alvin.docker.common.util;

import javax.servlet.http.HttpServletRequest;

import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

public final class Servlets {
    private Servlets() {
    }

    public static HttpServletRequest request() {
        var attr = RequestContextHolder.getRequestAttributes();
        if (attr == null) {
            return null;
        }

        return ((ServletRequestAttributes) attr).getRequest();
    }
}
