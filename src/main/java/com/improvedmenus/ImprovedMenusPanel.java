package com.improvedmenus;

import java.awt.*;
import java.util.*;
import javax.swing.*;
import net.runelite.client.ui.PluginPanel;

final class ImprovedMenusPanel extends PluginPanel
{
	private final ProfileStore store;
	private final MenuSidebarSettings settings;
	private final MenuEngine engine;
	private final JTextArea preview = new JTextArea(9, 18);
	private final JLabel status = new JLabel(" ");
	private final javax.swing.Timer timer;

	ImprovedMenusPanel(ProfileStore store, MenuEngine engine, Runnable openSettings,
			java.util.function.Supplier<String> compatibilityStatus)
	{
		super(false);
		this.store = store;
		this.engine = engine;
		settings = new MenuSidebarSettings(store, this::run);
		setLayout(new BorderLayout(0, 6));
		setBorder(BorderFactory.createEmptyBorder(8, 6, 8, 6));
		setBackground(SidebarStyle.BACKGROUND);
		add(SidebarStyle.title("IMPROVED RIGHT-CLICKS"), BorderLayout.NORTH);
		JTabbedPane tabs = new JTabbedPane();
		JPanel menus = SidebarStyle.column();
		settings.style(menus);
		JPanel themes = SidebarStyle.section(menus, "Right Click Styler", false);
		JButton styler = new JButton("Get Right Click Styler…");
		styler.setToolTipText("For editable menu colors and themes. Your saved colors are retained.");
		SidebarStyle.control(styler);
		styler.addActionListener(e -> net.runelite.client.util.LinkBrowser.browse(
				"https://runelite.net/plugin-hub/show/right-click-styler"));
		themes.add(styler);
		button(themes, "Export saved action colors…", this::exportColours);
		JPanel profiles = section("Settings profiles");
		button(profiles, "Saved profiles…", this::profiles);
		button(profiles, "Import / export…", this::imports);
		button(profiles, "Undo last change", () -> status.setText(store.undo() ? "Undone" : "Nothing to undo"));
		JPanel tools = section("Tools");
		preview.setEditable(false);
		preview.setLineWrap(true);
		preview.setWrapStyleWord(true);
		JPanel diagnostics = new JPanel(new BorderLayout(0, 6));
		diagnostics.add(help("Open a game menu to inspect its options."), BorderLayout.NORTH);
		diagnostics.add(new JScrollPane(preview), BorderLayout.CENTER);
		diagnostics.setVisible(false);
		JButton show = new JButton("▶ Diagnostics…");
		fullWidth(show);
		tools.add(show);
		tools.add(diagnostics);
		Runnable refresh = () -> preview.setText(engine.diagnostics() + "\n\n" + compatibilityStatus.get());
		show.addActionListener(e ->
		{
			boolean visible = !diagnostics.isVisible();
			diagnostics.setVisible(visible);
			show.setText((visible ? "▼ " : "▶ ") + "Diagnostics…");
			if (visible)
				refresh.run();
			tools.revalidate();
		});
		JPanel general = SidebarStyle.column();
		SidebarStyle.section(general, "Saved profiles", profiles, true);
		SidebarStyle.section(general, "More settings", tools, false);
		button(tools, "All plugin settings…", openSettings);
		tabs.addTab("Icons", scroll(settings.icons()));
		tabs.addTab("Actions", scroll(settings.actions()));
		tabs.addTab("Style", scroll(menus));
		tabs.addTab("General", scroll(general));
		SidebarStyle.tabs(tabs);
		settings.refresh();
		add(tabs, BorderLayout.CENTER);
		add(status, BorderLayout.SOUTH);
		timer = new javax.swing.Timer(500, e ->
		{
			if (isShowing())
			{
				settings.refresh();
				if (tabs.getSelectedIndex() == 3 && diagnostics.isVisible())
					refresh.run();
			}
		});
		timer.start();
	}

	private static JScrollPane scroll(JPanel contents)
	{
		return SidebarStyle.scroll(contents);
	}

	private static JPanel section(String title)
	{
		return SidebarStyle.column();
	}

	private static String wrapped(String text)
	{
		return "<html><body style='width: 130px; font-weight: normal'>"
				+ text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;") + "</body></html>";
	}

	private static JLabel help(String text)
	{
		JLabel label = new JLabel(wrapped(text));
		label.setBorder(BorderFactory.createEmptyBorder(5, 0, 7, 0));
		label.setAlignmentX(Component.LEFT_ALIGNMENT);
		return label;
	}

	private static void fullWidth(JComponent component)
	{
		component.setAlignmentX(Component.LEFT_ALIGNMENT);
		component.setMaximumSize(new Dimension(Integer.MAX_VALUE, component.getPreferredSize().height));
	}

	@Override
	public void onActivate()
	{
		settings.refresh();
	}

	void stop()
	{
		timer.stop();
	}

	private void button(JPanel parent, String name, Runnable action)
	{
		JButton b = new JButton(name);
		SidebarStyle.control(b);
		b.setMargin(new Insets(5, 4, 5, 4));
		fullWidth(b);
		b.addActionListener(e -> run(action));
		parent.add(b);
	}

