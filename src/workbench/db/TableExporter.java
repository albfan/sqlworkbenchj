/*
 * Created on 27. August 2002, 21:17
 */
package workbench.db;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.util.Date;
import workbench.db.WbConnection;

/**
 *
 * @author  workbench@kellerer.org
 */
public class TableExporter
{
	
	public TableExporter()
	{
	}
	
	public static void exportTable(WbConnection aConnection, String aTablename, String anOutputfile)
		throws IOException, SQLException
	{
		String sql = "select * from " + aTablename;
		Statement stmt = aConnection.createStatement();
		ResultSet rs = stmt.executeQuery(sql);
		StringBuffer line = new StringBuffer(500);
		ResultSetMetaData meta = rs.getMetaData();
		int colCount = meta.getColumnCount();
		int types[]  = new int[colCount];
		for (int i=1; i <= colCount; i++)
		{
			types[i-1] = meta.getColumnType(i);
		}
		Object value = null;
		DbDateFormatter formatter = aConnection.getMetadata().getDateLiteralFormatter();
		int colType = 0;
		PrintWriter pw = new PrintWriter(new BufferedWriter(new FileWriter(anOutputfile), 512*1024));
		
		while (rs.next())
		{
			line.setLength(0);
			line.ensureCapacity(500);
			for (int i=1; i <= colCount; i++)
			{
				value = rs.getObject(i);
				if (!rs.wasNull() && value != null)
				{
					if ( types[i-1] == Types.DATE || types[i-1] == Types.TIMESTAMP)
					{
						line.append(formatter.getLiteral((Date)value));
					}
					else
					{
						line.append(value.toString());
					}
				}
			}
			pw.println(line.toString());
		}
		pw.close();
		rs.close();
		stmt.close();
	}
	
}
