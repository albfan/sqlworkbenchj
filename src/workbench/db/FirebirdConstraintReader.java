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
 *
 * @author  workbench@kellerer.org
 */
public class FirebirdConstraintReader extends AbstractConstraintReader
{
	
	private static final String COL_SQL="select f.rdb$field_name,rdb$trigger_source \n" + 
           "from rdb$relation_constraints rc,  \n" + 
           "     rdb$check_constraints cc, \n" + 
           "     rdb$triggers trg, \n" + 
           "     rdb$dependencies dep, \n" + 
           "     rdb$relation_fields f \n" + 
           "where rc.rdb$relation_name = ? \n" + 
           "and   rc.rdb$constraint_type = 'CHECK' \n" + 
           "and   rc.rdb$constraint_name = cc.rdb$constraint_name \n" + 
           "and   cc.rdb$trigger_name = trg.rdb$trigger_name \n" + 
           "and   dep.rdb$depended_on_name = rc.rdb$relation_name \n" + 
           "and   dep.rdb$field_name = f.rdb$field_name \n" + 
           "and   dep.rdb$dependent_name = trg.rdb$trigger_name \n" + 
           "and   f.rdb$relation_name = rc.rdb$relation_name \n" + 
           "and   trg.rdb$trigger_type = 1 \n";
	
	private static final String TABLE_SQL = "select rdb$trigger_source \n" + 
           "from rdb$relation_constraints rc,  \n" + 
           "     rdb$check_constraints cc, \n" + 
           "     rdb$triggers trg \n" + 
           "where rc.rdb$relation_name = ? \n" + 
           "and   rc.rdb$constraint_type = 'CHECK' \n" + 
           "and   rc.rdb$constraint_name = cc.rdb$constraint_name \n" + 
           "and   cc.rdb$trigger_name = trg.rdb$trigger_name \n" + 
           "and   trg.rdb$trigger_type = 1 \n";

/** Creates a new instance of FirebirdColumnConstraintReader */
	public FirebirdConstraintReader()
	{
	}
	
	public String getColumnConstraintSql() { return null; }
	public String getTableConstraintSql() { return TABLE_SQL; }
}
