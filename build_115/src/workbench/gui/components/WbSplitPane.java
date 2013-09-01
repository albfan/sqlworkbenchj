/*
 * WbSplitPane.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2013, Thomas Kellerer
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
 *
 * @author  Thomas Kellerer
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

	@Override
	public void updateUI()
	{
		int divider = this.getDividerSize();
		this.setDividerSize(divider);
		//super.updateUI();
		if (this.getUI() == null)
		{
			super.setUI(new WbSplitPaneUI());
		}
		revalidate();
	}

	@Override
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
