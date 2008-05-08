/*
 * TriggerDisplayPanel.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2008, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.gui.dbobjects;

import java.awt.BorderLayout;

import javax.swing.JPanel;
import javax.swing.JSplitPane;
import javax.swing.ListSelectionModel;
import javax.swing.border.EtchedBorder;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import workbench.db.TableIdentifier;
import workbench.db.TriggerReader;
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
 * @author  support@sql-workbench.net
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
	
	public TriggerDisplayPanel()
	{
		this.triggers = new WbTable();
		WbScrollPane scroll = new WbScrollPane(this.triggers);
		scroll.setBorder(new EtchedBorder());
		//scroll.setBorder(WbSwingUtilities.EMPTY_BORDER);
		
		this.source = EditorPanel.createSqlEditor();
		this.source.setEditable(false);
		this.source.setBorder(WbSwingUtilities.EMPTY_BORDER);
		this.setBorder(WbSwingUtilities.EMPTY_BORDER);
		
		this.setLayout(new BorderLayout());
		this.splitPane = new WbSplitPane(JSplitPane.VERTICAL_SPLIT, scroll, this.source);
		this.add(splitPane, BorderLayout.CENTER);
		this.triggers.getSelectionModel().addListSelectionListener(this);
		this.triggers.getSelectionModel().setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
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
		this.reader = new TriggerReader(aConnection);
		this.source.setDatabaseConnection(aConnection);
		this.reset();
	}
	
	public void reset()
	{
		this.triggers.reset();
		this.source.setText("");
		this.triggerSchema = null;
		this.triggerCatalog = null;
	}
	
	public void readTriggers(TableIdentifier table)
	{
		try
		{
			if (table == null) return;
			DataStore trg = reader.getTableTriggers(table);
			DataStoreTableModel rs = new DataStoreTableModel(trg);
			triggers.setModel(rs, true);
			triggers.adjustOrOptimizeColumns();
			this.triggerCatalog = table.getCatalog();
			this.triggerSchema = table.getSchema();
			if (triggers.getRowCount() > 0)
				this.triggers.getSelectionModel().setSelectionInterval(0,0);
			else
				this.source.setText("");
		}
		catch (Exception e)
		{
			LogMgr.logError("TriggerDisplayPanel.readTriggers()", "Error retrieving triggers", e);
			this.reset();
		}
	}
	
	/**
	 * Called whenever the value of the selection changes.
	 * @param e the event that characterizes the change.
	 */
	public void valueChanged(ListSelectionEvent e)
	{
		if (e.getValueIsAdjusting()) return;
		int row = this.triggers.getSelectedRow();
		if (row < 0) return;
		
		try
		{
			String triggerName = this.triggers.getValueAsString(row, TriggerReader.COLUMN_IDX_TABLE_TRIGGERLIST_TRG_NAME);
			String sql = reader.getTriggerSource(this.triggerCatalog, this.triggerSchema, triggerName);
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

