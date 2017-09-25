/*
 * TextComponentMouseListener.java
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
package workbench.gui.components;

import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.ArrayList;
import java.util.List;

import javax.swing.InputMap;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.KeyStroke;
import javax.swing.event.CaretEvent;
import javax.swing.event.CaretListener;
import javax.swing.text.JTextComponent;

import workbench.resource.PlatformShortcuts;
import workbench.resource.Settings;

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
	private List<JMenuItem> additionalItems = new ArrayList<>();
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

	public static void addListener(JTextComponent ... components)
	{
		if (components == null || components.length == 0) return;
		for (JTextComponent comp : components)
		{
			new TextComponentMouseListener(comp);
		}
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

		boolean extendedCutCopyPaste = Settings.getInstance().getBoolProperty("workbench.editor.extended.cutcopypaste", true);

		if (extendedCutCopyPaste)
		{
			setExtendedCopyAndPasteKeys();
		}
	}

	private void setExtendedCopyAndPasteKeys()
	{
		InputMap im = text.getInputMap();
		KeyStroke ksnew = KeyStroke.getKeyStroke(KeyEvent.VK_INSERT, PlatformShortcuts.getDefaultModifier());

		Object cmd = im.get(popup.getCopyAction().getAccelerator());
		im.put(ksnew, cmd);

		ksnew = KeyStroke.getKeyStroke(KeyEvent.VK_INSERT, InputEvent.SHIFT_MASK);

		cmd = im.get(popup.getPasteAction().getAccelerator());
		im.put(ksnew, cmd);

		ksnew = KeyStroke.getKeyStroke(KeyEvent.VK_DELETE, InputEvent.SHIFT_MASK);

		cmd = im.get(popup.getCutAction().getAccelerator());
		im.put(ksnew, cmd);
	}


  public void addActionAtStart(WbAction action, boolean withSeparator)
  {
    if (popup != null)
    {
      popup.add(action.getMenuItem(), 0);
      if (withSeparator)
      {
        popup.add(new JPopupMenu.Separator(), 1);
      }
    }
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

	@Override
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
	@Override
	public void mouseEntered(MouseEvent e)
	{
	}

	/** Invoked when the mouse exits a component.
	 *
	 */
	@Override
	public void mouseExited(MouseEvent e)
	{
	}

	/** Invoked when a mouse button has been pressed on a component.
	 *
	 */
	@Override
	public void mousePressed(MouseEvent e)
	{
	}

	/** Invoked when a mouse button has been released on a component.
	 *
	 */
	@Override
	public void mouseReleased(MouseEvent e)
	{
	}

	@Override
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

	public void dispose()
	{
		WbMenu.disposeMenu(popup);
		if (text != null)
		{
			text.removeMouseListener(this);
		}
		text = null;
		this.additionalItems.clear();
	}
}
