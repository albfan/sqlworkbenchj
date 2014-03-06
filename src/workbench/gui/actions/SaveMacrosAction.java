/*
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2014 Thomas Kellerer.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * To contact the author please send an email to: support@sql-workbench.net
 */

package workbench.gui.actions;

import java.awt.event.ActionEvent;

import workbench.interfaces.MacroChangeListener;
import workbench.resource.ResourceMgr;

import workbench.sql.macros.MacroFileSelector;
import workbench.sql.macros.MacroManager;

import workbench.util.WbFile;

/**
 *
 * @author Thomas Kellerer
 */
public class SaveMacrosAction
	extends WbAction
	implements MacroChangeListener
{

	public SaveMacrosAction()
	{
		super();
		this.initMenuDefinition("MnuTxtSaveMacros");
		this.setMenuItemName(ResourceMgr.MNU_TXT_MACRO);
		this.setIcon(null);
		MacroManager.getInstance().getMacros().addChangeListener(this);
		String fname = MacroManager.getInstance().getMacros().getCurrentMacroFilename();
		setTooltip(fname);
	}

	@Override
	public void executeAction(ActionEvent e)
	{
		MacroFileSelector selector = new MacroFileSelector();
		WbFile f = selector.selectStorageFile(true, MacroManager.getInstance().getMacros().getCurrentFile());
		if (f == null) return;
		MacroManager.getInstance().save(f);
		setTooltip(f.getFullPath());
	}

	@Override
	public void macroListChanged()
	{
		String fname = MacroManager.getInstance().getMacros().getCurrentMacroFilename();
		setTooltip(fname);
	}

	@Override
	public void dispose()
	{
		super.dispose();
		MacroManager.getInstance().getMacros().removeChangeListener(this);
	}

}
