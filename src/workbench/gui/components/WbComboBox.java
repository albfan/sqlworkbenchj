/*
 * WbComboBox.java
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

import java.awt.Dimension;

import javax.swing.ComboBoxModel;
import javax.swing.JComboBox;

/**
 * A JComboBox allowing a popup that is wider than the combobox itself.
 *
 * As this is not natively supported by Java, this is done using an ugly hack
 * that is floating around on the internet, and which I have found in various places.
 *
 * @author Thomas Kellerer
 */
public class WbComboBox
	extends JComboBox
{
	private boolean layingOut;
	private int popupWidth;

	public WbComboBox()
	{
	}

	public WbComboBox(ComboBoxModel aModel)
	{
		super(aModel);
	}

	/**
	 * Overriden to handle the popup Size
	 */
	@Override
	public void doLayout()
	{
		try
		{
			layingOut = true;
			super.doLayout();
		}
		finally
		{
			layingOut = false;
		}
	}

	public void setPopupWidth(int width)
	{
		this.popupWidth = width;
	}

	/**
	 * Overriden to handle the popup Size
	 */
	@Override
	public Dimension getSize()
	{
		Dimension dim = super.getSize();
		if (!layingOut && popupWidth != 0 && dim.width < popupWidth)
		{
			dim.width = popupWidth;
		}
		return dim;
	}
}
