package alvin.study.maven.spring.common.model;

import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class Error {
    private final String errorCode;
    private final String errorMessage;
}
