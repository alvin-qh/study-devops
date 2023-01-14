package alvin.study.maven.invalid.bugs;

@SuppressWarnings("all")
public class UselessClass {
    public static void run() {
        var a = 1 / 0;
        System.out.println("Hello World!");
    }
}
