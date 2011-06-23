/*
 * UnIndentSelection.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2011, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.gui.editor.actions;

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
import workbench.gui.actions.WbAction;
import workbench.gui.completion.ParameterTipProvider;
import workbench.gui.editor.JEditTextArea;
import workbench.log.LogMgr;

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
		setMenuTextByKey("MnuTxtShowInsertParms");
		setDefaultAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_NUMBER_SIGN, KeyEvent.CTRL_MASK));
		initializeShortcut();
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
		currentTooltip = area.createToolTip();
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
		int height = area.getFontMetrics(area.getFont()).getHeight();
		Point pos = new Point(s.x + p.x,  (s.y + p.y) - 2* height);
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
