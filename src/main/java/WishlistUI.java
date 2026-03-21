import javax.swing.*;
import javax.swing.table.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;

public class WishlistUI extends JFrame {
    public JTable mainTable, detailTable;
    public DefaultTableModel mainModel, detailModel;
    public JButton startBtn, itemBtn, stopBtn, pauseBtn, copyBtn, exportBtn, browserBtn;
    public JCheckBox bolCb, legoCb, amazonCb, dreamCb;
    public JProgressBar progress;
    public JLabel status;

    public WishlistUI() {
        setTitle("Wishlist Admin - Modern UI");
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
        mainTable.setRowHeight(28);

        detailModel = new DefaultTableModel(new String[]{"Winkel", "JSON Prijs", "Live Prijs", "URL"}, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        detailTable = new JTable(detailModel);
        detailTable.setRowHeight(28);
        detailTable.setDefaultRenderer(Object.class, new PriceColorRenderer());

        JPanel left = new JPanel();
        left.setLayout(new BoxLayout(left, BoxLayout.Y_AXIS));
        left.setBorder(new EmptyBorder(15, 15, 15, 15));
        left.setPreferredSize(new Dimension(240, 0));

        JLabel title = new JLabel("CRAWLER SETTINGS");
        title.setFont(new Font("SansSerif", Font.BOLD, 13));
        left.add(title);
        left.add(Box.createVerticalStrut(10));

        bolCb = new JCheckBox("Bol.com");
        legoCb = new JCheckBox("Lego.com");
        amazonCb = new JCheckBox("Amazon");
        dreamCb = new JCheckBox("DreamLand");

        left.add(bolCb); left.add(legoCb); left.add(amazonCb); left.add(dreamCb);
        left.add(Box.createVerticalStrut(20));

        startBtn = createBtn("Volledige Scan", true);
        itemBtn = createBtn("Geselecteerd Item", false);
        pauseBtn = createBtn("Pauzeer", false);
        stopBtn = createBtn("Stop", false);

        left.add(startBtn); left.add(Box.createVerticalStrut(5));
        left.add(itemBtn); left.add(Box.createVerticalStrut(5));
        left.add(pauseBtn); left.add(Box.createVerticalStrut(5));
        left.add(stopBtn);

        left.add(Box.createVerticalGlue());

        browserBtn = createBtn("Open Link", false);
        copyBtn = createBtn("Copy JSON", false);
        exportBtn = createBtn("Save naar File", false);

        left.add(browserBtn); left.add(Box.createVerticalStrut(5));
        left.add(copyBtn); left.add(Box.createVerticalStrut(5));
        left.add(exportBtn);

        JSplitPane split = new JSplitPane(JSplitPane.VERTICAL_SPLIT, new JScrollPane(mainTable), new JScrollPane(detailTable));
        split.setDividerLocation(450);

        JPanel bottom = new JPanel(new BorderLayout());
        bottom.setBorder(new EmptyBorder(5, 10, 5, 10));
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

    private JButton createBtn(String t, boolean primary) {
        JButton b = new JButton(t);
        b.setMaximumSize(new Dimension(Integer.MAX_VALUE, 35));
        b.setAlignmentX(Component.LEFT_ALIGNMENT);
        if(primary) b.putClientProperty("JButton.buttonType", "roundRect");
        return b;
    }

    private class PriceColorRenderer extends DefaultTableCellRenderer {
        @Override public Component getTableCellRendererComponent(JTable t, Object v, boolean s, boolean f, int r, int c) {
            Component comp = super.getTableCellRendererComponent(t, v, s, f, r, c);
            if (c == 2 && v != null && v.toString().contains("€")) {
                try {
                    double o = Double.parseDouble(t.getValueAt(r, 1).toString().replaceAll("[^0-9,]", "").replace(",", "."));
                    double n = Double.parseDouble(v.toString().replaceAll("[^0-9,]", "").replace(",", "."));
                    comp.setForeground(n < o ? new Color(100, 220, 100) : (n > o ? new Color(255, 100, 100) : null));
                } catch (Exception e) { comp.setForeground(null); }
            } else comp.setForeground(null);
            return comp;
        }
    }
}