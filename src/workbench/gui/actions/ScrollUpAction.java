/*
 * ScrollUpAction.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2011, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.gui.actions;

import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import javax.swing.KeyStroke;
import workbench.gui.editor.LineScroller;

/**
 *
 * @author Thomas Kellerer
 */
public class ScrollUpAction
	extends WbAction
{
	private LineScroller client;

	public ScrollUpAction(LineScroller scroller)
	{
		super();
		client = scroller;
		initMenuDefinition("MnuTxtScrollUp", KeyStroke.getKeyStroke(KeyEvent.VK_UP, KeyEvent.CTRL_MASK));
	}

	public boolean isEnabled()
	{
		return client != null && client.canScrollUp();
	}

	@Override
	public void executeAction(ActionEvent e)
	{
		client.scrollUp();
	}

}
