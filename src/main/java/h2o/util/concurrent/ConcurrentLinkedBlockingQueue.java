
package h2o.util.concurrent;

import java.io.Serializable;
import java.util.AbstractQueue;
import java.util.Collection;
import java.util.Iterator;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.LockSupport;

/**
 * An optionally bounded concurrent blocking queue implemented with
 * {@link java.util.concurrent.ConcurrentLinkedQueue ConcurrentLinkedQueue}.
 * <p>
 * Written by Hanson Char and released to the public domain, as explained at <a
 * href=http://creativecommons.org/licenses/publicdomain>CreativeCommons</a>
 * 
 * @author Hanson Char
 * @param <E> the type of elements held in this collection
 */
public class ConcurrentLinkedBlockingQueue<E> extends AbstractQueue<E>
    implements Serializable, BlockingQueue<E>
{
    private static final long serialVersionUID = 1L;

    private final BlockingQueue<E> _impl;

    public ConcurrentLinkedBlockingQueue()
    {
        _impl = new Unbounded<E>();
    }

    public ConcurrentLinkedBlockingQueue(Collection<? extends E> c)
    {
        _impl = new Unbounded<E>(c);
    }

    public ConcurrentLinkedBlockingQueue(int capacity)
    {
        _impl = new Bounded<E>(capacity);
    }

    public ConcurrentLinkedBlockingQueue(int capacity, Collection<? extends E> c)
    {
        _impl = new Bounded<E>(capacity, c);
    }

    @Override
    public Iterator<E> iterator()
    {
        return _impl.iterator();
    }

    @Override
    public int size()
    {
        return _impl.size();
    }

    @Override
    public boolean offer(E e)
    {
        return _impl.offer(e);
    }

    @Override
    public E peek()
    {
        return _impl.peek();
    }

    @Override
    public E poll()
    {
        return _impl.poll();
    }

    @Override
    public E take() throws InterruptedException
    {
        return _impl.take();
    }

    @Override
    public E poll(final long timeout, final TimeUnit unit) throws InterruptedException
    {
        return _impl.poll(timeout, unit);
    }

    @Override
    public void put(E e) throws InterruptedException
    {
        _impl.offer(e);
    }

    @Override
    public boolean offer(E e, long timeout, TimeUnit unit) throws InterruptedException
    {
        return _impl.offer(e);
    }

    @Override
    public int remainingCapacity()
    {
        return _impl.remainingCapacity();
    }

    @Override
    public int drainTo(Collection<? super E> c)
    {
        return _impl.drainTo(c);
    }

    @Override
    public int drainTo(Collection<? super E> c, int maxElements)
    {
        return _impl.drainTo(c, maxElements);
    }

    protected static class ThreadMarker
    {
        final Thread thread;
        // assumed parked until found otherwise.
        volatile boolean parked = true;

        ThreadMarker(Thread t)
        {
            thread = t;
        }
    }

    protected static class Unbounded<E> extends AbstractQueue<E> implements BlockingQueue<E>
    {
        protected final ConcurrentLinkedQueue<ThreadMarker> _parkq;
        protected final ConcurrentLinkedQueue<E> _q;

        public Unbounded()
        {
            _parkq = new ConcurrentLinkedQueue<ThreadMarker>();
            _q = new ConcurrentLinkedQueue<E>();
        }

        public Unbounded(Collection<? extends E> c)
        {
            _parkq = new ConcurrentLinkedQueue<ThreadMarker>();
            _q = new ConcurrentLinkedQueue<E>(c);
        }

        @Override
        public Iterator<E> iterator()
        {
            return _q.iterator();
        }

        @Override
        public int size()
        {
            return _q.size();
        }

        @Override
        public boolean offer(E e)
        {
            _q.offer(e);

            for (;;)
            {
                ThreadMarker marker = _parkq.poll();

                if (marker == null)
                {
                    return true;
                }

                if (marker.parked)
                {
                    LockSupport.unpark(marker.thread);
                    return true;
                }
            }
        }

        @Override
        public E peek()
        {
            return _q.peek();
        }

        @Override
        public E poll()
        {
            return _q.poll();
        }

        @Override
        public E take() throws InterruptedException
        {
            for (;;)
            {
                E e = _q.poll();

                if (e != null)
                {
                    return e;
                }

                ThreadMarker m = new ThreadMarker(Thread.currentThread());

                if (Thread.interrupted())
                {
                    // avoid the parkq.offer(m) if already interrupted
                    throw new InterruptedException();
                }

                _parkq.offer(m);
                // check again in case there is data race
                e = _q.poll();

                if (e != null)
                {
                    // data race indeed
                    m.parked = false;
                    return e;
                }

                LockSupport.park();
                m.parked = false;

                if (Thread.interrupted())
                {
                    throw new InterruptedException();
                }
            }
        }

        @Override
        public E poll(final long timeout, final TimeUnit unit) throws InterruptedException
        {
            if (timeout < 0)
            {
                // treat negative timeout same as to wait forever
                return take();
            }

            final long t1 = System.nanoTime() + unit.toNanos(timeout);

            for (;;)
            {
                E e = _q.poll();

                if (e != null)
                {
                    return e;
                }

                final long duration = t1 - System.nanoTime();

                if (duration <= 0)
                {
                    return null; // time out
                }

                ThreadMarker m = new ThreadMarker(Thread.currentThread());

                if (Thread.interrupted())
                {
                    // avoid the parkq.offer(m) if already interrupted
                    throw new InterruptedException();
                }

                _parkq.offer(m);
                // check again in case there is data race
                e = _q.poll();

                if (e != null)
                {
                    // data race indeed
                    m.parked = false;
                    return e;
                }

                LockSupport.parkNanos(duration);
                m.parked = false;

                if (Thread.interrupted())
                {
                    throw new InterruptedException();
                }
            }
        }

        @Override
        public void put(E e) throws InterruptedException
        {
            offer(e);
        }

        @Override
        public boolean offer(E e, long timeout, TimeUnit unit) throws InterruptedException
        {
            return offer(e);
        }

        @Override
        public int remainingCapacity()
        {
            return Integer.MAX_VALUE;
        }

        @Override
        public int drainTo(Collection<? super E> c)
        {
            int i = 0;
            E e;

            for (; (e = _q.poll()) != null; i++)
            {
                c.add(e);
            }

            return i;
        }

        @Override
        public int drainTo(Collection<? super E> c, int maxElements)
        {
            int i = 0;
            E e;

            for (; i < maxElements && (e = _q.poll()) != null; i++)
            {
                c.add(e);
            }

            return i;
        }

    }

    protected static class Bounded<E> extends Unbounded<E>
    {
        private final AtomicInteger _capacity;
        private final ConcurrentLinkedQueue<ThreadMarker> _putparkq;

        public Bounded(int capacity)
        {
            if (capacity <= 0)
            {
                throw new IllegalArgumentException("capacity must be > 0");
            }

            _capacity = new AtomicInteger(capacity);
            _putparkq = new ConcurrentLinkedQueue<ThreadMarker>();
        }

        public Bounded(int capacity, Collection<? extends E> c)
        {
            this(capacity);

            if (capacity < c.size())
            {
                throw new IllegalArgumentException("capacity must be > collection");
            }

            for (E e : c)
            {
                add(e);
            }
        }

        @Override
        public boolean offer(E e)
        {
            if (tryDecrementCapacity())
            {
                return super.offer(e);
            }

            return false;
        }

        private boolean tryDecrementCapacity()
        {
            int capacity;

            do
            {
                capacity = _capacity.get();

                if (capacity == 0)
                {
                    return false;
                }
            }
            while (!_capacity.compareAndSet(capacity, capacity - 1));

            return true;
        }

        @Override
        public E poll()
        {
            E e = super.poll();

            if (e != null)
            {
                _capacity.incrementAndGet();
                unparkIfAny();
            }

            return e;
        }

        @Override
        public E take() throws InterruptedException
        {
            E e = super.take();
            _capacity.incrementAndGet();
            unparkIfAny();
            return e;
        }

        private void unparkIfAny()
        {
            for (;;)
            {
                ThreadMarker marker = _putparkq.poll();

                if (marker == null)
                {
                    return;
                }

                if (marker.parked)
                {
                    LockSupport.unpark(marker.thread);
                    return;
                }
            }
        }

        @Override
        public E poll(final long timeout, final TimeUnit unit) throws InterruptedException
        {
            E e = super.poll(timeout, unit);

            if (e != null)
            {
                _capacity.incrementAndGet();
                unparkIfAny();
            }

            return e;
        }

        @Override
        public void put(E e) throws InterruptedException
        {
            for (;;)
            {
                if (tryDecrementCapacity())
                {
                    super.put(e);
                    return;
                }

                ThreadMarker m = new ThreadMarker(Thread.currentThread());

                if (Thread.interrupted())
                {
                    // avoid the putparkq.offer(m) if already interrupted
                    throw new InterruptedException();
                }

                _putparkq.offer(m);

                // check again in case there is data race
                if (tryDecrementCapacity())
                {
                    // data race indeed
                    m.parked = false;
                    super.put(e);
                    return;
                }

                LockSupport.park();
                m.parked = false;

                if (Thread.interrupted())
                {
                    throw new InterruptedException();
                }
            }
        }

        @Override
        public boolean offer(E e, long timeout, TimeUnit unit) throws InterruptedException
        {
            if (timeout < 0)
            {
                // treat negative timeout same as to wait forever
                put(e);
                return true;
            }

            final long t1 = System.nanoTime() + unit.toNanos(timeout);

            for (;;)
            {
                if (tryDecrementCapacity())
                {
                    return super.offer(e, timeout, unit);
                }

                final long duration = t1 - System.nanoTime();

                if (duration <= 0)
                {
                    return false; // time out
                }

                ThreadMarker m = new ThreadMarker(Thread.currentThread());

                if (Thread.interrupted())
                {
                    // avoid the putparkq.offer(m) if already interrupted
                    throw new InterruptedException();
                }

                _putparkq.offer(m);
                // check again in case there is data race
                if (tryDecrementCapacity())
                { // data race indeed
                    m.parked = false;
                    super.offer(e);
                    return true;
                }

                LockSupport.parkNanos(duration);
                m.parked = false;

                if (Thread.interrupted())
                {
                    throw new InterruptedException();
                }
            }
        }

        @Override
        public int remainingCapacity()
        {
            return _capacity.get();
        }

        @Override
        public int drainTo(Collection<? super E> c)
        {
            int i = 0;
            E e;

            for (; (e = poll()) != null; i++)
            {
                c.add(e);
            }

            return i;
        }

        @Override
        public int drainTo(Collection<? super E> c, int maxElements)
        {
            int i = 0;
            E e;

            for (; i < maxElements && (e = poll()) != null; i++)
            {
                c.add(e);
            }

            return i;
        }

    }

}
