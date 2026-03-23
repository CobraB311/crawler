import javax.swing.*;
import com.formdev.flatlaf.FlatDarkLaf;
import java.util.*;
import java.awt.Toolkit;
import java.awt.datatransfer.StringSelection;
import java.net.URI;
import java.awt.Desktop;
import java.io.*;
import org.json.JSONObject;

public class WishlistAdmin {
    private final WishlistUI ui;
    private final WishlistController controller;

    public WishlistAdmin() {
        ui = new WishlistUI();
        controller = new WishlistController();
        ui.getRootPane().putClientProperty("controller", controller);
        
        setupActions();
        loadConfigAndData();
        ui.setVisible(true);
    }

    private void setupActions() {
        ui.startBtn.addActionListener(e -> startFullScan());
        ui.itemBtn.addActionListener(e -> scanSelected());
        ui.pauseBtn.addActionListener(e -> {
            controller.setPaused(!controller.isPaused());
            ui.pauseBtn.setText(controller.isPaused() ? "Hervatten" : "Pauzeer");
        });
        ui.stopBtn.addActionListener(e -> controller.setStop(true));
        ui.mainTable.getSelectionModel().addListSelectionListener(e -> updateDetails());
        ui.browserBtn.addActionListener(e -> openUrl());
        ui.exportBtn.addActionListener(e -> handleSave(false));
        ui.copyBtn.addActionListener(e -> handleSave(true));
    }

    private void loadConfigAndData() {
        Properties props = new Properties();
        try (InputStream in = getConfigStream()) {
            if (in == null) return;
            props.load(in);
            ui.bolCb.setSelected(Boolean.parseBoolean(props.getProperty("crawler.bol.enabled", "true")));
            ui.legoCb.setSelected(Boolean.parseBoolean(props.getProperty("crawler.lego.enabled", "false")));
            ui.amazonCb.setSelected(Boolean.parseBoolean(props.getProperty("crawler.amazon.enabled", "true")));
            ui.dreamCb.setSelected(Boolean.parseBoolean(props.getProperty("crawler.dreamland.enabled", "true")));
            
            List<String> urls = Arrays.asList(props.getProperty("wishlist.urls", "").split(","));
            String[] pathsArr = props.getProperty("wishlist.localPaths", "").split(",");
            Map<String, String> paths = new HashMap<>();
            
            ui.fileCheckboxesPanel.removeAll();
            ui.fileCheckBoxes.clear();

            for(int i = 0; i < urls.size(); i++) {
                String url = urls.get(i).trim();
                if(i < pathsArr.length) paths.put(url, pathsArr[i].trim());
                
                // Voeg checkbox toe per bestand
                String fileName = url.substring(url.lastIndexOf("/") + 1);
                JCheckBox cb = new JCheckBox(fileName, true);
                ui.fileCheckBoxes.put(url, cb);
                ui.fileCheckboxesPanel.add(cb);
            }
            
            controller.loadData(urls, paths, () -> SwingUtilities.invokeLater(this::refreshTable));
        } catch (Exception e) { e.printStackTrace(); }
    }

    private InputStream getConfigStream() throws IOException {
        InputStream res = getClass().getClassLoader().getResourceAsStream("config.properties");
        if (res != null) return res;
        File f = new File("config.properties");
        return f.exists() ? new FileInputStream(f) : null;
    }

    private void refreshTable() {
        ui.mainModel.setRowCount(0);
        for (var item : controller.getAllItems()) {
            String p = item.getJSONArray("winkels").length() > 0 ? item.getJSONArray("winkels").getJSONObject(0).getString("prijs") : "N/A";
            ui.mainModel.addRow(new Object[]{item.getString("id"), item.getString("naam"), p, "Gereed"});
        }
        ui.revalidate(); ui.repaint();
    }

