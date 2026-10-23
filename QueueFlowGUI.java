import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;
import javax.swing.plaf.basic.BasicArrowButton;
import javax.swing.plaf.basic.BasicComboBoxUI;
import javax.swing.plaf.basic.BasicScrollBarUI;
import java.awt.*;
import java.awt.event.*;
import java.util.ArrayList;
import java.util.List;

/** Dark-themed Swing front end for QueueFlow. Uses the existing QueueManager unchanged. */
class QueueFlowGUI extends JFrame {

    // ---------- Theme ----------
    static final Color BG      = new Color(0x0F1115);
    static final Color SURFACE = new Color(0x161920);
    static final Color CARD    = new Color(0x1E2229);
    static final Color FIELD   = new Color(0x14171C);
    static final Color BORDER  = new Color(0x2C323B);
    static final Color TEXT    = new Color(0xE6EDF3);
    static final Color MUTED   = new Color(0x8B949E);
    static final Color ACCENT  = new Color(0x4C8DFF);
    static final Color GREEN   = new Color(0x2EA043);
    static final Color RED     = new Color(0xDA3633);
    static final Color AMBER   = new Color(0xD29922);

    static final String FONT = "Segoe UI";

    // ---------- Model ----------
    private final QueueManager manager = new QueueManager();
    private boolean updating = false;

    // ---------- Customer page ----------
    private final JComboBox<String> custService = styledCombo(new JComboBox<>());
    private final JTextField custName = field(16);
    private final JTextField tokenLookup = field(10);
    private final JTextArea ticket = new JTextArea();
    private final List<RoundButton> prioButtons = new ArrayList<>();
    private Token.Priority selectedPriority = Token.Priority.NORMAL;

    // ---------- Staff page ----------
    private final JComboBox<Counter> staffCounter = styledCombo(new JComboBox<>());
    private final JLabel nowToken = label("-", 40, Font.BOLD, ACCENT);
    private final JLabel nowInfo = label("No counter selected", 13, Font.PLAIN, MUTED);

    // ---------- Admin page ----------
    private final JTextField newServiceName = field(12);
    private final JTextField newServicePrefix = field(4);
    private final JComboBox<String> adminService = styledCombo(new JComboBox<>());
    private final JTextArea statsArea = new JTextArea();

