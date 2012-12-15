clbq aka ConcurrentLinkedBlockingQueue
======================================

ConcurrentLinkedBlockingQueue is an experimental BlockingQueue implementation built on
top of ConcurrentLinkedQueue for scenarios that require low-latency handoff, at the cost
of increased overhead when consumers transition between active and blocked (waiting) states.
This overhead may or may not be amortized by the lower overall latency.

I am not the original author of this code; I merely saved it from the net some time ago
and merged the unbounded & bounded implementation into a single class.

USAGE
=====

* The main benchmark/test driver is "QueueTest", which in turn will run separate drivers
for different individual queue implementations. Simply run it without parameters and it
tries to do some rule-of-thumb thread pool/producer/consumer auto-sizing.

* Alternatively pass "numConsumers=x" and "numProducers=y" properties to see the
performance tradeoff. With more producers than (one or two) consumers the performance
should always be better than LinkedBlockingQueue; this takes a turn for the worse as
more consumers are added.

* The individual drivers can also be run on their own.

IDEAS
=====

As stated above this is an experiment in providing a lower-latency alternative to LBQ for
certain scenarios. The impact of "user-level" (aka nonnative) blocking/signaling might
still be reduced further, so if anybody wants to explore these ideas further, please
feel free:

* Consumer state transition (active -> potentially-blocking) currently implies an
- unfortunately necessary - second hit into the internal CLQ, which (as far as I can
tell) is responsible for the performance impact with multiple consumers, probably due to
excessive CAS. I'd be curious to hear about alternative approaches.

* I'm looking for reduced allocation of ThreadMarkers. One possible way might be to use
ThreadLocals and merely flip a signal, but I have not yet thought that through
(I generally dislike ThreadLocals unless I can als control any interacting threads).

* Instead of crapping on the console write to CSV so that the results can be more easily
graphed/compared.

Patches & further thoughts welcome!
