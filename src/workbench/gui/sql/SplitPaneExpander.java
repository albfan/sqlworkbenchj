/*
 * SplitPaneExpander.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2008, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.gui.sql;

import java.awt.EventQueue;
import javax.swing.JSplitPane;
import workbench.gui.WbSwingUtilities;

/**
 * @author support@sql-workbench.net
 */
public class SplitPaneExpander
{
	private JSplitPane contentPanel;
	private int lastDivider = -1;
	private boolean upperPartExpanded = false;
	private boolean lowerPartExpanded = false;
	
	
	public SplitPaneExpander(JSplitPane client)
	{
		this.contentPanel = client;
	}
	
	public void undoExpand()
	{
		if (lastDivider != -1)
		{
			this.contentPanel.setDividerLocation(this.lastDivider);
		}
		else
		{
			int newLoc = (int)(this.contentPanel.getHeight() / 2);
			this.contentPanel.setDividerLocation(newLoc);
		}
		this.lastDivider = -1;
		repaintClient();
	}

	public void toggleUpperComponentExpand()
	{
		if (upperPartExpanded)
		{
			undoExpand();
			upperPartExpanded = false;
		}
		else
		{
			if (!lowerPartExpanded)
			{
				lastDivider = this.contentPanel.getDividerLocation();
			}
			this.contentPanel.setDividerLocation(this.contentPanel.getHeight());
			upperPartExpanded = true;
		}
		this.lowerPartExpanded = false;
		repaintClient();
	}

	public void toggleLowerComponentExpand()
	{
		if (this.lowerPartExpanded)
		{
			undoExpand();
			lowerPartExpanded = false;
		}
		else
		{
			if (!upperPartExpanded)
			{
				lastDivider = this.contentPanel.getDividerLocation();
			}
			this.contentPanel.setDividerLocation(0);
			this.lowerPartExpanded = true;
		}
		this.upperPartExpanded = false;
		repaintClient();
	}

	private void repaintClient()
	{
		WbSwingUtilities.invoke(new Runnable()
		{
			public void run()
			{
				WbSwingUtilities.callRepaint(contentPanel);
				WbSwingUtilities.callRepaint(contentPanel.getTopComponent());
				WbSwingUtilities.callRepaint(contentPanel.getBottomComponent());
			}
		});
	}
}
