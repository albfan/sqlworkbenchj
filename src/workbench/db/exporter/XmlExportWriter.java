/*
 * XmlExportWriter.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2016, Thomas Kellerer
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at.
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
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
package workbench.db.exporter;

import workbench.log.LogMgr;

import workbench.db.DbMetadata;
import workbench.db.WbConnection;

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
				transformer.transform(exportFile, output, xsltFile, exporter.getXsltParameters());
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
