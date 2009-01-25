/*
 * 
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 * Copyright 2002-2008, Thomas Kellerer
 * 
 * No part of this code maybe reused without the permission of the author
 * 
 * To contact the author please send an email to: support@sql-workbench.net
 * 
 */

package workbench.gui.sql;

import javax.swing.JTabbedPane;
import workbench.interfaces.MainPanel;
import workbench.resource.GuiSettings;
import workbench.util.NumberStringCache;

/**
 *
 * @author support@sql-workbench.net
 */
public class PanelTitleSetter
{
	public static void setTabTitle(final JTabbedPane tab, MainPanel panel, int index, String plainTitle)
	{
		String title = plainTitle;
		if (panel.isLocked())
		{
			title = "<html><i>" + title + "</i> ";
			if (GuiSettings.getShowTabIndex())
			{
				if (index < 9)
				{
					title += "<u>";
				}
				title += NumberStringCache.getNumberString (index+1);
				if (index < 9) 
				{
					title += "</u>";
				}
			}
			title += "</html>";
			tab.setTitleAt(index, title);
		}
		else
		{
			if (GuiSettings.getShowTabIndex())
			{
				 title += " " + NumberStringCache.getNumberString(index+1);
			}
			tab.setTitleAt(index, title);

			if (index < 9 && GuiSettings.getShowTabIndex())
			{
				char c = Integer.toString(index+1).charAt(0);
				int pos = plainTitle.length() + 1;
				tab.setMnemonicAt(index, c);
				// The Mnemonic index has to be set explicitely otherwise
				// the display would be wrong if the tab title contains
				// the mnemonic character
				tab.setDisplayedMnemonicIndexAt(index, pos);
			}
		}
	}
}
