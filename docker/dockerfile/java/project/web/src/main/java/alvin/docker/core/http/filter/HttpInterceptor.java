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

/**
 * 对于所有 HTTP 请求进行拦截的拦截器
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class HttpInterceptor implements HandlerInterceptor {
    // 注入应用程序信息对象
    private final ApplicationInfo applicationInfo;

    /**
     * 在请求处理前进行拦截
     */
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        log.info("visiting {} from {}", request.getRequestURI(), request.getRemoteHost());

        // 获取跨域配置项
        var crossDomain = applicationInfo.getCrossDomain();
        if (crossDomain.isEnabled()) {
            // 若允许跨域, 则处理跨域请求
            response.addHeader("Access-Control-Allow-Origin", crossDomain.getAllowOrigin());
            response.addHeader("Access-Control-Allow-Headers", crossDomain.getAllowHeaders());
            response.addHeader("Access-Control-Allow-Methods", crossDomain.getAllowMethods());
            response.addHeader("Access-Control-Max-Age", String.valueOf(crossDomain.getMaxAge()));

            // 对于 OPTIONS 请求, 直接进行处理
            if (HttpMethod.OPTIONS.matches(request.getMethod())) {
                response.setStatus(HttpStatus.ACCEPTED.value());
                return false;
            }
        }

        return true;
    }
}
