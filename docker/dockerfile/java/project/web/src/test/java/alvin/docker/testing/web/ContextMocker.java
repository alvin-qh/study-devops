package alvin.docker.testing.web;

import java.util.Locale;

import javax.inject.Inject;

import org.springframework.context.MessageSource;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;

import alvin.docker.core.context.Context;
import alvin.docker.core.context.WebContext;
import alvin.docker.core.i18n.I18n;

@Component
public class ContextMocker {

    @Inject
    private MessageSource messageSource;

    public void mock() {
        final Context context = new WebContext();
        context.setRequestPath("fake-request-path");
        context.setI18n(new I18n(messageSource, Locale.US));
        Context.register(context);
    }

    public void clear() {
        RequestContextHolder.resetRequestAttributes();
    }
}