	void run(Runnable action)
	{
		try
		{
			action.run();
		}
		catch (IllegalArgumentException e)
		{
			status.setText("Check settings");
			JOptionPane.showMessageDialog(this, e.getMessage(), "Improved Right-Clicks", JOptionPane.ERROR_MESSAGE);
		}
	}

	private void exportColours()
	{
		SavedColourExport export = new SavedColourExport(store.value(SettingsCatalog.ALL.get("colors")));
		JPanel contents = new JPanel(new BorderLayout(0, 8));
		contents.add(help("Copy these into Right Click Styler's action highlights. Review its existing rules before pasting. Your saved colours stay unchanged."), BorderLayout.NORTH);
		JTextArea rules = new JTextArea(export.rules, 8, 24);
		rules.setEditable(false);
		contents.add(new JScrollPane(rules), BorderLayout.CENTER);
		if (!export.retained.isEmpty())
		{
			JPanel retained = new JPanel(new BorderLayout());
			retained.add(help("Not exported (target/wildcard rules, conflicting colours, or Styler limits). These remain saved:"), BorderLayout.NORTH);
			JTextArea skipped = new JTextArea(export.retained, 5, 24);
			skipped.setEditable(false);
			retained.add(new JScrollPane(skipped), BorderLayout.CENTER);
			contents.add(retained, BorderLayout.SOUTH);
		}
		JOptionPane.showMessageDialog(this, contents, "Export saved colours", JOptionPane.PLAIN_MESSAGE);
	}

	private void profiles()
	{
		JPanel contents = new JPanel(new GridLayout(0, 1, 4, 4));
		JTextField name = new JTextField();
		JComboBox<String> saved = new JComboBox<>();
		saved.setRenderer(PlainText.renderer());
		saved.setModel(new DefaultComboBoxModel<>(store.names().toArray(new String[0])));
		contents.add(new JLabel("Save current settings as"));
		contents.add(name);
		button(contents, "Save profile", () ->
		{
			store.save(name.getText());
			status.setText("Profile saved");
			saved.setModel(new DefaultComboBoxModel<>(store.names().toArray(new String[0])));
		});
		contents.add(saved);
		button(contents, "Preview and load", () ->
		{
			Object selected = saved.getSelectedItem();
			if (selected == null)
			{
				throw new IllegalArgumentException("Save a profile first");
			}
			Map<String, String> values = store.load(selected.toString());
			if (JOptionPane.showConfirmDialog(this, describe(values), "Load profile",
					JOptionPane.OK_CANCEL_OPTION) == JOptionPane.OK_OPTION)
			{
				applySettings(values);
			}
		});
		JOptionPane.showMessageDialog(this, contents, "Profiles", JOptionPane.PLAIN_MESSAGE);
	}

	private void imports()
	{
		JPanel contents = new JPanel(new BorderLayout(0, 6));
		JTextArea json = new JTextArea(store.exportSettings(store.snapshot()), 14, 24);
		json.setLineWrap(true);
		contents.add(new JLabel("Copy to export; paste to preview an import."), BorderLayout.NORTH);
		contents.add(new JScrollPane(json), BorderLayout.CENTER);
		JPanel controls = new JPanel(new GridLayout(0, 1, 4, 4));
		JComboBox<String> source = new JComboBox<>(new String[] { "No Examine", "No Misclicks In Bank",
				"Item Option Hider", "Tool Required", "Misclick Guard", "No Use Player",
				"Don't Telegrab NPCs", "Mouseover Options+" });
		controls.add(source);
		button(controls, "Read existing settings", () ->
		{
			Map<String, String> values = store.legacyPreview((String) source.getSelectedItem());
			if (values.isEmpty())
			{
				throw new IllegalArgumentException("No explicitly saved settings were found for this plugin");
			}
			json.setText(store.exportSettings(values));
			status.setText("Ready to preview");
		});
		button(controls, "Preview and apply", () ->
		{
			Map<String, String> values = store.previewImport(json.getText());
			if (JOptionPane.showConfirmDialog(this, describe(values), "Apply these settings?",
					JOptionPane.OK_CANCEL_OPTION) == JOptionPane.OK_OPTION)
			{
				applySettings(values);
			}
		});
		contents.add(controls, BorderLayout.SOUTH);
		JOptionPane.showMessageDialog(this, contents, "Import / export", JOptionPane.PLAIN_MESSAGE);
	}

	void applySettings(Map<String, String> values)
	{
		Map<String, String> before = store.snapshot();
		store.apply(values);
		status.setText(before.equals(store.snapshot()) ? "No changes" : "Settings applied");
	}

	private static JScrollPane describe(Map<String, String> values)
	{
		StringBuilder text = new StringBuilder();
		values.forEach((key, value) -> text.append(SettingsCatalog.ALL.get(key).label).append(": ").append(value)
				.append('\n'));
		JTextArea area = new JTextArea(text.toString(), 14, 24);
		area.setEditable(false);
		area.setLineWrap(true);
		return new JScrollPane(area);
	}
}
