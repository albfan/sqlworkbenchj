package workbench.gui.components;


import java.awt.Component;
import java.awt.event.*;
import java.awt.AWTEvent;
import java.lang.Boolean;
import javax.swing.table.*;
import javax.swing.event.*;
import java.util.EventObject;
import javax.swing.tree.*;
import java.io.Serializable;
import javax.swing.AbstractCellEditor;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.table.TableCellRenderer;
import workbench.gui.WbSwingUtilities;

public class WbCellEditor 
	extends AbstractCellEditor
	implements TableCellEditor
{
	private JTextArea editor;
	private JScrollPane scroll;
	
	protected int clickCountToStart = 2;
	
	public WbCellEditor()
	{
		editor = new JTextArea();
		scroll = new JScrollPane(editor);
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
	
	public void setClickCountToStart(int count)
	{
		clickCountToStart = count;
	}
	
	public int getClickCountToStart()
	{
		return clickCountToStart;
	}
	
	public Object getCellEditorValue()
	{
		return editor.getText();
	}
	
	public boolean isCellEditable(EventObject anEvent)
	{
		if (anEvent instanceof MouseEvent)
		{
			return ((MouseEvent)anEvent).getClickCount() >= clickCountToStart;
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
		
	public boolean startCellEditing(EventObject anEvent)
	{
		return true;
	}
		
	public boolean stopCellEditing()
	{
		fireEditingStopped();
		return true;
	}
		
	public void cancelCellEditing()
	{
		fireEditingCanceled();
	}
		
	public void actionPerformed(ActionEvent e)
	{
		this.stopCellEditing();
	}
		
	public void itemStateChanged(ItemEvent e)
	{
		this.stopCellEditing();
	}

	
} 
