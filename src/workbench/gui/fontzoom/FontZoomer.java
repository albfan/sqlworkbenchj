/*
 * FontZoomer
 * 
 *  This file is part of SQL Workbench/J, http://www.sql-workbench.net
 * 
 *  Copyright 2002-2011, Thomas Kellerer
 *  No part of this code may be reused without the permission of the author
 * 
 *  To contact the author please send an email to: support@sql-workbench.net
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
