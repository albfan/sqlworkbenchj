/*
 * EditorPanelTest.java
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
package workbench.gui.sql;

import java.awt.event.ActionEvent;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.Writer;

import workbench.TestUtil;
import workbench.WbTestCase;
import workbench.resource.Settings;

import workbench.gui.editor.InputHandler;

import workbench.util.CharacterRange;
import workbench.util.EncodingUtil;
import workbench.util.FileUtil;
import workbench.util.StringUtil;

import org.junit.Test;

import static org.junit.Assert.*;

/**
 * @author Thomas Kellerer
 */
public class EditorPanelTest
	extends WbTestCase
{
	private TestUtil util;

	public EditorPanelTest()
	{
		super("EditorPanelTest");
		util = getTestUtil();
	}

	private int writeTestFile(File f, String nl)
		throws IOException
	{
		int count = 100;
		Writer w = new OutputStreamWriter(new FileOutputStream(f), "UTF-8");
		for (int i = 0; i < count; i++)
		{
			w.write("This is test line " + i);
			w.write(nl);
		}
		w.close();
		return count;
	}

	public void testSaveFile()
	{
		String dir = util.getBaseDir();
		try
		{
			Settings set = Settings.getInstance();
			EditorPanel p = EditorPanel.createTextEditor();

			set.setExternalEditorLineEnding(Settings.UNIX_LINE_TERMINATOR_PROP_VALUE);
			set.setInternalEditorLineEnding(Settings.DOS_LINE_TERMINATOR_PROP_VALUE);

			ActionEvent evt = new ActionEvent(p,1,"break");
			p.setAutoIndent(false);
			p.appendLine("Line1");
			InputHandler.INSERT_BREAK.actionPerformed(evt);
			p.appendLine("Line2");
			InputHandler.INSERT_BREAK.actionPerformed(evt);
			p.appendLine("Line3");
			InputHandler.INSERT_BREAK.actionPerformed(evt);

			String content = p.getText();
			int pos = content.indexOf("Line2\r\n");
			assertEquals("Wrong internal line ending (DOS) used", 7, pos);

			File f = new File(dir, "editor_unx.txt");
			f.delete();
			p.saveFile(f, "UTF-8", "\n");

			Reader r = EncodingUtil.createReader(f, "UTF-8");
			content = FileUtil.readCharacters(r);
			f.delete();

			pos = content.indexOf("Line2\n");
			assertEquals("Wrong external line ending (Unix) used", 6, pos);

			set.setExternalEditorLineEnding(Settings.DOS_LINE_TERMINATOR_PROP_VALUE);
			set.setInternalEditorLineEnding(Settings.UNIX_LINE_TERMINATOR_PROP_VALUE);

			p = EditorPanel.createTextEditor();
			evt = new ActionEvent(p,1,"break");
			p.setAutoIndent(false);
			p.appendLine("Line1");
			InputHandler.INSERT_BREAK.actionPerformed(evt);
			p.appendLine("Line2");
			InputHandler.INSERT_BREAK.actionPerformed(evt);
			p.appendLine("Line3");
			InputHandler.INSERT_BREAK.actionPerformed(evt);

			content = p.getText();
			System.out.println(StringUtil.escapeText(content, CharacterRange.RANGE_8BIT));
			pos = content.indexOf("Line2\n");
			assertEquals("Wrong internal line ending (Unix) used", 6, pos);

			f = new File(dir, "editor_dos.txt");
			f.delete();
			p.saveFile(f, "UTF-8", "\r\n");
			r = EncodingUtil.createReader(f, "UTF-8");
			content = FileUtil.readCharacters(r);

			pos = content.indexOf("Line2\r\n");
			assertEquals("Wrong exteranl line ending (DOS) used", 7, pos);
		}
		catch (Exception e)
		{
			e.printStackTrace();
			fail("Error loading file");
		}
	}

	@Test
	public void testReadFile()
	{
		String dir = util.getBaseDir();
		File f = new File(dir, "editor.txt");

		try
		{
			Settings set = Settings.getInstance();
			//set.setInternalEditorLineEnding(Settings.UNIX_LINE_TERMINATOR_PROP_VALUE);
			EditorPanel p = EditorPanel.createTextEditor();
			int lines = writeTestFile(f, "\n");

			p.readFile(f, "UTF-8");
			assertEquals("File not loaded", true, p.hasFileLoaded());
			assertEquals("Wrong line count", lines + 1, p.getLineCount());

			p.dispose();

			set.setInternalEditorLineEnding(Settings.DOS_LINE_TERMINATOR_PROP_VALUE);
			p = EditorPanel.createTextEditor();
			lines = writeTestFile(f, "\r\n");

			p.readFile(f, "UTF-8");
			assertEquals("File not loaded", true, p.hasFileLoaded());
			assertEquals("Wrong line count", lines + 1, p.getLineCount());
		}
		catch (Exception e)
		{
			e.printStackTrace();
			fail("Error loading file");
		}
	}

}
