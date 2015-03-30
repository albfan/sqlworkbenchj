/*
 * OpenFileDirAction.java
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
package workbench.gui.actions;

import java.io.File;

import java.awt.Desktop;
import java.awt.event.ActionEvent;

import workbench.gui.WbSwingUtilities;
import workbench.interfaces.TextFileContainer;
import workbench.log.LogMgr;

/**
 *
 * @author Thomas Kellerer
 */
public class OpenFileDirAction
	extends WbAction
{
	private TextFileContainer editor;

	public OpenFileDirAction(TextFileContainer textContainer)
	{
		this.editor = textContainer;
		this.initMenuDefinition("MnuTxtOpenFileDir");
		boolean hasFile = this.editor.getCurrentFile() != null;
		boolean desktopAvailable = false;
		if (hasFile)
		{
			desktopAvailable = Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.OPEN);
			if (!desktopAvailable)
			{
				LogMgr.logWarning("OpenFileDirAction", "Desktop or Desktop.open() not supported!");
			}
		}
		this.setEnabled(hasFile && desktopAvailable);
	}

	@Override
	public void executeAction(ActionEvent e)
	{
		if (editor == null) return;

		File file = editor.getCurrentFile();
		if (file == null) return;

		File dir = file.getParentFile();

		if (dir != null)
		{
			try
			{
				Desktop.getDesktop().open(dir);
			}
			catch (Exception io)
			{
				WbSwingUtilities.showErrorMessage(io.getLocalizedMessage());
			}
		}
	}

}
