/*
 * RowDataReceiver.java
 *
 * Created on October 17, 2003, 11:14 PM
 */

package workbench.db.importer;

import java.sql.SQLException;
import workbench.db.ColumnIdentifier;
import workbench.db.WbConnection;

/**
 *
 * @author  thomas
 */
public interface RowDataReceiver
{
	void processRow(Object[] row) throws SQLException;
	void setTargetTable(String tableName, ColumnIdentifier[] columns)	throws SQLException;
	void importFinished();
	void importCancelled();
}
