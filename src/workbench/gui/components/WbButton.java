/*
 * WbButton.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2006, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.gui.components;

import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import javax.swing.Action;
import javax.swing.JButton;
import javax.swing.UIDefaults;
import javax.swing.UIManager;
import javax.swing.border.Border;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.plaf.basic.BasicBorders;

/**
 *
 * @author  thomas
 */
public class WbButton
	extends JButton
	implements MouseListener
{
	private Border rolloverBorder;
	private Border emptyBorder;
	private Border lastBorder;
	public WbButton()
	{
		super();
		init();
	}
	
	public WbButton(Action a)
	{
		super(a);
		init();
	}
	
	public WbButton(String aText)
	{
		super(aText);
		init();
	}
	
	private void init()
	{
		putClientProperty("jgoodies.isNarrow", Boolean.FALSE);
	}
	
	public void setText(String newText)
	{
		int pos = newText.indexOf('&');
		if (pos > -1)
		{
			char mnemonic = newText.charAt(pos + 1);
			newText = newText.substring(0, pos) + newText.substring(pos + 1);
			this.setMnemonic((int)mnemonic);
			super.setText(newText);
		}
		else
		{
			super.setText(newText);
		}
	}
	
	public void setRollover(boolean flag)
	{
		this.setRolloverEnabled(flag);
		if (flag)
		{
			UIDefaults table = UIManager.getLookAndFeelDefaults();
			Border out = new BasicBorders.RolloverButtonBorder(
					   table.getColor("controlShadow"),
             table.getColor("controlDkShadow"),
             table.getColor("controlHighlight"),
             table.getColor("controlLtHighlight"));
			Border in = new EmptyBorder(3,3,3,3);
			this.rolloverBorder = new CompoundBorder(out, in);
			this.emptyBorder = new EmptyBorder(6,6,6,6);;
			this.setBorder(emptyBorder);
			this.addMouseListener(this);
		}
		else
		{
			this.addMouseListener(this);
		}
	}

	public void mouseClicked(MouseEvent e)
	{
	}

	public void mousePressed(MouseEvent e)
	{
	}

	public void mouseReleased(MouseEvent e)
	{
	}

	public void mouseEntered(MouseEvent e)
	{
		this.setBorder(this.rolloverBorder);
	}

	public void mouseExited(MouseEvent e)
	{
		this.setBorder(this.emptyBorder);
	}
	
}
