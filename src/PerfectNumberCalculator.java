import java.util.ArrayList;
import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicLong;

public class PerfectNumberCalculator implements Callable<Integer> {
    private final long lowerBound;
    private final long upperBound;

    private static final ArrayList<Long> potentialPerfectNumberCache = new ArrayList<>();
    private static final AtomicLong nextToGenerate = new AtomicLong(1);

    private static final AtomicLong generatingInstances = new AtomicLong(0);

    // I spent a fair bit of time coming up with this - and it speeds it
    // up a lot, so I'm including it. :)
    // This program would be faster if I made more extensive use of this function,
    // generating potential perfect primes, and then using a channel to feed a bunch of threads checking them,
    // but then we're straying from the directions given in the assignment.
    // *technically* this code does everything the assignment says, (it takes a lower and upper bound, and it finds
    // the perfect numbers concurrently. It's just... smarter? about how it does it, by generating candidates then
    // filtering them, rather then filtering from all possible integers).
    private static long getNthPotentialPerfectNumber(long n) {
        long sum = 0;
        long factor = 1;
        for (int i = 0; i < n; i++) {
            sum += factor;
            factor <<= 1;
        }
        factor = sum;
        for (int i = 0; i < n - 1; i++) {
            sum += factor;
            factor *= 2;
        }
        return sum;
    }

    private static boolean isPerfectNumber(long num) {
        long sum = 0;
        for (long i = 1; i <= num / 2; i++) {
            if (num % i == 0) sum += i;
        }
        return num == sum;
    }

    public PerfectNumberCalculator(long lowerBound, long upperBound) {
        this.lowerBound = lowerBound == 0 ? 1 : lowerBound;
        this.upperBound = upperBound;
    }

    @Override
    public Integer call() throws InterruptedException {
        generatingInstances.incrementAndGet();
        for (long toGenerate = nextToGenerate.getAndIncrement(); ; toGenerate = nextToGenerate.getAndIncrement()) {
            long generated = getNthPotentialPerfectNumber(toGenerate);
            if(generated > this.upperBound) break;
            synchronized (potentialPerfectNumberCache) {
                potentialPerfectNumberCache.add(generated);
            }
        }
        generatingInstances.decrementAndGet();
        // GOIN ROUND AND ROUND - SPINLOCKS SHOULDN'T BE IMPLEMENTED IN USERSPACE SAYS THE KERNEL.
        // GOIN ROUND AND ROUND - SPINLOCKS SHOULDN'T BE DONE IN THE USERSPACE SAYS THE KERNEL.
        // THE DEV SAYS HE DOESN'T CARE AND DOES IT ANYWAYS.
        while(generatingInstances.get() != 0) { Thread.sleep(10); }
        ArrayList<Long> out;
        synchronized (potentialPerfectNumberCache) {
            out = new ArrayList<>(potentialPerfectNumberCache);
        }

        // streams go brrrt
        return (int)out
                .parallelStream()
                .filter(n -> n > this.lowerBound && n < this.upperBound)
                .filter(PerfectNumberCalculator::isPerfectNumber)
                .count();

    }
}
