package alvin.docker.core.context;

import java.util.HashMap;
import java.util.Map;

public class WebContext implements Context {
    public static final String REQUEST_PATH = "request_path";
    public static final String I18N = "i18n";

    private static final String CONTEXT_KEY_PREFIX = "_ctx_";

    private Map<String, Object> contextMap = new HashMap<>();

    @Override
    public void clear() {
        var oldMap = contextMap;
        contextMap = new HashMap<>();
        oldMap.clear();
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T get(String name) {
        return (T) contextMap.get(CONTEXT_KEY_PREFIX + name);
    }

    @Override
    public void set(String name, Object value) {
        contextMap.put(CONTEXT_KEY_PREFIX + name, value);
    }
}
