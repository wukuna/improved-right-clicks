package com.improvedmenus;

import java.awt.*;
import javax.swing.*;
import javax.swing.plaf.basic.BasicArrowButton;
import javax.swing.plaf.basic.BasicButtonUI;
import javax.swing.plaf.basic.BasicComboBoxUI;
import javax.swing.plaf.basic.BasicSpinnerUI;
import javax.swing.plaf.basic.BasicTabbedPaneUI;
import javax.swing.text.JTextComponent;
import net.runelite.client.ui.components.ColorJButton;

final class SidebarStyle
{
	static final Color BACKGROUND = new Color(31, 34, 39);
	static final Color CARD = new Color(41, 45, 52);
	static final Color HEADER = new Color(49, 54, 62);
	static final Color CONTROL = new Color(27, 30, 35);
	static final Color BORDER = new Color(68, 74, 84);
	static final Color TEXT = new Color(232, 235, 240);
	static final Color ACCENT = new Color(61, 181, 218);
	static final Font FONT = new Font(Font.SANS_SERIF, Font.PLAIN, 12);

	private SidebarStyle()
	{
	}

	static void control(JComponent component)
	{

		if (component instanceof JTabbedPane)
		{
			tabs((JTabbedPane) component);
			return;
		}
		component.setFont(FONT);
		component.setForeground(TEXT);
		if (component instanceof JPanel && component.isOpaque())
			component.setBackground(CARD);
		if (component instanceof ColorJButton)
			return;
		if (component instanceof JComboBox)
		{
			JComboBox<?> combo = (JComboBox<?>) component;
			combo.setUI(new BasicComboBoxUI()
			{
				@Override
				protected JButton createArrowButton()
				{
					return arrow(SwingConstants.SOUTH);
				}

				@Override
				public void paintCurrentValueBackground(Graphics g, Rectangle bounds, boolean focus)
				{
					g.setColor(CONTROL);
					g.fillRect(bounds.x, bounds.y, bounds.width, bounds.height);
				}

				@Override
				public void paintCurrentValue(Graphics g, Rectangle bounds, boolean focus)
				{
					JLabel value = new JLabel(display(comboBox.getSelectedItem()));
					value.putClientProperty("html.disable", Boolean.TRUE);
					value.setFont(FONT);
					value.setForeground(TEXT);
					value.setBorder(BorderFactory.createEmptyBorder(0, 6, 0, 0));
					currentValuePane.paintComponent(g, value, comboBox, bounds.x, bounds.y, bounds.width,
							bounds.height);
				}
			});
			combo.setBackground(CONTROL);
			combo.setBorder(BorderFactory.createLineBorder(BORDER));
			combo.setPreferredSize(new Dimension(100, 28));
			combo.setMinimumSize(new Dimension(0, 28));
			return;
		}
		if (component instanceof JSpinner)
		{
			JSpinner spinner = (JSpinner) component;
			spinner.setUI(new BasicSpinnerUI()
			{
				@Override
				protected Component createNextButton()
				{
					JButton button = arrow(SwingConstants.NORTH);
					installNextButtonListeners(button);
					return button;
				}

				@Override
				protected Component createPreviousButton()
				{
					JButton button = arrow(SwingConstants.SOUTH);
					installPreviousButtonListeners(button);
					return button;
				}
			});
			spinner.setBorder(BorderFactory.createLineBorder(BORDER));
			spinner.setPreferredSize(new Dimension(100, 26));
		}
		else if (component instanceof JCheckBox)
		{
			JCheckBox check = (JCheckBox) component;
			check.setOpaque(false);
			check.setIcon(new Icon()
			{
				public int getIconWidth()
				{
					return 16;
				}

				public int getIconHeight()
				{
					return 16;
				}

				public void paintIcon(Component c, Graphics g, int x, int y)
				{
					g.setColor(CONTROL);
					g.fillRect(x, y, 15, 15);
					g.setColor(check.isSelected() ? ACCENT : BORDER);
					g.drawRect(x, y, 15, 15);
					if (check.isSelected())
					{
						Graphics2D copy = (Graphics2D) g.create();
						copy.setStroke(new BasicStroke(2));
						copy.drawPolyline(new int[] { x + 3, x + 6, x + 12 }, new int[] { y + 8, y + 11, y + 4 }, 3);
						copy.dispose();
					}
				}
			});
			check.setBorder(BorderFactory.createEmptyBorder(5, 0, 5, 0));
		}
		else if (component instanceof AbstractButton)
		{
			AbstractButton button = (AbstractButton) component;
			button.setUI(new BasicButtonUI());
			button.setBackground(HEADER);
			button.setOpaque(true);
			button.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(BORDER),
					BorderFactory.createEmptyBorder(5, 6, 5, 6)));
			button.setFocusPainted(false);
		}
		else if (component instanceof JTextComponent)
		{
			component.setBackground(CONTROL);
			((JTextComponent) component).setCaretColor(TEXT);
			component.setBorder(BorderFactory.createEmptyBorder(4, 5, 4, 5));
		}
		for (Component child : component.getComponents())
		{
			if (child instanceof JComponent)
				control((JComponent) child);
		}
	}

	private static String display(Object value)
	{
		String text = String.valueOf(value);
		if (!text.matches("[A-Z][A-Z_]+"))
			return text;
		String lower = text.toLowerCase(java.util.Locale.ROOT).replace('_', ' ');
		return Character.toUpperCase(lower.charAt(0)) + lower.substring(1);
	}

	private static JButton arrow(int direction)
	{
		JButton button = new BasicArrowButton(direction, HEADER, HEADER, TEXT, HEADER);
		button.setBorder(BorderFactory.createEmptyBorder());
		button.setPreferredSize(new Dimension(18, 12));
		return button;
	}

	static void tabs(JTabbedPane tabs)
	{
		tabs.setFont(FONT.deriveFont(11f));
		tabs.setForeground(TEXT);
		tabs.setBackground(BACKGROUND);
		tabs.setBorder(BorderFactory.createEmptyBorder());
		tabs.setTabLayoutPolicy(JTabbedPane.SCROLL_TAB_LAYOUT);
		tabs.setUI(new BasicTabbedPaneUI()
		{
			@Override
			protected void installDefaults()
			{
				super.installDefaults();
				tabInsets = new Insets(7, 3, 7, 3);
				selectedTabPadInsets = new Insets(0, 0, 0, 0);
				tabAreaInsets = new Insets(0, 0, 5, 0);
				contentBorderInsets = new Insets(0, 0, 0, 0);
			}

			@Override
			protected void paintTabBackground(Graphics g, int placement, int index, int x, int y, int w, int h,
					boolean selected)
			{
				g.setColor(selected ? HEADER : BACKGROUND);
				g.fillRect(x, y, w, h);
			}

			@Override
			protected void paintTabBorder(Graphics g, int placement, int index, int x, int y, int w, int h,
					boolean selected)
			{
				g.setColor(selected ? ACCENT : BORDER);
				g.fillRect(x, y + h - 2, w, 2);
			}

			@Override
			protected void paintContentBorder(Graphics g, int placement, int selected)
			{
			}

			@Override
			protected void paintFocusIndicator(Graphics g, int placement, Rectangle[] rects, int index, Rectangle icon,
					Rectangle text, boolean selected)
			{
			}
		});
	}

	static JScrollPane scroll(JPanel content)
	{
		JPanel page = new Page();
		page.setLayout(new BorderLayout());
		page.setBackground(BACKGROUND);
		page.add(content, BorderLayout.NORTH);
		JScrollPane scroll = new JScrollPane(page);
		scroll.setBorder(BorderFactory.createEmptyBorder());
		scroll.getViewport().setBackground(BACKGROUND);
		scroll.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
		scroll.getVerticalScrollBar().setUnitIncrement(20);
		return scroll;
	}

	private static final class Page extends JPanel implements Scrollable
	{
		public Dimension getPreferredScrollableViewportSize()
		{
			return getPreferredSize();
		}

		public boolean getScrollableTracksViewportWidth()
		{
			return true;
		}

		public boolean getScrollableTracksViewportHeight()
		{
			return false;
		}

		public int getScrollableUnitIncrement(Rectangle r, int axis, int direction)
		{
			return 20;
		}

		public int getScrollableBlockIncrement(Rectangle r, int axis, int direction)
		{
			return Math.max(20, r.height - 20);
		}
	}

	static JPanel column()
	{
		JPanel panel = new JPanel();
		panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
		panel.setBackground(BACKGROUND);
		panel.setAlignmentX(0);
		return panel;
	}

	static JLabel title(String text)
	{
		JLabel label = new JLabel(text);
		label.setFont(FONT.deriveFont(Font.BOLD, 13));
		label.setForeground(TEXT);
		label.setBorder(BorderFactory.createEmptyBorder(4, 0, 8, 0));
		return label;
	}

	static JPanel section(JPanel parent, String title, JPanel body, boolean expanded)
	{
		JPanel group = column();
		group.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createEmptyBorder(0, 0, 6, 0),
				BorderFactory.createLineBorder(BORDER)));
		JButton header = new JButton((expanded ? "▾ " : "▸ ") + title);
		control(header);
		header.setName("expand:" + title);
		header.setHorizontalAlignment(SwingConstants.LEFT);
		header.setForeground(ACCENT);
		header.setFont(FONT.deriveFont(Font.BOLD));
		header.setBorder(BorderFactory.createEmptyBorder(7, 8, 7, 8));
		header.setAlignmentX(0);
		header.setMaximumSize(new Dimension(Integer.MAX_VALUE, 32));
		body.setName("section:" + title);
		body.setAlignmentX(0);
		body.setBackground(CARD);
		body.setBorder(BorderFactory.createEmptyBorder(4, 8, 8, 8));
		body.setVisible(expanded);
		header.addActionListener(event ->
		{
			boolean open = !body.isVisible();
			body.setVisible(open);
			header.setText((open ? "▾ " : "▸ ") + title);
			group.revalidate();
			group.repaint();
		});
		group.add(header);
		group.add(body);
		parent.add(group);
		return body;
	}

	static JPanel section(JPanel parent, String title, boolean expanded)
	{
		return section(parent, title, column(), expanded);
	}

	static void row(JPanel parent, String title, JComponent editor)
	{
		JPanel row = column();
		row.setOpaque(false);
		row.setBorder(BorderFactory.createEmptyBorder(3, 0, 5, 0));
		JLabel label = new JLabel(title);
		label.setForeground(TEXT);
		label.setLabelFor(editor);
		label.setAlignmentX(0);
		label.setBorder(BorderFactory.createEmptyBorder(0, 0, 4, 0));
		control(editor);
		editor.setAlignmentX(0);
		editor.setMaximumSize(new Dimension(Integer.MAX_VALUE, 28));
		row.add(label);
		row.add(editor);
		parent.add(row);
	}
}
