package alvin.study.maven;

import com.google.common.base.Strings;

public final class Main {
    public static void main(String[] args) {
        new Main().execute(args);
    }

    protected void execute(String[] args) {
        System.out.println("Hello maven project");

        if (args.length == 2) {
            var x = parseArg(args, 0);
            var y = parseArg(args, 1);
            if (x != null && y != null) {
                System.out.println(String.format("The result is: %s", x + y));
            }
        }
    }

    private Double parseArg(String[] args, int index) {
        var arg = args[index];
        arg = arg.trim();
        if (Strings.isNullOrEmpty(arg)) {
            return null;
        }
        return Double.parseDouble(arg);
    }
}
