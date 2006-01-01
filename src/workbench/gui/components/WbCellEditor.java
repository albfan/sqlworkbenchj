/*
 * WbCellEditor.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2006, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.gui.components;


import java.awt.Component;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.InputEvent;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EventObject;

import javax.swing.AbstractAction;
import javax.swing.AbstractCellEditor;
import javax.swing.InputMap;
import javax.swing.JComponent;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.KeyStroke;
import javax.swing.event.CellEditorListener;
import javax.swing.event.ChangeEvent;
import javax.swing.table.TableCellEditor;

import workbench.gui.WbSwingUtilities;

public class WbCellEditor 
	extends AbstractCellEditor
	implements TableCellEditor
{
	private TextAreaEditor editor;
	private JScrollPane scroll;
	private ArrayList listeners;
	private ChangeEvent changedEvent;
	
	private static final KeyStroke CTRL_TAB = KeyStroke.getKeyStroke("control TAB");
	private static final KeyStroke TAB = KeyStroke.getKeyStroke("TAB");
	private static final KeyStroke ENTER = KeyStroke.getKeyStroke("ENTER");
	private static final KeyStroke CTRL_ENTER = KeyStroke.getKeyStroke("control ENTER");
	
	public WbCellEditor()
	{
		editor = new TextAreaEditor();
    setDefaultCopyPasteKeys(editor);
		scroll = new TextAreaScrollPane(editor);
		scroll.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_NEVER);
		scroll.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
		editor.setLineWrap(false);
		editor.setWrapStyleWord(true);
		editor.setBorder(WbSwingUtilities.EMPTY_BORDER);
		scroll.setBorder(WbSwingUtilities.EMPTY_BORDER);
		editor.addMouseListener(new TextComponentMouseListener());
	}
	
  public static void setDefaultCopyPasteKeys(JComponent editor)
  {
    InputMap im = editor.getInputMap();
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
		if (anEvent instanceof MouseEvent)
		{
			return ((MouseEvent)anEvent).getClickCount() >= 2;
		}
		else if (anEvent instanceof KeyEvent)
		{
			return (((KeyEvent)anEvent).getKeyCode() == KeyEvent.VK_F2);
		}
		return true;
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

	public boolean isManagingFocus() { return false; }
	
	public void addCellEditorListener(CellEditorListener l)
	{
		if (this.listeners == null) this.listeners = new ArrayList();
		this.listeners.add(l);
	}
	
	public void cancelCellEditing()
	{
		this.fireEditingCanceled();
	}
	
	public void removeCellEditorListener(CellEditorListener l)
	{
		if (this.listeners == null) return;
		this.listeners.remove(l);
	}
	
	public boolean startCellEditing()
	{
		return true;
	}
	
	public boolean stopCellEditing()
	{
		this.fireEditingStopped(); 
		return true;
	}

	public void fireEditingCanceled()
	{
		if (this.listeners == null) return;
		for (int i=0; i < this.listeners.size(); i++)
		{
			CellEditorListener l = (CellEditorListener)this.listeners.get(i);
			if (this.changedEvent == null) this.changedEvent = new ChangeEvent(this);
			if (l != null) l.editingCanceled(this.changedEvent);
		}
	}
	
	public void fireEditingStopped()
	{
		if (this.listeners == null) return;
		for (int i=0; i < this.listeners.size(); i++)
		{
			CellEditorListener l = (CellEditorListener)this.listeners.get(i);
			if (this.changedEvent == null) this.changedEvent = new ChangeEvent(this);
			if (l != null) l.editingStopped(this.changedEvent);
		}
	}
	
	class TextAreaEditor 
		extends JTextArea
		implements ItemListener
	{
		public TextAreaEditor()
		{
			super();
			this.setFocusCycleRoot(false);
			//this.setFocusTraversalKeys(WHEN_FOCUSED, Collections.EMPTY_SET);
			Object tabAction = this.getInputMap().get(TAB);
			
			this.getInputMap().put(TAB, "wb-do-nothing-at-all");

			if (tabAction != null) 
			{
				this.getInputMap().put(CTRL_TAB, tabAction);
				this.getInputMap().put(CTRL_TAB, tabAction);
			}

			Object enterAction = this.getInputMap().get(ENTER);

			this.getInputMap().put(ENTER, "wb-stop-editing");
			this.getActionMap().put("stopEditing", new AbstractAction("wb-stop-editing")
				{
						public void actionPerformed(ActionEvent e)
						{
								stopCellEditing();
						}
				}
			); 
			if (enterAction != null)
			{
				this.getInputMap().put(CTRL_ENTER, enterAction);
			}

		}

//		public boolean processKeyBinding(KeyStroke ks, KeyEvent e, int condition, boolean pressed)
//		{
//			if (ks.equals(TAB))
//			{
//				//System.out.println("TAB pressed");
//				//int pos = this.getCaretPosition();
//				//this.insert("\t", pos);
//				e.consume();
//				return true;
//			}
//			return super.processKeyBinding(ks, e, condition, pressed);
//		}

		/* (non-Javadoc)
		 * @see java.awt.event.ItemListener#itemStateChanged(java.awt.event.ItemEvent)
		 */
		public void itemStateChanged(ItemEvent e)
		{
			stopCellEditing();
		}

		//public boolean isManagingFocus() { return false; }
	}
	
	public class TextAreaScrollPane 
		extends JScrollPane
		implements ItemListener
	{
		TextAreaEditor editor;
		
		public TextAreaScrollPane(Component content)
		{
			super(content);
			this.setFocusCycleRoot(false);
			this.setFocusTraversalKeys(WHEN_FOCUSED, Collections.EMPTY_SET);
			if (content instanceof TextAreaEditor)
			{
				editor = (TextAreaEditor)content;
			}
		}

//		public boolean processKeyBinding(KeyStroke ks, KeyEvent e, int condition, boolean pressed)
//		{
//			return editor.processKeyBinding(ks, e, condition, pressed);
//		}

		//public boolean isManagingFocus() { return false; }
		public void requestFocus()
		{
			this.editor.requestFocus();
		}
		
		public boolean requestFocusInWindow()
		{
			return this.editor.requestFocusInWindow();
		}

		/* (non-Javadoc)
		 * @see java.awt.event.ItemListener#itemStateChanged(java.awt.event.ItemEvent)
		 */
		public void itemStateChanged(ItemEvent e)
		{
			stopCellEditing();
		}
		
	}
	
} 
