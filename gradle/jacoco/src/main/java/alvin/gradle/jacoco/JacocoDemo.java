package alvin.gradle.jacoco;

public class JacocoDemo {

    public boolean isPrime(int number) {
        var counter = (int) Math.sqrt(number);
        for (var i = 2; i <= counter; i++) {
            if (number % i == 0) {
                return false;
            }
        }
        return true;
    }
}
