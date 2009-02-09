/*
 * PlatformShortcuts.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2009, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.resource;

import java.awt.Toolkit;
import java.awt.event.KeyEvent;
import javax.swing.KeyStroke;

/**
 *
 * @author support@sql-workbench.net
 */
public class PlatformShortcuts
{
	public static KeyStroke getDefaultCopyShortcut()
	{
		return KeyStroke.getKeyStroke(KeyEvent.VK_C, getDefaultModifier());
	}

	public static KeyStroke getDefaultCutShortcut()
	{
		return KeyStroke.getKeyStroke(KeyEvent.VK_X, getDefaultModifier());
	}

	public static KeyStroke getDefaultPasteShortcut()
	{
		return KeyStroke.getKeyStroke(KeyEvent.VK_V, getDefaultModifier());
	}

	public static int getDefaultModifier()
	{
		return Toolkit.getDefaultToolkit().getMenuShortcutKeyMask();
	}

}
