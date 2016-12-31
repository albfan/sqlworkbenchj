/*
 * TextPopup.java
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
package workbench.gui.menu;

import workbench.interfaces.ClipboardSupport;

import workbench.gui.actions.ClearAction;
import workbench.gui.actions.SelectAllAction;
import workbench.gui.actions.WbAction;

/**
 * An popup menu which adds a clear and select all action to the {@link CutCopyPastePopup} menu.
 *
 * @author Thomas Kellerer
 */
public class TextPopup
	extends CutCopyPastePopup
{
	private ClearAction clear;
	private SelectAllAction selectAll;

	public TextPopup(ClipboardSupport aClient)
	{
		super(aClient);
		this.addSeparator();
		this.clear = new ClearAction(aClient);
		this.add(this.clear.getMenuItem());
		this.selectAll = new SelectAllAction(aClient);
		this.add(this.selectAll.getMenuItem());
	}

	/**
	 * Add another action to this popup menu.
	 *
	 * @param anAction the action to be added
	 * @param withSep if true a separator is added to the menu before adding the action
	 */
	@Override
	public void addAction(WbAction anAction, boolean withSep)
	{
		if (withSep)
		{
			this.addSeparator();
		}
		this.add(anAction.getMenuItem());
	}

	public WbAction getSelectAllAction()
	{
		return this.selectAll;
	}

	public WbAction getClearAction()
	{
		return this.clear;
	}
}
