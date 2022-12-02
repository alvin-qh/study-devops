package alvin.docker.common.util;

import javax.servlet.http.HttpServletRequest;

import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * Servlet 相关工具类
 */
public final class Servlets {
    /**
     * 禁止实例化
     */
    private Servlets() {
    }

    /**
     * 获取当前会话的 {@link HttpServletRequest} 请求对象
     *
     * @return {@link HttpServletRequest} 对象
     */
    public static HttpServletRequest request() {
        var attr = RequestContextHolder.getRequestAttributes();
        if (attr == null) {
            return null;
        }
        return ((ServletRequestAttributes) attr).getRequest();
    }
}
