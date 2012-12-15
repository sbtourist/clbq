
package h2o.util.concurrent.queuebench;


import java.util.Queue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * Used to test the performance of LinkedBlockingQueue.
 * <p>
 * Written by Hanson Char and released to the public domain, as explained at
 * http://creativecommons.org/licenses/publicdomain
 * 
 * @see ConcurrentLinkedBlockingQueueTest
 * @author Hanson Char
 */
public class LinkedBlockingQueueTest extends AbstractBlockingQueueTest
{
    private final LinkedBlockingQueue<Integer> q = new LinkedBlockingQueue<Integer>();

    public LinkedBlockingQueueTest(float wcRatio, int numConsumer, int numProducer, Integer capacity)
    {
        super(wcRatio, numConsumer, numProducer, capacity);
    }

    public LinkedBlockingQueueTest()
    {
        super();
    }

    @Override
    protected Queue<Integer> getQueue()
    {
        return q;
    }

    @Override
    protected Callable<Void> newConumerCallable(final int max)
    {
        return new Callable<Void>()
        {
            public Void call() throws InterruptedException
            {
                for (int count = 0; count < max; count++)
                    q.take();
                return null;
            }
        };
    }

    @Override
    protected BlockingQueue<Runnable> newThreadPoolBlockingQueue(Integer capacity)
    {
        return capacity == null ? new LinkedBlockingQueue<Runnable>() : new LinkedBlockingQueue<Runnable>(
            capacity);
    }

    public static void main(String[] args) throws InterruptedException, ExecutionException
    {
        new LinkedBlockingQueueTest().call();
        System.exit(0);
    }
}
