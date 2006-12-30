/*
 * WbSplitPane.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2007, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.gui.components;

import java.awt.Component;

import javax.swing.JSplitPane;
import javax.swing.border.Border;
import javax.swing.plaf.ComponentUI;
import javax.swing.plaf.SplitPaneUI;
import javax.swing.plaf.basic.BasicSplitPaneDivider;
import javax.swing.plaf.basic.BasicSplitPaneUI;

import workbench.gui.WbSwingUtilities;

/**
 * A JSplitPane which restores the divider size after a UI Change
 * and an updateUI()
 * @author  support@sql-workbench.net
 */
public class WbSplitPane
	extends JSplitPane
{
	public int DEFAULT_DIVIDER_SIZE = 7;
	public WbSplitPane()
	{
		super();
		this.initDefaults();
	}

	public WbSplitPane(int orientation)
	{
		super(orientation);
		this.initDefaults();
	}

	public WbSplitPane(int newOrientation, boolean newContinuousLayout)
	{
		super(newOrientation, newContinuousLayout);
		this.initDefaults();
	}

	public WbSplitPane(int newOrientation, boolean newContinuousLayout, Component newLeftComponent, Component newRightComponent)
	{
		super(newOrientation, newContinuousLayout, newLeftComponent, newRightComponent);
		this.initDefaults();
	}

	public WbSplitPane(int newOrientation, Component newLeftComponent, Component newRightComponent)
	{
		super(newOrientation, newLeftComponent, newRightComponent);
		this.initDefaults();
	}

	public void updateUI()
	{
		int divider = this.getDividerSize();
		//super.updateUI();
		super.setUI(new WbSplitPaneUI());
		revalidate();

		this.setDividerSize(divider);
	}

	public void setUI(ComponentUI newUI)
	{
		int divider = this.getDividerSize();
		super.setUI(newUI);
		this.setDividerSize(divider);
	}

	public void setOneTouchTooltip(String tip)
	{
		SplitPaneUI currentUI = getUI();
		if (currentUI instanceof WbSplitPaneUI)
		{
			((WbSplitPaneUI)currentUI).setOneTouchTooltip(tip);
		}
	}
	private void initDefaults()
	{
		this.setDividerSize(DEFAULT_DIVIDER_SIZE);
		this.setBorder(WbSwingUtilities.EMPTY_BORDER);
		//this.setDividerBorder(WbSwingUtilities.EMPTY_BORDER);
		this.setContinuousLayout(true);
	}

	public Border getDividerBorder()
	{
		Border result = null;
		try
		{
			BasicSplitPaneUI currentUI = (BasicSplitPaneUI)this.getUI();
			BasicSplitPaneDivider div = currentUI.getDivider();
			result = div.getBorder();
		}
		catch (Exception e)
		{
			result = null;
		}
		return result;
	}
	public void setDividerBorder(Border newBorder)
	{
		try
		{
			int divider = this.getDividerSize();
			BasicSplitPaneUI currentUI = (BasicSplitPaneUI)this.getUI();
			BasicSplitPaneDivider div = currentUI.getDivider();
			div.setBorder(newBorder);
			this.setDividerSize(divider);
		}
		catch (Exception e)
		{
		}
	}
}
