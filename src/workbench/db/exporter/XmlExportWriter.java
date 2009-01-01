/*
 * XmlExportWriter.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2009, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.db.exporter;

import workbench.log.LogMgr;
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

	@Override
	public RowDataConverter createConverter()
	{
		return new XmlRowDataConverter();
	}

	@Override
	public void configureConverter()
	{
		super.configureConverter();
		XmlRowDataConverter conv = (XmlRowDataConverter)this.converter;
		conv.setUseCDATA(this.exporter.getUseCDATA());
		conv.setLineEnding(exporter.getLineEnding());
		conv.setUseVerboseFormat(exporter.getUseVerboseFormat());
		conv.setXMLVersion(exporter.getXMLVersion());
		conv.setTableNameToUse(exporter.getTableName());
		conv.setWriteClobToFile(exporter.getWriteClobAsFile());
		//conv.setBaseFilename(exporter.getOutputFilename());
	}

	@Override
	public long exportFinished()
	{
		long rowsWritten = super.exportFinished();
		String exportFile = this.exporter.getFullOutputFilename();
		String xsltFile = this.exporter.getXsltTransformation();
		String output = this.exporter.getXsltTransformationOutput();
		if (xsltFile != null && output != null)
		{
			try
			{
				XsltTransformer transformer = new XsltTransformer();
				transformer.transform(exportFile, output, xsltFile);
			}
			catch (Exception e)
			{
				LogMgr.logError("DataSpooler.startExport()", "Error when transforming " + output + " using " + xsltFile, e);
			}
		}
		return rowsWritten;
	}
}
