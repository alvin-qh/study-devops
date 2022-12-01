package alvin.docker.core.http;

import java.time.Instant;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.context.support.DefaultMessageSourceResolvable;
import org.springframework.validation.FieldError;

import alvin.docker.common.util.Servlets;
import lombok.Getter;
import lombok.SneakyThrows;

@Getter
public class ClientError implements Cloneable {
    private String errKey;
    private Map<String, String> errorParameters;
    private Map<String, String[]> errorFields;

    private ClientError(String errKey) {
        var request = Servlets.request();
        if (request != null) {
            this.path = request.getContextPath();
        }
        this.timestamp = Instant.now();
        this.errKey = errKey;
    }

    @Override
    @SneakyThrows
    public ClientError clone() {
        return (ClientError) super.clone();
    }

    public static final class Builder {
        private ClientError clientError;

        private Builder(ClientError clientError) {
            this.clientError = clientError;
        }

        public Builder addParameterError(String argName, String errMsg) {
            if (clientError.errorParameters == null) {
                clientError.errorParameters = new LinkedHashMap<>();
            }
            clientError.errorParameters.put(argName, errMsg);
            return this;
        }

        public Builder errorFields(Collection<FieldError> errors) {
            clientError.errorFields = errors.stream()
                    .collect(Collectors.groupingBy(FieldError::getField))
                    .entrySet()
                    .stream()
                    .collect(Collectors.toMap(
                            Map.Entry::getKey,
                            e -> e.getValue().stream()
                                    .map(DefaultMessageSourceResolvable::getDefaultMessage)
                                    .toArray(String[]::new)));
            return this;
        }

        public ClientError build() {
            return clientError.clone();
        }
    }

    public static Builder newBuilder(String errKey) {
        return new Builder(new ClientError(errKey));
    }
}
