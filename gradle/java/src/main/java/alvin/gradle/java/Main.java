package alvin.gradle.java;

import com.google.common.base.Joiner;

public final class Main {

    String format(String[] args) {
        var joined = Joiner.on(",").join(args);
        return String.format("Main class is running, arguments is %s ...", joined);
    }

    private void run(String[] args) {
        System.out.println(format(args));
    }

    public static void main(String[] args) {
        new Main().run(args);
    }
}
