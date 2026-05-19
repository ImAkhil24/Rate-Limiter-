import java.time.Duration;
import java.time.Instant;
import java.util.LinkedList;
import java.util.Queue;


// atmost 3 top request in 5 minutes
// when new otp request comes, so check whether in the time window of current time - 5 minutes
// do we have more than 3 otp request
// if we do then reject else accept and add

class Request {
    String userId;
    Instant time;

    Request(String userId, Instant time) {
        this.userId = userId;
        this.time = time;
    }
}

public class RateLimiter {
    // maintain the 5 minutes time frame
    Queue<Request> requestQueue= new LinkedList<>();

    void purgeRequests(Instant now) {
        while(!requestQueue.isEmpty()) {
            if(Duration.between(requestQueue.peek().time, now).toMinutes() >= 5) {
                requestQueue.poll();
            } else {
                break;
            }
        }
    }

    boolean allowRequest(String userId) {
        Instant now = Instant.now();
        System.out.println(now);
        purgeRequests(now);
        int counter = 0;
        for(Request req: requestQueue) {
            if(req.userId.equals(userId)) counter++;
        }
        System.out.println(counter);
        if(counter >= 3) return false;
        requestQueue.add(new Request(userId, now));
        return true;
    }

    public static void main(String[] args) {
        RateLimiter rate = new RateLimiter();
        System.out.println(rate.allowRequest("akhil"));
        System.out.println(rate.allowRequest("akhil"));
        System.out.println(rate.allowRequest("akhil"));
        System.out.println(rate.allowRequest("akhil"));
    }
}