/*
 * XmlExportWriter.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2006, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.db.exporter;

import workbench.log.LogMgr;
import workbench.storage.ResultInfo;
import workbench.util.XsltTransformer;

/**
 *
 * @author  support@sql-workbench.net
 */
public class XmlExportWriter
	extends ExportWriter
{
	public XmlExportWriter(DataExporter exp)
	{
		super(exp);
	}

	public RowDataConverter createConverter()
	{
		XmlRowDataConverter converter = new XmlRowDataConverter();
		converter.setUseCDATA(this.exporter.getUseCDATA());
		converter.setLineEnding(exporter.getLineEnding());
		converter.setUseVerboseFormat(exporter.getUseVerboseFormat());
		converter.setTableNameToUse(exporter.getTableName());
		converter.setBaseFilename(exporter.getOutputFilename());
		return converter;
	}
	
	public void exportFinished()
	{
		super.exportFinished();
		String exportFile = this.exporter.getFullOutputFilename();
		String xsltFile = this.exporter.getXsltTransformation();
		String output = this.exporter.getXsltTransformationOutput();
		if (xsltFile != null && output != null)
		{
			try
			{
				XsltTransformer.transformFile(exportFile, output, xsltFile);
			}
			catch (Exception e)
			{
				LogMgr.logError("DataSpooler.startExport()", "Error when transforming " + output + " using " + xsltFile, e);
			}
		}
	}
}
