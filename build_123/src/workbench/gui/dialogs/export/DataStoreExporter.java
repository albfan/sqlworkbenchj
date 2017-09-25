/*
 * DataStoreExporter.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2017, Thomas Kellerer
 *
 * Licensed under a modified Apache License, Version 2.0
 * that restricts the use for certain governments.
 * You may not use this file except in compliance with the License.
 * You may obtain a copy of the License at.
 *
 *     http://sql-workbench.net/manual/license.html
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
package workbench.gui.dialogs.export;

import java.awt.Component;

import workbench.log.LogMgr;

import workbench.db.exporter.DataExporter;

import workbench.gui.WbSwingUtilities;

import workbench.storage.DataStore;
import workbench.util.ExceptionUtil;

import workbench.util.WbFile;

/**
 * @author  Thomas Kellerer
 */
public class DataStoreExporter
{
	private DataStore source;
	private Component caller;
	private ExportFileDialog dialog;
	private WbFile output;

	public DataStoreExporter(DataStore source, Component caller)
	{
		this.caller = caller;
		this.source = source;
	}

	public void saveAs()
	{
		this.dialog = new ExportFileDialog(this.caller, source);
		this.dialog.setSelectDirectoryOnly(false);
		this.output = null;
		boolean selected = dialog.selectOutput();
		if (selected)
		{
			this.output = new WbFile(dialog.getSelectedFilename());
			writeFile();
		}
	}

	public DataStore getSource()
	{
		return source;
	}

	public void setSource(DataStore source)
	{
		this.source = source;
	}

	private void writeFile()
	{
		if (this.source == null) return;
		if (this.output == null)
		{
			throw new NullPointerException("No outputfile defined");
		}
		DataExporter exporter = new DataExporter(this.source.getOriginalConnection());
		dialog.setExporterOptions(exporter);

		try
		{
			exporter.startExport(output, this.source, this.dialog.getColumnsToExport());
			if (!exporter.isSuccess())
			{
				CharSequence msg = exporter.getErrors();
				if (msg != null)
				{
					WbSwingUtilities.showErrorMessage(caller, msg.toString());
				}
			}
		}
		catch (Exception e)
		{
			LogMgr.logError("DataStoreExporter.writeFile()", "Error writing export file", e);
      WbSwingUtilities.showErrorMessage(caller, ExceptionUtil.getDisplay(e));
		}
	}

}
