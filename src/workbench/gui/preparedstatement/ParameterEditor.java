/*
 * ParameterEditor.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2010, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.gui.preparedstatement;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;

import java.awt.Frame;
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
import workbench.gui.components.ValidatingDialog;
import workbench.gui.components.WbTable;
import workbench.gui.components.WbTextCellEditor;
import workbench.interfaces.StatementParameterPrompter;
import workbench.interfaces.ValidatingComponent;
import workbench.log.LogMgr;
import workbench.resource.ResourceMgr;
import workbench.sql.preparedstatement.StatementParameters;
import workbench.util.SqlUtil;

/**
 *
 * @author  Thomas Kellerer
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

	public ParameterEditor(StatementParameters parms, boolean showNames)
	{
		super();
		this.parameters = parms;
		this.model = new StatementParameterTableModel(this.parameters, showNames);
		this.parameterTable = new WbTable();
		this.parameterTable.setRowSelectionAllowed(false);
		this.parameterTable.setColumnSelectionAllowed(false);
		this.parameterTable.setModel(this.model);
		this.parameterTable.setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);
		TableColumn col = this.parameterTable.getColumnModel().getColumn(2);
		col.setCellEditor(WbTextCellEditor.createInstance());

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
		int count = parameterTable.getColumnCount();
		this.parameterTable.setColumnSelectionInterval(count - 1, count - 1);
		this.parameterTable.editCellAt(0, count - 1);
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
				String error = ResourceMgr.getString("ErrInvalidPSParameterValue").replace("%value%", v);
				error = error.replace("%type%", SqlUtil.getTypeName(this.parameters.getParameterType(i)));
				WbSwingUtilities.showErrorMessage(this, error);
				return false;
			}
		}
		return true;
	}

	private static boolean dialogResult = false;

	public static final StatementParameterPrompter GUI_PROMPTER = new StatementParameterPrompter()
			{
				@Override
				public boolean showParameterDialog(StatementParameters parms, boolean showNames)
				{
					return ParameterEditor.showParameterDialog(parms, showNames);
				}
			};

	public static synchronized boolean showParameterDialog(final StatementParameters parms, final boolean showNames)
	{
		WbSwingUtilities.invoke(new Runnable()
		{
			public void run()
			{
				ParameterEditor editor = new ParameterEditor(parms, showNames);
				Dimension d = new Dimension(300,250);
				editor.setMinimumSize(d);
				editor.setPreferredSize(d);

				dialogResult = false;
				Frame parent = WbManager.getInstance().getCurrentWindow();
				boolean ok = ValidatingDialog.showConfirmDialog(parent, editor, ResourceMgr.getString("TxtEditPSParameterWindowTitle"));
				if (ok)
				{
					try
					{
						editor.applyValues();
						dialogResult = true;
					}
					catch (Exception e)
					{
						LogMgr.logError("VariablesEditor.showVariablesDialog()", "Error when saving values", e);
						dialogResult = false;
					}
				}
			}
		});
		return dialogResult;
	}

}
