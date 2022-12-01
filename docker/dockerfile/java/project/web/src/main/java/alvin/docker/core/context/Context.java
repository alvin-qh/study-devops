package alvin.docker.core.context;

import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;

public interface Context {

    void clear();

    <T> T get(String name);

    void set(String name, Object value);

    static Context current() {
        var attrs = RequestContextHolder.getRequestAttributes();
        if (attrs == null) {
            return null;
        }
        return (Context) attrs.getAttribute(CustomRequestAttributes.KEY, RequestAttributes.SCOPE_REQUEST);
    }

    static boolean isAvailable() {
        return current() != null;
    }
}
