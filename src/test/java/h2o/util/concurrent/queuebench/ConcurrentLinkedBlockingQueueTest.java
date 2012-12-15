
package h2o.util.concurrent.queuebench;

import h2o.util.concurrent.ConcurrentLinkedBlockingQueue;

import java.util.Queue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;

/**
 * Used to test the performance of ConcurrentLinkedBlockingQueue.
 * <p>
 * Written by Hanson Char and released to the public domain, as explained at
 * http://creativecommons.org/licenses/publicdomain
 * 
 * @see LinkedBlockingQueueTest
 * @author Hanson Char
 */
public class ConcurrentLinkedBlockingQueueTest extends AbstractBlockingQueueTest
{
    private final ConcurrentLinkedBlockingQueue<Integer> q = new ConcurrentLinkedBlockingQueue<Integer>();

    public ConcurrentLinkedBlockingQueueTest(float wcRatio, int numConsumer, int numProducer, Integer capacity)
    {
        super(wcRatio, numConsumer, numProducer, capacity);
    }

    public ConcurrentLinkedBlockingQueueTest()
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
        return capacity == null
                        ? new ConcurrentLinkedBlockingQueue<Runnable>()
                        : new ConcurrentLinkedBlockingQueue<Runnable>(capacity);
    }

    public static void main(String[] args) throws InterruptedException, ExecutionException
    {
        new ConcurrentLinkedBlockingQueueTest().call();
        System.exit(0);
    }
}
