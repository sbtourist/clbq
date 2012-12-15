clbq aka ConcurrentLinkedBlockingQueue
======================================

ConcurrentLinkedBlockingQueue is an experimental BlockingQueue implementation built on
top of ConcurrentLinkedQueue for scenario that require low-latency handoff, at the cost
of increased overhead when consumers are idle.

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
more consumers are added

IDEAS
=====

As stated above this is an experiment in providing a lower-latency alternative to LBQ for
certain scenarios. The impact of "user-level" (aka nonnative) blocking/signaling might
still be reduced further, so if anybody wants to explore these ideas please feel free:

* There is currently no intentional avoidance of false sharing; both the internal CLQ
and the idle-marker queue might be contended.

* I'm looking for reduced allocation of ThreadMarkers. One possible way might be to use
ThreadLocals and merely flip a signal.

Patches & further thoughts welcome!


LICENSE
=======

Written by Hanson Char and released to the public domain, as explained at
<a href=http://creativecommons.org/licenses/publicdomain>CreativeCommons</a>
