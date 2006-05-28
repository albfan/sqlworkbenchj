/*
 * PanelContentSender.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2006, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.gui.sql;

import java.awt.EventQueue;
import workbench.gui.MainWindow;

/**
 * @author support@sql-workbench.net
 */
public class PanelContentSender
{
	
	private MainWindow target;
	
	public PanelContentSender(MainWindow window)
	{
		this.target = window;
	}
	
	public void sendContent(String text, final int panelIndex)
	{
		if (text == null) return;
		
		final SqlPanel panel;

		if (panelIndex == -1)
		{
			panel = (SqlPanel)this.target.addTab(true, true);
		}
		else
		{
		 panel = (SqlPanel)this.target.getSqlPanel(panelIndex);
		}

		if (panel == null) return;
		
		panel.setStatementText(text);

		EventQueue.invokeLater(new Runnable()
		{
			public void run()
			{
				target.requestFocus();
				if (panelIndex > -1) target.selectTab(panelIndex);
				panel.selectEditor();
			}
		});
		
	}
	
}
