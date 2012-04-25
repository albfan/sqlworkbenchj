/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package workbench.storage;

import java.sql.Types;

/**
 * A class to turn the columns of a datastore into rows.
 *
 * @author Thomas Kellerer
 */
public class DatastoreTransposer
{

	private DataStore source;

	public DatastoreTransposer(DataStore sourceData)
	{
		this.source = sourceData;
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
		String name = source.getResultName();
		if (name == null)
		{
			name = "Row " + Integer.toString(row + 1);
		}
		else
		{
			name = name + " (Row " + Integer.toString(row + 1) + ")";
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
