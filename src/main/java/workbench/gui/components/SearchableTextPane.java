/*
 * SearchableTextPane.java
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
package workbench.gui.components;

import java.awt.Window;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.net.URL;
import javax.swing.JTextArea;
import workbench.gui.editor.SearchAndReplace;
import workbench.interfaces.TextContainer;
import workbench.resource.Settings;
import workbench.util.EncodingUtil;
import workbench.util.FileUtil;

/**
 * @author Thomas Kellerer
 */
public class SearchableTextPane
	extends JTextArea
	implements TextContainer
{
	public SearchableTextPane(Window owner)
	{
		super();
		SearchAndReplace searcher = new SearchAndReplace(owner, this);
		TextComponentMouseListener l = new TextComponentMouseListener(this);
		l.addAction(searcher.getFindAction());
		l.addAction(searcher.getFindNextAction());
	}

	@Override
	public void setSelectedText(String aText)
	{
		this.setText(aText);
	}

	@Override
	public boolean isTextSelected()
	{
		return getSelectionStart() < getSelectionEnd();
	}

	public void setPage(URL url)
		throws IOException
	{
		InputStream in = url.openStream();
		try
		{
			Reader r = EncodingUtil.createReader(in, Settings.getInstance().getDefaultEncoding());
			read(r, null);
		}
		finally
		{
			FileUtil.closeQuietely(in);
		}
	}

}
