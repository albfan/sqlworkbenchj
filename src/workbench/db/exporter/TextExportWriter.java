/*
 * TextExportWriter.java
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
import workbench.storage.BlobFormatterFactory;
import workbench.storage.BlobLiteralType;

/**
 * An ExportWriter to generate flat files.
 * 
 * @author  Thomas Kellerer
 */
public class TextExportWriter
	extends ExportWriter
{
	public TextExportWriter(DataExporter exp)
	{
		super(exp);
	}

	public RowDataConverter createConverter()
	{
		return new TextRowDataConverter();
	}

	public void configureConverter()
	{
		super.configureConverter();
		TextRowDataConverter conv = (TextRowDataConverter)this.converter;
		conv.setDelimiter(exporter.getTextDelimiter());
		conv.setQuoteCharacter(exporter.getTextQuoteChar());
		conv.setQuoteAlways(exporter.getQuoteAlways());
		conv.setEscapeRange(exporter.getEscapeRange());
		conv.setLineEnding(exporter.getLineEnding());
		conv.setWriteClobToFile(exporter.getWriteClobAsFile());
		conv.setQuoteEscaping(exporter.getQuoteEscaping());
		conv.setRowIndexColName(exporter.getRowIndexColumnName());
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
	
}
