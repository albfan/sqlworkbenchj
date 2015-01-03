/*
 * ShortcutDisplay.java
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
package workbench.gui.settings;

import javax.swing.KeyStroke;
import workbench.resource.ShortcutDefinition;
import workbench.resource.StoreableKeyStroke;

/**
 *
 * @author Thomas Kellerer
 */
class ShortcutDisplay
{
	public static final int TYPE_DEFAULT_KEY = 1;
	public static final int TYPE_PRIMARY_KEY = 2;
	public static final int TYPE_ALTERNATE_KEY = 3;

	private boolean isModified = false;
	private int displayType;
	private ShortcutDefinition shortcut;
	private boolean clearKey = false;
	private boolean resetToDefault = false;

	private StoreableKeyStroke newKey = null;

	ShortcutDisplay(ShortcutDefinition def, int type)
	{
		this.shortcut = def;
		this.displayType = type;
	}

	public ShortcutDefinition getShortcut()
	{
		return this.shortcut;
	}

	public boolean isModified()
	{
		return this.isModified;
	}

	public boolean isCleared()
	{
		return this.clearKey;
	}

	public void clearKey()
	{
		this.newKey = null;
		this.clearKey = true;
		this.isModified = true;
		this.resetToDefault = false;
	}

	public void setNewKey(KeyStroke aKey)
	{
		this.newKey = new StoreableKeyStroke(aKey);
		this.isModified = true;
		this.resetToDefault = false;
		this.clearKey = false;
	}

	public StoreableKeyStroke getNewKey()
	{
		return this.newKey;
	}

	public boolean isMappedTo(KeyStroke aKey)
	{
		boolean mapped = false;
		if (newKey != null)
		{
			mapped = newKey.equals(aKey);
		}
		if (!mapped)
		{
			mapped = this.shortcut.isMappedTo(aKey);
		}
		return mapped;
	}

	public boolean doReset()
	{
		return this.resetToDefault;
	}

	public void resetToDefault()
	{
		this.isModified = true;
		this.newKey = null;
		this.clearKey = false;
		this.resetToDefault = true;
	}

	@Override
	public String toString()
	{
		StoreableKeyStroke key = null;
		switch (this.displayType)
		{
			case TYPE_DEFAULT_KEY:
				key = this.shortcut.getDefaultKey();
				break;
			case TYPE_PRIMARY_KEY:
				if (this.clearKey)
				{
					key = null;
				}
				else if (this.resetToDefault)
				{
					key = this.shortcut.getDefaultKey();
				}
				else if (this.newKey == null)
				{
					key = this.shortcut.getActiveKey();
				}
				else
				{
					key = this.newKey;
				}
				break;
			case TYPE_ALTERNATE_KEY:
				key = this.shortcut.getAlternateKey();
				break;
		}
		if (key == null) return "";
		return key.toString();
	}
}
