package alvin.docker.core.context;

import org.springframework.aop.scope.ScopedProxyUtils;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;

import alvin.docker.core.i18n.LocalRequestAttributes;

public interface Context {
    String KEY = ScopedProxyUtils.getTargetBeanName("context");

    void clear();

    <T> T get(String name);

    void set(String name, Object value);

    static Context current() {
        var attributes = RequestContextHolder.getRequestAttributes();
        if (attributes == null) {
            return null;
        }
        return (Context) attributes.getAttribute(KEY, RequestAttributes.SCOPE_REQUEST);
    }

    static boolean isAvailable() {
        return current() != null;
    }

    static void register(Context context) {
        var attributes = new LocalRequestAttributes();
        attributes.setAttribute(KEY, context, RequestAttributes.SCOPE_REQUEST);
        RequestContextHolder.setRequestAttributes(attributes);
    }
}
