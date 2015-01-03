/*
 * CopyFileNameAction.java
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

import java.io.File;

import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.awt.event.ActionEvent;

import workbench.interfaces.TextFileContainer;
import workbench.util.WbFile;

/**
 *
 * @author Thomas Kellerer
 */
public class CopyFileNameAction
	extends WbAction
{
	private TextFileContainer editor;
	private boolean copyPathName;

	public CopyFileNameAction(TextFileContainer textContainer, boolean copyFullPath)
	{
		this.editor = textContainer;
		copyPathName = copyFullPath;
		if (copyPathName)
		{
			this.initMenuDefinition("MnuTxtCpyFilePath");
		}
		else
		{
			this.initMenuDefinition("MnuTxtCpyFileName");
		}
		this.setEnabled(this.editor.getCurrentFile() != null);
	}

	@Override
	public void executeAction(ActionEvent e)
	{
		if (editor == null) return;

		File file = editor.getCurrentFile();
		if (file == null) return;

		WbFile wfile = new WbFile(file);

		String toCopy = null;
		if (copyPathName)
		{
			toCopy = wfile.getFullPath();
		}
		else
		{
			toCopy = wfile.getName();
		}
		Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
		clipboard.setContents(new StringSelection(toCopy),null);
	}

}
