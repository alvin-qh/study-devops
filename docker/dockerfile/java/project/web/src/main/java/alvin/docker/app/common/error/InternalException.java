package alvin.docker.app.common.error;

public class InternalException extends RuntimeException {

    public InternalException(String message) {
        super(message);
    }

    public InternalException(Throwable cause) {
        super(cause);
    }

    public InternalException(String message, Throwable cause) {
        super(message, cause);
    }

    public static InternalException keyMissing(String resourceName, String idName, Object id) {
        return new InternalException(String.format("cannot find %s by %s id: \"%s\"", resourceName, idName, id));
    }

    public static InternalException keyMissing(String resourceName, Object id) {
        return keyMissing(resourceName, "id", id);
    }
}
