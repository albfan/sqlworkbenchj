/*
 * RowDataReceiver.java
 *
 * Created on October 17, 2003, 11:14 PM
 */

package workbench.db.importer;

import java.sql.SQLException;

/**
 *
 * @author  thomas
 */
public interface RowDataReceiver
{
	void processRow(Object[] row) throws SQLException;
	void setTargetTable(String tableName, String[] columns, int[] columnTypes);
	void importFinished();
	void importCancelled();
}
