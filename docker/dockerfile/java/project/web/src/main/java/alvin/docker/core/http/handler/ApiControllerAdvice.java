package alvin.docker.core.http.handler;

import java.util.ArrayList;

import javax.validation.ConstraintViolationException;
import javax.validation.Path;

import org.springframework.core.MethodParameter;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.validation.BindException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice;

import com.google.common.base.Joiner;

import alvin.docker.core.http.exception.ClientException;
import alvin.docker.core.http.model.ClientError;
import alvin.docker.core.http.model.ResponseWrapper;
import lombok.extern.slf4j.Slf4j;

/**
 * 全局异常处理
 */
@Slf4j
@RestController
@RestControllerAdvice(basePackages = { "alvin.docker" })
public class ApiControllerAdvice implements ResponseBodyAdvice<Object> {
    /**
     * 返回 {@code true} 或 {@code false}, 以决定 {@code beforeBodyWrite} 方法是否需要执行
     *
     * <p>
     * 该方法相当于是一个前置判断, 对于每一个被调用的 Controller 进行判断, 如果返回 {@code true}, 则下一步会执行
     * {@code beforeBodyWrite} 方法对本次的 Controller 的返回值进行处理
     * </p>
     *
     * <p>
     * 判断的依据为所调用 Controller 方法信息 ({@code returnType} 参数) 以及预定义的返回值处理对象
     * ({@code converterType}) 参数
     * </p>
     */
    @Override
    public boolean supports(MethodParameter returnType, Class<? extends HttpMessageConverter<?>> converterType) {
        // 获取 controller 方法的返回值类型
        var retType = returnType.getMethod().getReturnType();

        // 如果 controller 方法返回类型为指定类型, 则返回 false
        return !ResponseWrapper.class.isAssignableFrom(retType)
                && !ResponseEntity.class.isAssignableFrom(retType)
                && !CharSequence.class.isAssignableFrom(retType);
    }

    /**
     * 在响应数据写入返回数据流前进行处理
     *
     * <p>
     * 当 {@code supports} 方法返回 {@code true} 后, Controller 的返回值会传递到该方法中进行处理,
     * 以改变预定义的处理行为和结果
     * </p>
     *
     * <p>
     * 本例中, 对所有不是 {@link ResponseWrapper} 和 {@link ResponseEntity} 类型的返回值, 均包装为
     * {@link ResponseWrapper} 类型返回, 由此对 Controller 返回客户端的数据格式进行了统一
     * </p>
     *
     * @see ResponseWrapper#success(Object)
     */
    @Override
    public Object beforeBodyWrite(
            Object body, // controller 方法返回的返回值
            MethodParameter returnType,
            MediaType selectedContentType,
            Class<? extends HttpMessageConverter<?>> selectedConverterType,
            ServerHttpRequest request,
            ServerHttpResponse response) {
        // 返回表示正确的 Response 对象
        return ResponseWrapper.success(body);
    }

    /**
     * 处理其余未处理异常
     *
     * <p>
     * 处理 {@link Exception} 类型异常
     * </p>
     */
    @ResponseBody
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    @ExceptionHandler(Exception.class)
    public ResponseWrapper<ClientError> handle(Exception e) {
        log.warn("Some error raised and will return to client", e);

        return ResponseWrapper.error(ClientError.newBuilder("internal_error").build());
    }

    /**
     * 处理参数绑定异常
     *
     * <p>
     * 处理 {@link BindException} 类型异常
     * </p>
     *
     * <p>
     * 当 Controller 在绑定请求参数时出现错误时抛出的异常
     * </p>
     */
    @ResponseBody
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ExceptionHandler(BindException.class)
    public ResponseWrapper<ClientError> handle(BindException e) {
        return ResponseWrapper.error(
                ClientError.newBuilder("invalid_request_args")
                        .errorFields(e.getBindingResult().getFieldErrors())
                        .build());
    }

