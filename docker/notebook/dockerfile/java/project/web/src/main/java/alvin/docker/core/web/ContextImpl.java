package alvin.docker.core.web;

import alvin.docker.core.Context;
import lombok.Data;
import lombok.val;

import java.util.HashMap;
import java.util.Map;

@Data
public class ContextImpl implements Context {
    private static final String CONTEXT_KEY_PREFIX = "_ctx_pri_";
    private static final String CONTEXT_KEY_EXTERNAL_PREFIX = "_ctx_pub_";

    private static final String CONTEXT_KEY_REQUEST_PATH = "request_path";
    private static final String CONTEXT_KEY_TARGET = "target";
    private static final String CONTEXT_KEY_I18N = "i18n";

    private Map<String, Object> contextMap = new HashMap<>();

    @Override
    public void setRequestPath(String requestPath) {
        setValue(CONTEXT_KEY_REQUEST_PATH, requestPath);
    }

    @Override
    public String getRequestPath() {
        return getValue(CONTEXT_KEY_REQUEST_PATH);
    }

    @Override
    public void setI18n(I18n i18n) {
        setValue(CONTEXT_KEY_I18N, i18n);
    }

    @Override
    public I18n getI18n() {
        return getValue(CONTEXT_KEY_I18N);
    }

    @Override
    public Target getTarget() {
        return getValue(CONTEXT_KEY_TARGET);
    }

    @Override
    public void setTarget(Target value) {
        setValue(CONTEXT_KEY_TARGET, value);
    }

    @Override
    public void clear() {
        val oldMap = contextMap;
        contextMap = new HashMap<>();
        oldMap.clear();
    }

    @SuppressWarnings("unchecked")
    private <T> T getValue(String name) {
        return (T) contextMap.get(CONTEXT_KEY_PREFIX + name);
    }

    private void setValue(String name, Object value) {
        contextMap.put(CONTEXT_KEY_PREFIX + name, value);
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T> T get(String name) {
        return (T) contextMap.get(CONTEXT_KEY_EXTERNAL_PREFIX + name);
    }

    @Override
    public void set(String name, Object value) {
        contextMap.put(CONTEXT_KEY_EXTERNAL_PREFIX + name, value);
    }
}
