package alvin.docker.web;

import alvin.docker.filters.CrossDomainInterceptor;
import alvin.docker.filters.HttpInterceptor;
import org.springframework.boot.web.server.ErrorPage;
import org.springframework.boot.web.server.WebServerFactoryCustomizer;
import org.springframework.boot.web.servlet.server.ConfigurableServletWebServerFactory;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import javax.inject.Inject;

@Configuration
public class WebConfig implements WebMvcConfigurer,
        WebServerFactoryCustomizer<ConfigurableServletWebServerFactory> {

    private final HttpInterceptor httpInterceptor;
    private final CrossDomainInterceptor crossDomainInterceptor;

    @Inject
    public WebConfig(HttpInterceptor httpInterceptor,
                     CrossDomainInterceptor crossDomainInterceptor) {
        this.httpInterceptor = httpInterceptor;
        this.crossDomainInterceptor = crossDomainInterceptor;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(httpInterceptor)
                .addPathPatterns("/**")
                .excludePathPatterns("/static/**");

        registry.addInterceptor(crossDomainInterceptor)
                .addPathPatterns("/api/**");
    }

    @Override
    public void customize(ConfigurableServletWebServerFactory factory) {
        factory.addErrorPages(new ErrorPage("/error"));
    }
}