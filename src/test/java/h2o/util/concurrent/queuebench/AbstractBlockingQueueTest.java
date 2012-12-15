
package h2o.util.concurrent.queuebench;

import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.*;

/**
 * Base class for testing the performance of ConcurrentLinkedBlockingQueue vs
 * LinkedBlockingQueue.
 * <p>
 * Written by Hanson Char and released to the public domain, as explained at
 * http://creativecommons.org/licenses/publicdomain
 * 
 * @author Hanson Char
 */
public abstract class AbstractBlockingQueueTest implements Callable<Void>
{
    protected static final int TOTAL = 100000 * 10;

    private static Integer[] data = new Integer[TOTAL];

    static
    {
        for (int i = 0; i < data.length; i++)
            data[i] = i;
    }

    protected static final int REPEAT = 10;

    /** Wait-time to compute-time ratio. */
    protected final float wcRatio;
    protected final int numConsumer;
    protected final int numProducer;
    protected final Integer capacity;

    private final int producerThreadPoolSize;
    private final int consumerThreadPoolSize;
    private final int batchSize;
    protected final int totalSize;

    protected AbstractBlockingQueueTest(float wcRatio, int numConsumer, int numProducer, Integer capacity)
    {
        this.wcRatio = wcRatio < 0 ? 0 : wcRatio;
        this.numConsumer = numConsumer < 1 ? 1 : numConsumer;
        this.numProducer = numProducer < 1 ? 1 : numProducer;
        this.capacity = capacity;

        final int numProcessors = Runtime.getRuntime().availableProcessors();
        // JCiP section 8.2 - Sizing thread pools
        int threadPoolSize = (int)(numProcessors * (1 + this.wcRatio));

        if (threadPoolSize == numProcessors) threadPoolSize++;
        this.producerThreadPoolSize = numProducer > threadPoolSize ? threadPoolSize : numProducer;
        this.consumerThreadPoolSize = numConsumer > threadPoolSize ? threadPoolSize : numConsumer;
        this.batchSize = TOTAL / this.numProducer;
        this.totalSize = this.batchSize * this.numProducer;

        // System.out.println("wcRatio:" + this.wcRatio);
        // System.out.println("numConsumer:" + this.numConsumer);
        // System.out.println("numProducer:" + this.numProducer);
        // System.out.println("capacity:" + this.capacity);
        // System.out.println("producerThreadPoolSize:" + producerThreadPoolSize);
        // System.out.println("consumerThreadPoolSize:" + consumerThreadPoolSize);
        // System.out.println("batchSize:" + this.batchSize);
        // System.out.println("totalSize:" + this.totalSize);
        // System.out.println();
    }

    protected AbstractBlockingQueueTest()
    {
        this(0, 1, 10, null);
    }

    protected abstract Queue<Integer> getQueue();

    protected abstract Callable<Void> newConumerCallable(int max);

    protected abstract BlockingQueue<Runnable> newThreadPoolBlockingQueue(Integer capacity);

    public Void call() throws InterruptedException, ExecutionException
    {
        long totalDuration = 0;
        long min = Long.MAX_VALUE;
        long max = Long.MIN_VALUE;

        for (int i = 0; i < REPEAT; i++)
        {
            long duration = this.test();
            // System.out.println("Test#" + i + ", duration: " + duration + " ms");

            if (duration < min) min = duration;
            if (duration > max) max = duration;
            totalDuration += duration;
        }
        long average = totalDuration / REPEAT;
        System.out.println();
        System.out.println(getClass().getName());
        System.out.println("Producer thread pool size is " + producerThreadPoolSize);
        System.out.println("Consumer thread pool size is " + consumerThreadPoolSize);
        System.out.println("Total items per test: " + TOTAL + ", Tested: " + REPEAT + " times" + "\nAvg: "
                           + average + " ms" + "\nmin: " + min + " ms" + "\nmax: " + max + " ms");
        System.out.println();
        return null;
    }

    public long test() throws InterruptedException, ExecutionException
    {
        int takeSize = this.totalSize / numConsumer;
        int takeExtra = this.totalSize - takeSize * numConsumer;
        final ExecutorService producerExecutorService = newFixedThreadPool(producerThreadPoolSize);
        final ExecutorService consumerExecutorService = newFixedThreadPool(consumerThreadPoolSize);
        List<Future<Void>> consumerFutures = new ArrayList<Future<Void>>(numConsumer + 1);
        List<Future<Void>> producerFutures = new ArrayList<Future<Void>>(numProducer);

        final long t0 = System.nanoTime();
        // Submit the consumers
        consumerFutures.add(consumerExecutorService.submit(newConumerCallable(takeSize + takeExtra)));

        for (int i = 1; i < numConsumer; i++)
            consumerFutures.add(consumerExecutorService.submit(newConumerCallable(takeSize)));
        // Submit all producers
        for (int i = 0; i < numProducer; i++)
            producerFutures.add(producerExecutorService.submit(newProducer(i * batchSize)));
        // wait for all producers to complete
        for (Future<Void> producerFuture : producerFutures)
            producerFuture.get();
        // wait for all consumers to complete
        for (Future<Void> consumerFuture : consumerFutures)
            consumerFuture.get();
        // Calculate the duration
        long duration = (System.nanoTime() - t0) / 1000000;
        // Shutdown all thread pools
        producerExecutorService.shutdownNow();
        consumerExecutorService.shutdownNow();
        return duration;
    }

    private ExecutorService newFixedThreadPool(int nThreads)
    {
        return new ThreadPoolExecutor(nThreads, nThreads, 0L, TimeUnit.MILLISECONDS,
            newThreadPoolBlockingQueue(this.capacity));
    }

    /**
     * Returns a new producer, producing BATCH_SIZE number of items from the given start
     * number.
     */
    private Callable<Void> newProducer(final int start)
    {
        return new Callable<Void>()
        {
            public Void call() throws InterruptedException
            {
                Queue<Integer> q = getQueue();

                for (int i = start, end = start + batchSize; i < end; i++)
                    q.offer(data[i]);
                return null;
            }
        };
    }
}
