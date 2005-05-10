/*
 * AutoCompletionAction.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2005, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: info@sql-workbench.net
 *
 */
package workbench.gui.actions;

import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import javax.swing.KeyStroke;
import workbench.gui.completion.CompletionHandler;

/**
 * @author  info@sql-workbench.net
 */
public class AutoCompletionAction
	extends WbAction
{
	private CompletionHandler handler;
	
	public AutoCompletionAction(CompletionHandler h)
	{
		this.handler = h;
		this.initMenuDefinition("MnuTxtAutoComplete", KeyStroke.getKeyStroke(KeyEvent.VK_G,KeyEvent.CTRL_MASK));
		this.setEnabled(true);
	}

	public void executeAction(ActionEvent e)
	{
		this.handler.showCompletionPopup();
	}
}
