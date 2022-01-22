package alvin.study.maven;

import com.google.common.primitives.Doubles;

import alvin.study.maven.common.Expression;
import alvin.study.maven.common.Operator;
import alvin.study.maven.common.Unit;

public final class Application {
    public static void main(String[] args) {
        var app = new Application();
        app.exec(args);
    }

    private void exec(String[] args) {
        if (args == null || args.length < 2) {
            System.err.println("Please press enter two numeric arguments");
            return;
        }

        var a = Doubles.tryParse(args[0]);
        if (a == null) {
            System.err.println(String.format("Argument %s must be a number", args[0]));
            return;
        }

        var b = Doubles.tryParse(args[1]);
        if (b == null) {
            System.err.println(String.format("Argument %s must be a number", args[1]));
            return;
        }

        Operator opt = Operator.ADD;
        if (args.length > 2) {
            try {
                opt = Operator.valueOf(args[2].toUpperCase());
            } catch (IllegalArgumentException e) {
            }
        }

        var exp = new Expression(a, b, opt);
        var unit = new Unit();
        System.out.println(String.format("The result is: %s", unit.calculate(exp)));
    }
}
