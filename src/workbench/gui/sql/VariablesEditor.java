/*
 * VariablesEditor.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2012, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.gui.sql;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.EventQueue;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.SwingConstants;
import javax.swing.border.Border;
import javax.swing.event.ChangeEvent;
import javax.swing.table.TableCellEditor;

import workbench.WbManager;
import workbench.gui.WbSwingUtilities;
import workbench.gui.components.DataStoreTableModel;
import workbench.gui.components.ValidatingDialog;
import workbench.gui.components.WbTable;
import workbench.gui.components.WbTextCellEditor;
import workbench.gui.renderer.RendererSetup;
import workbench.interfaces.ValidatingComponent;
import workbench.log.LogMgr;
import workbench.resource.ResourceMgr;
import workbench.resource.Settings;
import workbench.sql.VariablePool;
import workbench.storage.DataStore;

/**
 * A panel to enter the value for Workbench variables inside SQL statements
 *
 * @see workbench.sql.VariablePool
 *
 * @author  Thomas Kellerer
 */
public class VariablesEditor
	extends JPanel
	implements ValidatingComponent
{
	private DataStore varData;
	private WbTable variablesTable;
	private ValidatingDialog parentDialog;
	private boolean autoAdvance;

	public VariablesEditor(DataStore data)
	{
		super();
		autoAdvance = Settings.getInstance().getBoolProperty("workbench.gui.variables.editor.autoadvance", true);

		this.variablesTable = new WbTable()
		{
			@Override
			public void editingStopped(ChangeEvent e)
			{
				final int editRow = getEditingRow();
				super.editingStopped(e);
				if (autoAdvance)
				{
					closeOrAdvance(editRow);
				}
			}
		};

		this.variablesTable.setRendererSetup(new RendererSetup(false));

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

	private void closeOrAdvance(final int editedRow)
	{
		if (editedRow == variablesTable.getRowCount() - 1)
		{
			parentDialog.approveAndClose();
		}
		else if (editedRow >= 0)
		{
			EventQueue.invokeLater(new Runnable()
			{
				@Override
				public void run()
				{
					startEditRow(editedRow + 1);
				}
			});
		}
	}

	@Override
	public void componentDisplayed()
	{
		startEditRow(0);
	}

	private void startEditRow(int row)
	{
		this.variablesTable.setColumnSelectionInterval(1,1);
		this.variablesTable.editCellAt(row, 1);
		TableCellEditor editor = this.variablesTable.getCellEditor();
		if (editor instanceof WbTextCellEditor)
		{
			WbTextCellEditor wbedit = (WbTextCellEditor)editor;
			wbedit.selectAll();
			wbedit.requestFocus();
		}
	}

	@Override
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
				msg = msg.replace("%varname%", varName);
				msg = msg + "\n" + ResourceMgr.getString("ErrVarDefWrongName");
				WbSwingUtilities.showErrorMessage(this, msg);
				return false;
			}
		}
		return true;
	}

	private static boolean dialogResult;

	public static boolean showVariablesDialog(final DataStore vardata)
	{

		WbSwingUtilities.invoke(new Runnable()
		{
			@Override
			public void run()
			{
				VariablesEditor editor = new VariablesEditor(vardata);
				Dimension d = new Dimension(300,250);
				editor.setMinimumSize(d);
				editor.setPreferredSize(d);
				editor.parentDialog = ValidatingDialog.createDialog(WbManager.getInstance().getCurrentWindow(), editor, ResourceMgr.getString("TxtEditVariablesWindowTitle"), null, 0, false);
				editor.parentDialog.setVisible(true);
				dialogResult = !editor.parentDialog.isCancelled();
			}
		});

		boolean result = false;
		if (dialogResult)
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
