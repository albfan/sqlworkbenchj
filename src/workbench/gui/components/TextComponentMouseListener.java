/*
 * TextComponentMouseListener.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2010, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.gui.components;

import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.LinkedList;
import java.util.List;
import javax.swing.JMenuItem;
import javax.swing.event.CaretEvent;
import javax.swing.event.CaretListener;
import javax.swing.text.JTextComponent;
import workbench.gui.actions.WbAction;
import workbench.gui.menu.TextPopup;

/**
 * Provide a Cut, Copy, Paste popup menu for Text components
 *
 * @author Thomas Kellerer Kellerer
 */
public class TextComponentMouseListener
	implements MouseListener, CaretListener
{
	private List<JMenuItem> additionalItems = new LinkedList<JMenuItem>();
	private TextPopup popup;
	private JTextComponent text;
	private int originalComponentCount = -1;

	/**
	 * Create a listener to display a context menu with Cut, Copy and Paste
	 * entries.
	 */
	public TextComponentMouseListener()
	{
	}

	/**
	 * Create a listener to display a context menu with Cut, Copy and Paste
	 * entries. The shortcuts of the menu actions will be added to the input
	 * map of the text component.
	 */
	public TextComponentMouseListener(JTextComponent component)
	{
		this.text = component;
		this.popup = createPopup(component);
		this.originalComponentCount = this.popup.getComponentCount();
		this.popup.getCutAction().addToInputMap(text);
		this.popup.getClearAction().addToInputMap(text);
		this.popup.getPasteAction().addToInputMap(text);
		this.popup.getCopyAction().addToInputMap(text);
		text.addCaretListener(this);
		component.addMouseListener(this);
	}

	/**
	 * Add an action to the popup menu. The action's shortcut will
	 * also be added to the underlying text component's input map
	 * (if one was supplied in the constructor)
	 */
	public void addAction(WbAction action)
	{
		this.addMenuItem(action.getMenuItem());
		if (this.text != null)
		{
			action.addToInputMap(text);
		}
	}

	/**
	 * Add additional menu items to the popup menu.
	 */
	public void addMenuItem(JMenuItem item)
	{
		if (popup != null)
		{
			if (this.originalComponentCount > -1 && popup.getComponentCount() == this.originalComponentCount)
			{
				this.popup.addSeparator();
			}
			this.popup.add(item);
		}
		else
		{
			this.additionalItems.add(item);
		}
	}

	public void mouseClicked(MouseEvent e)
	{
		if (e.getButton() == MouseEvent.BUTTON3 && e.getSource() instanceof JTextComponent)
		{
			try
			{
				JTextComponent component = (JTextComponent)e.getSource();
				TextPopup pop = this.popup;
				if (pop == null)
				{
					pop = createPopup(component);
				}
				checkActions(component, pop);
				pop.show(component,e.getX(),e.getY());
			}
			catch (Exception ex)
			{
				ex.printStackTrace();
			}
		}
	}

	private TextPopup createPopup(JTextComponent component)
	{
		ClipboardWrapper wrapp = new ClipboardWrapper(component);
		TextPopup pop = new TextPopup(wrapp);
		if (this.additionalItems.size() > 0)
		{
			pop.addSeparator();
			for (JMenuItem item : additionalItems)
			{
				pop.add(item);
			}
		}
		return pop;
	}
	/** Invoked when the mouse enters a component.
	 *
	 */
	public void mouseEntered(MouseEvent e)
	{
	}

	/** Invoked when the mouse exits a component.
	 *
	 */
	public void mouseExited(MouseEvent e)
	{
	}

	/** Invoked when a mouse button has been pressed on a component.
	 *
	 */
	public void mousePressed(MouseEvent e)
	{
	}

	/** Invoked when a mouse button has been released on a component.
	 *
	 */
	public void mouseReleased(MouseEvent e)
	{
	}

	public void caretUpdate(CaretEvent evt)
	{
		if (this.text != null && this.popup != null)
		{
			checkActions(this.text, this.popup);
		}
	}

	private void checkActions(JTextComponent component, TextPopup pop)
	{
		if (component == null || pop == null) return;

		boolean edit = component.isEditable();
		boolean selected = component.getSelectionEnd() > component.getSelectionStart();
		pop.getCutAction().setEnabled(edit && selected);
		pop.getClearAction().setEnabled(edit && selected);
		pop.getPasteAction().setEnabled(edit);
		pop.getCopyAction().setEnabled(selected);
	}
}
