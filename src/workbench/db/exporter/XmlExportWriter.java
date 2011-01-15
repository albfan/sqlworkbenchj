/*
 * XmlExportWriter.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.db.exporter;

import workbench.db.DbMetadata;
import workbench.db.WbConnection;
import workbench.log.LogMgr;
import workbench.storage.BlobFormatterFactory;
import workbench.storage.BlobLiteralType;
import workbench.util.XsltTransformer;

/**
 *
 * @author  Thomas Kellerer
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
		BlobMode mode = exporter.getBlobMode();
		if (mode == BlobMode.AnsiLiteral)
		{
			conv.setBlobFormatter(BlobFormatterFactory.createAnsiFormatter());
			conv.setWriteBlobToFile(false);
		}
		else if (mode == BlobMode.Base64)
		{
			conv.setBlobFormatter(BlobFormatterFactory.createInstance(BlobLiteralType.base64));
			conv.setWriteBlobToFile(false);
		}
		else if (mode == BlobMode.DbmsLiteral)
		{
			DbMetadata meta = null;
			WbConnection con = exporter.getConnection();
			if (con != null) meta = con.getMetadata();
			conv.setBlobFormatter(BlobFormatterFactory.createInstance(meta));
			conv.setWriteBlobToFile(false);
		}
		else if (mode == BlobMode.SaveToFile)
		{
			conv.setBlobFormatter(null);
			conv.setWriteBlobToFile(true);
		}
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
			XsltTransformer transformer = new XsltTransformer();
			try
			{
				transformer.transform(exportFile, output, xsltFile);
			}
			catch (Exception e)
			{
				LogMgr.logError("DataSpooler.startExport()", "Error when transforming " + output + " using " + xsltFile, e);
				exporter.addError(transformer.getAllOutputs(e));
			}
		}
		return rowsWritten;
	}
}
