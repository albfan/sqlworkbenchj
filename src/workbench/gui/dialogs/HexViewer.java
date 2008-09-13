/*
 * HexViewer.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2008, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.gui.dialogs;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JPanel;
import javax.swing.WindowConstants;
import workbench.WbManager;
import workbench.gui.WbSwingUtilities;
import workbench.gui.actions.EscAction;
import workbench.gui.components.HexPanel;
import workbench.resource.ResourceMgr;
import workbench.resource.Settings;

/**
 * @author support@sql-workbench.net
 */
public class HexViewer
	extends JDialog
	implements ActionListener, WindowListener
{
	private HexPanel panel;
	private JButton closeButton = new JButton(ResourceMgr.getString("LblClose"));
	private final String settingsId = "workbench.gui.imageviewer";
	private EscAction escAction;
	
	/** Creates a new instance of ImageViewer */
	public HexViewer(JDialog parent, String title)
	{
		super(parent, title, true);
		this.getContentPane().setLayout(new BorderLayout());
		this.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
		panel = new HexPanel();
		this.getContentPane().add(panel, BorderLayout.CENTER);
		JPanel buttonPanel = new JPanel();
		buttonPanel.setLayout(new FlowLayout(FlowLayout.CENTER, 10, 10));
		buttonPanel.add(closeButton);
		closeButton.addActionListener(this);
		this.getContentPane().add(buttonPanel, BorderLayout.SOUTH);
		this.addWindowListener(this);
		if (!Settings.getInstance().restoreWindowSize(this, settingsId))
		{
			setSize(320,200);
		}
		
		getRootPane().setDefaultButton(closeButton);
		escAction = new EscAction(this, this);
		WbSwingUtilities.center(this, WbManager.getInstance().getCurrentWindow());
	}

	public void setData(byte[] data)
	{
		panel.setData(data);
	}
	
	public void actionPerformed(ActionEvent e)
	{
		this.setVisible(false);
		this.dispose();
	}

	public void windowOpened(WindowEvent e)
	{
	}

	public void windowClosing(WindowEvent e)
	{
	}

	public void windowClosed(WindowEvent e)
	{
		Settings.getInstance().storeWindowSize(this, settingsId);
	}

	public void windowIconified(WindowEvent e)
	{
	}

	public void windowDeiconified(WindowEvent e)
	{
	}

	public void windowActivated(WindowEvent e)
	{
	}

	public void windowDeactivated(WindowEvent e)
	{
	}
	
}
