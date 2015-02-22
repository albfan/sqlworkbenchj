/*
 * WbToolbar.java
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
package workbench.gui.components;

import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JToolBar;
import javax.swing.border.Border;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;

import workbench.gui.WbSwingUtilities;
import workbench.gui.actions.WbAction;

/**
 *
 * @author  Thomas Kellerer
 */
public class WbToolbar
	extends JToolBar
{
	public WbToolbar()
	{
		super();
		this.setFloatable(false);
		this.setBorder(WbSwingUtilities.EMPTY_BORDER);
		this.setBorderPainted(true);
		this.setRollover(true);
	}

	@Override
	public JButton add(Action a)
	{
		JButton button;

		if (a instanceof WbAction)
		{
			button = ((WbAction)a).getToolbarButton();
		}
		else
		{
			button = new WbToolbarButton(a);
		}
		this.add(button);
		button.setRolloverEnabled(true);
		return button;
	}

	public JButton add(WbAction a)
	{
		return add(a, -1);
	}

	public JButton add(WbAction a, int index)
	{
		JButton button = a.getToolbarButton();
		button.setRolloverEnabled(true);
		this.add(button, index);
		return button;
	}

	@Override
	public void addSeparator()
	{
		this.addSeparator(-1);
	}

	public void addSeparator(int index)
	{
		this.add(new WbToolbarSeparator(), index);
	}

	public void addDefaultBorder()
	{
		Border b = new CompoundBorder(new EmptyBorder(1,0,1,0), BorderFactory.createEtchedBorder());
		this.setBorder(b);
		this.setBorderPainted(true);
		this.setRollover(true);
	}
}
