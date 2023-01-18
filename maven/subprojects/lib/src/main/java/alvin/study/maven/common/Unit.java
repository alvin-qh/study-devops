package alvin.study.maven.common;

public class Unit {
    public Number calculate(Expression expression) {
        double n1 = expression.getFirstNumber().doubleValue();
        double n2 = expression.getSecondNumber().doubleValue();
        double result = 0;

        switch (expression.getOperator()) {
        case ADD:
            result = n1 + n2;
            break;
        case SUB:
            result = n1 - n2;
            break;
        case MUL:
            result = n1 * n2;
            break;
        case DIV:
            result = n1 / n2;
            break;
        case MOD:
            result = n1 % n2;
            break;
        default:
            throw new IllegalArgumentException(String.format("invalid operator \"%s\"", expression.getOperator()));
        }
        return Double.valueOf(result);
    }
}
