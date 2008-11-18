/*
 * WhatsNewViewer.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2008, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.gui.help;

import workbench.gui.components.SearchableTextPane;
import java.awt.BorderLayout;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.net.URL;

import javax.swing.JDialog;
import javax.swing.JScrollPane;

import workbench.gui.WbSwingUtilities;
import workbench.log.LogMgr;
import workbench.resource.ResourceMgr;
import workbench.resource.Settings;

/**
 * @author support@sql-workbench.net  
 */ 
public class WhatsNewViewer 
	extends JDialog 
{
	public WhatsNewViewer(java.awt.Frame owner)
	{
		super(owner, false);
		setTitle(ResourceMgr.getFormattedString("TxtWhatsNewWindowTitle", ResourceMgr.getBuildNumber().toString()));
		SearchableTextPane display = new SearchableTextPane(this);
		display.setFont(Settings.getInstance().getEditorFont());
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
		
		try
		{

			URL file = this.getClass().getClassLoader().getResource("help/history.txt");
			if (file != null)
			{
				display.setPage(file);
			}
			else
			{
				display.setText("No history available! Please report this to support@sql-workbench.net");
			}
			
		}
		catch (Exception e)
		{
			LogMgr.logError("WhatsNewViewer.<init>", "Error creating dialog", e);
		}

		addWindowListener(new WindowAdapter()
		{
			public void windowClosing(WindowEvent evt)
			{
				saveSettings();
				setVisible(false);
				dispose();
			}
		});
	}

	protected void saveSettings()
	{
		Settings.getInstance().storeWindowPosition(this);
		Settings.getInstance().storeWindowSize(this);
	}

}
