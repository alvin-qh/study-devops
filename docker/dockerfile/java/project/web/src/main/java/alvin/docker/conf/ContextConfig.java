package alvin.docker.conf;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.web.context.annotation.RequestScope;

import alvin.docker.core.context.Context;
import alvin.docker.core.context.WebContext;
import alvin.docker.core.i18n.I18n;

@Configuration("core/context")
public class ContextConfig {
    @Lazy
    @RequestScope
    Context context() {
        return new WebContext();
    }

    @Bean
    public I18n i18nProver(Context context) {
        return new I18nProvider(context);
    }
}
