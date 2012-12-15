
package h2o.util.concurrent.queuebench;


import java.util.concurrent.ExecutionException;

/**
 * Used to test the performance of ConcurrentLinkedBlockingQueue vs LinkedBlockingQueue.
 * <p>
 * Written by Hanson Char and released to the public domain, as explained at
 * http://creativecommons.org/licenses/publicdomain
 * 
 * @see LinkedBlockingQueueTest
 * @see BlockingQueueTestMain
 * @author Hanson Char
 */
public class QueueTest
{

    public static void main(String[] args) throws InterruptedException, ExecutionException
    {
        final float wcRatio = floatValue("wcRatio", "0.0");
        final int numConsumer = intValue("numConsumer", "1");
        final int numProducer = intValue("numProducer", "10");
        final Integer capacity = integerValue("capacity");

        for (int i = 0; i < 10; i++)
        {
            new ConcurrentLinkedBlockingQueueTest(wcRatio, numConsumer, numProducer, capacity).call();
            // try to minimize residual memory effect
            System.gc();
            new LinkedBlockingQueueTest(wcRatio, numConsumer, numProducer, capacity).call();
            // try to minimize residual memory effect
            System.gc();
        }
        System.exit(0);
    }

    private static int intValue(String key, String def)
    {
        String val = System.getProperty(key, def);
        return Integer.parseInt(val);
    }

    private static Integer integerValue(String key)
    {
        String val = System.getProperty(key);
        return val == null ? null : new Integer(val);
    }

    private static float floatValue(String key, String def)
    {
        String val = System.getProperty(key, def);
        return Float.parseFloat(val);
    }
}
