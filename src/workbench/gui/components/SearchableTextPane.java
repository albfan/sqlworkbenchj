/*
 * SearchableTextPane.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2008, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.gui.components;

import java.awt.Window;
import javax.swing.JTextPane;
import workbench.gui.editor.SearchAndReplace;
import workbench.interfaces.TextContainer;

/**
 * @author support@sql-workbench.net
 */
public class SearchableTextPane
	extends JTextPane
	implements TextContainer
{
	private SearchAndReplace searcher;
	
	public SearchableTextPane(Window owner)
	{
		super();
		searcher = new SearchAndReplace(owner, this);
		TextComponentMouseListener l = new TextComponentMouseListener(this);
		l.addAction(searcher.getFindAction());
		l.addAction(searcher.getFindAgainAction());
	}

	public void setSelectedText(String aText)
	{
		this.setText(aText);
	}
	
}
