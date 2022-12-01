package alvin.docker.core.http;

import javax.servlet.http.HttpServletRequest;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MissingPathVariableException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice;

import alvin.docker.core.context.Context;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RestControllerAdvice(basePackages = { "alvin.docker" })
public class GlobalExceptionHandler implements ResponseBodyAdvice<Object> {
    private Object convertError(ClientError error) {
        if (context.getTarget() == Context.Target.API) {
            return new ResponseEntity<>(error, error.getStatus());
        }
        return new ModelAndView("errors/error_page", error.toMap(), error.getStatus());
    }

    @ResponseBody
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    @ExceptionHandler(Exception.class)
    public Object handleAllExceptions(Exception exception) {
        return convertError(ClientError.fromException(exception));
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public Object handleMissingServletRequestParameterException(MissingServletRequestParameterException exception) {
        return convertError(ClientError.errorParameters(exception).build());
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public Object handleHttpMessageNotReadableException(HttpMessageNotReadableException ignore) {
        return convertError(ClientError.badRequest("error.common.InvalidRequestBody").build());
    }

    @ExceptionHandler(MissingPathVariableException.class)
    public Object handleMissingPathVariableException(MissingPathVariableException ignore) {
        return convertError(ClientError.notFound().build());
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public Object handleHttpRequestMethodNotSupportedException(HttpRequestMethodNotSupportedException ignore) {
        return convertError(ClientError.status(HttpStatus.METHOD_NOT_ALLOWED).build());
    }

    private ClientError makeError(HttpServletRequest request) {
        final Integer errorCode = nullElse((Integer) request.getAttribute(ERROR_STATUS_CODE),
                HttpStatus.INTERNAL_SERVER_ERROR::value);
        final HttpStatus status = HttpStatus.resolve(errorCode);
        return ClientError.status(nullElse(status, HttpStatus.INTERNAL_SERVER_ERROR)).build();
    }

    @GetMapping(value = ERROR_URI, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public ClientError errorAsJson(HttpServletRequest request) {
        return makeError(request);
    }

    @GetMapping(value = ERROR_URI, produces = { MediaType.TEXT_HTML_VALUE, MediaType.APPLICATION_XHTML_XML_VALUE })
    public ModelAndView errorAsHtml(HttpServletRequest request) {
        final ClientError error = makeError(request);
        return new ModelAndView(getErrorPath(), error.toMap(), error.getStatus());
    }

    public String getErrorPath() {
        return "errors/error_page";
    }
}
