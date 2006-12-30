/*
 * CompletionHandler.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2007, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.gui.completion;

import workbench.db.WbConnection;
import workbench.gui.editor.JEditTextArea;
import workbench.interfaces.StatusBar;

/**
 * @author support@sql-workbench.net
 */
public interface CompletionHandler
{
	void setStatusBar(StatusBar bar);
	void setEditor(JEditTextArea ed);
	void setConnection(WbConnection conn);
	void showCompletionPopup();
	void cancelPopup();
}
