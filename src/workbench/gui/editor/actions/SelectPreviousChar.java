/*
 * SelectPreviousChar.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.gui.editor.actions;

import java.awt.event.KeyEvent;

/**
 *
 * @author Thomas Kellerer
 */
public class SelectPreviousChar
	extends PreviousChar
{
	public SelectPreviousChar()
	{
		super("TxtEdPrevCharSel", KeyEvent.VK_LEFT, KeyEvent.SHIFT_MASK);
		select = true;
	}
}
