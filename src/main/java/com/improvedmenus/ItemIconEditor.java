package com.improvedmenus;

import java.awt.*;
import java.io.*;
import java.util.*;
import java.util.List;
import javax.swing.*;
import net.runelite.client.util.Filepath;

final class ItemIconEditor
{
    private final ItemIcons icons;
    private final DefaultListModel<ItemIcons.Assignment> model=new DefaultListModel<>();
    private final JList<ItemIcons.Assignment> list=new JList<>(model);
    private JPanel panel;
    private final JLabel filterStatus=new JLabel();
    private JDialog dialog;
    private boolean closed;
    void close() { closed=true; if(dialog!=null) { dialog.dispose(); } }
    ItemIconEditor(ItemIcons icons) { this.icons=icons; }
    void open(Component owner)
    {
        closed=false;
        list.setCellRenderer(PlainText.renderer());
        panel=new JPanel(new BorderLayout(8,8));
        panel.setBorder(BorderFactory.createEmptyBorder(10,10,10,10));
        JPanel heading=new JPanel(new GridLayout(0,1,0,5));
        heading.add(new JLabel("<html>Import the same Filterscape script used by Loot Filters; it is not linked automatically.<br>Enable menu icons and choose Custom or Both in the side panel. Colors stay separate.</html>"));
        heading.add(filterStatus); panel.add(heading,BorderLayout.NORTH);
        heading.add(new JLabel("Manual overrides (imported rules are separate from this list)"));
        panel.add(new JScrollPane(list),BorderLayout.CENTER);
        JPanel buttons=new JPanel(new GridLayout(0,2,6,6));
        button(buttons,"Assign icon",this::assign);
        button(buttons,"Delete selected",() -> { List<ItemIcons.Assignment> next=icons.snapshot(); next.removeAll(list.getSelectedValuesList()); icons.save(next); refresh(); });
        button(buttons,"Import Loot Filters",this::importLoot);
        button(buttons,"Import/export icons",this::exchange);
        button(buttons,"Filter setup / backup",this::filterBackup);
        button(buttons,"Undo filter import",() -> { icons.filters.undo(); refresh(); JOptionPane.showMessageDialog(panel,icons.filters.status()); });
        button(buttons,"Undo / redo",() -> { icons.undo(); refresh(); });
        panel.add(buttons,BorderLayout.SOUTH);
        dialog=new JDialog(SwingUtilities.getWindowAncestor(owner),"Item icons",Dialog.ModalityType.MODELESS);
        dialog.addWindowListener(new java.awt.event.WindowAdapter() { @Override public void windowClosed(java.awt.event.WindowEvent e) { closed=true; } });
        dialog.setContentPane(panel); dialog.setSize(620,480); dialog.setMinimumSize(new Dimension(480,320));
        dialog.setLocationRelativeTo(owner); dialog.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE); refresh(); dialog.setVisible(true);
    }
    private void refresh() { if(icons.filters!=null) filterStatus.setText(icons.filters.status()); model.clear(); for(ItemIcons.Assignment a:icons.snapshot()) { model.addElement(a); } }
    private void button(JPanel parent,String label,Runnable action)
    {
        JButton b=new JButton(label); b.addActionListener(e -> { try { action.run(); } catch(IllegalArgumentException ex) { JOptionPane.showMessageDialog(panel,ex.getMessage(),"Item icons",JOptionPane.WARNING_MESSAGE); } }); parent.add(b);
    }
    private Object profile() { return icons.configs.getProfile(); }
    private void checkProfile(Object original) { if(closed) throw new IllegalArgumentException("Icon editor was closed."); if(!Objects.equals(original,profile())) { refresh(); throw new IllegalArgumentException("RuneLite profile changed. Reopen this action."); } }
    private void assign()
    {
        Object original=profile();
        JTextField name=new JTextField();
        JComboBox<ItemIcons.Kind> kind=new JComboBox<>(new ItemIcons.Kind[]{ItemIcons.Kind.HEART,ItemIcons.Kind.STAR,ItemIcons.Kind.DIAMOND,ItemIcons.Kind.CURRENT_ITEM,ItemIcons.Kind.IMAGE});
        JPanel form=new JPanel(new GridLayout(0,1,5,5));
        form.add(new JLabel("Exact item name (including untradeable items)")); form.add(name);
        JButton search=new JButton("Search tradeable items"); form.add(search);
        search.addActionListener(e -> {
            String term=name.getText().trim(); if(term.length()<2) { return; }
            java.util.List<net.runelite.http.api.item.ItemPrice> found=icons.items.search(term);
            String[] names=found.stream().map(net.runelite.http.api.item.ItemPrice::getName).distinct().limit(100).toArray(String[]::new);
            if(names.length==0) { JOptionPane.showMessageDialog(form,"No results. You can enter an exact name instead."); return; }
            Object selected=JOptionPane.showInputDialog(form,"Choose item","Item search",JOptionPane.PLAIN_MESSAGE,null,names,names[0]);
            if(selected!=null) { name.setText(selected.toString()); }
        });
        form.add(new JLabel("Icon")); form.add(kind);
        if(JOptionPane.showConfirmDialog(panel,form,"Assign item icon",JOptionPane.OK_CANCEL_OPTION)!=JOptionPane.OK_OPTION) { return; }
        checkProfile(original);
        ItemIcons.Assignment a=new ItemIcons.Assignment(); a.name=name.getText().trim(); a.kind=(ItemIcons.Kind)kind.getSelectedItem();
        if(a.kind==ItemIcons.Kind.IMAGE) {
            List<Filepath> files=new Filepath.Chooser().setIsOpen().setAcceptsFiles().addExtensionFilter("Images","png","jpg","jpeg","gif").showDialog(panel);
            if(files.isEmpty()) return;
            try { a.image=IconImages.read(files.get(0)); } catch(IOException e) { throw new IllegalArgumentException("Could not read image."); }
            checkProfile(original);
        }
        List<ItemIcons.Assignment> next=icons.snapshot(); next.removeIf(old -> old.key().equals(a.key())); next.add(a); icons.save(next); refresh();
    }
    private <T> void background(java.util.concurrent.Callable<T> work,java.util.function.Consumer<T> done)
    {
        new SwingWorker<T,Void>() {
            protected T doInBackground() throws Exception { return work.call(); }
            protected void done() {
                if(closed) return;
                try { done.accept(get()); }
                catch(Exception e) { Throwable cause=e.getCause()==null ? e : e.getCause(); JOptionPane.showMessageDialog(panel,cause.getMessage(),"Import not applied",JOptionPane.WARNING_MESSAGE); }
            }
        }.execute();
    }
    private static String readFilter(Filepath file) throws IOException
    {
        try(java.io.InputStream in=file.openInputStream()) {
            byte[] data=in.readNBytes(1000001); if(data.length>1000000) throw new IllegalArgumentException("Filter exceeds 1 MB.");
            return new String(data,java.nio.charset.StandardCharsets.UTF_8);
        }
    }
    void importLoot()
    {
        Object original=profile();
        Object[] choices={"Choose .rs2f file","Paste Filterscape text","Cancel"};
        int choice=JOptionPane.showOptionDialog(panel,"Import icon rules from a Filterscape export or Loot Filters script.\nThe original files and settings remain untouched.","Import Loot Filters",JOptionPane.DEFAULT_OPTION,JOptionPane.PLAIN_MESSAGE,null,choices,choices[0]);
        if(choice==0) {
            List<Filepath> files=new Filepath.Chooser().setIsOpen().setAcceptsFiles().addExtensionFilter("Loot Filters","rs2f","txt").showDialog(panel);
            if(files.isEmpty()) return;
            background(() -> readFilter(files.get(0)),source -> compileImport(source,original));
        } else if(choice==1) {
            JTextArea text=new JTextArea(18,48);
            if(JOptionPane.showConfirmDialog(panel,new JScrollPane(text),"Paste generated filter",JOptionPane.OK_CANCEL_OPTION)==JOptionPane.OK_OPTION) compileImport(text.getText(),original);
        }
    }
    private void compileImport(String source,Object original)
    {
        checkProfile(original);
        background(() -> com.improvedmenus.rs2f.LootFilter.compile(source),program -> {
            checkProfile(original);
            FilterIcons.Bundle bundle=new FilterIcons.Bundle(); bundle.compressedSource=FilterIcons.compress(source);
            Set<String> files=program.files();
            if(files.size()>128) throw new IllegalArgumentException("Use at most 128 custom images.");
            for(String file:files) IconImages.safeName(file);
            if(files.isEmpty()) { background(() -> icons.filters.prepare(bundle),ready -> previewFilter(ready,original)); return; }
            JOptionPane.showMessageDialog(panel,"This filter references "+files.size()+" images. Select the Loot Filters icons folder (usually .runelite/loot-filters/icons).\nImages will be embedded in our backup; originals stay untouched.\nAll referenced images must be present before import can apply.");
            List<Filepath> folders=new Filepath.Chooser().setIsOpen().setAcceptsDirectories().showDialog(panel);
            if(folders.isEmpty()) return;
            background(() -> {
                for(String file:files) {
                    Filepath path=folders.get(0);
                    for(String segment:IconImages.safeName(file).split("/")) path=path.joinSegment(segment);
                    bundle.images.put(file,IconImages.read(path));
                }
                return icons.filters.prepare(bundle);
            },ready -> previewFilter(ready,original));
        });
    }
    private void previewFilter(FilterIcons.Prepared ready,Object original)
    {
        checkProfile(original);
        String message=ready.program.rules.size()+" rules and "+ready.images.size()+" images ready.\n\nConditions, macros, rule order and inherited icons are preserved.\nOnly menu icons are evaluated. Colors, hiding, sound and loot beams stay separate.\nManual item assignments take priority. This replaces the previously imported filter; Undo filter import restores it.";
        if(JOptionPane.showConfirmDialog(panel,message,"Review filter import",JOptionPane.OK_CANCEL_OPTION)!=JOptionPane.OK_OPTION) return;
        checkProfile(original); icons.filters.save(ready); refresh(); JOptionPane.showMessageDialog(panel,"Imported. Select Custom or Both under Presentation to show these icons.");
    }
    private void filterBackup()
    {
        Object original=profile();
        Object[] options={"Copy / paste backup","Disable imported filter","Cancel"};
        int action=JOptionPane.showOptionDialog(panel,icons.filters.status(),"Imported filter",JOptionPane.DEFAULT_OPTION,JOptionPane.PLAIN_MESSAGE,null,options,options[0]);
        if(action==1) { checkProfile(original); icons.filters.save(null); refresh(); return; }
        if(action!=0) return;
        JTextArea text=new JTextArea(icons.filters.export(),18,48); text.setLineWrap(true);
        if(JOptionPane.showConfirmDialog(panel,new JScrollPane(text),"Copy to export; paste a filter setup to import",JOptionPane.OK_CANCEL_OPTION)!=JOptionPane.OK_OPTION) return;
        String raw=text.getText(); background(() -> icons.filters.parse(raw),ready -> previewFilter(ready,original));
    }
    private void preview(List<ItemIcons.Assignment> imported,Object original)
    {
        JTextArea text=new JTextArea(14,42); text.setEditable(false);
        text.append("ICON ASSIGNMENTS ONLY. Filter colors, sounds and hiding rules are not imported.\n\n");
        for(ItemIcons.Assignment a:imported) { text.append(a.toString()+"\n"); }
        Object[] actions={"Merge (keep existing)","Replace all","Cancel"};
        int choice=JOptionPane.showOptionDialog(panel,new JScrollPane(text),"Review icon import",JOptionPane.DEFAULT_OPTION,JOptionPane.PLAIN_MESSAGE,null,actions,actions[0]);
        if(choice!=0 && choice!=1) { return; }
        checkProfile(original);
        LinkedHashMap<String,ItemIcons.Assignment> next=new LinkedHashMap<>();
        if(choice==0) { for(ItemIcons.Assignment a:icons.snapshot()) { next.put(a.key(),a); } }
        for(ItemIcons.Assignment a:imported) { next.putIfAbsent(a.key(),a); }
        icons.save(new ArrayList<>(next.values())); refresh();
    }
    private void exchange()
    {
        Object original=profile(); JTextArea json=new JTextArea(icons.export(),16,42); json.setLineWrap(true);
        if(JOptionPane.showConfirmDialog(panel,new JScrollPane(json),"Copy to export; paste to import",JOptionPane.OK_CANCEL_OPTION)!=JOptionPane.OK_OPTION) { return; }
        checkProfile(original); preview(icons.parse(json.getText()),original);
    }
}
