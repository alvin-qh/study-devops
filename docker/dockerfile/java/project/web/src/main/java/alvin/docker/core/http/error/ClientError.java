package alvin.docker.core.http.error;

import java.time.Instant;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.context.support.DefaultMessageSourceResolvable;
import org.springframework.http.HttpStatus;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.client.HttpClientErrorException;

import alvin.docker.common.util.Servlets;
import alvin.docker.core.context.Context;
import lombok.Getter;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

@Getter
@Slf4j
public class ClientError implements Cloneable {
    private HttpStatus status;
    private String errKey;
    private String errMsg;
    private Map<String, String> errorParameters;
    private Map<String, String[]> errorFields;
    private Instant timestamp;
    private String path;

    ClientError() {
    }

    private ClientError(HttpStatus status) {
        var request = Servlets.request();
        if (request != null) {
            this.path = request.getContextPath();
        }
        this.status = status;
        this.timestamp = Instant.now();
    }

    public int statusCode() {
        return status.value();
    }

    public Map<String, Object> toMap() {
        return Map.of(
                "status", status.name(),
                "errMsg", errMsg == null ? "" : errMsg);
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

        public Builder message(String errKey, String errMsg) {
            clientError.errKey = errKey;
            clientError.errMsg = errMsg;
            return this;
        }

        public Builder fMessage(String errKey, Object... args) {
            clientError.errKey = errKey;
            clientError.errMsg = ClientError.message(errKey, errKey, args);
            return this;
        }

        public Builder addParameterError(String name, String errorKey, Object... args) {
            if (clientError.errorParameters == null) {
                clientError.errorParameters = new LinkedHashMap<>();
            }
            clientError.errorParameters.put(name, ClientError.message(errorKey, errorKey, args));
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

    public static Builder badRequest() {
        return status(HttpStatus.BAD_REQUEST);
    }

    public static Builder badRequest(String errorKey, Object... args) {
        return status(HttpStatus.BAD_REQUEST).fMessage(errorKey, args);
    }

    public static Builder errorFields(Collection<FieldError> errors) {
        return badRequest("error.common.InvalidRequestParameter").errorFields(errors);
    }

    public static Builder errorFields(FieldError... fieldErrors) {
        return errorFields(Arrays.asList(fieldErrors));
    }

    public static Builder errorParameters(MissingServletRequestParameterException ex) {
        return badRequest("error.common.InvalidRequestParameter")
                .addParameterError(ex.getParameterName(),
                        "error.common.InvalidRequestParameter.required", ex.getParameterName());
    }

    public static Builder errorParameters(String name, String error) {
        return badRequest("error.common.InvalidRequestParameter").addParameterError(name, error);
    }

    public static Builder internalServerError(String message) {
        return status(HttpStatus.INTERNAL_SERVER_ERROR).message("error.common.http.INTERNAL_SERVER_ERROR", message);
    }

    public static Builder unauthorized() {
        return status(HttpStatus.UNAUTHORIZED);
    }

    public static Builder unauthorized(String errorKey, Object... args) {
        return status(HttpStatus.UNAUTHORIZED).fMessage(errorKey, args);
    }

    public static Builder forbidden() {
        return status(HttpStatus.FORBIDDEN);
    }

    public static Builder forbidden(String errorKey, Object... args) {
        return status(HttpStatus.FORBIDDEN).fMessage(errorKey, args);
    }

    public static Builder notFound() {
        return status(HttpStatus.NOT_FOUND);
    }

    public static Builder notFound(String errorKey, Object... args) {
        return status(HttpStatus.NOT_FOUND).message(errorKey, message(errorKey, errorKey, args));
    }

    private static String message(String key, String defaultMessage, Object... args) {
        var context = Context.current();
        if (context == null) {
            return defaultMessage;
        }

        var i18n = context.getI18n();
        if (i18n == null) {
            return defaultMessage;
        }
        return i18n.getMessageOrElse(key, defaultMessage, args);
    }

    public static Builder status(HttpStatus status) {
        var key = "error.common.http." + status.name();
        return new Builder(new ClientError(status)).fMessage(key);
    }

    public static ClientError fromException(Throwable exception) {
        final ClientError clientError;

        if (exception instanceof HttpClientException) {
            var exp = (HttpClientException) exception;
            clientError = exp.getClientError();
        } else if (exception instanceof InternalException) {
            if (exception.getCause() != null) {
                return fromException(exception.getCause());
            }
            clientError = internalServerError(exception.getMessage()).build();
        } else {
            final Builder builder;
            if (exception instanceof HttpClientErrorException) {
                HttpClientErrorException exp = (HttpClientErrorException) exception;
                builder = status(exp.getStatusCode());
            } else {
                builder = internalServerError(exception.getMessage());
            }
            clientError = builder.build();
        }

        if (clientError.statusCode() >= HttpStatus.INTERNAL_SERVER_ERROR.value()) {
            log.error("INTERNAL_SERVER_ERROR caused when access url \"{}\", status is: {}, message is: \"{}\"",
                    clientError.getPath(), clientError.statusCode(), clientError.getMessage(), exception);
        } else {
            log.debug("{} caused when access url \"{}\", status is: {}, message is: \"{}\"",
                    clientError.getStatus().name(), clientError.getPath(), clientError.statusCode(),
                    clientError.getMessage(), exception);
        }

        return clientError;
    }
}
