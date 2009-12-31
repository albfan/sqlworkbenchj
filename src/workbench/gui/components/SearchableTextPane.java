/*
 * SearchableTextPane.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2010, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
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
		l.addAction(searcher.getFindAgainAction());
	}

	public void setSelectedText(String aText)
	{
		this.setText(aText);
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
			FileUtil.closeQuitely(in);
		}
	}
	
}
