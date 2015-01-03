/*
 * FontZoomer.java
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
package workbench.gui.fontzoom;

import java.awt.Font;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import javax.swing.JComponent;
import workbench.gui.actions.WbAction;

/**
 *
 * @author Thomas Kellerer
 */
public class FontZoomer
	implements MouseWheelListener
{

	private JComponent client;
	private Font originalFont;

	public FontZoomer(JComponent toZoom)
	{
		client = toZoom;
	}

	public void resetFontZoom()
	{
		if (originalFont != null)
		{
			client.setFont(originalFont);
		}
		originalFont = null;
	}

	public void increaseFontSize()
	{
		applyFontScale(1.1d);
	}

	public void decreaseFontSize()
	{
		applyFontScale(0.9d);
	}

	private void applyFontScale(double scale)
	{
		Font f = client.getFont();
		if (f == null)
		{
			return;
		}

		if (originalFont == null)
		{
			originalFont = f;
		}
		Font newFont = f.deriveFont((float) (f.getSize() * scale));
		client.setFont(newFont);
	}

	@Override
	public void mouseWheelMoved(MouseWheelEvent e)
	{
		if (e.getScrollType() == MouseWheelEvent.WHEEL_UNIT_SCROLL && WbAction.isCtrlPressed(e.getModifiers()))
		{
			if (e.getWheelRotation() > 0)
			{
				decreaseFontSize();
			}
			else
			{
				increaseFontSize();
			}
		}
	}
}
