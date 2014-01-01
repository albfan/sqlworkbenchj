/*
 * FileCloseAction.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2014, Thomas Kellerer
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

import java.awt.event.ActionEvent;

import workbench.WbManager;

import workbench.gui.MainWindow;

/**
 * Exit and close the application
 * @see workbench.WbManager#exitWorkbench()
 *
 * @author  Thomas Kellerer
 */
public class FileCloseAction
	extends WbAction
{
	private MainWindow window;

	public FileCloseAction(MainWindow toClose)
	{
		super();
		window = toClose;
		this.initMenuDefinition("MnuTxtFileCloseWin");
	}

	@Override
	public void executeAction(ActionEvent e)
	{
		WbManager.getInstance().closeMainWindow(window);
	}
}
