/*
 * ConnectionInfo.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2004, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: info@sql-workbench.net
 *
 */
package workbench.gui.components;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.GridLayout;

import javax.swing.JComponent;
import javax.swing.JTextField;
import javax.swing.border.EmptyBorder;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import workbench.db.WbConnection;
import workbench.resource.ResourceMgr;


/**
 *
 * @author  info@sql-workbench.net
 */
public class ConnectionInfo
	extends JComponent
	implements ChangeListener
{
	private JTextField display;
	private WbConnection sourceConnection;

	/** Creates a new instance of ConnectionInfo */
	public ConnectionInfo(Color aBackground)
	{
		super();
		EmptyBorder border = new EmptyBorder(0, 2, 0, 2);
		this.setBorder(border);

		this.display = new JTextField();
		FontMetrics fm = this.display.getFontMetrics(this.display.getFont());
		int height = fm.getHeight() + 1;
		Dimension d = new Dimension(32768, height);
		this.display.setPreferredSize(d);
		this.display.setMaximumSize(d);
		this.setPreferredSize(d);
		this.setMaximumSize(d);

		this.setLayout(new GridLayout(1,1));
		this.add(this.display);

		this.display.setBackground(aBackground);
		this.display.setEditable(false);
		this.display.setBorder(null);
		this.display.addMouseListener(new TextComponentMouseListener());
	}

	public void setConnection(WbConnection aConnection)
	{
		if (this.sourceConnection != null)
		{
			this.sourceConnection.removeChangeListener(this);
		}
		this.sourceConnection = aConnection;
		if (this.sourceConnection != null)
		{
			this.sourceConnection.addChangeListener(this);
		}
		this.updateDisplay();
	}

	private void updateDisplay()
	{
		if (this.sourceConnection != null)
		{
			this.display.setText(this.sourceConnection.getDisplayString());
			this.display.setToolTipText(this.sourceConnection.getDatabaseProductName());
		}
		else
		{
			this.display.setText(ResourceMgr.getString("TxtNotConnected"));
			this.display.setToolTipText(null);
		}
		//this.setCaretPosition(0);
	}
	public void stateChanged(ChangeEvent e)
	{
		if (e.getSource() == this.sourceConnection)
		{
			this.updateDisplay();
		}
	}

}
