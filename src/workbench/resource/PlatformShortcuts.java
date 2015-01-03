/*
 * PlatformShortcuts.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2015, Thomas Kellerer
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at.
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.resource;

import java.awt.Toolkit;
import java.awt.event.KeyEvent;
import javax.swing.KeyStroke;
import workbench.util.PlatformHelper;

/**
 * Centralize the definition of default keyboard shortcuts for MacOS and the rest of the world.
 *
 * KeyEvent.META_MASK is the "Command" (or "Apple") Key<br/>
 * KeyEvent.ALT_MASK is the "Option" key<br/>s
 * KeyEvent.CTRL_MASK is the Control key<br/>
 * 
 * @author Thomas Kellerer
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

	public static KeyStroke getDefaultPrevWord(boolean select)
	{
		if (PlatformHelper.isMacOS())
		{
			return KeyStroke.getKeyStroke(KeyEvent.VK_LEFT, KeyEvent.ALT_MASK | (select ? KeyEvent.SHIFT_MASK : 0) );
		}
		return KeyStroke.getKeyStroke(KeyEvent.VK_LEFT, KeyEvent.CTRL_MASK | (select ? KeyEvent.SHIFT_MASK : 0));
	}

	public static KeyStroke getDefaultNextWord(boolean select)
	{
		if (PlatformHelper.isMacOS())
		{
			return KeyStroke.getKeyStroke(KeyEvent.VK_RIGHT, KeyEvent.ALT_MASK | (select ? KeyEvent.SHIFT_MASK : 0) );
		}
		return KeyStroke.getKeyStroke(KeyEvent.VK_RIGHT, KeyEvent.CTRL_MASK | (select ? KeyEvent.SHIFT_MASK : 0));
	}

	public static KeyStroke getDefaultEndOfLine(boolean select)
	{
		if (PlatformHelper.isMacOS())
		{
			return KeyStroke.getKeyStroke(KeyEvent.VK_RIGHT, KeyEvent.META_MASK | (select ? KeyEvent.SHIFT_MASK : 0) );
		}
		return KeyStroke.getKeyStroke(KeyEvent.VK_END, (select ? KeyEvent.SHIFT_MASK : 0));
	}

	public static KeyStroke getDefaultStartOfLine(boolean select)
	{
		if (PlatformHelper.isMacOS())
		{
			return KeyStroke.getKeyStroke(KeyEvent.VK_LEFT, KeyEvent.META_MASK | (select ? KeyEvent.SHIFT_MASK : 0) );
		}
		return KeyStroke.getKeyStroke(KeyEvent.VK_HOME, (select ? KeyEvent.SHIFT_MASK : 0));
	}

	public static KeyStroke getDefaultStartOfDoc(boolean select)
	{
		if (PlatformHelper.isMacOS())
		{
			return KeyStroke.getKeyStroke(KeyEvent.VK_UP, KeyEvent.META_MASK | (select ? KeyEvent.SHIFT_MASK : 0) );
		}
		return KeyStroke.getKeyStroke(KeyEvent.VK_HOME, KeyEvent.CTRL_MASK | (select ? KeyEvent.SHIFT_MASK : 0));
	}

	public static KeyStroke getDefaultEndOfDoc(boolean select)
	{
		if (PlatformHelper.isMacOS())
		{
			return KeyStroke.getKeyStroke(KeyEvent.VK_DOWN, KeyEvent.META_MASK | (select ? KeyEvent.SHIFT_MASK : 0) );
		}
		return KeyStroke.getKeyStroke(KeyEvent.VK_END, KeyEvent.CTRL_MASK | (select ? KeyEvent.SHIFT_MASK : 0));
	}

	/**
	 * Return the shortcut to select the next statement in the statement history
	 */
	public static KeyStroke getDefaultNextStatement()
	{
		if (PlatformHelper.isMacOS())
		{
			return null;
		}
		return KeyStroke.getKeyStroke(KeyEvent.VK_RIGHT, KeyEvent.ALT_MASK);
	}

	/**
	 * Return the shortcut to select the previous statement in the statement history
	 *
	 */
	public static KeyStroke getDefaultPrevStatement()
	{
		if (PlatformHelper.isMacOS())
		{
			return null;
		}
		return KeyStroke.getKeyStroke(KeyEvent.VK_LEFT, KeyEvent.ALT_MASK);
	}

}
