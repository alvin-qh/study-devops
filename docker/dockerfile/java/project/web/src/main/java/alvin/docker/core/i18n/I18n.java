package alvin.docker.core.i18n;

import java.util.Locale;

import org.springframework.context.MessageSource;

public class I18n {
    private final MessageSource messageSource;
    private final Locale locale;

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
}
