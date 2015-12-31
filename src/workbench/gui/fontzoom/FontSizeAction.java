/*
 * FontSizeAction.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2016, Thomas Kellerer
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
package workbench.gui.fontzoom;

import java.awt.event.ActionEvent;

import javax.swing.KeyStroke;

import workbench.gui.actions.WbAction;

/**
 * An action to be used for font zooming.
 *
 * @author Thomas Kellerer
 */
public abstract class FontSizeAction
	extends WbAction
{

	private FontZoomer zoomer;

	protected FontSizeAction(String resourceKey, int keyCode, int keyMask)
	{
		this(resourceKey, keyCode, keyMask, null);
	}

	protected FontSizeAction(String resourceKey, int keyCode, int keyMask, FontZoomer fontZoomer)
	{
		super();
		initMenuDefinition(resourceKey, KeyStroke.getKeyStroke(keyCode, keyMask));
		zoomer = fontZoomer;
	}

	@Override
	public void actionPerformed(ActionEvent evt)
	{
		FontZoomer toUse = zoomer;

		if (toUse == null && evt.getSource() instanceof FontZoomProvider)
		{
			// If no zoomer has been registered, check if the source component
			// can be zoomed. If yes, then use the zoomer provided by it
			FontZoomProvider provider = (FontZoomProvider) evt.getSource();
			toUse = provider.getFontZoomer();
		}

		if (toUse != null)
		{
			doFontChange(toUse);
		}
	}

	public abstract void doFontChange(FontZoomer fontZoomer);

}
