package alvin.gradle.jacoco;

public class JacocoDemo {

    public boolean isPrime(int number) {
        int counter = (int) Math.sqrt(number);
        for (int i = 2; i <= counter; i++) {
            if (number % i == 0) {
                return false;
            }
        }
        return true;
    }
}
