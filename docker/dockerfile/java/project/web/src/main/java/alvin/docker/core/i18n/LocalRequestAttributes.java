package alvin.docker.core.i18n;

import java.util.HashMap;
import java.util.Map;

import org.springframework.web.context.request.AbstractRequestAttributes;

public class LocalRequestAttributes extends AbstractRequestAttributes {

    private Map<String, Object> attributesMap = new HashMap<>();

    @Override
    protected void updateAccessedSessionAttributes() {
    }

    @Override
    public Object getAttribute(String name, int scope) {
        return attributesMap.get(name);
    }

    @Override
    public void setAttribute(String name, Object value, int scope) {
        attributesMap.put(name, value);
    }

    @Override
    public void removeAttribute(String name, int scope) {
        attributesMap.remove(name);
    }

    @Override
    public String[] getAttributeNames(int scope) {
        return attributesMap.keySet().toArray(new String[0]);
    }

    @Override
    public void registerDestructionCallback(String s, Runnable runnable, int i) {
    }

    @Override
    public Object resolveReference(String s) {
        return null;
    }

    @Override
    public String getSessionId() {
        return null;
    }

    @Override
    public Object getSessionMutex() {
        return null;
    }
}
