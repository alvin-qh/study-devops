package alvin.docker.common.context;

import lombok.val;
import org.springframework.web.context.request.AbstractRequestAttributes;

import java.io.Closeable;
import java.util.HashMap;
import java.util.Map;

public class LocalRequestAttributes extends AbstractRequestAttributes implements Closeable {

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

    @Override
    public void close() {
        val attributesMap = this.attributesMap;
        this.attributesMap = new HashMap<>();
        attributesMap.clear();
    }
}
