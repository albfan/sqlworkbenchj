/*
 * TableReplacer.java
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
import java.awt.EventQueue;
import javax.swing.SwingUtilities;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import workbench.db.WbConnection;
import workbench.gui.WbSwingUtilities;
import workbench.gui.actions.FindDataAction;
import workbench.gui.actions.FindDataAgainAction;
import workbench.gui.actions.ReplaceDataAction;
import workbench.interfaces.Replaceable;
import workbench.interfaces.Searchable;
import workbench.resource.ResourceMgr;
import workbench.storage.DataStoreReplacer;
import workbench.storage.Position;
import workbench.storage.filter.ColumnComparator;
import workbench.storage.filter.ColumnExpression;
import workbench.storage.filter.ContainsComparator;
import workbench.storage.filter.RegExComparator;
import workbench.util.ConverterException;
import workbench.util.ExceptionUtil;

/**
 * @author support@sql-workbench.net
 */
public class TableReplacer
	implements Searchable, Replaceable, TableModelListener
{
	private WbTable client;
	private FindDataAction findAction;
	private FindDataAgainAction findAgainAction;
	private ReplaceDataAction replaceAction;
	private DataStoreReplacer replacer;
	private boolean tableChanging = false;
	
	public TableReplacer(WbTable table)
	{
		this.client = table;
		this.client.addTableModelListener(this);
		this.replacer = new DataStoreReplacer();
		this.findAction = new FindDataAction(this);
		this.findAction.setEnabled(false);
		this.findAction.setCreateMenuSeparator(true);
		this.findAgainAction = new FindDataAgainAction(this);
		this.findAgainAction.setEnabled(false);
		this.replaceAction = new ReplaceDataAction(this);
		this.replaceAction.setEnabled(false);
	}
	
	public FindDataAction getFindAction() { return this.findAction; }
	public FindDataAgainAction getFindAgainAction() { return this.findAgainAction; }
	public ReplaceDataAction getReplaceAction() { return this.replaceAction; }
	
	public int find()
	{
		boolean showDialog = true;
		String crit = this.replacer.getLastCriteria();
		SearchCriteriaPanel p = new SearchCriteriaPanel(crit, "workbench.data.search", true);

		Component parent = SwingUtilities.getWindowAncestor(this.client);
		Position pos = Position.NO_POSITION;
		while (showDialog)
		{
			boolean doFind = p.showFindDialog(parent, ResourceMgr.getString("TxtWindowTitleSearchDataText"));
			if (!doFind) return -1;
			String criteria = p.getCriteria();
			boolean ignoreCase = p.getIgnoreCase();
			boolean wholeWord = p.getWholeWordOnly();
			boolean useRegex = p.getUseRegex();
			try
			{
				this.findAgainAction.setEnabled(false);
				if (p.getHighlightAll())
				{
					initTableHighlighter(criteria, ignoreCase, useRegex);
				}
				else
				{
					client.clearHighlightExpression();
					pos = this.replacer.find(criteria, ignoreCase, wholeWord, useRegex);
				}
				showDialog = false;
				this.findAgainAction.setEnabled(pos.isValid());
			}
			catch (Exception e)
			{
				pos = Position.NO_POSITION;
				WbSwingUtilities.showErrorMessage(parent, ExceptionUtil.getDisplay(e));
				showDialog = true;
			}
		}
		
		highlightPosition(pos);
		
		return pos.getRow();
	}
	
	protected void initTableHighlighter(String criteria, boolean ignoreCase, boolean useRegex)
	{
		ColumnComparator comp = null;
		if (useRegex)
		{
			comp = new RegExComparator();
		}
		else
		{
			comp = new ContainsComparator();
		}
		ColumnExpression filter = new ColumnExpression(comp, criteria);
		filter.setIgnoreCase(ignoreCase);
		client.applyHighlightExpression(filter);
	}
	
	protected void highlightPosition(final Position pos)
	{
		final int row = pos.getRow();
		int col = pos.getColumn();
		final int realCol = (this.client.getShowStatusColumn() ? col + 1 : col);
		EventQueue.invokeLater(new Runnable()
		{
			public void run()
			{
				client.clearSelection();
				if (pos.isValid())
				{
					client.selectCell(row, realCol);
				}
			}
		});
	}
	
	public int findNext()
	{
		Position pos = this.replacer.findNext();
		highlightPosition(pos);
		return pos.getRow();
	}
	
	public void replace()
	{
		if (this.client == null) return;
		
		if (!this.client.checkPkColumns(true))
		{
			return;
		}
		ReplacePanel panel = new ReplacePanel(this, "workbench.data.replace", ResourceMgr.getString("LblSelectedRowsOnly"));
		String title = ResourceMgr.getString("TxtWindowTitleReplaceDataText");
		panel.showReplaceDialog(this.client, this.replacer.getLastCriteria(), title);
	}

	/**
	 * Called by the ReplacePanel.
	 */
	public int findFirst(String text, boolean ignoreCase, boolean wholeWord, boolean useRegex)
	{
		this.replacer.reset();
		Position pos = this.replacer.find(text, ignoreCase, wholeWord, useRegex);
		highlightPosition(pos);
		return pos.getRow();
	}
	
	public boolean replaceCurrent(String aReplacement, boolean useRegex)
	{
		boolean replaced = false;
		try
		{
			Position pos = this.replacer.getLastFoundPosition();
			replaced = this.replacer.replaceCurrent(aReplacement);
			if (replaced)
			{
				fireTableChanged(pos);
			}
		}
		catch (ConverterException e)
		{
			WbSwingUtilities.showErrorMessage(client, e.getMessage());
			replaced = false;
		}
		return replaced;
	}
	
	public boolean replaceNext(String aReplacement, boolean useRegex)
	{
		boolean replaced = false;
		replaced = replaceCurrent(aReplacement, useRegex);
		if (replaced) 
		{
			Position pos = this.replacer.findNext();
			highlightPosition(pos);
		}
		return replaced;
	}
	
	private void fireTableChanged(Position pos)
	{
		try
		{
			tableChanging = true;
			
			this.client.setShowStatusColumn(true);
			
			TableModelEvent event = null;
			if (pos == null)
			{
				event = new TableModelEvent(this.client.getModel(), 0, this.client.getRowCount() -1);
			}
			else
			{
				event = new TableModelEvent(this.client.getModel(), pos.getRow(), pos.getRow());
			}
			this.client.getDataStoreTableModel().fireTableDataChanged();
		}
		finally
		{
			tableChanging = false;
		}
	}
	
	public int replaceAll(String value, String replacement, boolean selectedText,
		boolean ignoreCase, boolean wholeWord,
		boolean useRegex)
	{
		int[] rows = null;
	
		if (selectedText)
		{
			rows = this.client.getSelectedRows();
		}
		int replaced = 0;
		try
		{
			replaced = this.replacer.replaceAll(value, replacement, rows, ignoreCase, wholeWord, useRegex);
			if (replaced > 0)
			{
				fireTableChanged(null);
			}
		}
		catch (ConverterException e)
		{
			WbSwingUtilities.showErrorMessage(client, e.getMessage());
		}
		return replaced;
	}
	
	public boolean isTextSelected()
	{
		return (this.client.getSelectedColumnCount() > 0);
	}
	
	public void tableChanged(TableModelEvent arg0)
	{
		if (tableChanging) return;
		
		this.replacer.setDataStore(this.client.getDataStore());
		
		WbConnection con = client.getDataStore().getOriginalConnection();
		final boolean readOnly = (con == null ? false : con.getProfile().isReadOnly());
		final boolean hasData = (client.getRowCount() > 0);
		EventQueue.invokeLater(new Runnable()
		{
			public void run()
			{
				findAction.setEnabled(hasData);
				replaceAction.setEnabled(hasData && !readOnly);
			}
		});
		
	}
	
}
