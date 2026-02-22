import javax.swing.*;
import javax.swing.table.*;
import java.awt.*;

public class WishlistUI extends JFrame {
    public JTable mainTable, detailTable;
    public DefaultTableModel mainModel, detailModel;
    public JButton startBtn, itemBtn, stopBtn, pauseBtn, copyBtn, exportBtn, browserBtn;
    public JCheckBox bolCb, legoCb, amazonCb, dreamCb; // dreamCb toegevoegd
    public JProgressBar progress;
    public JLabel status;

    public WishlistUI() {
        setTitle("Wishlist Admin - DreamLand Integrated");
        setSize(1300, 850);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setupUI();
    }

    private void setupUI() {
        mainModel = new DefaultTableModel(new String[]{"ID", "Product", "Huidige (JSON)", "Status"}, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        mainTable = new JTable(mainModel);
        
        detailModel = new DefaultTableModel(new String[]{"Winkel", "JSON Prijs", "Live Prijs", "URL"}, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        detailTable = new JTable(detailModel);
        detailTable.setDefaultRenderer(Object.class, new PriceColorRenderer());

        JPanel left = new JPanel();
        left.setLayout(new BoxLayout(left, BoxLayout.Y_AXIS));
        left.setBorder(BorderFactory.createEmptyBorder(10,10,10,10));

        bolCb = new JCheckBox("Bol.com");
        legoCb = new JCheckBox("Lego.com");
        amazonCb = new JCheckBox("Amazon");
        dreamCb = new JCheckBox("DreamLand"); // Nieuw
        
        startBtn = createBtn("Start Volledige Scan");
        itemBtn = createBtn("Scan Selectie");
        pauseBtn = createBtn("Pauzeer");
        stopBtn = createBtn("Stop");
        browserBtn = createBtn("Open Browser");
        copyBtn = createBtn("Copy JSON");
        exportBtn = createBtn("Save JSON");

        left.add(new JLabel("Crawlers:"));
        left.add(bolCb); left.add(legoCb); left.add(amazonCb); left.add(dreamCb);
        left.add(Box.createVerticalStrut(20));
        left.add(startBtn); left.add(itemBtn); left.add(pauseBtn); left.add(stopBtn);
        left.add(Box.createVerticalStrut(20));
        left.add(browserBtn); left.add(copyBtn); left.add(exportBtn);

        JSplitPane split = new JSplitPane(JSplitPane.VERTICAL_SPLIT, new JScrollPane(mainTable), new JScrollPane(detailTable));
        split.setDividerLocation(400);

        JPanel bottom = new JPanel(new BorderLayout());
        status = new JLabel(" Gereed");
        progress = new JProgressBar();
        progress.setStringPainted(true);
        progress.setVisible(false);
        bottom.add(status, BorderLayout.WEST);
        bottom.add(progress, BorderLayout.EAST);

        add(left, BorderLayout.WEST);
        add(split, BorderLayout.CENTER);
        add(bottom, BorderLayout.SOUTH);
    }

    private JButton createBtn(String t) {
        JButton b = new JButton(t);
        b.setMaximumSize(new Dimension(200, 40));
        b.setAlignmentX(Component.CENTER_ALIGNMENT);
        return b;
    }

    private class PriceColorRenderer extends DefaultTableCellRenderer {
        @Override public Component getTableCellRendererComponent(JTable t, Object v, boolean s, boolean f, int r, int c) {
            Component comp = super.getTableCellRendererComponent(t, v, s, f, r, c);
            if (c == 2 && v != null && v.toString().contains("€")) {
                try {
                    double o = Double.parseDouble(t.getValueAt(r, 1).toString().replaceAll("[^0-9,]", "").replace(",", "."));
                    double n = Double.parseDouble(v.toString().replaceAll("[^0-9,]", "").replace(",", "."));
                    comp.setForeground(n < o ? new Color(0, 150, 0) : (n > o ? Color.RED : Color.BLACK));
                } catch (Exception e) { comp.setForeground(Color.BLACK); }
            } else comp.setForeground(Color.BLACK);
            return comp;
        }
    }
}