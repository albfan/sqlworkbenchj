/*
 * CreateSnippetAction.java
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

import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.awt.event.ActionEvent;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;

import java.io.BufferedReader;
import java.io.StringReader;
import javax.swing.KeyStroke;

import workbench.interfaces.TextContainer;
import workbench.log.LogMgr;
import workbench.resource.ResourceMgr;
import workbench.resource.Settings;
import workbench.util.FileUtil;
import workbench.util.SqlUtil;
import workbench.util.StringUtil;

/**
 * Action to create a piece of Java code that declares the currently
 * selected SQL statement as a variable.
 * @author Thomas Kellerer
 */
public class CreateSnippetAction extends WbAction
{
	private TextContainer client;

	public CreateSnippetAction(TextContainer aClient)
	{
		super();
		this.client = aClient;
		this.initMenuDefinition("MnuTxtCreateSnippet", KeyStroke.getKeyStroke(KeyEvent.VK_O, InputEvent.ALT_MASK));
		this.setMenuItemName(ResourceMgr.MNU_TXT_SQL);
	}

	@Override
	public void executeAction(ActionEvent e)
	{
		String sql = this.client.getSelectedText();
		if (sql == null)
		{
			sql = this.client.getText();
		}
		boolean removeSemicolon = true;
		if (invokedByMouse(e) && isCtrlPressed(e))
		{
			removeSemicolon = false;
		}
		String code = makeJavaString(sql, removeSemicolon);
		Clipboard clp = Toolkit.getDefaultToolkit().getSystemClipboard();
		StringSelection sel = new StringSelection(code);
		clp.setContents(sel, sel);
	}

	public static String makeJavaString(String text, boolean removeSemicolon)
	{
		if (text == null) return "";

		String prefix = Settings.getInstance().getProperty("workbench.clipcreate.codeprefix", "String sql = ");
		String concat = Settings.getInstance().getProperty("workbench.clipcreate.concat", "+");
		int indentSize = Settings.getInstance().getIntProperty("workbench.clipcreate.indent", -1);
		boolean includeNewLine = Settings.getInstance().getBoolProperty("workbench.clipcreate.includenewline", true);
		boolean newLineAfterPrefix = Settings.getInstance().getBoolProperty("workbench.clipcreate.startnewline", false);

		StringBuilder result = new StringBuilder(text.length() + prefix.length() + 10);
		result.append(prefix);

		boolean first = true;

		if (newLineAfterPrefix)
		{
			result.append('\n');
			first = false;
		}
		else if (prefix.endsWith("="))
		{
			result.append(" ");
		}

		if (indentSize <= 0)
		{
			indentSize = result.length();
		}

		StringBuilder indent = new StringBuilder(indentSize);
		for (int i=0; i < indentSize; i++) indent.append(' ');

		BufferedReader reader = new BufferedReader(new StringReader(text));
		try
		{
			String line = reader.readLine();
			while (line != null)
			{
				line = StringUtil.replace(line, "\"", "\\\"");
				if (first) first = false;
				else result.append(indent);
				result.append('"');
				if (removeSemicolon)
				{
					line = SqlUtil.trimSemicolon(line);
				}
				result.append(line);

				line = reader.readLine();
				if (line != null)
				{
					if (includeNewLine)
					{
						result.append(" \\n\"");
					}
					else
					{
						result.append(" \"");
					}
					result.append(' ').append(concat).append('\n');
				}
				else
				{
					result.append('"');
				}
			}
			result.append(';');
		}
		catch (Exception e)
		{
			result.append("(Error when creating Java code, see logfile for details)");
			LogMgr.logError("CreateSnippetActions.makeJavaString()", "Error creating Java String", e);
		}
		finally
		{
			FileUtil.closeQuietely(reader);
		}
		return result.toString();
	}

}
