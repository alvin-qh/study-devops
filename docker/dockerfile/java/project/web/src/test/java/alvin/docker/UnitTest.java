package alvin.docker;

import java.io.InputStreamReader;
import java.io.Reader;

import com.google.common.base.Charsets;
import com.google.common.io.CharStreams;

import lombok.SneakyThrows;

/**
 * 单元测试超类
 */
public abstract class UnitTest {
    /**
     * 读取资源内容, 返回字符串
     *
     * @param resourceName 资源名称
     * @return 资源内容字符串
     */
    @SneakyThrows
    protected String loadResourceAsString(String resourceName) {
        try (Reader reader = new InputStreamReader(getClass().getResourceAsStream(resourceName), Charsets.UTF_8)) {
            return CharStreams.toString(reader);
        }
    }

    /**
     * 令线程休眠一段时间
     *
     * @param millis 休眠的毫秒数
     */
    @SneakyThrows
    protected void delay(long millis) {
        Thread.sleep(millis);
    }
}
