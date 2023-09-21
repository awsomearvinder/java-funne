import java.util.ArrayList;
import java.util.Scanner;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadPoolExecutor;

public class Driver {
    public static int computeCount(long cap, int threads) {
        var pool = (ThreadPoolExecutor)Executors.newFixedThreadPool(threads);
        var jobs = new ArrayList<Future<Integer>>();
        for(int i = 0; i < threads; i++) {
            long lowerBound = i * cap / threads;
            long upperBound = (i + 1) * cap / threads;
            jobs.add(pool.submit(new PerfectNumberCalculator(lowerBound, upperBound)));
        }
        var count = jobs.stream().map(i -> {
            try {
                return i.get();
            } catch (Exception e) { // this shouldn't ever actually throw
                throw new RuntimeException(e);
            }
        }).reduce((i, j) -> i + j);
        return count.orElse(0);
    }

    public static void main(String[] args) {
        try(var userInput = new Scanner(System.in)) {
            System.out.print("Up until how large of an N would you like to attempt: ");
            System.out.flush();
            long cap = userInput.nextLong();
            if(cap > 10 * Math.pow(10, 9)) {
                System.out.println("Too big of an N!");
                System.exit(1);
            }
            System.out.print("How many threads: ");
            int threads = userInput.nextInt();
            var time = System.currentTimeMillis();
            var count = computeCount(cap, threads);
            var timeAfter = System.currentTimeMillis();
            System.out.println("You have " + count + " perfect numbers in that space.");
            System.out.println("It took " + (timeAfter - time) + " millis");
        }
    }
}