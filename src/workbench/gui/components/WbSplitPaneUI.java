package workbench.gui.components;

import javax.swing.plaf.basic.*;
import javax.swing.*;

import javax.swing.plaf.basic.BasicSplitPaneUI;
import javax.swing.plaf.basic.BasicSplitPaneDivider;


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

