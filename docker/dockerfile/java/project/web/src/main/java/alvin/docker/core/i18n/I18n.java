package alvin.docker.core.i18n;

import java.util.Locale;

import org.springframework.context.MessageSource;

import com.google.common.base.Strings;

import alvin.docker.common.util.Servlets;

/**
 * 本地化语言类型
 */
public class I18n {
    // 语言资源对象
    private final MessageSource messageSource;

    // 本地化对象
    private final Locale locale;

    /**
     * 构造本地化语言对象
     *
     * @param messageSource 语言资源对象
     * @param locale        本地化对象
     */
    public I18n(MessageSource messageSource, Locale locale) {
        this.messageSource = messageSource;
        this.locale = locale;
    }

    public String getMessage(Locale locale, String key, String defaultMessage, Object... args) {
        if (locale == null) {
            locale = Locale.ENGLISH;
        }
        return messageSource.getMessage(key, args, defaultMessage, locale);
    }

    public String getMessage(String key, Object... args) {
        return getMessage(locale, key, key, args);
    }

    public String getMessageOrElse(String key, String defaultMessage, Object... args) {
        return getMessage(locale, key, defaultMessage, args);
    }

    public Locale getLocale() {
        return locale;
    }

    /**
     * 从请求对象中创建本地化对象
     *
     * @return 本地化对象
     */
    public static Locale createRequestLocale() {
        // 获取请求对象, 如果获取不到, 则返回默认本地化对象
        var req = Servlets.request();
        if (req == null) {
            return Locale.ENGLISH;
        }

        // 从请求参数中获取 lang 参数
        var lang = req.getParameter("lang");
        if (!Strings.isNullOrEmpty(lang)) {
            // 根据参数创建本地化对象
            return Locale.forLanguageTag(lang);
        }

        // 根据 Accept-Language 请求头获取本地化对象
        return req.getLocale();
    }
}
