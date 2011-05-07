/*
 * TriggerDisplayPanel.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2011, Thomas Kellerer
 * No part of this code may be reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.gui.dbobjects;

import java.awt.BorderLayout;

import javax.swing.JPanel;
import javax.swing.JSplitPane;
import javax.swing.ListSelectionModel;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import workbench.db.TableIdentifier;
import workbench.db.TriggerReader;
import workbench.db.TriggerReaderFactory;
import workbench.db.WbConnection;
import workbench.gui.WbSwingUtilities;
import workbench.gui.components.DataStoreTableModel;
import workbench.gui.components.WbScrollPane;
import workbench.gui.components.WbSplitPane;
import workbench.gui.components.WbTable;
import workbench.gui.sql.EditorPanel;
import workbench.interfaces.Resettable;
import workbench.log.LogMgr;
import workbench.resource.Settings;
import workbench.storage.DataStore;

/**
 *
 * @author Thomas Kellerer
 */
public class TriggerDisplayPanel
	extends JPanel
	implements ListSelectionListener, Resettable
{
	private TriggerReader reader;
	private WbTable triggers;
	private EditorPanel source;
	private WbSplitPane splitPane;
	private String triggerSchema;
	private String triggerCatalog;
	private TableIdentifier triggerTable;

	public TriggerDisplayPanel()
	{
		super(new BorderLayout());
		triggers = new WbTable();

		source = EditorPanel.createSqlEditor();
		source.setEditable(false);
		source.setBorder(WbSwingUtilities.EMPTY_BORDER);
		setBorder(WbSwingUtilities.EMPTY_BORDER);

		JPanel list = new JPanel(new BorderLayout());
		list.add(new WbScrollPane(this.triggers), BorderLayout.CENTER);

		splitPane = new WbSplitPane(JSplitPane.VERTICAL_SPLIT, list, this.source);
		splitPane.setDividerSize(8);
		add(splitPane, BorderLayout.CENTER);
		triggers.getSelectionModel().addListSelectionListener(this);
		triggers.getSelectionModel().setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
	}

	public void saveSettings()
	{
		Settings.getInstance().setProperty(this.getClass().getName() + ".divider", this.splitPane.getDividerLocation());
	}

	public void restoreSettings()
	{
		int loc = Settings.getInstance().getIntProperty(this.getClass().getName() + ".divider", 200);
		this.splitPane.setDividerLocation(loc);
	}

	public void setConnection(WbConnection aConnection)
	{
		this.reader = TriggerReaderFactory.createReader(aConnection);
		this.source.setDatabaseConnection(aConnection);
		this.reset();
	}

	@Override
	public void reset()
	{
		this.triggers.reset();
		this.source.setText("");
		this.triggerSchema = null;
		this.triggerCatalog = null;
	}

	public void readTriggers(final TableIdentifier table)
	{
		try
		{
			if (table == null) return;
			triggerTable = table;
			DataStore trg = reader.getTableTriggers(table);
			final DataStoreTableModel rs = new DataStoreTableModel(trg);
			WbSwingUtilities.invoke(new Runnable()
			{
				@Override
				public void run()
				{
					triggers.setModel(rs, true);
					triggers.adjustRowsAndColumns();
					triggerCatalog = table.getCatalog();
					triggerSchema = table.getSchema();
					if (triggers.getRowCount() > 0)
					{
						triggers.getSelectionModel().setSelectionInterval(0, 0);
					}
					else
					{
						source.setText("");
					}
				}
			});
		}
		catch (Exception e)
		{
			LogMgr.logError("TriggerDisplayPanel.readTriggers()", "Error retrieving triggers", e);
			this.reset();
		}
	}

	@Override
	public void valueChanged(ListSelectionEvent e)
	{
		if (e.getValueIsAdjusting()) return;
		int row = this.triggers.getSelectedRow();
		if (row < 0) return;

		try
		{
			String triggerName = this.triggers.getValueAsString(row, TriggerReader.COLUMN_IDX_TABLE_TRIGGERLIST_TRG_NAME);
			String comment = this.triggers.getValueAsString(row, TriggerReader.COLUMN_IDX_TABLE_TRIGGERLIST_TRG_COMMENT);
			String sql = reader.getTriggerSource(this.triggerCatalog, this.triggerSchema, triggerName, triggerTable, comment, true);
			this.source.setText(sql);
			this.source.setCaretPosition(0);
		}
		catch (Exception ex)
		{
			ex.printStackTrace();
			this.source.setText("");
		}
	}

}

