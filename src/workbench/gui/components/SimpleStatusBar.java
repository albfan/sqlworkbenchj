/*
 * SimpleStatusBar.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2011, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.gui.components;

import javax.swing.JLabel;
import workbench.gui.WbSwingUtilities;
import workbench.interfaces.StatusBar;
import workbench.storage.RowActionMonitor;

/**
 *
 * @author Thomas Kellerer
 */
public class SimpleStatusBar
	extends JLabel
	implements StatusBar
{
	public SimpleStatusBar()
	{
		super();
	}

	public RowActionMonitor getMonitor()
	{
		return new GenericRowMonitor(this);
	}

	@Override
	public void setStatusMessage(String message, int duration)
	{
		setStatusMessage(message);
	}

	@Override
	public void doRepaint()
	{
		repaint();
	}

	@Override
	public void setStatusMessage(final String message)
	{
		WbSwingUtilities.invoke(new Runnable()
		{
			@Override
			public void run()
			{
				setText(message);
			}
		});
	}

	@Override
	public void clearStatusMessage()
	{
		WbSwingUtilities.invoke(new Runnable()
		{
			@Override
			public void run()
			{
				setText("");
			}
		});
	}

	@Override
	public String getText()
	{
		return super.getText();
	}
}
