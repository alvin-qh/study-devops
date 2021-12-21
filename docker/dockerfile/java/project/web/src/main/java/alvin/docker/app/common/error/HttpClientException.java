package alvin.docker.app.common.error;

import com.google.common.base.Joiner;
import lombok.Getter;
import lombok.val;

import java.util.Map;

import static org.hibernate.internal.util.collections.CollectionHelper.isNotEmpty;

@Getter
public class HttpClientException extends RuntimeException {
    private final ClientError clientError;

    public HttpClientException(ClientError clientError) {
        super(makeMessage(clientError));
        this.clientError = clientError;
    }

    public HttpClientException(ClientError clientError, Throwable throwable) {
        super(makeMessage(clientError), throwable);
        this.clientError = clientError;
    }

    private static String makeMessage(ClientError clientError) {
        val builder = new StringBuilder(String.format("Client error caused, status: %d, message: \"%s\"",
                clientError.statusCode(), clientError.getMessage()));
        if (isNotEmpty(clientError.getErrorFields())) {
            builder.append(", fields=").append(mapToString(clientError.getErrorFields()));
        }
        if (isNotEmpty(clientError.getErrorParameters())) {
            builder.append(", parameters={").append(mapToString(clientError.getErrorParameters())).append("}");
        }
        return builder.toString();
    }

    private static <T> String mapToString(Map<String, T> map) {
        val builder = new StringBuilder("{");
        map.forEach((k, v) -> {
            if (builder.length() > 1) {
                builder.append(",");
            }
            if (v.getClass().isArray()) {
                builder.append(k).append("=[").append(Joiner.on(";").join((Object[]) v)).append("]");
            } else {
                builder.append(k).append("=").append(v.toString());
            }
        });
        builder.append("}");
        return builder.toString();
    }
}
