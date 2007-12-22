/*
 * VariablesEditor.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2008, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.gui.sql;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.SwingConstants;
import javax.swing.border.Border;
import javax.swing.table.TableCellEditor;

import workbench.WbManager;
import workbench.gui.WbSwingUtilities;
import workbench.gui.components.DataStoreTableModel;
import workbench.gui.components.ValidatingDialog;
import workbench.gui.components.WbTable;
import workbench.gui.components.WbTextCellEditor;
import workbench.interfaces.ValidatingComponent;
import workbench.log.LogMgr;
import workbench.resource.ResourceMgr;
import workbench.sql.VariablePool;
import workbench.storage.DataStore;

/**
 * A panel to enter the value for Workbench variables inside SQL statements
 * @see VariablePrompter
 * @see workbench.sql.VariablePool
 * 
 * @author  support@sql-workbench.net
 */
public class VariablesEditor 
	extends JPanel 
	implements ValidatingComponent
{
	private DataStore varData;
	private WbTable variablesTable;

	public VariablesEditor(DataStore data)
	{

		this.variablesTable = new WbTable();
		this.variablesTable.setRowSelectionAllowed(false);
		this.variablesTable.setColumnSelectionAllowed(false);
		this.varData = data;
		DataStoreTableModel model = new DataStoreTableModel(data);
		model.setLockedColumn(0);
		this.variablesTable.setModel(model);
		this.variablesTable.setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);

		JLabel l = new JLabel(ResourceMgr.getString("TxtVariableInputText"));
		Border b = BorderFactory.createEmptyBorder(5, 2, 5, 2);
		l.setBorder(b);
		l.setBackground(Color.WHITE);
		l.setOpaque(true);
		l.setHorizontalAlignment(SwingConstants.CENTER);
		
		this.setLayout(new BorderLayout());

		JScrollPane scroll = new JScrollPane(this.variablesTable);
		b = BorderFactory.createEmptyBorder(5, 0, 0, 0);
		Border b2 = BorderFactory.createCompoundBorder(b, scroll.getBorder());
		scroll.setBorder(b2);
		
		this.add(l, BorderLayout.NORTH);
		this.add(scroll, BorderLayout.CENTER);
	}

	public void componentDisplayed()
	{
		this.variablesTable.setColumnSelectionInterval(1,1);
		this.variablesTable.editCellAt(0, 1);
		TableCellEditor editor = this.variablesTable.getCellEditor();
		if (editor instanceof WbTextCellEditor)
		{
			WbTextCellEditor wbedit = (WbTextCellEditor)editor;
			wbedit.selectAll();
			wbedit.requestFocus();
		}
		
	}
	
	public boolean validateInput()
	{
		this.variablesTable.stopEditing();
		int rows = this.varData.getRowCount();
		for (int i=0; i < rows; i++)
		{
			String varName = this.varData.getValueAsString(i, 0);
			if (!VariablePool.getInstance().isValidVariableName(varName))
			{
				String msg = ResourceMgr.getString("ErrIllegalVariableName");
				msg = msg.replaceAll("%varname%", varName);
				msg = msg + "\n" + ResourceMgr.getString("ErrVarDefWrongName");
				WbSwingUtilities.showErrorMessage(this, msg);
				return false;
			}
		}
		return true;
	}
	
	public static boolean showVariablesDialog(DataStore vardata)
	{
		VariablesEditor editor = new VariablesEditor(vardata);
		Dimension d = new Dimension(300,250);
		editor.setMinimumSize(d);
		editor.setPreferredSize(d);

		boolean result = false; 
		boolean ok = ValidatingDialog.showConfirmDialog(WbManager.getInstance().getCurrentWindow(), editor, ResourceMgr.getString("TxtEditVariablesWindowTitle"));
		if (ok)
		{
			try
			{
				vardata.updateDb(null,null);
				result = true;
			}
			catch (Exception e)
			{
				LogMgr.logError("VariablesEditor.showVariablesDialog()", "Error when saving values", e);
				result = false;
			}
		}
		return result;
	}
	
}
