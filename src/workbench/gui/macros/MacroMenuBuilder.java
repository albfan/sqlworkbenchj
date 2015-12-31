/*
 * MacroMenuBuilder.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2016, Thomas Kellerer
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
package workbench.gui.macros;

import java.util.List;

import javax.swing.JMenu;

import workbench.resource.ResourceMgr;

import workbench.gui.MainWindow;
import workbench.gui.actions.RunMacroAction;
import workbench.gui.components.WbMenu;
import workbench.gui.components.WbTable;

import workbench.sql.MacroAnnotation;
import workbench.sql.macros.MacroDefinition;
import workbench.sql.macros.MacroGroup;
import workbench.sql.macros.MacroManager;
import workbench.sql.macros.MacroStorage;

import workbench.util.CollectionUtil;
import workbench.util.StringUtil;

/**
 *
 * @author Thomas Kellerer
 */
public class MacroMenuBuilder
{
  public static final String MENU_ITEM_NAME = "macroDataMenu";

	public void buildMacroMenu(MainWindow main, JMenu macroMenu)
	{
		List<MacroGroup> groups = MacroManager.getInstance().getMacros(main.getMacroClientId()).getVisibleGroups();

		if (groups == null || groups.isEmpty()) return;

		macroMenu.addSeparator();

		int groupCount = groups.size();
		int groupIndex = 0;
		for (MacroGroup group : groups)
		{
			groupIndex ++;
			WbMenu groupMenu = (groupCount > 1 ? new WbMenu(group.getName(), groupIndex) : null);

			List<MacroDefinition> macros = group.getVisibleMacros();

			int index = 1;
			for (MacroDefinition macro : macros)
			{
				if (StringUtil.isBlank(macro.getText())) continue;
				RunMacroAction run = new RunMacroAction(main, macro, index);
				if (groupMenu == null)
				{
					run.addToMenu(macroMenu);
				}
				else
				{
					run.addToMenu(groupMenu);
				}
				index++;
			}
			if (groupMenu != null) macroMenu.add(groupMenu);
		}
	}

	public WbMenu buildDataMacroMenu(MainWindow main, WbTable table, List<MacroAnnotation> macroAnnotations)
	{
		if (table == null) return null;
		if (CollectionUtil.isEmpty(macroAnnotations)) return null;

		MacroStorage macros = MacroManager.getInstance().getMacros(main.getMacroClientId());
		WbMenu result = new WbMenu(ResourceMgr.getString(ResourceMgr.MNU_TXT_MACRO));
    result.setName(MENU_ITEM_NAME);

		for (MacroAnnotation annotation: macroAnnotations)
		{
			MacroDefinition macro = macros.getMacro(annotation.getMacroName());
			if (macro != null)
			{
				RunMacroAction run = new RunMacroAction(main, macro, -1);
				run.setTooltip(null);
				run.setDataTable(table, annotation.getColumnMap());
				run.addToMenu(result);
			}
		}
		return result;
	}


}
