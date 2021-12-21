package alvin.docker.testing.web;

import alvin.docker.core.Context;
import alvin.docker.core.web.ContextImpl;
import alvin.docker.core.web.I18n;
import org.springframework.context.MessageSource;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;

import javax.inject.Inject;
import java.util.Locale;

@Component
public class ContextMocker {

    @Inject
    private MessageSource messageSource;

    public void mock() {
        final Context context = new ContextImpl();
        context.setRequestPath("fake-request-path");
        context.setI18n(new I18n(messageSource, Locale.US));
        Context.register(context);
    }

    public void clear() {
        RequestContextHolder.resetRequestAttributes();
    }
}
