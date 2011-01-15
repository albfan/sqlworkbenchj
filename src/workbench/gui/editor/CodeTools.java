/*
 * CodeTools.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2011, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.gui.editor;

import java.awt.EventQueue;
import workbench.resource.Settings;
import workbench.util.StringUtil;

/**
 *
 * @author Thomas Kellerer
 */
public class CodeTools 
{
	private JEditTextArea editor;
	
	public CodeTools(JEditTextArea client)
	{
		this.editor = client;
	}

	/**
	 *	Change the currently selected text so that it can be used for a SQL IN statement with
	 *	character datatype.
	 *	e.g.
	 *<pre>
	 *1234
	 *5678
	 *</pre>
	 *will be converted to
	 *<pre>
	 *('1234',
	 *'4456')
	 *</pre>
	 */
	public void makeInListForChar()
	{
		this.makeInList(true);
	}

	public void makeInListForNonChar()
	{
		this.makeInList(false);
	}

	protected void makeInList(boolean quoteElements)
	{
		int startline = editor.getSelectionStartLine();
		int endline = editor.getSelectionEndLine();
		int count = (endline - startline + 1);
		final StringBuilder newText = new StringBuilder(count * 80);
		String nl = Settings.getInstance().getInternalEditorLineEnding();
		
		try
		{
			// make sure at least one character from the last line is selected
			// if the selection does not extend into the line, then
			// the line is ignored
			int selectionLength = editor.getSelectionEnd(endline) - editor.getLineStartOffset(endline);
			if (selectionLength <= 0) endline--;
		}
		catch (Exception e)
		{
			// ignore it
		}
		
		int maxElementsPerLine = 5;
		if (quoteElements)
		{
			maxElementsPerLine = Settings.getInstance().getMaxCharInListElements();
		}
		else
		{
			maxElementsPerLine = Settings.getInstance().getMaxNumInListElements();
		}
		int elements = 0;
		
		boolean newLinePending = false;
		
		for (int i=startline; i <= endline; i++)
		{
			String line = editor.getLineText(i);
			if (StringUtil.isEmptyString(line)) continue;
			
			if (i == startline)
			{
				newText.append('(');
			}
			else
			{
				newText.append(", ");
			}
			if (newLinePending)
			{
				newText.append(nl);
				newText.append(' ');
				newLinePending = false;
			}
			if (quoteElements) newText.append('\'');
			newText.append(line.trim());
			if (quoteElements) newText.append('\'');
			elements ++;
			if (i < endline)
			{
				if ((elements & maxElementsPerLine) == maxElementsPerLine) 
				{
					newLinePending = true;
					elements = 0;
				}
			}
		}
		newText.append(')');
		newText.append(nl);
		EventQueue.invokeLater(new Runnable()
		{
			public void run()
			{
				editor.setSelectedText(newText.toString());
			}
		});
	}
	
}
