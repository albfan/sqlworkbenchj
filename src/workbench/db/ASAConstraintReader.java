/*
 * FirebirdColumnConstraintReader.java
 *
 * Created on February 14, 2004, 1:35 PM
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
 * Constraint reader for Adaptive Server Anywhere
 * @author  workbench@kellerer.org
 */
public class ASAConstraintReader extends AbstractConstraintReader
{
	
	
	private static final String TABLE_SQL = "select chk.check_defn \n" + 
           "from syscheck chk, sysconstraint cons, systable tbl \n" + 
           "where chk.check_id = cons.constraint_id \n" + 
           "and   cons.constraint_type = 'T' \n" + 
           "and   cons.table_id = tbl.table_id \n" + 
           "and   tbl.table_name = ? \n";

/** Creates a new instance of FirebirdColumnConstraintReader */
	public ASAConstraintReader()
	{
	}
	
	public String getColumnConstraintSql() { return null; }
	public String getTableConstraintSql() { return TABLE_SQL; }
}
