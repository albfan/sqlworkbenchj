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
public class SqlServerConstraintReader extends AbstractConstraintReader
{
	private static final String TABLE_SQL = 
					 "select c.text \n" + 
           "from sysobjects cons, \n" + 
           "     syscomments c, \n" + 
           "     sysobjects tab \n" + 
           "where cons.xtype = 'C' \n" + 
           "and   cons.id = c.id \n" + 
           "and   cons.parent_obj = tab.id \n" + 
           "and   tab.name = ? \n";	
	public SqlServerConstraintReader()
	{
	}
	
	public String getPrefixTableConstraintKeyword() { return "check"; }
	public String getColumnConstraintSql() { return null; }
	public String getTableConstraintSql() { return TABLE_SQL; }


}
