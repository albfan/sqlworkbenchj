/*
 * MacroDefinition.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2017, Thomas Kellerer
 *
 * Licensed under a modified Apache License, Version 2.0
 * that restricts the use for certain governments.
 * You may not use this file except in compliance with the License.
 * You may obtain a copy of the License at.
 *
 *     http://sql-workbench.net/manual/license.html
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
package workbench.sql.macros;

import workbench.resource.GuiSettings;
import workbench.resource.StoreableKeyStroke;
import workbench.util.HtmlUtil;

import workbench.util.StringUtil;

/**
 * A single Macro that maps a name to a SQL text.
 * <br/>
 * Each Macro defines a sort order (that is maintained through the GUI).
 * A macro can have a keyboard shortcut assigned and can be hidden from the
 * menu.
 *
 * A macro can also be defined as "expandable", in that case it will be checked during editing if the
 * macro name is entered. If that is the case the typed word will be replaced with the actual macro text.
 *
 * @author Thomas Kellerer
 */
public class MacroDefinition
	implements Sortable
{
	private String name;
	private String text;
  private String tooltip;
	private int sortOrder;
	private boolean modified;
	private StoreableKeyStroke shortcut;
	private boolean showInMenu = true;
	private boolean showInPopup = true;
	private boolean expandWhileTyping;
	private boolean appendResult;
	private boolean shortcutChanged;

	public MacroDefinition()
	{
	}

	public MacroDefinition(String macroName, String macroText)
	{
		this.name = macroName;
		this.text = macroText;
	}

  public String getDisplayTooltip()
  {
    if (tooltip == null && GuiSettings.useMacroSourceForMenuTooltip())
    {
      int len = GuiSettings.getMacroSourceTooltipLength();
      return "<html><pre>" + HtmlUtil.escapeXML(StringUtil.getMaxSubstring(getText(), len), false) + "</pre></html>";
    }
    return tooltip;
  }

  public String getTooltip()
  {
    return tooltip;
  }

  public void setTooltip(String tip)
  {
    modified = modified || StringUtil.stringsAreNotEqual(tooltip, tip);
    tooltip = StringUtil.trimToNull(tip);
  }

	public void setExpandWhileTyping(boolean flag)
	{
		modified = modified || (expandWhileTyping != flag);
		this.expandWhileTyping = flag;
	}

	public boolean getExpandWhileTyping()
	{
		return this.expandWhileTyping;
	}

	public boolean isVisibleInMenu()
	{
		return showInMenu;
	}

	public void setVisibleInMenu(boolean flag)
	{
		modified = modified || (showInMenu != flag);
		this.showInMenu = flag;
	}

	public boolean isVisibleInPopup()
	{
		return showInPopup;
	}

	public void setVisibleInPopup(boolean flag)
	{
		modified = modified || (showInPopup != flag);
		showInPopup = flag;
	}

	public boolean isAppendResult()
	{
		return appendResult;
	}

	public void setAppendResult(boolean flag)
	{
		modified = modified || (appendResult != flag);
		this.appendResult = flag;
	}

	@Override
	public int getSortOrder()
	{
		return sortOrder;
	}

	@Override
	public void setSortOrder(int order)
	{
		modified = modified || (order != sortOrder);
		this.sortOrder = order;
	}

	public String getName()
	{
		return name;
	}

	public void setName(String macroName)
	{
		modified = modified || StringUtil.stringsAreNotEqual(this.name, macroName);
		this.name = macroName;
	}

	public String getText()
	{
		return text;
	}

	public void setText(String macroText)
	{
		modified = modified || StringUtil.stringsAreNotEqual(this.text, macroText);
		this.text = macroText;
	}

	public void copyTo(MacroDefinition def)
	{
		def.setName(this.name);
    def.setTooltip(this.tooltip);
		def.setText(this.text);
		def.setSortOrder(this.sortOrder);
		def.setVisibleInMenu(this.showInMenu);
		def.setVisibleInPopup(this.showInPopup);
		def.setShortcut(this.shortcut);
		def.setExpandWhileTyping(this.expandWhileTyping);
		def.setAppendResult(this.appendResult);
	}

	public MacroDefinition createCopy()
	{
		MacroDefinition newMacro = new MacroDefinition();
		this.copyTo(newMacro);
		newMacro.modified = false;
		return newMacro;
	}

	@Override
	public boolean equals(Object obj)
	{
		if (obj == null)
		{
			return false;
		}
		if (getClass() != obj.getClass())
		{
			return false;
		}
		final MacroDefinition other = (MacroDefinition) obj;
		if ((this.name == null) ? (other.name != null) : !this.name.equalsIgnoreCase(other.name))
		{
			return false;
		}
		return true;
	}

	@Override
	public int hashCode()
	{
		int hash = 7;
		hash = 47 * hash + (this.name != null ? this.name.toLowerCase().hashCode() : 0);
		return hash;
	}

	public boolean isModified()
	{
		return modified;
	}

	public void resetModified()
	{
		modified = false;
		shortcutChanged = true;
	}

	public boolean isShortcutChanged()
	{
		return shortcutChanged;
	}

	@Override
	public String toString()
	{
		return name;
	}

	public StoreableKeyStroke getShortcut()
	{
		return shortcut;
	}

	public void setShortcut(StoreableKeyStroke keystroke)
	{
		if (keystroke != null && this.shortcut == null)
		{
			modified = true;
			shortcutChanged = true;
		}
		else if (keystroke == null && shortcut != null)
		{
			modified = true;
			shortcutChanged = true;
		}
		else if (keystroke != null && shortcut != null)
		{
			modified = !keystroke.equals(shortcut);
			shortcutChanged = true;
		}
		this.shortcut = keystroke;
	}
}
