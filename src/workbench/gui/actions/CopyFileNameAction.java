/*
 * CopyFileNameAction.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2012, Thomas Kellerer
 * No part of this code may be reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
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
