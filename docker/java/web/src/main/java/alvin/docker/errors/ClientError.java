package alvin.docker.errors;

import alvin.docker.common.Context;
import com.google.common.collect.ImmutableMap;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import lombok.var;
import org.springframework.context.support.DefaultMessageSourceResolvable;
import org.springframework.http.HttpStatus;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.context.request.RequestContextHolder;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;

import static alvin.docker.utils.Values.nullElse;
import static org.springframework.web.context.request.RequestAttributes.SCOPE_REQUEST;

@Getter
@Slf4j
public class ClientError implements Cloneable {
    private HttpStatus status;
    private String errorKey;
    private String message;
    private Map<String, String> errorParameters;
    private Map<String, String[]> errorFields;
    private LocalDateTime timestamp;
    private String path;

    ClientError() {
    }

    private ClientError(HttpStatus status) {
        var attrib = RequestContextHolder.getRequestAttributes();
        if (attrib != null) {
            val context = (Context) attrib.getAttribute(Context.KEY, SCOPE_REQUEST);
            if (context != null) {
                this.path = context.getRequestPath();
            }
        }
        this.status = status;
        this.timestamp = LocalDateTime.now(ZoneOffset.UTC);
    }

    public int statusCode() {
        return status.value();
    }

    public Map<String, Object> toMap() {
        return ImmutableMap.of("status", status.name(), "message", message == null ? "" : message);
    }

    @Override
    public ClientError clone() {
        try {
            return (ClientError) super.clone();
        } catch (CloneNotSupportedException ignore) {
            return null;
        }
    }

    public static final class Builder {
        private ClientError clientError;

        private Builder(ClientError clientError) {
            this.clientError = clientError;
        }

        public Builder message(String errorKey, String message) {
            this.clientError.errorKey = errorKey;
            this.clientError.message = message;
            return this;
        }

        public Builder fMessage(String errorKey, Object... args) {
            this.clientError.errorKey = errorKey;
            this.clientError.message = ClientError.message(errorKey, errorKey, args);
            return this;
        }

        public Builder addParameterError(String name, String errorKey, Object... args) {
            this.clientError.errorParameters = nullElse(this.clientError.errorParameters, LinkedHashMap::new);
            this.clientError.errorParameters.put(name, ClientError.message(errorKey, errorKey, args));
            return this;
        }

        public Builder errorFields(Collection<FieldError> errors) {
            this.clientError.errorFields = errors.stream()
                    .collect(Collectors.groupingBy(FieldError::getField))
                    .entrySet()
                    .stream()
                    .collect(Collectors.toMap(Map.Entry::getKey,
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
        val context = Context.current();
        if (context == null) {
            return defaultMessage;
        }

        val i18n = context.getI18n();
        if (i18n == null) {
            return defaultMessage;
        }
        return i18n.getMessageOrElse(key, defaultMessage, args);
    }

    public static Builder status(HttpStatus status) {
        val key = "error.common.http." + status.name();
        return new Builder(new ClientError(status)).fMessage(key);
    }

    public static ClientError fromException(Throwable exception) {
        final ClientError clientError;

        if (exception instanceof HttpClientException) {
            val exp = (HttpClientException) exception;
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
