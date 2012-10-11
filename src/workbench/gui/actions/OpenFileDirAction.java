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
