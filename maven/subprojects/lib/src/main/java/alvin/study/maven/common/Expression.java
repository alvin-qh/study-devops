package alvin.study.maven.common;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class Expression {
    private final Number firstNumber;
    private final Number secondNumber;
    private final Operator operator;
}
