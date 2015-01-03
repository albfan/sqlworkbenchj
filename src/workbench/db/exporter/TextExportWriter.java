/*
 * TextExportWriter.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2015, Thomas Kellerer
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

import workbench.db.DbMetadata;
import workbench.db.WbConnection;
import workbench.storage.BlobFormatterFactory;
import workbench.storage.BlobLiteralType;
import workbench.storage.PostgresBlobFormatter;

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

	@Override
	public RowDataConverter createConverter()
	{
		return new TextRowDataConverter();
	}

	@Override
	public void configureConverter()
	{
		super.configureConverter();
		TextRowDataConverter conv = (TextRowDataConverter)this.converter;
		conv.setDelimiter(exporter.getTextDelimiter());
		conv.setQuoteCharacter(exporter.getTextQuoteChar());
		conv.setQuoteAlways(exporter.getQuoteAlways());
		conv.setEscapeRange(exporter.getEscapeRange());
		conv.setNullString(exporter.getNullString());
		conv.setLineEnding(exporter.getLineEnding());
		conv.setWriteClobToFile(exporter.getWriteClobAsFile());
		conv.setQuoteEscaping(exporter.getQuoteEscaping());
		conv.setRowIndexColName(exporter.getRowIndexColumnName());
		conv.setEscapeType(exporter.getEscapeType());
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
		else if (mode == BlobMode.pgHex || mode == BlobMode.pgEscape)
		{
			PostgresBlobFormatter formatter = new PostgresBlobFormatter(mode);
			conv.setBlobFormatter(formatter);
			conv.setWriteBlobToFile(false);
		}
		else if (mode == BlobMode.SaveToFile)
		{
			conv.setBlobFormatter(null);
			conv.setWriteBlobToFile(true);
		}
	}

}
