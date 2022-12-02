package alvin.docker.conf;

import org.springframework.context.MessageSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.support.ReloadableResourceBundleMessageSource;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;
import org.springframework.web.context.annotation.RequestScope;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import alvin.docker.core.context.Context;
import alvin.docker.core.context.WebContext;
import alvin.docker.core.http.filter.HttpInterceptor;
import alvin.docker.core.i18n.I18n;
import lombok.RequiredArgsConstructor;

@EnableAsync
@Configuration("conf/web")
@RequiredArgsConstructor
public class WebConfig implements WebMvcConfigurer {
    private final HttpInterceptor httpInterceptor;

    @Lazy
    @Bean
    @RequestScope
    Context context() {
        return new WebContext();
    }

    @Bean
    MessageSource messageSource() {
        var messageSource = new ReloadableResourceBundleMessageSource();
        messageSource.setBasename("classpath:/i18n/messages");
        messageSource.setDefaultEncoding("UTF-8");
        return messageSource;
    }

    @Bean
    LocalValidatorFactoryBean getValidator(MessageSource messageSource) {
        var bean = new LocalValidatorFactoryBean();
        bean.setValidationMessageSource(messageSource);
        return bean;
    }

    @Lazy
    @Bean
    @RequestScope
    I18n i18n(MessageSource messageSource) {
        return new I18n(messageSource, I18n.createRequestLocale());
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(httpInterceptor)
                .addPathPatterns("/**")
                .excludePathPatterns("/static/**");
    }
}
