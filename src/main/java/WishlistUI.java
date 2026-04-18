import javax.swing.*;
import javax.swing.table.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;
import java.awt.*;
import org.json.JSONArray;
import org.json.JSONObject;
import java.util.HashMap;
import java.util.Map;

public class WishlistUI extends JFrame {
    public JTable mainTable, detailTable;
    public DefaultTableModel mainModel, detailModel;
    public JButton startBtn, itemBtn, stopBtn, pauseBtn, copyBtn, exportBtn, browserBtn, validateBtn;
    public JCheckBox bolCb, legoCb, amazonCb, dreamCb, fnacCb, supraCb, mediaCb, coolblueCb;
    public JPanel fileCheckboxesPanel;
    public Map<String, JCheckBox> fileCheckBoxes = new HashMap<>();
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
        mainTable.getColumnModel().getColumn(2).setCellRenderer(new MainPriceColorRenderer());

        detailModel = new DefaultTableModel(new String[]{"Winkel", "JSON Prijs", "Live Prijs", "URL"}, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        detailTable = new JTable(detailModel);
        detailTable.setRowHeight(28);
        detailTable.setDefaultRenderer(Object.class, new DetailPriceColorRenderer());

        JPanel left = new JPanel();
        left.setLayout(new BoxLayout(left, BoxLayout.Y_AXIS));
        left.setBorder(new EmptyBorder(15, 15, 15, 15));
        left.setPreferredSize(new Dimension(260, 0));

        fileCheckboxesPanel = new JPanel();
        fileCheckboxesPanel.setLayout(new BoxLayout(fileCheckboxesPanel, BoxLayout.Y_AXIS));
        fileCheckboxesPanel.setBorder(BorderFactory.createTitledBorder(null, "INPUT BESTANDEN", TitledBorder.LEFT, TitledBorder.TOP, new Font("SansSerif", Font.BOLD, 11)));
        left.add(fileCheckboxesPanel);
        left.add(Box.createVerticalStrut(15));

        JPanel shopPanel = new JPanel();
        shopPanel.setLayout(new BoxLayout(shopPanel, BoxLayout.Y_AXIS));
        shopPanel.setBorder(BorderFactory.createTitledBorder(null, "WINKELS", TitledBorder.LEFT, TitledBorder.TOP, new Font("SansSerif", Font.BOLD, 11)));
        
        bolCb = new JCheckBox("Bol.com");
        legoCb = new JCheckBox("Lego.com");
        amazonCb = new JCheckBox("Amazon");
        dreamCb = new JCheckBox("DreamLand");
        fnacCb = new JCheckBox("Fnac.be");
        supraCb = new JCheckBox("SupraBazar");
        mediaCb = new JCheckBox("MediaMarkt");
        coolblueCb = new JCheckBox("Coolblue");
        
        shopPanel.add(bolCb); shopPanel.add(legoCb); shopPanel.add(amazonCb); 
        shopPanel.add(dreamCb); shopPanel.add(fnacCb); shopPanel.add(supraCb); 
        shopPanel.add(mediaCb); shopPanel.add(coolblueCb);
        left.add(shopPanel);
        left.add(Box.createVerticalStrut(20));

        startBtn = createBtn("Volledige Prijs Scan", true);
        validateBtn = createBtn("Check Kapotte Links", false);
        itemBtn = createBtn("Scan Selectie", false);
        pauseBtn = createBtn("Pauzeer", false);
        stopBtn = createBtn("Stop", false);
        
        left.add(startBtn); left.add(Box.createVerticalStrut(5));
        left.add(validateBtn); left.add(Box.createVerticalStrut(5));
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

    private class DetailPriceColorRenderer extends DefaultTableCellRenderer {
        @Override public Component getTableCellRendererComponent(JTable t, Object v, boolean s, boolean f, int r, int c) {
            Component comp = super.getTableCellRendererComponent(t, v, s, f, r, c);
            if (c == 2 && v != null && v.toString().contains("€")) {
                try {
                    double o = Double.parseDouble(t.getValueAt(r, 1).toString().replaceAll("[^0-9,]", "").replace(",", "."));
                    double n = Double.parseDouble(v.toString().replaceAll("[^0-9,]", "").replace(",", "."));
                    if (n < o) comp.setForeground(new Color(100, 220, 100));
                    else if (n > o) comp.setForeground(new Color(255, 100, 100));
                    else comp.setForeground(null);
                } catch (Exception e) { comp.setForeground(null); }
            } else if (v != null && v.toString().startsWith("FOUT")) {
                comp.setForeground(Color.RED);
            } else comp.setForeground(null);
            return comp;
        }
    }

    private class MainPriceColorRenderer extends DefaultTableCellRenderer {
        @Override public Component getTableCellRendererComponent(JTable t, Object v, boolean s, boolean f, int r, int c) {
            Component comp = super.getTableCellRendererComponent(t, v, s, f, r, c);
            Window window = SwingUtilities.getWindowAncestor(t);
            if (window instanceof WishlistUI uiFrame) {
                Object clientProp = uiFrame.getRootPane().getClientProperty("controller");
                if (clientProp instanceof WishlistController ctrl) {
                    if (r < ctrl.getAllItems().size()) {
                        JSONObject item = ctrl.getAllItems().get(r);
                        JSONArray winkels = item.getJSONArray("winkels");
                        boolean hasGreens = false, hasReds = false;
                        for (int i = 0; i < winkels.length(); i++) {
                            JSONObject w = winkels.getJSONObject(i);
                            if (w.has("live_prijs") && w.getString("live_prijs").contains("€")) {
                                try {
                                    double oldP = Double.parseDouble(w.getString("prijs").replaceAll("[^0-9,]", "").replace(",", "."));
                                    double newP = Double.parseDouble(w.getString("live_prijs").replaceAll("[^0-9,]", "").replace(",", "."));
                                    if (newP < oldP) hasGreens = true;
                                    if (newP > oldP) hasReds = true;
                                } catch (Exception e) {}
                            }
                        }
                        if (hasGreens && hasReds) comp.setForeground(new Color(100, 150, 255));
                        else if (hasGreens) comp.setForeground(new Color(100, 220, 100));
                        else if (hasReds) comp.setForeground(new Color(255, 100, 100));
                        else comp.setForeground(null);
                    }
                }
            }
            return comp;
        }
    }
}