/*
 * ExportWriter.java
 *
 * Created on September 8, 2004, 11:36 PM
 */

package workbench.db.exporter;

import java.io.IOException;
import java.io.Writer;
import java.sql.ResultSet;
import java.sql.SQLException;
import workbench.storage.ResultInfo;
import workbench.storage.RowData;
import workbench.storage.RowDataConverter;
import workbench.util.StrBuffer;

/**
 *
 * @author  workbench@kellerer.org
 */
public abstract class ExportWriter
{
	protected DataExporter exporter;
	protected boolean cancel = false;

	public ExportWriter(DataExporter exp)
	{
		this.exporter = exp;
	}
	
	public abstract RowDataConverter createConverter(ResultInfo info);
	
	public void writeExport(Writer out, ResultSet rs, ResultInfo info)
		throws SQLException, IOException
	{
		RowDataConverter converter = createConverter(info);
		this.cancel = false;
		StrBuffer data = converter.getStart();
		if (data != null)
		{
			data.writeTo(out);
		}
		int currentRow = 0;
		int colCount = info.getColumnCount();
		while (rs.next())
		{
			if (this.cancel) break;
			RowData row = new RowData(colCount);
			row.read(rs, info);
			data = converter.convertRowData(row, currentRow);
			data.writeTo(out);
		}
		data = converter.getEnd();
		if (data != null)
		{
			data.writeTo(out);
		}
	}
	
	public void exportFinished()
	{
	}
	
	public void cancel()
	{
		this.cancel = true;
	}
	
}
