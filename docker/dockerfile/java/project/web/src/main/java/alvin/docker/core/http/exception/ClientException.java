package alvin.docker.core.http.exception;

/**
 * 需要客户端关注的异常
 */
public class ClientException extends RuntimeException {
    public ClientException(String message) {
        super(message);
    }

    public ClientException(Throwable cause) {
        super(cause);
    }

    public ClientException(String message, Throwable cause) {
        super(message, cause);
    }
}
