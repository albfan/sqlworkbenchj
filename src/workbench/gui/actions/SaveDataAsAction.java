/*
 * SaveDataAsAction.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.gui.actions;

import java.awt.event.ActionEvent;
import javax.swing.SwingUtilities;
import workbench.gui.components.WbTable;
import workbench.gui.dialogs.export.DataStoreExporter;

import workbench.resource.ResourceMgr;
import workbench.util.EncodingUtil;
import workbench.util.WbThread;

/**
 *	Save the content of the ResultSet as an external file
 * @see workbench.gui.dialogs.export.DataStoreExporter
 *	@author  Thomas Kellerer
 */
public class SaveDataAsAction
	extends WbAction
{
	private WbTable client;

	public SaveDataAsAction(WbTable aClient)
	{
		super();
		this.client = aClient;
		this.initMenuDefinition("MnuTxtSaveDataAs");
		this.setIcon("SaveAs");
		this.setMenuItemName(ResourceMgr.MNU_TXT_DATA);
		this.setEnabled(false);
	}

	public void executeAction(ActionEvent e)
	{
		WbThread encodings = new WbThread("Fetch Encodings")
		{
			@Override
			public void run()
			{
				// Prefetch encodings in a background thread while
				// the GUI is initialized. Getting the encodings can take
				// quite a while on slow systems
				EncodingUtil.getEncodings();
			}
		};
		encodings.start();

		SwingUtilities.invokeLater(new Runnable()
		{
			public void run()
			{
				DataStoreExporter exporter = new DataStoreExporter(client.getDataStore(), client);
				exporter.saveAs();
			}
		});
	}
}
