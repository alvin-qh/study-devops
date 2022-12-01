package alvin.docker.app.common.filter;

import java.util.regex.Pattern;

import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.context.MessageSource;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.support.RequestContextUtils;

import alvin.docker.core.context.Context;
import alvin.docker.core.i18n.I18n;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class HttpInterceptor implements HandlerInterceptor {
    private final Context context;
    private final Pattern apiPattern;
    private final MessageSource messageSource;

    @Inject
    public HttpInterceptor(Context context, MessageSource messageSource) {
        this.context = context;
        this.messageSource = messageSource;
        this.apiPattern = Pattern.compile("^(/[\\w-]+)?/api/.*");
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        log.debug("visiting {}", request.getRequestURI());

        context.setRequestPath(request.getRequestURI());
        context.setI18n(new I18n(messageSource, RequestContextUtils.getLocale(request)));

        if (apiPattern.matcher(request.getRequestURI()).matches()) {
            context.setTarget(Context.Target.API);
        } else {
            context.setTarget(Context.Target.WEB);
        }
        return true;
    }
}
