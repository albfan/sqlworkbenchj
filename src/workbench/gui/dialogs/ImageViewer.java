/*
 * ImageViewer.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2011, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.gui.dialogs;

import java.awt.BorderLayout;
import java.awt.Dialog;
import java.awt.FlowLayout;
import java.awt.Frame;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.io.File;
import java.sql.Blob;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JPanel;
import javax.swing.WindowConstants;
import workbench.gui.WbSwingUtilities;
import workbench.gui.actions.EscAction;
import workbench.log.LogMgr;
import workbench.resource.ResourceMgr;
import workbench.resource.Settings;

/**
 * A dialog to display a {@link ImagePanel}
 * 
 * @author Thomas Kellerer
 */
public class ImageViewer
	extends JDialog
	implements ActionListener, WindowListener
{
	private ImagePanel panel;
	private JButton closeButton = new JButton(ResourceMgr.getString("LblClose"));
	private final String settingsId = "workbench.gui.imageviewer";
	
	public ImageViewer(Frame parent, String title)
	{
		super(parent, title, true);
		init();
		WbSwingUtilities.center(this, parent);
	}

	public ImageViewer(Dialog parent, String title)
	{
		super(parent, title, true);
		init();
		WbSwingUtilities.center(this, parent.getParent());
	}

	private void init()
	{
		this.getContentPane().setLayout(new BorderLayout());
		this.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
		panel = new ImagePanel();
		this.getContentPane().add(panel, BorderLayout.CENTER);
		JPanel buttonPanel = new JPanel();
		buttonPanel.setLayout(new FlowLayout(FlowLayout.CENTER, 10, 10));
		buttonPanel.add(closeButton);
		closeButton.addActionListener(this);
		this.getContentPane().add(buttonPanel, BorderLayout.SOUTH);
		this.addWindowListener(this);
		if (!Settings.getInstance().restoreWindowSize(this, settingsId))
		{
			setSize(640,480);
		}
		
		getRootPane().setDefaultButton(closeButton);
		new EscAction(this, this);
	}

	public void setData(Object data)
	{
		try
		{
			if (data instanceof Blob)
			{
				this.panel.setImage((Blob)data);
			}
			else if (data instanceof byte[])
			{
				this.panel.setImage((byte[])data);
			}
			else if (data instanceof File)
			{
				this.panel.setImage((File)data);
			}
		}
		catch (Exception e)
		{
			LogMgr.logError("ImageViewer.setData()", "Error reading image", e);
		}
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
