package alvin.docker.filters;

import alvin.docker.common.ApplicationInfo;
import alvin.docker.common.Context;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@Slf4j
@Component
public class CrossDomainInterceptor implements HandlerInterceptor {
    private final Context context;
    private final ApplicationInfo.CrossDomain crossDomain;

    @Inject
    public CrossDomainInterceptor(Context context, ApplicationInfo applicationInfo) {
        this.context = context;
        this.crossDomain = applicationInfo.getCrossDomain();
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        if (this.context.getTarget() == Context.Target.API) {
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
        }

        return true;
    }
}
