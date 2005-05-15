/*
 * ParameterEditor.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2005, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.gui.preparedstatement;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.sql.Types;
import java.util.Properties;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.SwingConstants;
import javax.swing.border.Border;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableColumn;

import workbench.WbManager;
import workbench.gui.WbSwingUtilities;
import workbench.gui.actions.DeleteListEntryAction;
import workbench.gui.actions.NewListEntryAction;
import workbench.gui.components.DataStoreTableModel;
import workbench.gui.components.ValidatingDialog;
import workbench.gui.components.WbTable;
import workbench.gui.components.WbTextCellEditor;
import workbench.interfaces.FileActions;
import workbench.interfaces.ValidatingComponent;
import workbench.log.LogMgr;
import workbench.resource.ResourceMgr;
import workbench.sql.VariablePool;
import workbench.sql.preparedstatement.StatementParameters;
import workbench.storage.DataStore;
import workbench.gui.sql.*;
import workbench.util.SqlUtil;

/**
 *
 * @author  support@sql-workbench.net
 *
 * An component to enter parameters for prepared statements
 */
public class ParameterEditor 
	extends JPanel 
	implements ValidatingComponent
{
	private WbTable parameterTable;
	private StatementParameters parameters;
	private StatementParameterTableModel model;
	
	public ParameterEditor(StatementParameters parms)
	{
		this.parameters = parms;
		this.model = new StatementParameterTableModel(this.parameters);
		this.parameterTable = new WbTable();
		this.parameterTable.setRowSelectionAllowed(false);
		this.parameterTable.setColumnSelectionAllowed(false);
		this.parameterTable.setModel(this.model);
		this.parameterTable.setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);
		TableColumn col = this.parameterTable.getColumnModel().getColumn(2);
		col.setCellEditor(WbTextCellEditor.createInstance(true));

		JLabel l = new JLabel(ResourceMgr.getString("TxtPSParameterInputText"));
		Border b = BorderFactory.createEmptyBorder(5, 2, 5, 2);
		l.setBorder(b);
		l.setBackground(Color.WHITE);
		l.setOpaque(true);
		l.setHorizontalAlignment(SwingConstants.LEFT);
		
		this.setLayout(new BorderLayout());

		JScrollPane scroll = new JScrollPane(this.parameterTable);
		b = BorderFactory.createEmptyBorder(5, 0, 0, 0);
		Border b2 = BorderFactory.createCompoundBorder(b, scroll.getBorder());
		scroll.setBorder(b2);
		
		this.add(l, BorderLayout.NORTH);
		this.add(scroll, BorderLayout.CENTER);
	}

	public void applyValues()
	{
		this.parameterTable.stopEditing();
		int count = this.parameters.getParameterCount();
		for (int i=0; i < count; i++)
		{
			String v = this.model.getParameterValue(i);
			this.parameters.setParameterValue(i, v);
		}
	}
	
	public void componentDisplayed()
	{
		this.parameterTable.setColumnSelectionInterval(2,2);
		this.parameterTable.editCellAt(0, 2);
		TableCellEditor editor = this.parameterTable.getCellEditor();
		if (editor instanceof WbTextCellEditor)
		{
			WbTextCellEditor wbedit = (WbTextCellEditor)editor;
			wbedit.selectAll();
			wbedit.requestFocus();
		}
	}
	
	public boolean validateInput()
	{
		this.parameterTable.stopEditing();
		int count = this.parameters.getParameterCount();
		for (int i=0; i < count; i++)
		{
			String v = this.model.getParameterValue(i);
			if (!this.parameters.isValueValid(i, v)) 
			{
				String error = ResourceMgr.getString("ErrorInvalidPSParameterValue").replaceAll("%value%", v);
				error = error.replaceAll("%type%", SqlUtil.getTypeName(this.parameters.getParameterType(i)));
				WbSwingUtilities.showErrorMessage(this, error);
				return false;
			}
		}
		return true;
	}
	
	public static boolean showParameterDialog(StatementParameters parms)
	{
		ParameterEditor editor = new ParameterEditor(parms);
		Dimension d = new Dimension(300,250);
		editor.setMinimumSize(d);
		editor.setPreferredSize(d);

		boolean result = false; 
		//boolean ok = ValidatingDialog.showConfirmDialog(WbManager.getInstance().getCurrentWindow(), editor, ResourceMgr.getString("TxtEditPSParameterWindowTitle"));
		boolean ok = ValidatingDialog.showConfirmDialog(null, editor, ResourceMgr.getString("TxtEditPSParameterWindowTitle"));
		if (ok)
		{
			try
			{
				editor.applyValues();
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
