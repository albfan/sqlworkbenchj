/*
 * TableReplacer.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2016, Thomas Kellerer
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at.
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
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

import java.awt.Component;
import java.awt.EventQueue;
import java.util.Collections;
import java.util.List;

import javax.swing.SwingUtilities;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;

import workbench.interfaces.Replaceable;
import workbench.interfaces.Searchable;
import workbench.resource.ResourceMgr;

import workbench.db.WbConnection;

import workbench.gui.WbSwingUtilities;
import workbench.gui.actions.FindDataAction;
import workbench.gui.actions.FindDataAgainAction;
import workbench.gui.actions.ReplaceDataAction;
import workbench.gui.editor.SearchResult;

import workbench.storage.DataStore;
import workbench.storage.DataStoreReplacer;
import workbench.storage.Position;
import workbench.storage.filter.ColumnComparator;
import workbench.storage.filter.ColumnExpression;
import workbench.storage.filter.ContainsComparator;
import workbench.storage.filter.RegExComparator;

import workbench.util.ConverterException;
import workbench.util.ExceptionUtil;

/**
 * @author Thomas Kellerer
 */
public class TableReplacer
	implements Searchable, Replaceable, TableModelListener
{
	private WbTable client;
	private FindDataAction findAction;
	private FindDataAgainAction findAgainAction;
	private ReplaceDataAction replaceAction;
	private DataStoreReplacer replacer;
	private boolean tableChanging;

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

	public FindDataAction getFindAction()
	{
		return this.findAction;
	}

	public FindDataAgainAction getFindAgainAction()
	{
		return this.findAgainAction;
	}

	public ReplaceDataAction getReplaceAction()
	{
		return this.replaceAction;
	}

	@Override
	public void setWrapSearch(boolean flag)
	{

	}

	@Override
	public int findPrevious()
	{
		return -1;
	}

	@Override
	public int find()
	{
		boolean showDialog = true;
		String crit = this.replacer.getLastCriteria();
		SearchCriteriaPanel p = new SearchCriteriaPanel(crit, "workbench.data", true);

		Component parent = SwingUtilities.getWindowAncestor(this.client);
		Position pos = Position.NO_POSITION;

		boolean scrollOnly = false;
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
					pos = this.replacer.find(criteria, ignoreCase, wholeWord, useRegex);
					scrollOnly = true;
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

		if (scrollOnly && pos.isValid())
		{
			client.scrollToRow(pos.getRow());
		}
		else
		{
			highlightPosition(pos);
		}

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

	protected void scrollTo(final Position pos)
	{
		final int row = pos.getRow();
		EventQueue.invokeLater(new Runnable()
		{
			@Override
			public void run()
			{
				client.scrollToRow(row);
			}
		});
	}
	protected void highlightPosition(final Position pos)
	{
		final int row = pos.getRow();
		int col = pos.getColumn();
		final int realCol = (this.client.isStatusColumnVisible() ? col + 1 : col);
		EventQueue.invokeLater(new Runnable()
		{
			@Override
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

	@Override
	public int findNext()
	{
		Position pos = this.replacer.findNext();
		highlightPosition(pos);
		return pos.getRow();
	}

	@Override
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
	@Override
	public int findFirst(String text, boolean ignoreCase, boolean wholeWord, boolean useRegex)
	{
		this.replacer.reset();
		Position pos = this.replacer.find(text, ignoreCase, wholeWord, useRegex);
		highlightPosition(pos);
		return pos.getRow();
	}

	@Override
	public boolean replaceCurrent(String aReplacement, boolean useRegex)
	{
		boolean replaced = false;
		try
		{
			replaced = this.replacer.replaceCurrent(aReplacement);
			if (replaced)
			{
				fireTableChanged();
			}
		}
		catch (ConverterException e)
		{
			WbSwingUtilities.showErrorMessage(client, e.getMessage());
			replaced = false;
		}
		return replaced;
	}

	@Override
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

	private void fireTableChanged()
	{
		try
		{
			tableChanging = true;
			this.client.showStatusColumn();
			this.client.getDataStoreTableModel().fireTableDataChanged();
		}
		finally
		{
			tableChanging = false;
		}
	}

	@Override
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
				fireTableChanged();
			}
		}
		catch (ConverterException e)
		{
			WbSwingUtilities.showErrorMessage(client, e.getMessage());
		}
		return replaced;
	}

	@Override
	public boolean isTextSelected()
	{
		return (this.client.getSelectedColumnCount() > 0);
	}

	@Override
	public void tableChanged(TableModelEvent evt)
	{
		if (tableChanging) return;

		this.replacer.setDataStore(this.client.getDataStore());

		DataStore ds = client.getDataStore();
		WbConnection con = (ds != null ? ds.getOriginalConnection() : null);
		final boolean readOnly = (con == null ? true : con.isSessionReadOnly());
		final boolean hasData = (client.getRowCount() > 0);
		EventQueue.invokeLater(new Runnable()
		{
			@Override
			public void run()
			{
				findAction.setEnabled(hasData);
				replaceAction.setEnabled(hasData && !readOnly);
			}
		});
	}

  @Override
  public List<SearchResult> findAll(String expression, boolean ignoreCase, boolean wholeWord, boolean isRegex, int contextLines)
  {
    // not supported yet.
    return Collections.emptyList();
  }

}
