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
public class HsqlConstraintReader extends AbstractConstraintReader
{
	
	
	private static final String TABLE_SQL = "select chk.check_clause \n" + 
           "from system_check_constraints chk, system_table_constraints cons \n" + 
           "where chk.constraint_name = cons.constraint_name  \n" + 
           "and cons.constraint_type = 'CHECK' \n" + 
           "and cons.table_name = ?; \n";

/** Creates a new instance of FirebirdColumnConstraintReader */
	public HsqlConstraintReader()
	{
	}
	public String getPrefixTableConstraintKeyword() { return "check("; }
	public String getSuffixTableConstraintKeyword() { return ")"; }
	public String getColumnConstraintSql() { return null; }
	public String getTableConstraintSql() { return TABLE_SQL; }
}
