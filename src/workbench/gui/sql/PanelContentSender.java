/*
 * PanelContentSender.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2007, Thomas Kellerer
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
	public static final int NEW_PANEL = -1;
	
	protected MainWindow target;
	
	public PanelContentSender(MainWindow window)
	{
		this.target = window;
	}
	
	public void showResult(final String sql, final String comment, final int panelIndex, final boolean logText)
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
				final boolean isCurrent = (target.getCurrentPanelIndex() == panelIndex);
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
							ResultReceiver.ShowType type;
							if (panelIndex == NEW_PANEL)
							{
								type = ResultReceiver.ShowType.replaceText;
							}
							else if (panel.hasFileLoaded())
							{
								type = ResultReceiver.ShowType.logText;
							}
							else
							{
								type = (logText ? ResultReceiver.ShowType.logText : ResultReceiver.ShowType.appendText);
							}
							panel.showResult(sql, comment, type);
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
		
		if (index == NEW_PANEL)
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

