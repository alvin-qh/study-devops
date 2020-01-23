package alvin.docker;

import alvin.docker.common.Context;
import alvin.docker.common.I18nProvider;
import alvin.docker.common.context.ContextImpl;
import alvin.docker.utils.IDGenerator;
import alvin.docker.utils.Times;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.context.annotation.RequestScope;

@Configuration
public class ApplicationConfig {

    @Bean
    @RequestScope
    Context context() {
        return new ContextImpl();
    }

    @Bean
    public Times times() {
        return new Times();
    }

    @Bean
    public IDGenerator idGenerator(Times times) {
        return new IDGenerator(times);
    }

    @Bean
    public I18nProvider i18nProver(Context context) {
        return new I18nProvider(context);
    }
}
