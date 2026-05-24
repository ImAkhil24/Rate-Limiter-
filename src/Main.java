import java.time.Duration;
import java.time.Instant;
import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;

class Request {

    Instant time;

    Request(Instant time) {
        this.time = time;
    }
}

class RateLimiterManager {

    ConcurrentHashMap<String, RateLimiter> rateLimiter =
            new ConcurrentHashMap<>();

    RateLimiter getRateLimiter(String userId) {

        return rateLimiter.computeIfAbsent(
                userId,
                k -> new RateLimiter()
        );
    }
}

class RateLimiter {

    Queue<Request> requestQueue =
            new LinkedList<>();

    void purgeRequests(Instant now) {

        while (!requestQueue.isEmpty()) {

            if (Duration.between(
                    requestQueue.peek().time,
                    now
            ).toMinutes() >= 5) {

                requestQueue.poll();

            } else {
                break;
            }
        }
    }

    synchronized boolean allowRequest() {

        Instant now = Instant.now();

        purgeRequests(now);

        int counter = requestQueue.size();

        System.out.println(
                Thread.currentThread().getName()
                        + " -> current count: "
                        + counter
        );

        if (counter >= 3) {
            return false;
        }

        requestQueue.add(
                new Request(now)
        );

        return true;
    }
}

public class Main {

    public static void main(String[] args) {

        RateLimiterManager manager =
                new RateLimiterManager();

        System.out.println(
                manager.getRateLimiter("akhil")
                        .allowRequest()
        );

        System.out.println(
                manager.getRateLimiter("akhil")
                        .allowRequest()
        );

        Thread t1 = new Thread(() -> {

            System.out.println(
                    manager.getRateLimiter("akhil")
                            .allowRequest()
            );

        }, "Thread-1");

        Thread t2 = new Thread(() -> {

            System.out.println(
                    manager.getRateLimiter("akhil")
                            .allowRequest()
            );

        }, "Thread-2");

        t1.start();
        t2.start();
    }
}