package alvin.gradle.java;

import com.google.common.base.Joiner;

public final class Main {

    private void run(String args) {
        System.out.printf("Main class is running, arguments is %s ...%n", args);
    }

    public static void main(String[] args) {
        new Main().run(Joiner.on(",").join(args));
    }
}
