/*
 * XmlExportWriter.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2007, Thomas Kellerer
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
		return new XmlRowDataConverter();
	}
	
	public void configureConverter()
	{
		super.configureConverter();
		XmlRowDataConverter conv = (XmlRowDataConverter)this.converter;
		conv.setUseCDATA(this.exporter.getUseCDATA());
		conv.setLineEnding(exporter.getLineEnding());
		conv.setUseVerboseFormat(exporter.getUseVerboseFormat());
		conv.setTableNameToUse(exporter.getTableName());
		conv.setWriteClobToFile(exporter.getWriteClobAsFile());
		//conv.setBaseFilename(exporter.getOutputFilename());
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
