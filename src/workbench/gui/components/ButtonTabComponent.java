/*
 * ButtonTabComponent.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2009, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.gui.components;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import javax.swing.AbstractButton;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;
import workbench.gui.WbSwingUtilities;
import workbench.resource.ResourceMgr;

/**
 * Component to be used as tabComponent;
 * Contains a JLabel to show the text and
 * a JButton to close the tab it belongs to
 * 
 * @author support@sql-workbench.net
 */
public class ButtonTabComponent
	extends JPanel
	implements ActionListener
{
	private final WbTabbedPane pane;
	private final JLabel label;
	private final JButton closeButton;
	
	public ButtonTabComponent(String title, final WbTabbedPane pane)
	{
		super(new FlowLayout(FlowLayout.LEFT, 0, 0));
		if (pane == null)
		{
			throw new NullPointerException("TabbedPane is null");
		}
		this.pane = pane;
		setOpaque(false);

		label = new JLabel()
		{
			@Override
			public Insets getInsets()
			{
				return WbSwingUtilities.EMPTY_INSETS;
			}
		};
		label.setText(title);
		
		add(label);
		label.setBorder(new EmptyBorder(0, 0, 0, 2));
		closeButton = new TabButton();
		closeButton.addActionListener(this);
		closeButton.setToolTipText(ResourceMgr.getString("TxtCloseTab"));
		add(closeButton);
	}

	@Override
	public Insets getInsets()
	{
		return WbSwingUtilities.EMPTY_INSETS;
	}

	public void setEnabled(boolean flag)
	{
		closeButton.setEnabled(flag);
		if (flag)
		{
			closeButton.setToolTipText(ResourceMgr.getString("TxtCloseTab"));
		}
		else
		{
			closeButton.setToolTipText(null);
		}
	}
	
	public void setTitle(String title)
	{
		label.setText(title);
	}

	@Override
	public void actionPerformed(ActionEvent e)
	{
		int i = pane.indexOfTabComponent(this);
		if (i != -1)
		{
			pane.closeButtonClicked(i);
		}
	}
	
	private class TabButton
		extends JButton
	{
		public TabButton()
		{
			super(ResourceMgr.getPng("closePanel"));
			Dimension d = new Dimension(14, 15);
			setPreferredSize(d);
			setMaximumSize(d);
			setMinimumSize(d);
			setFocusable(false);
			setBorder(BorderFactory.createEtchedBorder());
			setBorderPainted(false);
			addMouseListener(buttonMouseListener);
		}

		@Override
		public Insets getInsets()
		{
			return new Insets(2, 3, 3, 3);
		}
	}

	private final static MouseListener buttonMouseListener = new MouseAdapter()
	{
		public void mouseEntered(MouseEvent e)
		{
			Component component = e.getComponent();
			if (component instanceof AbstractButton)
			{
				AbstractButton button = (AbstractButton) component;
				button.setBorderPainted(button.isEnabled());
			}
		}

		public void mouseExited(MouseEvent e)
		{
			Component component = e.getComponent();
			if (component instanceof AbstractButton)
			{
				AbstractButton button = (AbstractButton) component;
				button.setBorderPainted(false);
			}
		}
	};
}


