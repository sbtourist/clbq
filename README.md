clbq aka ConcurrentLinkedBlocingQueue
=====================================

ConcurrentLinkedBlockingQueue is an experimental BlockingQueue implementation built on
top of ConcurrentLinkedQueue for scenario that require low-latency handoff, at the cost
of increased overhead when consumers are idle.
 