/*
 * CutCopyPastePopup.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2016, Thomas Kellerer
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

import javax.swing.JPopupMenu;

import workbench.interfaces.ClipboardSupport;

import workbench.gui.actions.CopyAction;
import workbench.gui.actions.CutAction;
import workbench.gui.actions.PasteAction;
import workbench.gui.actions.WbAction;
import workbench.gui.components.WbMenu;

/**
 * A popup menu with the usual Cut, Copy and Paste entries for text fields.
 *
 * @author  Thomas Kellerer
 */
public class CutCopyPastePopup
	extends JPopupMenu
{
	private CopyAction copy;
	private PasteAction paste;
	private CutAction cut;

	public CutCopyPastePopup(ClipboardSupport aClient)
	{
		super();
		this.cut = new CutAction(aClient);
		this.add(cut.getMenuItem());
		this.copy = new CopyAction(aClient);
		this.add(this.copy.getMenuItem());
		this.paste = new PasteAction(aClient);
		this.add(this.paste.getMenuItem());
	}

	public void addAction(WbAction anAction, boolean withSep)
	{
		if (withSep) this.addSeparator();
		this.add(anAction.getMenuItem());
	}

	public WbAction getCopyAction()
	{
		return this.copy;
	}

	public WbAction getCutAction()
	{
		return this.cut;
	}

	public WbAction getPasteAction()
	{
		return this.paste;
	}

	public void dispose()
	{
		WbMenu.disposeMenu(this);
	}

}
