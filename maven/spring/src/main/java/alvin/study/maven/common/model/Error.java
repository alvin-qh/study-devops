package alvin.study.maven.common.model;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class Error {
    private final String errorCode;
    private final String errorMessage;
}
