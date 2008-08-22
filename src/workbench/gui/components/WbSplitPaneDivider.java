/*
 * WbSplitPaneDivider.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2008, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.gui.components;

import javax.swing.JButton;
import javax.swing.plaf.basic.BasicSplitPaneDivider;
import javax.swing.plaf.basic.BasicSplitPaneUI;

/**
 * 
 * @author support@sql-workbench.net  
 */ 
public class WbSplitPaneDivider
	extends BasicSplitPaneDivider
{
	private String oneTouchTooltip;
	
	public WbSplitPaneDivider(BasicSplitPaneUI ui)
	{
		super(ui);
	}
	
	public void setOneTouchTooltip(String tip)
	{
		this.oneTouchTooltip = tip;
		this.updateTooltip();
	}
	
	protected JButton createLeftOneTouchButton()	
	{
		JButton b = super.createLeftOneTouchButton();
		if (this.oneTouchTooltip != null)
		{
			b.setToolTipText(this.oneTouchTooltip);
		}
		return b;
	}
	
	protected JButton createRightOneTouchButton()
	{
		JButton b = super.createRightOneTouchButton();
		if (this.oneTouchTooltip != null)
		{
			b.setToolTipText(this.oneTouchTooltip);
		}
		return b;
	}
	
	private void updateTooltip()
	{
		if (this.leftButton != null)
		{
			this.leftButton.setToolTipText(this.oneTouchTooltip);
		}
		if (this.rightButton != null)
		{
			this.rightButton.setToolTipText(this.oneTouchTooltip);
		}
	}
}
