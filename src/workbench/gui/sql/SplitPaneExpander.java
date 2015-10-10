/*
 * SplitPaneExpander.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2015, Thomas Kellerer
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at.
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.gui.sql;

import javax.swing.JSplitPane;

import workbench.gui.WbSwingUtilities;

/**
 * @author Thomas Kellerer
 */
public class SplitPaneExpander
{
	private JSplitPane contentPanel;
	private int lastDivider = -1;
	private boolean upperPartExpanded;
	private boolean lowerPartExpanded;

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
			int newLoc = this.contentPanel.getHeight() / 2;
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
    contentPanel.invalidate();
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
    contentPanel.invalidate();
	}

	private void repaintClient()
	{
		WbSwingUtilities.invokeLater(new Runnable()
		{
			@Override
			public void run()
			{
				contentPanel.validate();
			}
		});
	}
}
