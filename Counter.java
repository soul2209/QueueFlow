/** A service desk/counter that serves one token at a time from one ServiceQueue. */
public class Counter {

    private final int id;
    private final ServiceQueue service;
    private Token current;

    public Counter(int id, ServiceQueue service) {
        this.id = id;
        this.service = service;
    }

    public int getId() { return id; }
    public ServiceQueue getService() { return service; }
    public Token getCurrent() { return current; }
    public void setCurrent(Token current) { this.current = current; }
    public boolean isBusy() { return current != null; }
}
