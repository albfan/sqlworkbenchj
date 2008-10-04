/*
 * WbCheckBoxLabel.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2008, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.gui.components;

import java.awt.Component;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import javax.swing.JCheckBox;
import javax.swing.JLabel;

/**
 * @author support@sql-workbench.net
 */
public class WbCheckBoxLabel
	extends JLabel
	implements MouseListener, FocusListener
{
//	private Border oldBorder = new EmptyBorder(1,1,1,1);
//	private Border focusBorder = new LineBorder(Color.GRAY, 1);
	
	public WbCheckBoxLabel()
	{
		super();
		this.addMouseListener(this);
	}
	
	public WbCheckBoxLabel(String label)
	{
		super(label);
		this.addMouseListener(this);
	}

	public void mouseClicked(MouseEvent e)
	{
		Component c = this.getLabelFor();
		if (c instanceof JCheckBox)
		{
			JCheckBox cbx = (JCheckBox)c;
			cbx.setSelected(!cbx.isSelected());
		}
	}

	public void mousePressed(MouseEvent e)
	{
	}

	public void mouseReleased(MouseEvent e)
	{
	}

	public void mouseEntered(MouseEvent e)
	{
	}

	public void mouseExited(MouseEvent e)
	{
	}

	public void focusGained(FocusEvent e)
	{
		// the checkbox gained focus, create an indicator around the label
//		this.setBorder(focusBorder);
	}

	public void focusLost(FocusEvent e)
	{
//		this.setBorder(oldBorder);
	}
	
}
