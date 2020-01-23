package alvin.docker.common.i18n;

import org.springframework.context.MessageSource;

import java.util.Locale;

import static alvin.docker.utils.Values.nullElse;

public class I18n {
    private final MessageSource messageSource;
    private final Locale locale;

    public I18n(MessageSource messageSource, Locale locale) {
        this.messageSource = messageSource;
        this.locale = locale;
    }

    public String getMessage(Locale locale, String key, String defaultMessage, Object... args) {
        locale = nullElse(locale, () -> Locale.ENGLISH);
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
