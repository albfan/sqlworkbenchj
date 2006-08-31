/*
 * WhatsNewViewer.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2006, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.gui.help;

import java.awt.BorderLayout;
import java.awt.Font;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.net.URL;

import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JScrollPane;
import javax.swing.JTextPane;

import workbench.gui.WbSwingUtilities;
import workbench.resource.ResourceMgr;
import workbench.resource.Settings;


public class WhatsNewViewer 
	extends JDialog 
{
	private JTextPane display;
	
	public WhatsNewViewer(java.awt.Frame owner)
	{
		
		super(owner, ResourceMgr.getString("TxtWhatsNewWindowTitle"), false);
		display = new JTextPane();
		display.setFont(new Font("Monospaced", Font.PLAIN, 12));
		display.setEditable(false);
		JScrollPane scroll = new JScrollPane(display);
		
		getContentPane().setLayout(new BorderLayout());
		getContentPane().add(scroll, BorderLayout.CENTER);

		if (!Settings.getInstance().restoreWindowSize(this))
		{
			setSize(800,600);
		}
		
		if (!Settings.getInstance().restoreWindowPosition(this))
		{
			WbSwingUtilities.center(this, owner);
		}
		
//		setSize(800, 600);
		
		try
		{

			URL file = this.getClass().getClassLoader().getResource("help/history.txt");
			if (file == null)
			{
				file = this.getClass().getClassLoader().getResource("workbench/gui/help/NotFound.html");
			}
			
			if (file != null)
			{
				display.setPage(file);
			}
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
		
		addWindowListener(new WindowAdapter()
		{
			public void windowClosing(WindowEvent evt)
			{
				saveSettings();
				hide();
				dispose();
			}
		});
	}

	private void saveSettings()
	{
		Settings.getInstance().storeWindowPosition(this);
		Settings.getInstance().storeWindowSize(this);
	}

}