    // ---------- Board ----------
    private final JComboBox<String> boardService = styledCombo(new JComboBox<>());
    private final DefaultTableModel tableModel = new DefaultTableModel(
            new String[]{"#", "Token", "Customer", "Priority"}, 0) {
        @Override public boolean isCellEditable(int r, int c) { return false; }
    };
    private final JTable table = new JTable(tableModel) {
        @Override protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            if (getRowCount() == 0) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
                g2.setColor(MUTED);
                g2.setFont(new Font(FONT, Font.PLAIN, 14));
                String msg = "Queue is empty";
                int w = g2.getFontMetrics().stringWidth(msg);
                g2.drawString(msg, (getWidth() - w) / 2, getHeight() / 2);
                g2.dispose();
            }
        }
    };
    private final JPanel countersPanel = new JPanel(new GridLayout(0, 2, 10, 10));

    // ---------- Chrome ----------
    private final JLabel chipWaiting = label("0", 20, Font.BOLD, TEXT);
    private final JLabel chipServed = label("0", 20, Font.BOLD, TEXT);
    private final JLabel chipCounters = label("0/0", 20, Font.BOLD, TEXT);
    private final JLabel status = label(" ", 13, Font.PLAIN, MUTED);
    private final CardLayout cards = new CardLayout();
    private final JPanel cardHost = new JPanel(cards);
    private final List<RoundButton> navButtons = new ArrayList<>();

    public QueueFlowGUI() {
        super("QueueFlow - Smart Virtual Queue & Token Management");
        seedDemoData();

        setDefaultCloseOperation(EXIT_ON_CLOSE);
        getContentPane().setBackground(BG);
        setLayout(new BorderLayout());

        add(buildHeader(), BorderLayout.NORTH);
        add(buildNav(), BorderLayout.WEST);

        JPanel center = new JPanel(new GridBagLayout());
        center.setBackground(BG);
        center.setBorder(BorderFactory.createEmptyBorder(16, 16, 8, 16));
        GridBagConstraints gc = new GridBagConstraints();
        gc.fill = GridBagConstraints.BOTH;
        gc.gridy = 0;
        gc.gridx = 0; gc.weightx = 0; gc.weighty = 1;
        gc.insets = new Insets(0, 0, 0, 16);
        cardHost.setOpaque(false);
        cardHost.setPreferredSize(new Dimension(400, 400));
        cardHost.add(buildCustomerPage(), "Customer");
        cardHost.add(buildStaffPage(), "Staff");
        cardHost.add(buildAdminPage(), "Admin");
        center.add(cardHost, gc);
        gc.gridx = 1; gc.weightx = 1; gc.insets = new Insets(0, 0, 0, 0);
        center.add(buildBoard(), gc);
        add(center, BorderLayout.CENTER);

        JPanel bar = new JPanel(new BorderLayout());
        bar.setBackground(SURFACE);
        bar.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(1, 0, 0, 0, BORDER),
                BorderFactory.createEmptyBorder(8, 18, 8, 18)));
        bar.add(status, BorderLayout.WEST);
        add(bar, BorderLayout.SOUTH);

        showPage("Customer");
        refresh();
        setStatus("Ready.", MUTED);

        setSize(1220, 780);
        setMinimumSize(new Dimension(1040, 660));
        setLocationRelativeTo(null);
    }

    // =====================================================================
    // Layout builders
    // =====================================================================

    private JComponent buildHeader() {
        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(SURFACE);
        header.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, BORDER),
                BorderFactory.createEmptyBorder(14, 20, 14, 20)));

        JPanel titles = transparent(new GridLayout(2, 1, 0, 2));
        titles.add(label("QueueFlow", 24, Font.BOLD, TEXT));
        titles.add(label("Smart virtual queue & token management", 12, Font.PLAIN, MUTED));
        header.add(titles, BorderLayout.WEST);

        JPanel chips = transparent(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        chips.add(chip("WAITING", chipWaiting));
        chips.add(chip("SERVED", chipServed));
        chips.add(chip("COUNTERS BUSY", chipCounters));
        header.add(chips, BorderLayout.EAST);
        return header;
    }

    private JComponent chip(String caption, JLabel value) {
        Card c = new Card(CARD, 14);
        c.setLayout(new GridLayout(2, 1, 0, 0));
        c.setBorder(BorderFactory.createEmptyBorder(6, 16, 6, 16));
        c.add(value);
        c.add(label(caption, 10, Font.BOLD, MUTED));
        return c;
    }

    private JComponent buildNav() {
        JPanel nav = new JPanel();
        nav.setLayout(new BoxLayout(nav, BoxLayout.Y_AXIS));
        nav.setBackground(SURFACE);
        nav.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 0, 1, BORDER),
                BorderFactory.createEmptyBorder(16, 12, 16, 12)));
        nav.setPreferredSize(new Dimension(170, 100));

        String[][] items = {{"Customer", "Customer"}, {"Staff desk", "Staff"}, {"Admin", "Admin"}};
        for (String[] it : items) {
            RoundButton b = new RoundButton(it[0], ACCENT, RoundButton.Mode.NAV);
            b.setAlignmentX(Component.LEFT_ALIGNMENT);
            b.setMaximumSize(new Dimension(Integer.MAX_VALUE, 42));
            b.addActionListener(e -> showPage(it[1]));
            b.putClientProperty("page", it[1]);
            navButtons.add(b);
            nav.add(b);
            nav.add(Box.createVerticalStrut(6));
        }
        return nav;
    }

    private void showPage(String page) {
        cards.show(cardHost, page);
        for (RoundButton b : navButtons) {
            b.setActive(page.equals(b.getClientProperty("page")));
        }
    }

    private JComponent buildCustomerPage() {
        Card card = pageCard("Get a token", "Pick a service and join the virtual line.");
        Form f = new Form();

        f.row("SERVICE", custService);
        f.row("YOUR NAME", custName);

        JPanel prio = transparent(new GridLayout(1, 3, 8, 0));
        Token.Priority[] ps = Token.Priority.values();
        String[] names = {"Normal", "Senior", "Emergency"};
        Color[] cols = {ACCENT, AMBER, RED};
        for (int i = 0; i < ps.length; i++) {
            final Token.Priority p = ps[i];
            RoundButton b = new RoundButton(names[i], cols[i], RoundButton.Mode.CHIP);
            b.setBorder(BorderFactory.createEmptyBorder(8, 6, 8, 6));
            b.addActionListener(e -> selectPriority(p));
            prioButtons.add(b);
            prio.add(b);
        }
        selectPriority(Token.Priority.NORMAL);
        f.row("PRIORITY", prio);

        RoundButton get = new RoundButton("Get token", ACCENT, RoundButton.Mode.SOLID);
        get.addActionListener(e -> issueToken());
        f.gap(14);
        f.row(null, get);

        f.gap(12);
        f.row("HAVE A TOKEN?", tokenLookup);
        JPanel two = transparent(new GridLayout(1, 2, 8, 0));
        RoundButton check = new RoundButton("Check status", ACCENT, RoundButton.Mode.CHIP);
        check.addActionListener(e -> checkToken());
        RoundButton cancel = new RoundButton("Cancel token", RED, RoundButton.Mode.SOLID);
        cancel.addActionListener(e -> cancelToken());
        two.add(check);
        two.add(cancel);
        f.gap(8);
        f.row(null, two);

        styleTextArea(ticket);
        ticket.setText("Your token details will appear here.");
        JScrollPane sp = new JScrollPane(ticket);
        styleScroll(sp);
        sp.setBorder(BorderFactory.createLineBorder(BORDER));
        sp.setPreferredSize(new Dimension(100, 110));
        f.gap(12);
        f.fill(sp);

        custName.addActionListener(e -> issueToken());
        tokenLookup.addActionListener(e -> checkToken());
        card.add(f, BorderLayout.CENTER);
        return card;
    }

    private JComponent buildStaffPage() {
        Card card = pageCard("Staff desk", "Call, complete or skip customers at a counter.");
        Form f = new Form();
        f.row("COUNTER", staffCounter);
        staffCounter.addActionListener(e -> {
            if (!updating) updateNowServing();
        });

        Card now = new Card(FIELD, 16);
        now.setLayout(new GridLayout(3, 1, 0, 2));
        now.setBorder(BorderFactory.createEmptyBorder(16, 18, 16, 18));
        now.add(label("NOW SERVING", 10, Font.BOLD, MUTED));
        now.add(nowToken);
        now.add(nowInfo);
        f.gap(14);
        f.row(null, now);

        RoundButton call = new RoundButton("Call next", ACCENT, RoundButton.Mode.SOLID);
        call.addActionListener(e -> callNext());
        RoundButton done = new RoundButton("Complete", GREEN, RoundButton.Mode.SOLID);
        done.addActionListener(e -> completeCurrent());
        RoundButton skip = new RoundButton("Skip (no-show)", AMBER, RoundButton.Mode.SOLID);
        skip.addActionListener(e -> skipCurrent());
        f.gap(14);
        f.row(null, call);
        f.gap(8);
        JPanel two = transparent(new GridLayout(1, 2, 8, 0));
        two.add(done);
        two.add(skip);
        f.row(null, two);

        f.gap(14);
        f.row(null, label("Tip: click a counter card on the right to select it.", 12, Font.PLAIN, MUTED));
        f.spacer();
        card.add(f, BorderLayout.CENTER);
        return card;
    }

    private JComponent buildAdminPage() {
        Card card = pageCard("Admin", "Add services and counters, and view statistics.");
        Form f = new Form();

        f.row("NEW SERVICE NAME", newServiceName);
        f.row("TOKEN PREFIX (e.g. B)", newServicePrefix);
        RoundButton addS = new RoundButton("Add service", ACCENT, RoundButton.Mode.SOLID);
        addS.addActionListener(e -> addService());
        f.gap(10);
        f.row(null, addS);

        f.gap(10);
        f.row("OPEN A COUNTER FOR", adminService);
        RoundButton addC = new RoundButton("Add counter", ACCENT, RoundButton.Mode.CHIP);
        addC.addActionListener(e -> addCounter());
        f.gap(8);
        f.row(null, addC);

        styleTextArea(statsArea);
        statsArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        statsArea.setLineWrap(false);
        JScrollPane sp = new JScrollPane(statsArea);
        styleScroll(sp);
        sp.setBorder(BorderFactory.createLineBorder(BORDER));
        sp.setPreferredSize(new Dimension(100, 120));
        f.gap(12);
        f.row("STATISTICS", new JLabel());
        f.fill(sp);

        card.add(f, BorderLayout.CENTER);
        return card;
    }

    private JComponent buildBoard() {
        JPanel col = transparent(new GridLayout(2, 1, 0, 16));

        // Live queue
        Card queue = new Card(CARD, 18);
        queue.setLayout(new BorderLayout(0, 10));
        queue.setBorder(BorderFactory.createEmptyBorder(16, 18, 16, 18));
        JPanel top = transparent(new BorderLayout());
        top.add(label("Live queue", 17, Font.BOLD, TEXT), BorderLayout.WEST);
        boardService.setPreferredSize(new Dimension(200, 34));
        top.add(boardService, BorderLayout.EAST);
        boardService.addActionListener(e -> {
            if (!updating) rebuildTable();
        });
        queue.add(top, BorderLayout.NORTH);

        table.setBackground(FIELD);
        table.setForeground(TEXT);
        table.setRowHeight(34);
        table.setShowGrid(false);
        table.setIntercellSpacing(new Dimension(0, 0));
        table.setFillsViewportHeight(true);
        table.setSelectionBackground(new Color(0x24324A));
        table.setSelectionForeground(TEXT);
        table.setFont(new Font(FONT, Font.PLAIN, 13));
        JTableHeader th = table.getTableHeader();
        th.setReorderingAllowed(false);
        th.setPreferredSize(new Dimension(100, 32));
        th.setDefaultRenderer(new HeaderRenderer());
        table.setDefaultRenderer(Object.class, new CellRenderer());
        table.getColumnModel().getColumn(0).setMaxWidth(50);
        table.getColumnModel().getColumn(1).setMaxWidth(110);
        JScrollPane sp = new JScrollPane(table);
        styleScroll(sp);
        sp.setBorder(BorderFactory.createLineBorder(BORDER));
        queue.add(sp, BorderLayout.CENTER);

        // Counters
        Card counters = new Card(CARD, 18);
        counters.setLayout(new BorderLayout(0, 10));
        counters.setBorder(BorderFactory.createEmptyBorder(16, 18, 16, 18));
        counters.add(label("Counters", 17, Font.BOLD, TEXT), BorderLayout.NORTH);
        countersPanel.setOpaque(false);
        JPanel wrap = transparent(new BorderLayout());
        wrap.add(countersPanel, BorderLayout.NORTH);
        JScrollPane csp = new JScrollPane(wrap);
        styleScroll(csp);
        csp.setOpaque(false);
        csp.getViewport().setOpaque(false);
        csp.setBorder(BorderFactory.createEmptyBorder());
        counters.add(csp, BorderLayout.CENTER);

        col.add(queue);
        col.add(counters);
        return col;
    }

    private Card pageCard(String title, String subtitle) {
        Card card = new Card(CARD, 18);
        card.setLayout(new BorderLayout(0, 6));
        card.setBorder(BorderFactory.createEmptyBorder(18, 20, 18, 20));
        JPanel head = transparent(new GridLayout(2, 1, 0, 2));
        head.add(label(title, 19, Font.BOLD, TEXT));
        head.add(label(subtitle, 12, Font.PLAIN, MUTED));
        card.add(head, BorderLayout.NORTH);
        return card;
    }

    // =====================================================================
    // Actions
    // =====================================================================

    private interface Action {
        String run() throws QueueFlowException;
    }

    private void act(Action a) {
        try {
            setStatus(a.run(), new Color(0x3FB950));
        } catch (QueueFlowException e) {
            setStatus(e.getMessage(), new Color(0xF85149));
        }
        refresh();
    }

    private void selectPriority(Token.Priority p) {
        selectedPriority = p;
        Token.Priority[] ps = Token.Priority.values();
        for (int i = 0; i < prioButtons.size(); i++) {
            prioButtons.get(i).setActive(ps[i] == p);
        }
    }

    private void issueToken() {
        act(() -> {
            String svc = (String) custService.getSelectedItem();
            if (svc == null) throw new QueueFlowException("No service available. Add one in Admin.");
            Token t = manager.issueToken(svc, custName.getText(), selectedPriority);
            ticket.setText("YOUR TOKEN:  " + t.getId() + "\n\n" + manager.describe(t.getId()));
            tokenLookup.setText(t.getId());
            custName.setText("");
            boardService.setSelectedItem(svc);
            return "Token " + t.getId() + " issued.";
        });
    }

    private void checkToken() {
        act(() -> {
            ticket.setText(manager.describe(tokenLookup.getText()));
            return "Status loaded.";
        });
    }

    private void cancelToken() {
        act(() -> {
            String id = tokenLookup.getText().trim();
            manager.cancelToken(id);
            ticket.setText("Token " + id.toUpperCase() + " was cancelled.");
            return "Token " + id.toUpperCase() + " cancelled.";
        });
    }

    private Counter selectedCounter() throws QueueFlowException {
        Counter c = (Counter) staffCounter.getSelectedItem();
        if (c == null) throw new QueueFlowException("No counter available. Add one in Admin.");
        return c;
    }

    private void callNext() {
        act(() -> {
            Token t = manager.callNext(selectedCounter().getId());
            return "Now serving " + t.getId() + " (" + t.getCustomerName() + ", " + pretty(t.getPriority()) + ").";
        });
    }

    private void completeCurrent() {
        act(() -> "Completed " + manager.completeCurrent(selectedCounter().getId()).getId() + ".");
    }

    private void skipCurrent() {
        act(() -> "Skipped " + manager.skipCurrent(selectedCounter().getId()).getId() + " (no-show).");
    }

    private void addService() {
        act(() -> {
            manager.addService(newServiceName.getText(), newServicePrefix.getText());
            String n = newServiceName.getText().trim();
            newServiceName.setText("");
            newServicePrefix.setText("");
            adminService.setSelectedItem(n);
            return "Service '" + n + "' added. Open a counter for it below.";
        });
    }

    private void addCounter() {
        act(() -> {
            Counter c = manager.addCounter((String) adminService.getSelectedItem());
            return "Counter " + c.getId() + " opened for " + c.getService().getName() + ".";
        });
    }

    // =====================================================================
    // Refreshing the view
    // =====================================================================

    private void refresh() {
        updating = true;
        try {
            fillServices(custService);
            fillServices(boardService);
            fillServices(adminService);
            fillCounters();
            rebuildTable();
            rebuildCounterCards();
            updateNowServing();
            updateChips();
            statsArea.setText(manager.stats());
            statsArea.setCaretPosition(0);
        } finally {
            updating = false;
        }
    }

    private void fillServices(JComboBox<String> box) {
        Object keep = box.getSelectedItem();
        box.removeAllItems();
        for (ServiceQueue s : manager.getServices()) {
            box.addItem(s.getName());
        }
        if (keep != null) box.setSelectedItem(keep);
        if (box.getSelectedItem() == null && box.getItemCount() > 0) box.setSelectedIndex(0);
    }

    private void fillCounters() {
        Counter keep = (Counter) staffCounter.getSelectedItem();
        staffCounter.removeAllItems();
        Counter match = null;
        for (Counter c : manager.getCounters()) {
            staffCounter.addItem(c);
            if (keep != null && c.getId() == keep.getId()) match = c;
        }
        if (match != null) staffCounter.setSelectedItem(match);
        else if (staffCounter.getItemCount() > 0) staffCounter.setSelectedIndex(0);
    }

    private void rebuildTable() {
        tableModel.setRowCount(0);
        String svc = (String) boardService.getSelectedItem();
        if (svc == null) return;
        try {
            int pos = 1;
            for (Token t : manager.waitingList(svc)) {
                tableModel.addRow(new Object[]{pos++, t.getId(), t.getCustomerName(), pretty(t.getPriority())});
            }
        } catch (QueueFlowException ignored) {
            // service disappeared; nothing to show
        }
    }

    private void rebuildCounterCards() {
        countersPanel.removeAll();
        for (Counter c : manager.getCounters()) {
            Card card = new Card(FIELD, 14);
            card.setLayout(new BorderLayout(8, 0));
            card.setBorder(BorderFactory.createEmptyBorder(10, 14, 10, 14));
            card.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

            JPanel left = transparent(new GridLayout(2, 1));
            left.add(label("Counter " + c.getId(), 14, Font.BOLD, TEXT));
            left.add(label(c.getService().getName(), 12, Font.PLAIN, MUTED));
            card.add(left, BorderLayout.WEST);

            JPanel right = transparent(new GridLayout(2, 1));
            JLabel state = label(c.isBusy() ? c.getCurrent().getId() : "FREE", 14, Font.BOLD,
                    c.isBusy() ? AMBER : new Color(0x3FB950));
            state.setHorizontalAlignment(SwingConstants.RIGHT);
            JLabel who = label(c.isBusy() ? c.getCurrent().getCustomerName() : "ready", 12, Font.PLAIN, MUTED);
            who.setHorizontalAlignment(SwingConstants.RIGHT);
            right.add(state);
            right.add(who);
            card.add(right, BorderLayout.EAST);

            card.addMouseListener(new MouseAdapter() {
                @Override public void mouseClicked(MouseEvent e) {
                    for (int i = 0; i < staffCounter.getItemCount(); i++) {
                        if (staffCounter.getItemAt(i).getId() == c.getId()) {
                            staffCounter.setSelectedIndex(i);
                            break;
                        }
                    }
                    showPage("Staff");
                }
            });
            countersPanel.add(card);
        }
        countersPanel.revalidate();
        countersPanel.repaint();
    }

    private void updateNowServing() {
        Counter c = (Counter) staffCounter.getSelectedItem();
        if (c == null) {
            nowToken.setText("-");
            nowInfo.setText("No counter selected");
        } else if (c.isBusy()) {
            Token t = c.getCurrent();
            nowToken.setText(t.getId());
            nowInfo.setText(t.getCustomerName() + "  |  " + pretty(t.getPriority()));
        } else {
            nowToken.setText("-");
            nowInfo.setText("Counter " + c.getId() + " is free");
        }
    }

    private void updateChips() {
        int waiting = 0, served = 0, busy = 0;
        for (ServiceQueue s : manager.getServices()) {
            waiting += s.getWaitingCount();
            served += s.getServedCount();
        }
        List<Counter> cs = manager.getCounters();
        for (Counter c : cs) if (c.isBusy()) busy++;
        chipWaiting.setText(String.valueOf(waiting));
        chipServed.setText(String.valueOf(served));
        chipCounters.setText(busy + "/" + cs.size());
    }

    private void setStatus(String msg, Color color) {
        status.setForeground(color);
        status.setText("\u25CF  " + msg);
    }

    private static String pretty(Token.Priority p) {
        String s = p.name();
        return s.charAt(0) + s.substring(1).toLowerCase();
    }

    private static String counterLabel(Counter c) {
        return "Counter " + c.getId() + "  -  " + c.getService().getName()
                + (c.isBusy() ? "  (serving " + c.getCurrent().getId() + ")" : "  (free)");
    }

    private void seedDemoData() {
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

    // =====================================================================
    // Styling helpers & custom components
    // =====================================================================

    static JLabel label(String text, int size, int style, Color color) {
        JLabel l = new JLabel(text);
        l.setFont(new Font(FONT, style, size));
        l.setForeground(color);
        return l;
    }

    static JPanel transparent(LayoutManager lm) {
        JPanel p = new JPanel(lm);
        p.setOpaque(false);
        return p;
    }

    static JTextField field(int cols) {
        JTextField f = new JTextField(cols);
        f.setBackground(FIELD);
        f.setForeground(TEXT);
        f.setCaretColor(TEXT);
        f.setSelectionColor(ACCENT);
        f.setSelectedTextColor(Color.WHITE);
        f.setFont(new Font(FONT, Font.PLAIN, 14));
        Runnable normal = () -> f.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER), BorderFactory.createEmptyBorder(8, 10, 8, 10)));
        Runnable focus = () -> f.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(ACCENT), BorderFactory.createEmptyBorder(8, 10, 8, 10)));
        normal.run();
        f.addFocusListener(new FocusAdapter() {
            @Override public void focusGained(FocusEvent e) { focus.run(); }
            @Override public void focusLost(FocusEvent e) { normal.run(); }
        });
        return f;
    }

    static void styleTextArea(JTextArea a) {
        a.setEditable(false);
        a.setLineWrap(true);
        a.setWrapStyleWord(true);
        a.setBackground(FIELD);
        a.setForeground(TEXT);
        a.setSelectionColor(ACCENT);
        a.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 13));
        a.setBorder(BorderFactory.createEmptyBorder(10, 12, 10, 12));
    }

    static void styleScroll(JScrollPane sp) {
        sp.getViewport().setBackground(FIELD);
        sp.setBackground(FIELD);
        sp.setBorder(BorderFactory.createEmptyBorder());
        sp.getVerticalScrollBar().setUI(new DarkScrollBarUI());
        sp.getHorizontalScrollBar().setUI(new DarkScrollBarUI());
        sp.getVerticalScrollBar().setPreferredSize(new Dimension(10, 0));
        sp.getHorizontalScrollBar().setPreferredSize(new Dimension(0, 10));
        JPanel corner = new JPanel();
        corner.setBackground(FIELD);
        sp.setCorner(JScrollPane.LOWER_RIGHT_CORNER, corner);
    }

    @SuppressWarnings("unchecked")
    static <T> JComboBox<T> styledCombo(JComboBox<T> box) {
        box.setUI(new DarkComboUI());
        box.setRenderer((ListCellRenderer<? super T>) new DarkListRenderer());
        box.setBackground(FIELD);
        box.setForeground(TEXT);
        box.setFont(new Font(FONT, Font.PLAIN, 14));
        box.setBorder(BorderFactory.createLineBorder(BORDER));
        box.setMaximumRowCount(8);
        box.setPreferredSize(new Dimension(100, 38));
        return box;
    }

    /** Rounded, translucent-free card panel. Children should be non-opaque. */
    static class Card extends JPanel {
        private final Color fill;
        private final int arc;

        Card(Color fill, int arc) {
            this.fill = fill;
            this.arc = arc;
            setOpaque(false);
        }

        @Override protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(fill);
            g2.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, arc, arc);
            g2.setColor(BORDER);
            g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, arc, arc);
            g2.dispose();
            super.paintComponent(g);
        }
    }

    /** Simple vertical form: optional small caption above each row. */
    static class Form extends JPanel {
        private int y = 0;

        Form() {
            super(new GridBagLayout());
            setOpaque(false);
        }

        void row(String caption, Component c) {
            if (caption != null) {
                GridBagConstraints g = base();
                g.insets = new Insets(10, 0, 4, 0);
                super.add(label(caption, 10, Font.BOLD, MUTED), g);
            }
            super.add(c, base());
        }

        void fill(Component c) {
            GridBagConstraints g = base();
            g.fill = GridBagConstraints.BOTH;
            g.weighty = 1;
            super.add(c, g);
        }

        void gap(int h) {
            GridBagConstraints g = base();
            super.add(Box.createVerticalStrut(h), g);
        }

        void spacer() {
            GridBagConstraints g = base();
            g.weighty = 1;
            g.fill = GridBagConstraints.BOTH;
            super.add(Box.createGlue(), g);
        }

        private GridBagConstraints base() {
            GridBagConstraints g = new GridBagConstraints();
            g.gridx = 0;
            g.gridy = y++;
            g.weightx = 1;
            g.anchor = GridBagConstraints.NORTHWEST;
            g.fill = GridBagConstraints.HORIZONTAL;
            return g;
        }
    }

    /** Custom-painted rounded button with solid, chip (toggle-look) and nav styles. */
    static class RoundButton extends JButton {
        enum Mode { SOLID, CHIP, NAV }

        private final Color base;
        private final Mode mode;
        private boolean hover;
        private boolean active;

        RoundButton(String text, Color base, Mode mode) {
            super(text);
            this.base = base;
            this.mode = mode;
            setContentAreaFilled(false);
            setFocusPainted(false);
            setBorderPainted(false);
            setOpaque(false);
            setFont(new Font(FONT, Font.BOLD, 13));
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            setBorder(BorderFactory.createEmptyBorder(10, 16, 10, 16));
            addMouseListener(new MouseAdapter() {
                @Override public void mouseEntered(MouseEvent e) { hover = true; repaint(); }
                @Override public void mouseExited(MouseEvent e) { hover = false; repaint(); }
            });
        }

        void setActive(boolean a) {
            active = a;
            repaint();
        }

        @Override protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            int w = getWidth(), h = getHeight();
            Color fill = null;
            Color fg = TEXT;
            switch (mode) {
                case SOLID:
                    fill = !isEnabled() ? BORDER : (hover ? base.brighter() : base);
                    fg = Color.WHITE;
                    break;
                case CHIP:
                    fill = active ? base : (hover ? BORDER : FIELD);
                    fg = active ? Color.WHITE : TEXT;
                    break;
                default:
                    fill = active ? CARD : (hover ? new Color(0x1B1F26) : null);
                    fg = active ? TEXT : MUTED;
            }
            if (fill != null) {
                g2.setColor(fill);
                g2.fillRoundRect(0, 0, w - 1, h - 1, 12, 12);
            }
            if (mode == Mode.CHIP && !active) {
                g2.setColor(BORDER);
                g2.drawRoundRect(0, 0, w - 1, h - 1, 12, 12);
            }
            if (mode == Mode.NAV && active) {
                g2.setColor(ACCENT);
                g2.fillRoundRect(0, 9, 4, h - 18, 4, 4);
            }
            g2.setFont(getFont());
            g2.setColor(fg);
            FontMetrics fm = g2.getFontMetrics();
            int tx = mode == Mode.NAV ? 20 : (w - fm.stringWidth(getText())) / 2;
            int ty = (h - fm.getHeight()) / 2 + fm.getAscent();
            g2.drawString(getText(), tx, ty);
            g2.dispose();
        }
    }

    static class DarkComboUI extends BasicComboBoxUI {
        @Override protected JButton createArrowButton() {
            JButton b = new BasicArrowButton(BasicArrowButton.SOUTH, FIELD, FIELD, MUTED, FIELD);
            b.setBorder(BorderFactory.createEmptyBorder());
            return b;
        }

        @Override public void paintCurrentValueBackground(Graphics g, Rectangle bounds, boolean hasFocus) {
            g.setColor(FIELD);
            g.fillRect(bounds.x, bounds.y, bounds.width, bounds.height);
        }
    }

    static class DarkListRenderer extends DefaultListCellRenderer {
        @Override public Component getListCellRendererComponent(JList<?> list, Object value, int index,
                                                                boolean selected, boolean focus) {
            super.getListCellRendererComponent(list, value, index, selected, focus);
            if (value instanceof Counter) setText(counterLabel((Counter) value));
            setBackground(selected ? new Color(0x24324A) : FIELD);
            setForeground(TEXT);
            setBorder(BorderFactory.createEmptyBorder(7, 10, 7, 10));
            return this;
        }
    }

    static class DarkScrollBarUI extends BasicScrollBarUI {
        @Override protected void configureScrollBarColors() {
            thumbColor = new Color(0x3A414B);
            trackColor = FIELD;
        }

        @Override protected JButton createDecreaseButton(int o) { return zero(); }
        @Override protected JButton createIncreaseButton(int o) { return zero(); }

        private JButton zero() {
            JButton b = new JButton();
            Dimension d = new Dimension(0, 0);
            b.setPreferredSize(d);
            b.setMinimumSize(d);
            b.setMaximumSize(d);
            return b;
        }

        @Override protected void paintThumb(Graphics g, JComponent c, Rectangle r) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(thumbColor);
            g2.fillRoundRect(r.x + 2, r.y + 2, r.width - 4, r.height - 4, 8, 8);
            g2.dispose();
        }

        @Override protected void paintTrack(Graphics g, JComponent c, Rectangle r) {
            g.setColor(trackColor);
            g.fillRect(r.x, r.y, r.width, r.height);
        }
    }

    static class HeaderRenderer extends DefaultTableCellRenderer {
        @Override public Component getTableCellRendererComponent(JTable t, Object v, boolean sel, boolean foc,
                                                                 int row, int col) {
            super.getTableCellRendererComponent(t, v, false, false, row, col);
            setBackground(SURFACE);
            setForeground(MUTED);
            setFont(new Font(FONT, Font.BOLD, 11));
            setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createMatteBorder(0, 0, 1, 0, BORDER),
                    BorderFactory.createEmptyBorder(0, 12, 0, 12)));
            setText(String.valueOf(v).toUpperCase());
            return this;
        }
    }

    static class CellRenderer extends DefaultTableCellRenderer {
        @Override public Component getTableCellRendererComponent(JTable t, Object v, boolean sel, boolean foc,
                                                                 int row, int col) {
            super.getTableCellRendererComponent(t, v, sel, false, row, col);
            setBorder(BorderFactory.createEmptyBorder(0, 12, 0, 12));
            setBackground(sel ? t.getSelectionBackground() : (row % 2 == 0 ? FIELD : new Color(0x181C22)));
            Color fg = TEXT;
            if (col == 0) fg = MUTED;
            if (col == 1) fg = ACCENT;
            if (col == 3) {
                String p = String.valueOf(v);
                fg = p.equals("Emergency") ? new Color(0xF85149) : p.equals("Senior") ? new Color(0xE3B341) : MUTED;
            }
            setForeground(fg);
            setFont(new Font(FONT, col == 1 ? Font.BOLD : Font.PLAIN, 13));
            return this;
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new QueueFlowGUI().setVisible(true));
    }
}
