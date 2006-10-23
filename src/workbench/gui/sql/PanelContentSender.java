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
	
	protected MainWindow target;
	
	public PanelContentSender(MainWindow window)
	{
		this.target = window;
	}
	
	public void sendContent(final String text, final int panelIndex)
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
		 target.selectTab(panelIndex);
		}

		if (panel == null) return;
		
		EventQueue.invokeLater(new Runnable()
		{
			public void run()
			{
				panel.setStatementText(text);
				target.requestFocus();
				panel.selectEditor();
			}
		});
		
	}
	
}
