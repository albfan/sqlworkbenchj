/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package workbench.storage;

import java.sql.Types;
import java.util.List;
import workbench.util.SqlUtil;

/**
 * A class to turn the columns of a datastore into rows.
 *
 * @author Thomas Kellerer
 */
public class DatastoreTransposer
{
	private DataStore source;
	private String resultName;

	public DatastoreTransposer(DataStore sourceData)
	{
		this.source = sourceData;
		retrieveResultName();
	}

	private void retrieveResultName()
	{
		if (source == null)
		{
			resultName = "";
			return;
		}
		resultName = source.getResultName();
		if (resultName == null)
		{
			String sql = source.getGeneratingSql();
			List<String> tables = SqlUtil.getTables(sql, false);
			if (tables.size() == 1)
			{
				resultName = tables.get(0);
			}
		}
	}

	public DataStore transposeRow(int row)
	{
		if (row < 0 || row >= source.getRowCount()) return null;
		DataStore ds = createDatastore();
		int colCount = source.getColumnCount();
		for (int col=0; col < colCount; col ++)
		{
			int newRow = ds.addRow();
			ds.setValue(newRow, 0, source.getColumnDisplayName(col));
			ds.setValue(newRow, 1, source.getValueAsString(row, col));
		}

		String name = "Row " + Integer.toString(row + 1);
		if (resultName != null)
		{
			name = resultName + " (" + name + ")";
		}
		ds.setResultName(name);
		ds.resetStatus();
		return ds;
	}

	private DataStore createDatastore()
	{
		String[] columns = new String[] { "Column", "Value" };
		int[] types = new int[] { Types.VARCHAR, Types.VARCHAR };
		return new DataStore(columns, types);
	}
}
