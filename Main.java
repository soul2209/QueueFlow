import java.util.List;
import java.util.Scanner;

/** Console UI for QueueFlow. */
public class Main {

    private static final Scanner in = new Scanner(System.in);
    private static final QueueManager manager = new QueueManager();

    public static void main(String[] args) {
        seedDemoData();
        System.out.println("=== QueueFlow: Smart Virtual Queue & Token Management ===");

        boolean running = true;
        while (running) {
            printMenu();
            int choice = readInt("Choose: ");
            try {
                switch (choice) {
                    case 1: issueToken(); break;
                    case 2: checkToken(); break;
                    case 3: cancelToken(); break;
                    case 4: callNext(); break;
                    case 5: complete(); break;
                    case 6: skip(); break;
                    case 7: viewQueue(); break;
                    case 8: addService(); break;
                    case 9: addCounter(); break;
                    case 10: System.out.println(manager.stats()); break;
                    case 0: running = false; break;
                    default: System.out.println("Invalid option, try again.");
                }
            } catch (QueueFlowException e) {
                System.out.println("! " + e.getMessage());
            }
        }
        System.out.println("Goodbye!");
    }

    private static void printMenu() {
        System.out.println("\n--- CUSTOMER ---");
        System.out.println(" 1. Get a token      2. Check token status      3. Cancel token");
        System.out.println("--- STAFF ---");
        System.out.println(" 4. Call next        5. Complete current        6. Skip current (no-show)");
        System.out.println(" 7. View queue");
        System.out.println("--- ADMIN ---");
        System.out.println(" 8. Add service      9. Add counter            10. Stats");
        System.out.println(" 0. Exit");
    }

    // ---------- Actions ----------

    private static void issueToken() throws QueueFlowException {
        printServices();
        String service = readLine("Service name: ");
        String name = readLine("Your name: ");
        System.out.println("Priority: 1 = Normal, 2 = Senior citizen, 3 = Emergency");
        int p = readInt("Choose priority: ");
        Token.Priority priority;
        switch (p) {
            case 2: priority = Token.Priority.SENIOR; break;
            case 3: priority = Token.Priority.EMERGENCY; break;
            default: priority = Token.Priority.NORMAL;
        }
        Token token = manager.issueToken(service, name, priority);
        System.out.println("Your token: " + token.getId());
        System.out.println(manager.describe(token.getId()));
    }

    private static void checkToken() throws QueueFlowException {
        System.out.println(manager.describe(readLine("Token id: ")));
    }

    private static void cancelToken() throws QueueFlowException {
        String id = readLine("Token id to cancel: ");
        manager.cancelToken(id);
        System.out.println("Token " + id.toUpperCase() + " cancelled.");
    }

    private static void callNext() throws QueueFlowException {
        printCounters();
        Token t = manager.callNext(readInt("Counter id: "));
        System.out.println(">>> Now serving " + t.getId() + " (" + t.getCustomerName() + ", " + t.getPriority() + ")");
    }

    private static void complete() throws QueueFlowException {
        Token t = manager.completeCurrent(readInt("Counter id: "));
        System.out.println("Completed " + t.getId() + ".");
    }

    private static void skip() throws QueueFlowException {
        Token t = manager.skipCurrent(readInt("Counter id: "));
        System.out.println("Skipped " + t.getId() + " (no-show).");
    }

    private static void viewQueue() throws QueueFlowException {
        printServices();
        List<Token> list = manager.waitingList(readLine("Service name: "));
        if (list.isEmpty()) {
            System.out.println("Queue is empty.");
            return;
        }
        int pos = 1;
        for (Token t : list) {
            System.out.printf("%d. %s%n", pos++, t);
        }
    }

    private static void addService() throws QueueFlowException {
        String name = readLine("New service name: ");
        String prefix = readLine("Token prefix (e.g. B): ");
        manager.addService(name, prefix);
        System.out.println("Service added. Remember to add a counter for it (option 9).");
    }

    private static void addCounter() throws QueueFlowException {
        printServices();
        Counter c = manager.addCounter(readLine("Service for new counter: "));
        System.out.println("Counter " + c.getId() + " opened for " + c.getService().getName() + ".");
    }

    // ---------- Helpers ----------

    private static void printServices() {
        StringBuilder sb = new StringBuilder("Services: ");
        for (ServiceQueue s : manager.getServices()) {
            sb.append(s.getName()).append(" (").append(s.getWaitingCount()).append(" waiting)  ");
        }
        System.out.println(sb);
    }

    private static void printCounters() {
        for (Counter c : manager.getCounters()) {
            String state = c.isBusy() ? "serving " + c.getCurrent().getId() : "free";
            System.out.printf("Counter %d [%s] - %s%n", c.getId(), c.getService().getName(), state);
        }
    }

    private static String readLine(String prompt) {
        System.out.print(prompt);
        return in.hasNextLine() ? in.nextLine().trim() : "";
    }

    private static int readInt(String prompt) {
        while (true) {
            if (!in.hasNextLine()) {
                return 0; // input closed: behave like Exit
            }
            System.out.print(prompt);
            String line = in.nextLine().trim();
            try {
                return Integer.parseInt(line);
            } catch (NumberFormatException e) {
                System.out.println("Please enter a number.");
            }
        }
    }

    private static void seedDemoData() {
        try {
            manager.addService("Billing", "B");
            manager.addService("Enquiry", "E");
            manager.addService("Registration", "R");
            manager.addCounter("Billing");
            manager.addCounter("Billing");
            manager.addCounter("Enquiry");
            manager.addCounter("Registration");
        } catch (QueueFlowException e) {
            throw new IllegalStateException(e);
        }
    }
}
