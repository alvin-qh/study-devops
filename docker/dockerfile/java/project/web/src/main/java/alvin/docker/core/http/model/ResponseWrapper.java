package alvin.docker.core.http.model;

import java.time.Instant;

import alvin.docker.common.util.Servlets;
import lombok.Getter;

@Getter
public class ResponseWrapper<T> {
    private static final int CODE_SUCCEED = 0;

    private final int code;
    private final T payload;
    private final String path;
    private final Instant timestamp;

    private ResponseWrapper(int code, T payload) {
        this.code = code;
        this.payload = payload;

        var request = Servlets.request();
        if (request == null) {
            this.path = null;
        } else {
            this.path = request.getContextPath();
        }

        this.timestamp = Instant.now();
    }

    public static <T> ResponseWrapper<T> success(T payload) {
        return new ResponseWrapper<T>(CODE_SUCCEED, payload);
    }

    public static ResponseWrapper<ClientError> error(ClientError error) {
        return new ResponseWrapper<>(CODE_SUCCEED, error);
    }
}
