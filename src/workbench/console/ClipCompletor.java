/*
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2016, Thomas Kellerer.
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

package workbench.console;

import java.util.List;
import java.util.Map;

import workbench.log.LogMgr;

import workbench.sql.macros.MacroDefinition;
import workbench.sql.macros.MacroManager;

import jline.Completor;
/**
 *
 * An implementation of a JLine Completor that completes clip keywords with the macro text.
 *
 * @author Thomas Kellerer
 */
public class ClipCompletor
	implements Completor
{

	@Override
	public int complete(String buffer, int cursor, List candidates)
	{
		LogMgr.logDebug("ClipCompletor.complete()", "Checking completion for: " + buffer);
		Map<String, MacroDefinition> macros = MacroManager.getInstance().getExpandableMacros(MacroManager.DEFAULT_STORAGE);
		MacroDefinition def = macros.get(buffer);
		if (def != null)
		{
			candidates.add(def.getText());
			return 0;
		}
		return -1;
	}

}
