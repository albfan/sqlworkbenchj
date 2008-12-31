/*
 * WbCellEditor.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2008, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.gui.components;

import java.awt.Component;
import java.awt.Font;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.Collections;
import java.util.EventObject;
import java.util.Set;
import javax.swing.AbstractCellEditor;
import javax.swing.InputMap;
import javax.swing.JComponent;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.KeyStroke;
import javax.swing.table.TableCellEditor;
import workbench.gui.WbSwingUtilities;

/**
 *
 * @author support@sql-workbench.net
 */
public class WbCellEditor
	extends AbstractCellEditor
	implements TableCellEditor, MouseListener
{
	private TextAreaEditor editor;
	private WbTable parentTable;
	private JScrollPane scroll;

	public WbCellEditor(WbTable parent)
	{
		super();
		parentTable = parent;
		editor = new TextAreaEditor();
		setDefaultCopyPasteKeys(editor);
		setFont(parent.getFont());
		scroll = new TextAreaScrollPane(editor);
		scroll.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
		scroll.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
		editor.setLineWrap(false);
		editor.setWrapStyleWord(true);
		editor.setBorder(WbSwingUtilities.EMPTY_BORDER);
		scroll.setBorder(WbSwingUtilities.EMPTY_BORDER);
		editor.addMouseListener(new TextComponentMouseListener());
		editor.addMouseListener(this);
	}

	protected void setDefaultCopyPasteKeys(JComponent edit)
	{
		InputMap im = edit.getInputMap();
		KeyStroke ks = KeyStroke.getKeyStroke(KeyEvent.VK_C, InputEvent.CTRL_MASK);
		KeyStroke ksnew = KeyStroke.getKeyStroke(KeyEvent.VK_INSERT, InputEvent.CTRL_MASK);

		Object cmd = im.get(ks);
		im.put(ksnew, cmd);

		ks = KeyStroke.getKeyStroke(KeyEvent.VK_V, InputEvent.CTRL_MASK);
		ksnew = KeyStroke.getKeyStroke(KeyEvent.VK_INSERT, InputEvent.SHIFT_MASK);

		cmd = im.get(ks);
		im.put(ksnew, cmd);

		ks = KeyStroke.getKeyStroke(KeyEvent.VK_X, InputEvent.CTRL_MASK);
		ksnew = KeyStroke.getKeyStroke(KeyEvent.VK_DELETE, InputEvent.SHIFT_MASK);

		cmd = im.get(ks);
		im.put(ksnew, cmd);
	}

	@Override
	public void cancelCellEditing()
	{
		super.cancelCellEditing();
		// For some reason Swing resets the row height for all rows when
		// stopping the editing. If automatic row height calculation is turned on
		// we need to re-calculate the row heights
		parentTable.adjustRowHeight();
	}

	@Override
	public boolean stopCellEditing()
	{
		boolean result = super.stopCellEditing();
		// For some reason Swing resets the row height for all rows when
		// stopping the editing. If automatic row height calculation is turned on
		// we need to re-calculate the row heights
		parentTable.adjustRowHeight();
		return result;
	}


	public void setFont(Font aFont)
	{
		this.editor.setFont(aFont);
	}

	public Component getComponent()
	{
		return scroll;
	}

	public Object getCellEditorValue()
	{
		return editor.getText();
	}

	public boolean isCellEditable(EventObject anEvent)
	{
		boolean result = true;
		if (anEvent instanceof MouseEvent)
		{
			result = ((MouseEvent) anEvent).getClickCount() >= 2;
		}
		if (result) WbSwingUtilities.requestFocus(editor);
		return result;
	}

	public Component getTableCellEditorComponent(JTable table, Object value,
		boolean isSelected,
		int row, int column)
	{
		editor.setText((value != null) ? value.toString() : "");
		return scroll;
	}

	public boolean shouldSelectCell(EventObject anEvent)
	{
		return true;
	}

	public boolean isManagingFocus()
	{
		return false;
	}

	public void mouseClicked(MouseEvent evt)
	{
		if (evt.getClickCount() == 2 && evt.getButton() == MouseEvent.BUTTON1 && this.parentTable != null)
		{
			parentTable.openEditWindow();
		}
	}

	public void mousePressed(MouseEvent evt)
	{
	}

	public void mouseReleased(MouseEvent evt)
	{
	}

	public void mouseEntered(MouseEvent evt)
	{
	}

	public void mouseExited(MouseEvent evt)
	{
	}


	static class TextAreaEditor
		extends JTextArea
	{
		public TextAreaEditor()
		{
			super();
			this.setFocusCycleRoot(false);
			Set<? extends java.awt.AWTKeyStroke> empty = Collections.emptySet();
			this.setFocusTraversalKeys(WHEN_FOCUSED, empty);

			Object tabAction = this.getInputMap().get(WbSwingUtilities.TAB);

			this.getInputMap().put(WbSwingUtilities.TAB, "wb-do-nothing-at-all");

			if (tabAction != null)
			{
				this.getInputMap().put(WbSwingUtilities.CTRL_TAB, tabAction);
			}

			// Remove the default action for the Enter key and replace it with
			// the "stop-editing" action which is the default for all other editors.
			Object enterAction = this.getInputMap().get(WbSwingUtilities.ENTER);

			this.getInputMap().put(WbSwingUtilities.ENTER, "wb-stop-editing");

			if (enterAction != null)
			{
				// This will map the original Action for Enter (basically: create a new line)
				// to Alt-Enter and Ctrl-Enter
				this.getInputMap().put(WbSwingUtilities.CTRL_ENTER, enterAction);
				this.getInputMap().put(WbSwingUtilities.ALT_ENTER, enterAction);
			}
		}

		@Override
		public boolean isManagingFocus()
		{
			return false;
		}
	}

	public class TextAreaScrollPane
		extends JScrollPane
	{
		TextAreaScrollPane(TextAreaEditor content)
		{
			super(content);
			setFocusCycleRoot(false);
			Set<? extends java.awt.AWTKeyStroke> empty = Collections.emptySet();
			this.setFocusTraversalKeys(WHEN_FOCUSED, empty);
		}

		@Override
		public boolean isManagingFocus()
		{
			return false;
		}

		@Override
		public boolean requestFocus(boolean temp)
		{
			return editor.requestFocus(temp);
		}

		@Override
		public void requestFocus()
		{
			editor.requestFocus();
		}

		@Override
		public boolean requestFocusInWindow()
		{
			return editor.requestFocusInWindow();
		}

		@Override
		public boolean requestFocusInWindow(boolean temp)
		{
			return editor.requestFocusInWindow();
		}

	}
}
