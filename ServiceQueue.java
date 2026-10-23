import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.PriorityQueue;

/**
 * Queue for one service (e.g. Billing). Higher priority tokens go first;
 * within the same priority it is strictly first-come-first-served.
 * Keeps a running average of real service time to estimate waiting time.
 */
public class ServiceQueue {

    private static final double DEFAULT_AVG_SECONDS = 180;  // assume 3 min until we learn better
    private static final double MIN_AVG_SECONDS = 60;       // floor so demo runs give sensible estimates
    private static final double SMOOTHING = 0.3;            // weight given to the newest service time

    private final String name;
    private final String prefix;
    private final PriorityQueue<Token> waiting = new PriorityQueue<>(
            Comparator.comparingInt((Token t) -> t.getPriority().getRank()).reversed()
                    .thenComparingInt(Token::getSeq));

    private int nextSeq = 1;
    private double avgServiceSeconds = DEFAULT_AVG_SECONDS;
    private int servedCount = 0;

    public ServiceQueue(String name, String prefix) {
        this.name = name;
        this.prefix = prefix;
    }

    public String getName() { return name; }
    public String getPrefix() { return prefix; }
    public int getServedCount() { return servedCount; }
    public int getWaitingCount() { return waiting.size(); }
    public double getAvgServiceSeconds() { return avgServiceSeconds; }

    public Token issue(String customerName, Token.Priority priority) {
        int seq = nextSeq++;
        Token token = new Token(String.format("%s-%03d", prefix, seq), seq, name, customerName, priority);
        waiting.add(token);
        return token;
    }

    /** Removes and returns the next token to serve, or null if nobody is waiting. */
    public Token callNext() {
        Token token = waiting.poll();
        if (token != null) {
            token.markCalled();
        }
        return token;
    }

    public boolean cancel(Token token) {
        return waiting.remove(token);
    }

    /** 1-based position in line, or 0 if the token is not waiting. */
    public int position(Token token) {
        return waitingInOrder().indexOf(token) + 1;
    }

    public List<Token> waitingInOrder() {
        List<Token> sorted = new ArrayList<>(waiting);
        sorted.sort(waiting.comparator());
        return sorted;
    }

    /** Estimated seconds until this token is called, assuming a single counter. */
    public double estimatedWaitSeconds(Token token) {
        return position(token) * avgServiceSeconds;
    }

    /** Learn from a completed service so future estimates get more accurate. */
    public void recordServed(Token token) {
        servedCount++;
        double actual = Math.max(MIN_AVG_SECONDS, token.serviceSeconds());
        avgServiceSeconds = Math.max(MIN_AVG_SECONDS,
                (1 - SMOOTHING) * avgServiceSeconds + SMOOTHING * actual);
    }
}
