/*
 * CloudscapeColumnConstraintReader.java
 *
 */

package workbench.db;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import workbench.log.LogMgr;

/**
 * Constraint reader for Cloudscape database
 * @author  workbench@kellerer.org
 */
public class CloudscapeConstraintReader extends AbstractConstraintReader
{
	
	
	private static final String TABLE_SQL = "select 'check '|| c.checkdefinition \n" + 
             "from sys.syschecks c, sys.systables t, sys.sysconstraints cons, sys.sysschemas s \n" + 
             "where t.tableid = cons.tableid \n" + 
             "and   t.schemaid = s.schemaid \n" + 
             "and   cons.constraintid = c.constraintid \n" + 
             "and   t.tablename = ? \n" + 
             "and   s.schemaname = ?";

						 
	public CloudscapeConstraintReader()
	{
	}
	
	public String getColumnConstraintSql() { return null; }
	public String getTableConstraintSql() { return TABLE_SQL; }
	
	public int getIndexForTableNameParameter() { return 1; }
	public int getIndexForSchemaParameter() { return 2; }
}
