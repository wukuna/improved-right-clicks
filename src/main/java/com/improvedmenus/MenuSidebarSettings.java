package com.improvedmenus;

import java.awt.Component;
import java.awt.Container;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import javax.swing.*;

final class MenuSidebarSettings
{
	private final ProfileStore store;
	private final java.util.function.Consumer<Runnable> run;
	private final List<Runnable> updates = new ArrayList<>();
	private boolean refreshing;
	private long profile;

	MenuSidebarSettings(ProfileStore store, java.util.function.Consumer<Runnable> run)
	{
		this.store = store;
		this.run = run;
	}

	JPanel icons()
	{
		JPanel page = SidebarStyle.column();
		group(page, "Item icons", true, "groundIcons", "groundIconSource", "customIconProvider",
				"bothRespectFilterVisibility", "lootFiltersCompatibility", "useLootFiltersInMenu");
		dependent(page, "groundIconSource", "groundIcons");
		dependent(page, "customIconProvider", "groundIcons");
		dependent(page, "bothRespectFilterVisibility", "groundIcons");
		group(page, "Player badges", false, "playerAccountBadge", "playerFriendBadge", "playerIgnoreBadge",
				"playerFriendsChatBadge", "playerPartyBadge", "playerCopyName");
		return page;
	}

	JPanel actions()
	{
		JPanel page = SidebarStyle.column();
		group(page, "Tool requirements", true, "chop", "mine");
		group(page, "Menu cleanup", false, "potionEmpty", "hideCancel", "hideWalkHere", "hideSetHeading",
				"groupGroundItems", "groundExamineLast", "house", "mahogany");
		group(page, "Examine", false, "examineInventory", "examineGround", "examineNpc", "examineObject",
				"keepLoneNpc", "keepLoneObject");
		group(page, "Bank actions", false, "bankWield", "bankWear", "bankDrink", "bankEat");
		group(page, "Protected buttons", false, "autoRetaliate", "xpDrops", "alwaysSetPlaceholders", "report",
				"deprioritizeLeftClickOffEntries");
		group(page, "Target protection", false, "noUsePlayer", "playerUseItems", "noTelegrabNpc", "telegrabNpcNames");
		group(page, "More settings", false, "revealKey", "itemRules", "exceptions");
		group(page, "Beta features", false, "autoRetaliateRequireShift");
		return page;
	}

	void style(JPanel page)
	{
		group(page, "Saved styling", true, "enableColors", "followMenuStyle");
		group(page, "+N options", false, "optionCount", "minimumOptions", "countExamine", "countOnWidgets",
				"countHidePlayers", "countHideWalkHere", "countAnchor");
		for (String key : new String[] { "minimumOptions", "countExamine", "countOnWidgets",
				"countHidePlayers", "countHideWalkHere", "countAnchor" })
			dependent(page, key, "optionCount");
	}

	void refresh()
	{
		refreshing = true;
		try
		{
			updates.forEach(Runnable::run);
			profile = store.profileId();
		}
		finally
		{
			refreshing = false;
		}
	}

	private void group(JPanel page, String name, boolean expanded, String... keys)
	{
		JPanel body = SidebarStyle.section(page, name, expanded);
		for (String key : keys)
		{
			SettingsCatalog.Setting setting = SettingsCatalog.ALL.get(key);
			if (setting == null)
				continue;
			if (setting.choices.length == 2 && setting.choices[0].equals("false"))
			{
				JCheckBox control = new JCheckBox(setting.label);
				control.setName(key);
				SidebarStyle.control(control);
				body.add(control);
				updates.add(() -> control.setSelected(Boolean.parseBoolean(store.value(setting))));
				control.addActionListener(event -> save(key, Boolean.toString(control.isSelected())));
			}
			else if (setting.choices.length > 0)
			{
				JComboBox<String> control = new JComboBox<>(setting.choices);
				control.setName(key);
				SidebarStyle.row(body, setting.label, control);
				updates.add(() -> control.setSelectedItem(store.value(setting)));
				control.addActionListener(event -> save(key, (String) control.getSelectedItem()));
			}
			else if (key.equals("minimumOptions"))
			{
				JSpinner control = new JSpinner(new SpinnerNumberModel(1, 1, 500, 1));
				control.setName(key);
				SidebarStyle.row(body, setting.label, control);
				updates.add(() ->
				{
					if (!control.isFocusOwner() && !((JSpinner.DefaultEditor) control.getEditor()).getTextField().isFocusOwner())
						control.setValue(Integer.parseInt(store.value(setting)));
				});
				control.addChangeListener(event -> save(key, control.getValue().toString()));
			}
			else
			{
				javax.swing.text.JTextComponent control = key.equals("itemRules") || key.equals("exceptions")
						? new JTextArea(store.value(setting), 3, 12) : new JTextField(store.value(setting));
				control.setName(key);
				control.setToolTipText(control instanceof JTextField ? "Press Enter or leave the field to save."
						: "One rule per line. Leave the field to save.");
				if (control instanceof JTextArea)
				{
					SidebarStyle.control(control);
					JScrollPane scroll = new JScrollPane(control);
					SidebarStyle.row(body, setting.label, scroll);
					scroll.setPreferredSize(new java.awt.Dimension(180, 75));
					scroll.setMaximumSize(new java.awt.Dimension(Integer.MAX_VALUE, 75));
				}
				else
					SidebarStyle.row(body, setting.label, control);
				long[] owner = { store.profileId() };
				String[] before = { store.value(setting) };
				Runnable commit = () ->
				{
					if (!control.getText().equals(before[0]) && owner[0] == store.profileId()
							&& before[0].equals(store.value(setting)))
						save(key, control.getText());
					before[0] = store.value(setting);
				};
				if (control instanceof JTextField)
					((JTextField) control).addActionListener(event -> commit.run());
				control.addFocusListener(new FocusAdapter()
				{
					public void focusGained(FocusEvent event)
					{
						owner[0] = store.profileId();
						before[0] = store.value(setting);
					}

					public void focusLost(FocusEvent event)
					{
						commit.run();
						refresh();
					}
				});
				updates.add(() ->
				{
					if (!control.isFocusOwner())
						control.setText(store.value(setting));
				});
			}
		}
	}

	private void dependent(Container parent, String key, String enabledKey)
	{
		for (Component child : parent.getComponents())
		{
			if (key.equals(child.getName()))
			{
				Component row = child instanceof JCheckBox ? child : child.getParent();
				updates.add(() -> row.setVisible(Boolean.parseBoolean(store.value(SettingsCatalog.ALL.get(enabledKey)))));
				return;
			}
			if (child instanceof Container)
				dependent((Container) child, key, enabledKey);
		}
	}

	private void save(String key, String value)
	{
		if (!refreshing && profile == store.profileId())
		{
			run.accept(() -> store.apply(Collections.singletonMap(key, value)));
			refresh();
		}
	}
}
