package workbench.gui.components;


import java.awt.Component;
import java.awt.event.*;
import java.awt.AWTEvent;
import java.awt.event.KeyEvent;
import java.lang.Boolean;
import javax.swing.table.*;
import javax.swing.event.*;
import java.util.EventObject;
import javax.swing.tree.*;
import java.io.Serializable;
import java.util.ArrayList;
import javax.swing.AbstractAction;
import javax.swing.AbstractCellEditor;
import javax.swing.Action;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.KeyStroke;
import javax.swing.event.ChangeEvent;
import javax.swing.table.TableCellRenderer;
import javax.swing.text.Keymap;
import workbench.gui.WbSwingUtilities;

public class WbCellEditor 
	implements TableCellEditor
{
	private TextAreaEditor editor;
	private JScrollPane scroll;
	private ArrayList listeners;
	private ChangeEvent changedEvent;
	
	public WbCellEditor()
	{
		editor = new TextAreaEditor();
		scroll = new TextAreaScrollPane(editor);
		scroll.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_NEVER);
		scroll.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
		editor.setLineWrap(false);
		editor.setWrapStyleWord(true);
		editor.setBorder(WbSwingUtilities.EMPTY_BORDER);
		scroll.setBorder(WbSwingUtilities.EMPTY_BORDER);
		editor.addMouseListener(new TextComponentMouseListener());
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
	
	class TextAreaEditor extends JTextArea
	{
		private KeyStroke ctrlTab = KeyStroke.getKeyStroke("control TAB");
		public TextAreaEditor()
		{
			super();
			KeyStroke tab = KeyStroke.getKeyStroke("TAB");
			Object tabAction = this.getInputMap().get(tab);
			this.getInputMap().put(tab, "wb-do-nothing-at-all");
			if (tabAction != null) 
			{
				this.getInputMap().put(ctrlTab, tabAction);
				this.getInputMap().put(ctrlTab, tabAction);
			}

			KeyStroke enter = KeyStroke.getKeyStroke("ENTER");
			KeyStroke ctrlEnter = KeyStroke.getKeyStroke("control ENTER");
			Object enterAction = this.getInputMap().get(enter);

			this.getInputMap().put(enter, "wb-stop-editing");
			
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
				this.getInputMap().put(ctrlEnter, enterAction);
			}

		}

		public boolean processKeyBinding(KeyStroke ks, KeyEvent e, int condition, boolean pressed)
		{
//			if (ks.equals(ctrlTab))
//			{
//				System.out.println("Control-TAB pressed");
//				int pos = this.getCaretPosition();
//				this.insert("\t", pos);
//				e.consume();
//				return true;
//			}
//			else
//			{
//				return super.processKeyBinding(ks, e, condition, pressed);
//			}
			return super.processKeyBinding(ks, e, condition, pressed);
		}

		public boolean isManagingFocus() { return false; }
	}
	
	public class TextAreaScrollPane extends JScrollPane
	{
		TextAreaEditor editor;
		
		public TextAreaScrollPane(Component content)
		{
			super(content);
			if (content instanceof TextAreaEditor)
			{
				editor = (TextAreaEditor)content;
			}
		}

		public boolean processKeyBinding(KeyStroke ks, KeyEvent e, int condition, boolean pressed)
		{
			return editor.processKeyBinding(ks, e, condition, pressed);
		}

		public boolean isManagingFocus() { return false; }
	}
	
} 
