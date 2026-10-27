package com.improvedmenus;

import java.awt.*;
import java.util.*;
import javax.swing.*;
import net.runelite.client.ui.PluginPanel;

final class ImprovedMenusPanel extends PluginPanel
{
    private final ProfileStore store;
    private final MenuEngine engine;
    private final JTextArea preview = new JTextArea(9,18);
    private final JLabel status = new JLabel(" ");
    private final javax.swing.Timer timer;
    private ItemIconEditor iconEditor;
    private final JPanel iconSection = section("Ground loot menu icons");
    private Runnable refreshIcons = () -> {};

    ImprovedMenusPanel(ProfileStore store, MenuEngine engine, Runnable openSettings)
    {
        super(true);
        this.store=store; this.engine=engine;
        setLayout(new BorderLayout(0,8));
        JPanel top = new JPanel(new net.runelite.client.ui.DynamicGridLayout(0,1,0,6));
        JPanel general=section("Improved Right-Clicks");
        general.add(help("Customize menus, icons and misclick protection."));
        button(general,"Open Plugin Settings",openSettings);
        top.add(general); top.add(Box.createVerticalStrut(10));
        top.add(iconSection); top.add(Box.createVerticalStrut(10));
        JPanel actions = section("Settings profiles");
        button(actions,"Profiles",this::profiles); button(actions,"Import/export",this::imports);
        JButton undo = new JButton("Undo"); undo.setMargin(new Insets(3,4,3,4));
        undo.addActionListener(e -> status.setText(store.undo() ? "Undone" : "Nothing to undo")); fullWidth(undo); actions.add(undo);
        top.add(actions); top.add(status);
        for (Component child : top.getComponents()) { if (child instanceof JComponent) { ((JComponent)child).setAlignmentX(Component.LEFT_ALIGNMENT); } }
        add(top,BorderLayout.NORTH);
        preview.setEditable(false); preview.setLineWrap(true); preview.setWrapStyleWord(true); preview.setText(engine.diagnostics());
        JPanel bottom = new JPanel(new BorderLayout());
        bottom.add(new JLabel("<html>Log in and open a game menu<br>to inspect its options.</html>"),BorderLayout.NORTH);
        bottom.add(new JScrollPane(preview),BorderLayout.CENTER);
        bottom.setVisible(false);
        JButton diagnostics = new JButton("Show diagnostics");
        fullWidth(diagnostics);
        diagnostics.addActionListener(e -> {
            boolean show = !bottom.isVisible();
            bottom.setVisible(show);
            diagnostics.setText(show ? "Hide diagnostics" : "Show diagnostics");
            revalidate(); repaint();
        });
        top.add(diagnostics);
        add(bottom,BorderLayout.CENTER);
        timer = new javax.swing.Timer(500,e -> { if (isShowing()) { refreshIcons.run(); if(bottom.isVisible()) { preview.setText(engine.diagnostics()); } } });
        timer.start();
    }
    void addIconControls(ItemIcons icons)
    {
        ImprovedMenusConfig config=icons.configs.getConfig(ImprovedMenusConfig.class);
        JCheckBox enabled=new JCheckBox("Show icons in Take menus"); fullWidth(enabled);
        iconSection.add(enabled);
        iconSection.add(new JLabel("Icon source"));
        JComboBox<ImprovedMenusConfig.GroundIconSource> source=new JComboBox<>(ImprovedMenusConfig.GroundIconSource.values()); fullWidth(source);
        iconSection.add(source);
        iconSection.add(help("Native: item pictures. Custom: visible imported or assigned icons only. Both: native + visible custom icons."));
        iconSection.add(help("Loot Filters setup is not linked automatically. Import the same Filterscape script here first."));
        button(iconSection,"Import Loot Filters script",() -> { openIconEditor(icons); iconEditor.importLoot(); });
        JLabel imported=help(""); iconSection.add(imported);
        button(iconSection,"Manage icons / backups",() -> openIconEditor(icons));
        iconSection.add(help("Colors stay separate. Icons are saved per RuneLite profile."));
        boolean[] syncing={false};
        refreshIcons=() -> {
            syncing[0]=true;
            enabled.setSelected(config.groundIcons());
            source.setSelectedItem(config.groundIconSource());
            syncing[0]=false;
            String text=wrapped(icons.filters.status());
            if(!text.equals(imported.getText())) { imported.setText(text); revalidate(); }
        };
        enabled.addActionListener(e -> { if(!syncing[0]) icons.configs.setConfiguration(ImprovedMenusConfig.GROUP,"groundIcons",enabled.isSelected()); });
        source.addActionListener(e -> { if(!syncing[0]) icons.configs.setConfiguration(ImprovedMenusConfig.GROUP,"groundIconSource",source.getSelectedItem()); });
        refreshIcons.run();
    }
    private void openIconEditor(ItemIcons icons) { if(iconEditor!=null) iconEditor.close(); iconEditor=new ItemIconEditor(icons); iconEditor.open(this); }
    private static JPanel section(String title)
    {
        JPanel panel=new JPanel(new net.runelite.client.ui.DynamicGridLayout(0,1,0,5));
        panel.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createTitledBorder(title),BorderFactory.createEmptyBorder(4,6,6,6)));
        panel.setAlignmentX(Component.LEFT_ALIGNMENT); return panel;
    }
    private static String wrapped(String text) { return "<html><body style='width: 130px; font-weight: normal'>"+text+"</body></html>"; }
    private static JLabel help(String text)
    {
        JLabel label=new JLabel(wrapped(text));
        label.setBorder(BorderFactory.createEmptyBorder(5,0,7,0)); label.setAlignmentX(Component.LEFT_ALIGNMENT); return label;
    }
    private static void fullWidth(JComponent component) { component.setAlignmentX(Component.LEFT_ALIGNMENT); component.setMaximumSize(new Dimension(Integer.MAX_VALUE,component.getPreferredSize().height)); }
    void stop() { timer.stop(); if(iconEditor!=null) { iconEditor.close(); } }
    private void button(JPanel parent, String name, Runnable action)
    {
        JButton b = new JButton(name); b.setMargin(new Insets(5,4,5,4)); fullWidth(b); b.addActionListener(e -> run(action)); parent.add(b);
    }
    void run(Runnable action)
    {
        try { action.run(); }
        catch (IllegalArgumentException e) { status.setText("Check settings"); JOptionPane.showMessageDialog(this,e.getMessage(),"Improved Right-Clicks",JOptionPane.ERROR_MESSAGE); }
    }
    private void profiles()
    {
        JPanel contents = new JPanel(new GridLayout(0,1,4,4));
        JTextField name = new JTextField(); JComboBox<String> saved = new JComboBox<>();
        saved.setRenderer(PlainText.renderer());
        saved.setModel(new DefaultComboBoxModel<>(store.names().toArray(new String[0])));
        contents.add(new JLabel("Save current settings as")); contents.add(name);
        button(contents,"Save profile",() -> { store.save(name.getText()); status.setText("Profile saved"); saved.setModel(new DefaultComboBoxModel<>(store.names().toArray(new String[0]))); });
        contents.add(saved);
        button(contents,"Preview and load",() -> {
            Object selected = saved.getSelectedItem();
            if (selected == null) { throw new IllegalArgumentException("Save a profile first"); }
            Map<String,String> values = store.load(selected.toString());
            if (JOptionPane.showConfirmDialog(this,describe(values),"Load profile",JOptionPane.OK_CANCEL_OPTION) == JOptionPane.OK_OPTION)
            { applySettings(values); }
        });
        JOptionPane.showMessageDialog(this,contents,"Profiles",JOptionPane.PLAIN_MESSAGE);
    }
    private void imports()
    {
        JPanel contents = new JPanel(new BorderLayout(0,6));
        JTextArea json = new JTextArea(store.exportSettings(store.snapshot()),14,24); json.setLineWrap(true);
        contents.add(new JLabel("Copy to export; paste to preview an import."),BorderLayout.NORTH);
        contents.add(new JScrollPane(json),BorderLayout.CENTER);
        JPanel controls = new JPanel(new GridLayout(0,1,4,4));
        JComboBox<String> source = new JComboBox<>(new String[]{"No Examine","No Misclicks In Bank","Item Option Hider","Menu Colorizer","Tool Required","Misclick Guard","No Use Player","Don't Telegrab NPCs","Mouseover Options+"});
        controls.add(source);
        button(controls,"Read existing settings",() -> {
            Map<String,String> values = store.legacyPreview((String)source.getSelectedItem());
            if (values.isEmpty()) { throw new IllegalArgumentException("No explicitly saved settings were found for this plugin"); }
            json.setText(store.exportSettings(values));
            status.setText("Ready to preview");
        });
        button(controls,"Preview and apply",() -> {
            Map<String,String> values = store.previewImport(json.getText());
            if (JOptionPane.showConfirmDialog(this,describe(values),"Apply these settings?",JOptionPane.OK_CANCEL_OPTION) == JOptionPane.OK_OPTION)
            { applySettings(values); }
        });
        contents.add(controls,BorderLayout.SOUTH);
        JOptionPane.showMessageDialog(this,contents,"Import / export",JOptionPane.PLAIN_MESSAGE);
    }
    void applySettings(Map<String,String> values)
    {
        Map<String,String> before = store.snapshot();
        store.apply(values);
        status.setText(before.equals(store.snapshot()) ? "No changes" : "Settings applied");
    }
    private static JScrollPane describe(Map<String,String> values)
    {
        StringBuilder text = new StringBuilder();
        values.forEach((key,value) -> text.append(SettingsCatalog.ALL.get(key).label).append(": ").append(value).append('\n'));
        JTextArea area = new JTextArea(text.toString(),14,24); area.setEditable(false); area.setLineWrap(true);
        return new JScrollPane(area);
    }
}
