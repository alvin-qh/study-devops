package alvin.docker.common;

import alvin.docker.common.context.LocalRequestAttributes;
import alvin.docker.common.i18n.I18n;
import alvin.docker.utils.Values;
import lombok.val;
import org.springframework.aop.scope.ScopedProxyUtils;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;

import static org.springframework.web.context.request.RequestAttributes.SCOPE_REQUEST;

public interface Context {
    String KEY = ScopedProxyUtils.getTargetBeanName("context");

    enum Target {
        API, WEB
    }

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
        val attributes = new LocalRequestAttributes();
        attributes.setAttribute(KEY, context, SCOPE_REQUEST);
        RequestContextHolder.setRequestAttributes(attributes);
    }

    void setRequestPath(String requestPath);

    String getRequestPath();

    void setI18n(I18n i18n);

    I18n getI18n();

    void setTarget(Target target);

    Target getTarget();
}
