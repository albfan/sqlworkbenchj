/*
 * ActionRegistration.java
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
package workbench.gui.actions;

/**
 * 	Register actions with the ShortcutManager that are not created upon startup.
 * 	For this, a dummy action is created (with no client) which will
 * 	kick off the registration with the ShortcutManager
 *
 * @author Thomas Kellerer
 */
public class ActionRegistration
{
	public static void registerActions()
	{
		new ToggleTableSourceAction(null);
		new ReloadAction(null);
		new CopySelectedAsTextAction(null);
		new CopySelectedAsSqlDeleteInsertAction(null);
		new CopySelectedAsSqlInsertAction(null);
		new CopySelectedAsSqlUpdateAction(null);
		new SetNullAction(null);
		new SelectFkValueAction(null);
	}
}
