package alvin.docker.core.http.model;

import java.time.Instant;

import com.fasterxml.jackson.annotation.JsonProperty;

import alvin.docker.common.util.Servlets;
import lombok.Getter;

@Getter
public class ResponseWrapper<T> {
    private static final int CODE_SUCCEED = 0;

    private final int code;
    private final T payload;
    private final String path;
    private final Instant timestamp;

    ResponseWrapper(
            @JsonProperty("code") int code,
            @JsonProperty("payload") T payload,
            @JsonProperty("path") String path,
            @JsonProperty("timestamp") Instant timestamp) {
        this.code = code;
        this.payload = payload;

        if (path == null) {
            var request = Servlets.request();
            if (request == null) {
                this.path = null;
            } else {
                this.path = request.getRequestURI();
            }
        } else {
            this.path = path;
        }

        if (timestamp == null) {
            this.timestamp = Instant.now();
        } else {
            this.timestamp = timestamp;
        }
    }

    public static <T> ResponseWrapper<T> success(T payload) {
        return new ResponseWrapper<T>(CODE_SUCCEED, payload, null, null);
    }

    public static ResponseWrapper<ClientError> error(ClientError error) {
        return new ResponseWrapper<>(CODE_SUCCEED, error, null, null);
    }
}
