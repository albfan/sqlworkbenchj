/*
 * ClearAction.java
 *
 * Created on December 2, 2001, 1:32 AM
 */
package workbench.gui.actions;

import java.awt.event.ActionEvent;
import java.util.List;
import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.KeyStroke;
import workbench.WbManager;
import workbench.db.DataSpooler;
import workbench.db.WbConnection;
import workbench.gui.components.ExtensionFileFilter;
import workbench.interfaces.ClipboardSupport;
import workbench.interfaces.Exporter;
import workbench.interfaces.Spooler;
import workbench.log.LogMgr;
import workbench.resource.ResourceMgr;
import workbench.util.SqlUtil;

/**
 *	Action to copy the contents of a entry field into the clipboard
 *	
 */
public class SpoolDataAction extends WbAction
{
	private Spooler client;
	
	public SpoolDataAction(Spooler aClient)
	{
		super();
		this.client = aClient;
		this.putValue(Action.NAME, ResourceMgr.getString("MnuTxtSpoolData"));
		this.putValue(Action.SHORT_DESCRIPTION, ResourceMgr.getDescription("MnuTxtSpoolData"));
		this.putValue(Action.SMALL_ICON, ResourceMgr.getImage("SpoolData"));
		this.putValue(WbAction.MAIN_MENU_ITEM, ResourceMgr.MNU_TXT_SQL);
	}

	public void actionPerformed(ActionEvent e)
	{
		this.client.spoolData();
	}
	
	public void execute(WbConnection aConnection, String aSql)
	{
		List tables = SqlUtil.getTables(aSql);
		boolean includeSqlExport = (tables.size() == 1);
		String filename = WbManager.getInstance().getExportFilename(this.client.getParentWindow(), includeSqlExport);
		if (filename != null)
		{
			DataSpooler spool = new DataSpooler();
			spool.setShowProgress(true);
			try
			{
				if (ExtensionFileFilter.hasSqlExtension(filename))
				{
					spool.exportDataAsSqlInsert(aConnection, aSql, filename);
				}
				else
				{
					spool.exportDataAsText(aConnection, aSql, filename, true);
				}
			}
			catch (Exception e)
			{
				LogMgr.logError("DataSpoolThread", "Could not export data", e);
			}
		}
	}
	
}
