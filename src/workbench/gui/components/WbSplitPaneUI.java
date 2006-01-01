/*
 * WbSplitPaneUI.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2006, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.gui.components;

import javax.swing.plaf.basic.BasicSplitPaneDivider;
import javax.swing.plaf.basic.BasicSplitPaneUI;


/**
 * WB's own SplitPaneUI in order to be able to control the Divider
 */
public class WbSplitPaneUI
	extends BasicSplitPaneUI
{
	
	public WbSplitPaneUI()
	{
		super();
	}
	
	public BasicSplitPaneDivider createDefaultDivider()
	{
		return new WbSplitPaneDivider(this);
	}
	
	public void setOneTouchTooltip(String tip)
	{
		if (divider != null && divider instanceof WbSplitPaneDivider)
		{
			((WbSplitPaneDivider)divider).setOneTouchTooltip(tip);
		}
	}
}

