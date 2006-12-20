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
import workbench.interfaces.ResultReceiver;
import workbench.util.WbThread;

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
	
	public void showResult(final String sql, final String comment, final int panelIndex)
	{
		if (sql == null) return;
		
		// When adding a new panel, a new connection 
		// might be initiated automatically. As that is done in a separate
		// thread, the call to showResult() might occur
		// before the connection is actually established.
		// So we need to wait until the new panel is connected
		// that's what waitForConnection() is for. 
		// As this code might be execute on the EDT we have to make sure
		// we are not blocking the current thread, so a new thread
		// is created that will wait for the connection to succeed.
		// then the actual showing of the data can be executed (on the EDT)
		WbThread t = new WbThread("ShowThread")
		{
			public void run()
			{
				final SqlPanel panel = selectPanel(panelIndex);
				target.waitForConnection();
				
				EventQueue.invokeLater(new Runnable()
				{
					public void run()
					{
						if (panel != null)
						{
							target.requestFocus();
							panel.selectEditor();
							panel.showResult(sql, comment, ResultReceiver.ShowType.replaceText);
						}
					}
				});
			}
		};
		t.start();
	}
	
	public void sendContent(final String text, final int panelIndex)
	{
		if (text == null) return;
		
		final SqlPanel panel = selectPanel(panelIndex);
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
	
	private SqlPanel selectPanel(int index)
	{
		SqlPanel panel;
		
		if (index == -1)
		{
			panel = (SqlPanel)this.target.addTab(true, true);
		}
		else
		{
			panel = (SqlPanel)this.target.getSqlPanel(index);
			target.selectTab(index);
		}
		return panel;
	}
}

