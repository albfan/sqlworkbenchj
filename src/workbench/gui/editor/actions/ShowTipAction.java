/*
 * ShowTipAction.java
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
package workbench.gui.editor.actions;

import java.awt.Dimension;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.AdjustmentEvent;
import java.awt.event.AdjustmentListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;

import javax.swing.JToolTip;
import javax.swing.KeyStroke;
import javax.swing.Popup;
import javax.swing.PopupFactory;

import workbench.log.LogMgr;
import workbench.resource.ResourceMgr;

import workbench.gui.actions.WbAction;
import workbench.gui.completion.ParameterTipProvider;
import workbench.gui.components.MultiLineToolTip;
import workbench.gui.editor.InsertTipProvider;
import workbench.gui.editor.JEditTextArea;

/**
 * Display a tooltip for the current statement.
 * Currently this is only supported for INSERT statements.
 *
 * @see workbench.gui.editor.InsertTipProvider
 * @see workbench.gui.completion.ParameterTipProvider
 *
 * @author Thomas Kellerer
 */
public class ShowTipAction
	extends WbAction
	implements MouseListener, KeyListener, AdjustmentListener, FocusListener
{
	private JEditTextArea area;
	private Popup currentPopup;
	private JToolTip currentTooltip;
	private ParameterTipProvider tipProvider;

	public ShowTipAction(JEditTextArea edit, ParameterTipProvider provider)
	{
		super();
		area = edit;
		tipProvider = provider;
		initMenuDefinition("MnuTxtShowInsertParms", KeyStroke.getKeyStroke(KeyEvent.VK_NUMBER_SIGN, KeyEvent.CTRL_MASK));
		setMenuItemName(ResourceMgr.MNU_TXT_SQL);
	}

	/**
	 * Display the tooltip if available
	 * @param e
	 */
	@Override
	public void executeAction(ActionEvent e)
	{
		String tip = tipProvider.getCurrentTooltip();
		if (tip == null)
		{
			closeTooltip();
			return;
		}
		currentTooltip = new MultiLineToolTip();
		currentTooltip.setComponent(area);
		currentTooltip.setTipText(tip);

		showPopupAt(calculatePopupPosition());

		// Make sure we are informed about cursor changes
		// and can hide the tooltip if necessary
		area.notifiyKeyEvents(this);
		area.getPainter().addMouseListener(this);
		currentTooltip.addMouseListener(this);
		area.getHorizontalBar().addAdjustmentListener(this);
		area.getVerticalScrollBar().addAdjustmentListener(this);
		area.addFocusListener(this);
	}

	private void closeTooltip()
	{
		try
		{
			if (currentTooltip != null)
			{
				currentTooltip.removeMouseListener(this);
				currentTooltip = null;
			}
			if (currentPopup != null)
			{
				currentPopup.hide();
				currentPopup = null;
			}
			area.stopKeyNotification();
			area.getPainter().removeMouseListener(this);
			area.getHorizontalBar().removeAdjustmentListener(this);
			area.getVerticalScrollBar().removeAdjustmentListener(this);
			area.removeFocusListener(this);
		}
		catch (Exception e)
		{
			LogMgr.logError("ShowTipAction.closeTooltip()", "Error when closing tip!", e);
		}
	}

	@Override
	public void mouseClicked(MouseEvent e)
	{

		if (e.getSource() == currentTooltip)
		{
			// If the tooltip is clicked, close it
			closeTooltip();
		}
		else if (e.getSource() == area)
		{
			// Handle cursor change in the editor
			updateTooltip();
		}
		else
		{
			LogMgr.logDebug("ShowTipAction.mouseClicked()", "mouseListener was not removed!!", new Exception());
		}
	}

	@Override
	public void mousePressed(MouseEvent e)
	{
	}

	@Override
	public void mouseReleased(MouseEvent e)
	{
	}

	@Override
	public void mouseEntered(MouseEvent e)
	{
	}

	@Override
	public void mouseExited(MouseEvent e)
	{
	}

	@Override
	public void keyReleased(KeyEvent e)
	{
	}

	@Override
	public void keyTyped(KeyEvent e)
	{
		if (e.getSource() == area)
		{
			closeTooltip();
		}
	}

	@Override
	public void keyPressed(KeyEvent e)
	{
		if (e.getSource() == area)
		{
			switch (e.getKeyCode())
			{
				case KeyEvent.VK_RIGHT:
				case KeyEvent.VK_LEFT:
				case KeyEvent.VK_UP:
				case KeyEvent.VK_DOWN:
					updateTooltip();
					break;
				default:
					closeTooltip();
			}
		}
	}

	private void updateTooltip()
	{
		String tip = tipProvider.getCurrentTooltip();
		if (tip == null)
		{
			closeTooltip();
		}
		else
		{
			currentTooltip.setTipText(tip);
			showPopupAt(calculatePopupPosition());
		}
	}

	private Point calculatePopupPosition()
	{
		Point p = area.getCursorLocation();
		Point s = area.getLocationOnScreen();
		int line = area.getCaretLine();
		Dimension dim = currentTooltip.getPreferredSize();
		int height = (int)dim.getHeight();

		int lineY = area.lineToY(line);
		int popupY = s.y + lineY;  // y-position of the top of the line

		if (popupY - height > 0)
		{
			// only show the popup above the current line if it fits on the screen
			popupY -= height;
		}
		else
		{
			// otherwise show the popup below the current line
			popupY = s.y + area.lineToY(line + 1) + 5;
		}
		Point pos = new Point(s.x + p.x,  popupY);
		return pos;
	}

	private void showPopupAt(Point p)
	{
		if (currentPopup != null)
		{
			currentPopup.hide();
		}
		PopupFactory popupFactory = PopupFactory.getSharedInstance();
		currentPopup = popupFactory.getPopup(area, currentTooltip, p.x, p.y);
		currentPopup.show();
	}

	@Override
	public void adjustmentValueChanged(AdjustmentEvent e)
	{
		closeTooltip();
	}

	@Override
	public void focusGained(FocusEvent e)
	{
	}

	@Override
	public void focusLost(FocusEvent e)
	{
		if (e.getComponent() == area)
		{
			closeTooltip();
		}
	}
}