    private void updateDetails() {
        int row = ui.mainTable.getSelectedRow();
        ui.detailModel.setRowCount(0);
        if (row == -1) return;
        var winkels = controller.getAllItems().get(row).getJSONArray("winkels");
        for (int i = 0; i < winkels.length(); i++) {
            var w = winkels.getJSONObject(i);
            ui.detailModel.addRow(new Object[]{w.getString("naam"), w.getString("prijs"), w.optString("live_prijs", "-"), w.getString("link")});
        }
    }

    private void startFullScan() {
        new Thread(() -> {
            controller.setStop(false);
            ui.progress.setVisible(true);
            
            // Filteren welke items gescand moeten worden op basis van bestand-checkboxes
            List<Integer> indicesToScan = new ArrayList<>();
            for (String url : controller.getLoadedFiles().keySet()) {
                if (ui.fileCheckBoxes.get(url).isSelected()) {
                    var itemsInFile = controller.getLoadedFiles().get(url);
                    // Vind de globale index van deze items
                    for (int j = 0; j < itemsInFile.length(); j++) {
                        JSONObject itemObj = itemsInFile.getJSONObject(j);
                        indicesToScan.add(controller.getAllItems().indexOf(itemObj));
                    }
                }
            }

            ui.progress.setMaximum(indicesToScan.size());
            int processed = 0;

            for (Integer idx : indicesToScan) {
                if (controller.isShouldStop()) break;
                synchronized(controller.getPauseLock()){ 
                    try { while(controller.isPaused() && !controller.isShouldStop()) controller.getPauseLock().wait(); } catch(Exception e){}
                }
                
                final int currentIdx = idx;
                SwingUtilities.invokeLater(() -> ui.mainModel.setValueAt("Scannen...", currentIdx, 3));
                
                final int progressVal = ++processed;
                controller.scanSingleItemParallel(idx, ui.bolCb.isSelected(), ui.legoCb.isSelected(), 
                                                 ui.amazonCb.isSelected(), ui.dreamCb.isSelected(), f -> {
                    SwingUtilities.invokeLater(() -> {
                        ui.mainModel.setValueAt("Klaar", f, 3);
                        ui.progress.setValue(progressVal);
                        ui.mainModel.fireTableRowsUpdated(f, f);
                        if (ui.mainTable.getSelectedRow() == f) updateDetails();
                    });
                });
                try { Thread.sleep(2500); } catch (Exception e) {}
            }
            ui.status.setText(" Scan voltooid");
        }).start();
    }

    private void scanSelected() {
        int row = ui.mainTable.getSelectedRow();
        if (row != -1) {
            ui.mainModel.setValueAt("Scannen...", row, 3);
            controller.scanSingleItemParallel(row, ui.bolCb.isSelected(), ui.legoCb.isSelected(), 
                                             ui.amazonCb.isSelected(), ui.dreamCb.isSelected(), idx -> {
                SwingUtilities.invokeLater(() -> {
                    ui.mainModel.setValueAt("Bijgewerkt", idx, 3);
                    ui.mainModel.fireTableRowsUpdated(idx, idx);
                    updateDetails();
                });
            });
        }
    }

    private void handleSave(boolean copy) {
        String[] options = controller.getLoadedFiles().keySet().toArray(new String[0]);
        if (options.length == 0) return;
        String sel = (String) JOptionPane.showInputDialog(ui, "Kies bestand:", "Export", 3, null, options, options[0]);
        if (sel != null) {
            var json = controller.getLoadedFiles().get(sel);
            controller.applyLivePrices(json);
            String formatted = controller.formatJson(json);
            if (copy) Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new StringSelection(formatted), null);
            else try { controller.save(sel, formatted); JOptionPane.showMessageDialog(ui, "Opgeslagen!"); refreshTable(); } catch(Exception e){ e.printStackTrace(); }
        }
    }

    private void openUrl() {
        int row = ui.detailTable.getSelectedRow();
        if (row != -1) {
            try { Desktop.getDesktop().browse(URI.create(ui.detailTable.getValueAt(row, 3).toString())); } catch(Exception e){}
        }
    }

    public static void main(String[] args) { 
        FlatDarkLaf.setup();
        SwingUtilities.invokeLater(WishlistAdmin::new); 
    }
}