    /**
     * 将 {@link Path} 类型对象转为字符串
     *
     * <p>
     * {@code path} 参数表示导致错误的 Controller 方法的参数的路径, 有两种情况
     * </p>
     *
     * <ul>
     * <li>简单参数: 即 {@code int}, {@code String} 这类, 路径格式为 {@code 方法名.参数名}</li>
     * <li>符合对象参数: 即参数为一个对象, 路径格式为 {@code 方法名.参数名.字段名}</li>
     * </ul>
     *
     * <p>
     * 一般情况下这类异常反馈给客户端为 {@code 400 BAD_REQUEST} 错误响应
     * </p>
     *
     * @param path {@link Path} 类型对象, 表示导致错误的 Controller 参数路径
     * @return 字符串结果
     */
    private String pathToPropertyName(Path path) {
        var result = new ArrayList<String>();

        // 获取 path 对象的迭代器, 遍历所有的路径节点
        var it = path.iterator();
        // 跳过第一个节点, 该节点表示调用的 controller 方法名
        if (it.hasNext()) {
            it.next();
        }

        // 遍历之后的节点, 进行节点名进行保存
        while (it.hasNext()) {
            result.add(it.next().getName());
        }

        // 将节点名集合用 . 符合连接
        return Joiner.on(".").join(result);
    }

    /**
     * 处理请求参数校验错误异常
     *
     * <p>
     * 处理 {@link ConstraintViolationException} 类型异常
     * </p>
     *
     * <p>
     * 一般情况下这类异常反馈给客户端为 {@code 400 BAD_REQUEST} 错误响应
     * </p>
     *
     * @param e 异常对象
     * @return 包装为 {@link ResponseWrapper} 类型的响应结果
     */
    @ResponseBody
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseWrapper<ClientError> handle(ConstraintViolationException e) {
        log.warn("Some error raised and will return to client", e);

        var builder = ClientError.newBuilder("invalid_request_args");
        for (var v : e.getConstraintViolations()) {
            builder.addParameterError(pathToPropertyName(v.getPropertyPath()), v.getMessage());
        }
        return ResponseWrapper.error(builder.build());
    }

    /**
     * 处理 Controller 参数缺失异常
     *
     * <p>
     * 处理 {@link MissingServletRequestParameterException} 类型异常
     * </p>
     *
     * <p>
     * 该异常表示缺失必要的请求参数, 导致对应的 Controller 方法无法被调用
     * </p>
     *
     * <p>
     * 一般情况下这类异常反馈给客户端为 {@code 400 BAD_REQUEST} 错误响应
     * </p>
     *
     * @param e {@link MissingServletRequestParameterException} 类型异常对象
     * @return 包装为 {@link ResponseWrapper} 类型的响应结果
     */
    @ResponseBody
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseWrapper<ClientError> handle(MissingServletRequestParameterException e) {
        log.warn("Some error raised and will return to client", e);

        return ResponseWrapper.error(ClientError.newBuilder("missing_request_args")
                .addParameterError(e.getParameterName(), e.getMessage())
                .build());
    }

    /**
     * 处理请求方法无效异常
     *
     * <p>
     * 处理 {@link HttpRequestMethodNotSupportedException} 类型异常
     * </p>
     *
     * <p>
     * 表示 HTTP 请求方法无效
     * </p>
     *
     * <p>
     * 一般情况下这类异常反馈给客户端为 {@code 405 METHOD_NOT_ALLOWED} 错误响应
     * </p>
     *
     * @param e {@link MissingServletRequestParameterException} 类型异常对象
     * @return 包装为 {@link ResponseWrapper} 类型的响应结果
     */
    @ResponseBody
    @ResponseStatus(HttpStatus.METHOD_NOT_ALLOWED)
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseWrapper<ClientError> handle(HttpRequestMethodNotSupportedException e) {
        log.warn("Some error raised and will return to client", e);

        return ResponseWrapper.error(ClientError.newBuilder("method_not_allowed").build());
    }

    /**
     * 处理要通知客户端的异常
     *
     * <p>
     * 处理 {@link ClientException} 类型异常
     * </p>
     *
     * <p>
     * 该异常需要客户端关注
     * </p>
     *
     * <p>
     * 一般情况下这类异常反馈给客户端为 {@code 400 BAD_REQUEST} 错误响应
     * </p>
     *
     * @param e {@link ClientException} 异常对象
     * @return 包装为 {@link ResponseWrapper} 类型的响应结果
     */
    @ResponseBody
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ExceptionHandler(ClientException.class)
    public ResponseWrapper<ClientError> handle(ClientException e) {
        log.warn("Some error raised and will return to client", e);

        return ResponseWrapper.error(ClientError.newBuilder(e.getMessage()).build());
    }
}
