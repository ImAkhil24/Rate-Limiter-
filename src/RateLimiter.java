import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;


// atmost 3 top request in 5 minutes
// when new otp request comes, so check whether in the time window of current time - 5 minutes
// do we have more than 3 otp request
// if we do then reject else accept and add

// for V2 I can just make different queues based on the hash for each user, but it's bad
// just maintain the already existing count on the hash

class Request {
    String userId;
    Instant time;

    Request(String userId, Instant time) {
        this.userId = userId;
        this.time = time;
    }
}

class RateLimiterInit {
    ConcurrentHashMap<String, RateLimiter> rateLimiter= new ConcurrentHashMap<>();

    RateLimiter getRateLimiter(String userId) {
        RateLimiter limiter =
                rateLimiter.computeIfAbsent(
                        userId,
                        k -> new RateLimiter()
                );

        return limiter;
    }
}

public class RateLimiter {
    // maintain the 5 minutes time frame
    Map<String, Queue<Request>> requestQueue = new HashMap<>();

    void purgeRequests(Instant now, Queue<Request> requestQueue) {
        while(!requestQueue.isEmpty()) {
            if(Duration.between(requestQueue.peek().time, now).toMinutes() >= 5) {
                requestQueue.poll();
            } else {
                break;
            }
        }
    }

    synchronized boolean allowRequest(String userId) {
        Instant now = Instant.now();
        System.out.println(now);
        Queue<Request> userQueue = requestQueue.get(userId);
        if(userQueue != null) {
            purgeRequests(now, userQueue);
        } else {
            userQueue = new LinkedList<>();
        }

        // after that count is needed
        Integer counter = userQueue.size();
//        try {
//            Thread.sleep(5000);
//        } catch (InterruptedException e) {
//            e.printStackTrace();
//        }

        System.out.println(counter);
        if(counter >= 3) return false;
        userQueue.add(new Request(userId, now));
        requestQueue.put(userId, userQueue);
        return true;
    }

    public static void main(String[] args) {
        RateLimiterInit rateInit = new RateLimiterInit();

        System.out.println(rateInit.getRateLimiter("akhil").allowRequest("akhil"));
        System.out.println(rateInit.getRateLimiter("akhil").allowRequest("akhil"));

        Thread t1 = new Thread(() -> {
            System.out.println(rateInit.getRateLimiter("akhil").allowRequest("akhil"));
        });

        Thread t2 = new Thread(()-> {
            System.out.println(rateInit.getRateLimiter("akhil").allowRequest("akhil"));
        });

        t1.start();
        t2.start();


    }
}