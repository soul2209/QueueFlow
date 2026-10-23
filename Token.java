import java.time.Duration;
import java.time.LocalDateTime;

/** A single virtual queue token issued to a customer. */
public class Token {

    public enum Priority {
        NORMAL(0), SENIOR(1), EMERGENCY(2);

        private final int rank;

        Priority(int rank) {
            this.rank = rank;
        }

        public int getRank() {
            return rank;
        }
    }

    public enum Status { WAITING, SERVING, SERVED, SKIPPED, CANCELLED }

    private final String id;
    private final int seq;
    private final String service;
    private final String customerName;
    private final Priority priority;
    private Status status = Status.WAITING;
    private final LocalDateTime issuedAt = LocalDateTime.now();
    private LocalDateTime calledAt;
    private LocalDateTime completedAt;

    public Token(String id, int seq, String service, String customerName, Priority priority) {
        this.id = id;
        this.seq = seq;
        this.service = service;
        this.customerName = customerName;
        this.priority = priority;
    }

    public String getId() { return id; }
    public int getSeq() { return seq; }
    public String getService() { return service; }
    public String getCustomerName() { return customerName; }
    public Priority getPriority() { return priority; }
    public Status getStatus() { return status; }
    public LocalDateTime getIssuedAt() { return issuedAt; }

    public void markCalled() {
        status = Status.SERVING;
        calledAt = LocalDateTime.now();
    }

    public void markServed() {
        status = Status.SERVED;
        completedAt = LocalDateTime.now();
    }

    public void markSkipped() {
        status = Status.SKIPPED;
        completedAt = LocalDateTime.now();
    }

    public void markCancelled() {
        status = Status.CANCELLED;
        completedAt = LocalDateTime.now();
    }

    /** Seconds between being called and being completed (0 if not applicable). */
    public long serviceSeconds() {
        if (calledAt == null || completedAt == null) {
            return 0;
        }
        return Duration.between(calledAt, completedAt).getSeconds();
    }

    @Override
    public String toString() {
        return String.format("%s | %s | %s | %s | %s", id, customerName, service, priority, status);
    }
}
