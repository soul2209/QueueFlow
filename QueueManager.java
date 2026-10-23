import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Central brain of QueueFlow: owns services, counters and tokens, and enforces the rules. */
public class QueueManager {

    private static final String LOG_FILE = "queueflow_log.csv";

    private final Map<String, ServiceQueue> services = new LinkedHashMap<>();
    private final Map<Integer, Counter> counters = new LinkedHashMap<>();
    private final Map<String, Token> tokens = new LinkedHashMap<>();
    private int nextCounterId = 1;

    // ---------- Admin ----------

    public void addService(String name, String prefix) throws QueueFlowException {
        if (name == null || name.trim().isEmpty() || prefix == null || prefix.trim().isEmpty()) {
            throw new QueueFlowException("Service name and prefix cannot be empty.");
        }
        String key = name.trim().toLowerCase();
        if (services.containsKey(key)) {
            throw new QueueFlowException("Service '" + name.trim() + "' already exists.");
        }
        String cleanPrefix = prefix.trim().toUpperCase();
        for (ServiceQueue s : services.values()) {
            if (s.getPrefix().equals(cleanPrefix)) {
                throw new QueueFlowException("Prefix '" + cleanPrefix + "' is already used by " + s.getName() + ".");
            }
        }
        services.put(key, new ServiceQueue(name.trim(), cleanPrefix));
    }

    public Counter addCounter(String serviceName) throws QueueFlowException {
        ServiceQueue service = getService(serviceName);
        Counter counter = new Counter(nextCounterId++, service);
        counters.put(counter.getId(), counter);
        return counter;
    }

    // ---------- Customer ----------

    public Token issueToken(String serviceName, String customerName, Token.Priority priority)
            throws QueueFlowException {
        ServiceQueue service = getService(serviceName);
        if (customerName == null || customerName.trim().isEmpty()) {
            throw new QueueFlowException("Customer name cannot be empty.");
        }
        if (countersFor(service) == 0) {
            throw new QueueFlowException("No counter is open for " + service.getName() + " right now.");
        }
        Token token = service.issue(customerName.trim(), priority);
        tokens.put(token.getId(), token);
        return token;
    }

    public void cancelToken(String tokenId) throws QueueFlowException {
        Token token = getToken(tokenId);
        if (token.getStatus() != Token.Status.WAITING) {
            throw new QueueFlowException("Token " + token.getId() + " is " + token.getStatus() + " and cannot be cancelled.");
        }
        getService(token.getService()).cancel(token);
        token.markCancelled();
        log(token, "CANCELLED", 0);
    }

    /** Human-readable status line with position and estimated wait. */
    public String describe(String tokenId) throws QueueFlowException {
        Token token = getToken(tokenId);
        StringBuilder sb = new StringBuilder(token.toString());
        if (token.getStatus() == Token.Status.WAITING) {
            ServiceQueue service = getService(token.getService());
            int position = service.position(token);
            long minutes = estimatedWaitMinutes(service, token);
            sb.append(String.format("%nPosition in line: %d | Estimated wait: ~%d min", position, minutes));
        }
        return sb.toString();
    }

    public long estimatedWaitMinutes(ServiceQueue service, Token token) {
        double seconds = service.estimatedWaitSeconds(token) / Math.max(1, countersFor(service));
        return (long) Math.ceil(seconds / 60.0);
    }

    // ---------- Staff ----------

    public Token callNext(int counterId) throws QueueFlowException {
        Counter counter = getCounter(counterId);
        if (counter.isBusy()) {
            throw new QueueFlowException("Counter " + counterId + " is still serving " + counter.getCurrent().getId()
                    + ". Complete or skip it first.");
        }
        Token token = counter.getService().callNext();
        if (token == null) {
            throw new QueueFlowException("Nobody is waiting for " + counter.getService().getName() + ".");
        }
        counter.setCurrent(token);
        return token;
    }

    public Token completeCurrent(int counterId) throws QueueFlowException {
        Counter counter = getCounter(counterId);
        Token token = requireCurrent(counter);
        token.markServed();
        counter.getService().recordServed(token);
        counter.setCurrent(null);
        log(token, "SERVED", token.serviceSeconds());
        return token;
    }

    /** Use when the called customer does not show up (no-show). */
    public Token skipCurrent(int counterId) throws QueueFlowException {
        Counter counter = getCounter(counterId);
        Token token = requireCurrent(counter);
        token.markSkipped();
        counter.setCurrent(null);
        log(token, "SKIPPED", 0);
        return token;
    }

    // ---------- Views ----------

    public List<Token> waitingList(String serviceName) throws QueueFlowException {
        return getService(serviceName).waitingInOrder();
    }

    public List<ServiceQueue> getServices() {
        return new ArrayList<>(services.values());
    }

    public List<Counter> getCounters() {
        return new ArrayList<>(counters.values());
    }

    public String stats() {
        int served = 0, skipped = 0, cancelled = 0;
        for (Token t : tokens.values()) {
            switch (t.getStatus()) {
                case SERVED: served++; break;
                case SKIPPED: skipped++; break;
                case CANCELLED: cancelled++; break;
                default: break;
            }
        }
        StringBuilder sb = new StringBuilder();
        sb.append(String.format("Tokens issued: %d | Served: %d | Skipped: %d | Cancelled: %d%n",
                tokens.size(), served, skipped, cancelled));
        for (ServiceQueue s : services.values()) {
            sb.append(String.format("  %-14s waiting: %-3d served: %-3d avg service: %.0f sec | counters: %d%n",
                    s.getName(), s.getWaitingCount(), s.getServedCount(), s.getAvgServiceSeconds(), countersFor(s)));
        }
        return sb.toString();
    }

    // ---------- Helpers ----------

    private ServiceQueue getService(String name) throws QueueFlowException {
        ServiceQueue service = name == null ? null : services.get(name.trim().toLowerCase());
        if (service == null) {
            throw new QueueFlowException("Unknown service: " + name);
        }
        return service;
    }

    private Counter getCounter(int id) throws QueueFlowException {
        Counter counter = counters.get(id);
        if (counter == null) {
            throw new QueueFlowException("Unknown counter: " + id);
        }
        return counter;
    }

    private Token getToken(String id) throws QueueFlowException {
        Token token = id == null ? null : tokens.get(id.trim().toUpperCase());
        if (token == null) {
            throw new QueueFlowException("Token not found: " + id);
        }
        return token;
    }

    private Token requireCurrent(Counter counter) throws QueueFlowException {
        if (!counter.isBusy()) {
            throw new QueueFlowException("Counter " + counter.getId() + " is not serving anyone.");
        }
        return counter.getCurrent();
    }

    private int countersFor(ServiceQueue service) {
        int count = 0;
        for (Counter c : counters.values()) {
            if (c.getService() == service) {
                count++;
            }
        }
        return count;
    }

    /** Appends one line per finished token to a CSV file (simple persistence / audit trail). */
    private void log(Token token, String outcome, long serviceSeconds) {
        try (PrintWriter out = new PrintWriter(new FileWriter(LOG_FILE, true))) {
            out.printf("%s,%s,%s,%s,%s,%d%n", LocalDateTime.now(), token.getId(),
                    token.getCustomerName(), token.getPriority(), outcome, serviceSeconds);
        } catch (IOException e) {
            System.out.println("(Warning: could not write log file: " + e.getMessage() + ")");
        }
    }
}
