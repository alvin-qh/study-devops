package alvin.docker.testing.builder;

import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

@Component
public class BuilderFactory {
    private final ApplicationContext context;

    public BuilderFactory(ApplicationContext context) {
        this.context = context;
    }

    public <T> T newBuilder(Class<? extends T> type) {
        if (type.getAnnotation(Builder.class) == null) {
            throw new IllegalArgumentException("Need \"@Builder\" annotation for builder class");
        }
        return context.getBean(type);
    }
}
