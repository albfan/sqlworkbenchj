/*
 * WbToolbarButton.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2016, Thomas Kellerer
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
package workbench.gui.components;

import java.awt.Insets;

import javax.swing.Action;
import javax.swing.Icon;

import workbench.gui.actions.WbAction;

/**
 *
 * @author Thomas Kellerer
 */
public class WbToolbarButton
	extends WbButton
{
	public static final Insets MARGIN = new Insets(1,1,1,1);

	public WbToolbarButton()
	{
		super();
		init();
	}

	public WbToolbarButton(String aText)
	{
		super(aText);
		init();
	}

	public WbToolbarButton(Action a)
	{
		super(a);
		this.setText(null);
		iconButton = true;
		init();
	}

	public WbToolbarButton(Icon icon)
	{
		super(icon);
    this.setText(null);
		init();
	}

	public void setAction(WbAction a)
	{
		super.setAction(a);
    // only remove the text if we do not have an icon
		if (a.getIconKey() != null)
    {
      this.setText(null);
    }
		init();
	}

	private void init()
	{
		this.setMargin(MARGIN);
    this.setFocusable(false);
	}

}
