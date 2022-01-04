package alvin.docker.app.common.filter;

import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import alvin.docker.app.common.ApplicationInfo;
import alvin.docker.core.Context;

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
