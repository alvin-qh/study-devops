package alvin.docker.core;

import alvin.docker.core.web.I18n;
import alvin.docker.core.web.LocalRequestAttributes;
import alvin.docker.utils.Values;
import org.springframework.aop.scope.ScopedProxyUtils;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;

import static org.springframework.web.context.request.RequestAttributes.SCOPE_REQUEST;

public interface Context {
    String KEY = ScopedProxyUtils.getTargetBeanName("context");

    enum Target {
        API, WEB
    }

    void setRequestPath(String requestPath);

    String getRequestPath();

    void setI18n(I18n i18n);

    I18n getI18n();

    void setTarget(Target target);

    Target getTarget();

    void clear();

    <T> T get(String name);

    void set(String name, Object value);

    static Context current() {
        final RequestAttributes attributes = RequestContextHolder.getRequestAttributes();
        return Values.fetchIfNonNull(attributes, attr -> (Context) attr.getAttribute(KEY, SCOPE_REQUEST));
    }

    static boolean isAvailable() {
        return current() != null;
    }

    static void register(Context context) {
        var attributes = new LocalRequestAttributes();
        attributes.setAttribute(KEY, context, SCOPE_REQUEST);
        RequestContextHolder.setRequestAttributes(attributes);
    }
}
