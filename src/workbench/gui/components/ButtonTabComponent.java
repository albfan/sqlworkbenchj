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

import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.Icon;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;
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
	private final WbButton closeButton;
	
	public ButtonTabComponent(String title, final WbTabbedPane pane)
	{
		super(new FlowLayout(FlowLayout.LEFT, 0, 0));
		this.pane = pane;
		setOpaque(false);

		label = new JLabel();
		label.setText(title);
		setBorder(new EmptyBorder(1, 0, 0, 0));
		label.setBorder(new EmptyBorder(0,0,0,3));
		closeButton = new WbButton(ResourceMgr.getPng("closePanel"));
		Dimension d = new Dimension(14, 15);
		closeButton.setPreferredSize(d);
		closeButton.setFocusable(false);
		closeButton.enableToolbarRollover();
		closeButton.addActionListener(this);
		
		add(label);
		add(closeButton);
	}

	public void setEnabled(boolean flag)
	{
		closeButton.setEnabled(flag);
	}
	
	public void setTitle(String title)
	{
		label.setText(title);
	}

	public void setIcon(Icon icon)
	{
		label.setIcon(icon);
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

}


