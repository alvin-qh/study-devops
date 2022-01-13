package alvin.study.maven.spring.common.model;

import lombok.Getter;

@Getter
@RequiredArgsConstructor
public class Response<T> {
    private final String error;
    private final T payload;
}
