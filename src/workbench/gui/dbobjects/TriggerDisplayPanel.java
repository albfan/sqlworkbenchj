/*
 * Created on 10. August 2002, 15:40
 */
package workbench.gui.dbobjects;

import java.awt.BorderLayout;
import javax.swing.JPanel;
import javax.swing.JSplitPane;
import javax.swing.ListSelectionModel;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import workbench.WbManager;
import workbench.db.DbMetadata;
import workbench.db.WbConnection;
import workbench.gui.actions.FileSaveAsAction;
import workbench.gui.components.DataStoreTableModel;
import workbench.gui.components.WbScrollPane;
import workbench.gui.components.WbSplitPane;
import workbench.gui.components.WbTable;
import workbench.gui.sql.EditorPanel;
import workbench.storage.DataStore;

/**
 *
 * @author  workbench@kellerer.org
 */
public class TriggerDisplayPanel
	extends JPanel
	implements ListSelectionListener
{
	private WbConnection dbConnection;
	private WbTable triggers;
	private EditorPanel source;
	private WbSplitPane splitPane;
	private String triggerSchema;
	private String triggerCatalog;
	
	public TriggerDisplayPanel()
	{
		this.triggers = new WbTable();
		WbScrollPane scroll = new WbScrollPane(this.triggers);
		this.source = new EditorPanel();
		this.source.addPopupMenuItem(new FileSaveAsAction(this.source), true);
		this.source.setEditable(false);
		this.setLayout(new BorderLayout());
		this.splitPane = new WbSplitPane(JSplitPane.VERTICAL_SPLIT, scroll, this.source);
		this.add(splitPane, BorderLayout.CENTER);
		this.triggers.getSelectionModel().addListSelectionListener(this);
		this.triggers.getSelectionModel().setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
	}

	public void saveSettings()
	{
		WbManager.getSettings().setProperty(this.getClass().getName(), "divider", this.splitPane.getDividerLocation());
	}
	
	public void restoreSettings()
	{
		int loc = WbManager.getSettings().getIntProperty(this.getClass().getName(), "divider");
		if (loc == 0) loc = 200;
		this.splitPane.setDividerLocation(loc);
	}
	
	public void setConnection(WbConnection aConnection)
	{
		this.dbConnection = aConnection;
		this.source.getSqlTokenMarker().initDatabaseKeywords(aConnection.getSqlConnection());
		this.reset();
	}
	
	public void reset()
	{
		this.triggers.reset();
		this.source.setText("");
		this.triggerSchema = null;
		this.triggerCatalog = null;
	}
	
	public void readTriggers(String aCatalog, String aSchema, String aTable)
	{
		try
		{
			DbMetadata metaData = this.dbConnection.getMetadata();
			DataStore trg = metaData.getTableTriggers(aCatalog, aSchema, aTable);
			DataStoreTableModel rs = new DataStoreTableModel(trg);
			triggers.setModel(rs, true);
			triggers.adjustColumns();
			this.triggerCatalog = aCatalog;
			this.triggerSchema = aSchema;
			if (triggers.getRowCount() > 0)
				this.triggers.getSelectionModel().setSelectionInterval(0,0);
			else
				this.source.setText("");
		}
		catch (Exception e)
		{
			this.reset();
		}
	}
	
	/**
	 * Called whenever the value of the selection changes.
	 * @param e the event that characterizes the change.
	 *
	 */
	public void valueChanged(ListSelectionEvent e)
	{
		if (e.getValueIsAdjusting()) return;
		int row = this.triggers.getSelectedRow();
		if (row < 0) return;
		
		try
		{
			DbMetadata metaData = this.dbConnection.getMetadata();
			String triggerName = this.triggers.getValueAsString(row, DbMetadata.COLUMN_IDX_TABLE_TRIGGERLIST_TRG_NAME);
			String sql = metaData.getTriggerSource(this.triggerCatalog, this.triggerSchema, triggerName);
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

