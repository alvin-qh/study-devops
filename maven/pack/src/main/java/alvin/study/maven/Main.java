package alvin.study.maven;

public class Main {
    public static void main(String[] args) {
        System.out.println("Hello maven project");

        if (args.length == 2) {
            var x = Double.parseDouble(args[0]);
            var y = Double.parseDouble(args[1]);
            System.out.println(String.format("The result is: %s", x + y));
        }
    }
}
