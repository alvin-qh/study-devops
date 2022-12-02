package alvin.docker.core.http.filter;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import alvin.docker.core.http.model.ApplicationInfo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class HttpInterceptor implements HandlerInterceptor {
    private final ApplicationInfo applicationInfo;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        log.debug("visiting {}", request.getRequestURI());

        var crossDomain = applicationInfo.getCrossDomain();
        if (crossDomain.isEnabled()) {
            response.addHeader("Access-Control-Allow-Origin", crossDomain.getAllowOrigin());
            response.addHeader("Access-Control-Allow-Headers", crossDomain.getAllowHeaders());
            response.addHeader("Access-Control-Allow-Methods", crossDomain.getAllowMethods());
            response.addHeader("Access-Control-Max-Age", String.valueOf(crossDomain.getMaxAge()));

            if (HttpMethod.OPTIONS.matches(request.getMethod())) {
                response.setStatus(HttpStatus.ACCEPTED.value());
                return false;
            }
        }

        return true;
    }
}
