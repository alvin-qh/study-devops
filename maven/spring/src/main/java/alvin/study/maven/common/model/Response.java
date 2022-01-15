package alvin.study.maven.common.model;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * response 包装类
 */
@Getter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@NoArgsConstructor(access = AccessLevel.PACKAGE)
public final class Response<T> {
    // 错误码
    private int retCode;
    // 错误信息
    private String errMsg;
    // 负载
    private T payload;

    /**
     * 包装正确 response
     */
    public static <T> Response<T> ok(T payload) {
        return new Response<>(0, "OK", payload);
    }

    /**
     * 包装错误 response
     */
    public static Response<Void> error(int retCode, String errMsg) {
        return new Response<>(retCode, errMsg, null);
    }
}
