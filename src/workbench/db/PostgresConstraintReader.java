/*
 * FirebirdColumnConstraintReader.java
 *
 * Created on February 14, 2004, 1:35 PM
 */

package workbench.db;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import workbench.log.LogMgr;

/**
 *
 * @author  workbench@kellerer.org
 */
public class PostgresConstraintReader extends AbstractConstraintReader
{
	private static final String TABLE_SQL = 
					 "select rel.rcsrc \n" + 
           "from pg_class t, pg_relcheck rel \n" + 
           "where t.relname = ? \n" + 
           "and   t.oid = rel.rcrelid \n";
	
	public PostgresConstraintReader()
	{
	}
	
	public String getPrefixTableConstraintKeyword() { return "check"; }
	public String getColumnConstraintSql() { return null; }
	public String getTableConstraintSql() { return TABLE_SQL; }


}
