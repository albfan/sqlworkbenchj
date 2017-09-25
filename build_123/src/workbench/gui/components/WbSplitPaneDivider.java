/*
 * WbSplitPaneDivider.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2017, Thomas Kellerer
 *
 * Licensed under a modified Apache License, Version 2.0
 * that restricts the use for certain governments.
 * You may not use this file except in compliance with the License.
 * You may obtain a copy of the License at.
 *
 *     http://sql-workbench.net/manual/license.html
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

import javax.swing.JButton;
import javax.swing.plaf.basic.BasicSplitPaneDivider;
import javax.swing.plaf.basic.BasicSplitPaneUI;

/**
 *
 * @author Thomas Kellerer
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

	@Override
	protected JButton createLeftOneTouchButton()
	{
		JButton b = super.createLeftOneTouchButton();
		if (this.oneTouchTooltip != null)
		{
			b.setToolTipText(this.oneTouchTooltip);
		}
		return b;
	}

	@Override
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
