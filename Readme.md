# Concurrent Sliding Window Rate Limiter

## Overview

This project is a concurrent in-memory sliding window rate limiter built in Java.

The system simulates a backend OTP rate limiting service where:

* each user can request at most 3 OTPs
* within a 5-minute window
* under concurrent traffic conditions

The project was intentionally evolved step-by-step to understand:

* backend scalability bottlenecks
* shared mutable state
* race conditions
* synchronization
* lock granularity
* concurrent collections
* worker thread pools

Rather than directly building the final implementation, the project evolved similarly to how real backend systems evolve in production.

---

# Problem Statement

Banking systems receive massive OTP traffic during login, signup, and transaction verification flows.

Without proper rate limiting:

* users can spam OTP endpoints
* SMS costs increase
* backend systems become overloaded
* attackers can abuse APIs

Goal:

```text
Allow at most 3 OTP requests per user within 5 minutes.
```

---

# Evolution of the System

## V1 — Naive Global Queue

### Initial Design

The first implementation maintained:

```text
One global queue containing requests from all users.
```

Every incoming request:

1. purged expired requests
2. iterated entire queue
3. counted requests for current user
4. decided allow/reject

### Problems

At larger scale:

```text
50 million OTP requests/day
```

the design became inefficient.

Issues:

* every request scanned global data
* lookup cost increased with traffic
* unrelated users affected each other
* cleanup overhead increased significantly

### Key Learning

A globally shared data structure becomes a scalability bottleneck.

---

## V2 — Per-User Queues

### Improvement

Instead of one global queue:

```text
userId -> Queue<Request>
```

Each user maintained independent request history.

### Benefits

* isolated user state
* faster lookups
* localized cleanup
* improved scalability

### Key Learning

Partitioning state by ownership boundary improves scalability.

In this project:

```text
ownership boundary = userId
```

---

## V3 — Race Conditions Under Concurrency

### Problem

When multiple threads accessed:

```java
allowRequest()
```

simultaneously, race conditions appeared.

Example:

```text
Thread A reads count = 2
Thread B reads count = 2
Both insert requests
Final count = 4
```

Rate limiting rule broke.

### Why?

The workflow:

```text
purge -> check -> insert
```

was not atomic.

Even though individual statements executed correctly, the overall business invariant was violated.

### Key Learning

Thread-safe operations do not automatically create thread-safe business logic.

Concurrency bugs usually occur in:

```text
check-then-act workflows
```

---

## V4 — Synchronization

### Solution

Critical section protected using:

```java
synchronized
```

This ensured:

```text
Only one thread could execute
purge -> check -> insert
at a time.
```

### Benefits

* race conditions removed
* invariant preserved
* correctness guaranteed

### Problem Introduced

Synchronization was too coarse-grained.

Example:

```text
Akhil request blocks Rahul request
```

even though users are unrelated.

### Key Learning

Correctness alone is insufficient.

Lock granularity strongly affects scalability.

---

## V5 — Per-User Synchronization

### Improvement

Architecture evolved into:

```text
ConcurrentHashMap
    userId
        -> RateLimiter
```

Each user receives:

* independent RateLimiter object
* independent queue
* independent synchronization lock

### Benefits

* Akhil traffic no longer blocks Rahul traffic
* contention partitioned by user
* concurrency improved significantly

### Key Learning

Synchronization should protect:

```text
ownership boundaries
```

not entire applications.

---

## V6 — ConcurrentHashMap

### Problem

Even user-state creation became concurrency-sensitive.

Example:

```text
Two threads creating limiter for same user simultaneously
```

### Solution

Used:

```java
ConcurrentHashMap.computeIfAbsent()
```

for atomic user-state initialization.

### Key Learning

Thread-safe containers solve:

```text
data structure concurrency
```

but NOT:

```text
higher-level business atomicity
```

Both are separate concerns.

---

## V7 — Thread Pool Based Request Processing

### Problem

Initial concurrent simulation used:

```java
new Thread(...)
```

for every request.

Under heavy traffic:

* thread creation becomes expensive
* stack memory usage increases
* context switching overhead increases
* scheduler overhead increases

### Solution

Migrated to:

```java
ExecutorService
```

with reusable worker threads.

Architecture:

```text
Incoming tasks
    -> Task Queue
    -> Reusable Worker Threads
```

### Benefits

* reduced thread creation overhead
* reusable workers
* more realistic backend server model
* improved scalability

### Key Learning

Modern backend systems think in:

```text
tasks/work
```

rather than manually managing thread lifecycles.

---

# Final Architecture

```text
Incoming Request
        ↓
ExecutorService Thread Pool
        ↓
RateLimiterManager
        ↓
ConcurrentHashMap<userId, RateLimiter>
        ↓
Per-user synchronized queue
```

---

# Core Concepts Learned

## Concurrency

* threads share heap memory
* threads have independent stacks
* race conditions
* interleaving
* atomicity
* synchronization
* monitor locks

## Backend Scalability

* lock contention
* coarse vs fine-grained locking
* partitioning by ownership boundary
* worker thread reuse
* task-based processing

## Java Concurrency APIs

* synchronized
* Thread
* ExecutorService
* Future
* ConcurrentHashMap
* computeIfAbsent

---

# Future Improvements

Possible future extensions:

* Redis-backed distributed rate limiter
* Token bucket algorithm
* Configurable rate limits
* Background cleanup workers
* Metrics and monitoring
* REST API integration

---

# Conclusion

This project evolved from a simple queue-based implementation into a scalable concurrent backend component.

The primary goal was not just implementing rate limiting, but understanding how real backend systems evolve under:

* increasing traffic
* concurrency
* scalability bottlenecks
* synchronization challenges

The project provided hands-on understanding of practical backend concurrency and systems design concepts in Java.
