/*
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 * Copyright 2002-2008, Thomas Kellerer
 *
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */

package workbench.gui.editor.actions;

import java.awt.event.KeyEvent;
import workbench.resource.PlatformShortcuts;

/**
 *
 * @author support@sql-workbench.net
 */
public class SelectPrevWord
	extends PrevWord
{

	public SelectPrevWord()
	{
		super("TxtEdPrvWordSel", PlatformShortcuts.getDefaultPrevWord(true));
		select = true;
	}

}
