package alvin.study.maven.invalid.bugs;

public class UselessClass {
    @SuppressWarnings("unused")
    public static void run() {
        var a = 1 / 0;
        System.out.println("Hello World!");
    }
}
