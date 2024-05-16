package alvin.docker.core.http.model;

import lombok.Data;

/**
 * 应用程序信息类型, 对应 {@code classpath:application.yml} 文件中 {@code application} 相关配置项
 */
@Data
public class ApplicationInfo {
    // 跨域配置
    private final CrossDomain crossDomain = new CrossDomain();

    // 应用程序版本号
    private String version;

    // 应用程序时区
    private String zone;

    /**
     * 跨域配置项
     */
    @Data
    public static class CrossDomain {
        // 是否允许跨域请求
        private boolean enabled;

        // 跨域范围设置
        private String allowOrigin;
        private String allowHeaders;
        private String allowMethods;

        // 允许跨域访问的时长
        private int maxAge;
    }
}
