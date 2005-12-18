/*
 * FirebirdConstraintReader.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2005, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.db.firebird;

import workbench.db.AbstractConstraintReader;

/**
 * An implementation of {@link AbstractConstraintReader} for the Firebird database server
 * @author  support@sql-workbench.net
 */
public class FirebirdConstraintReader 
	extends AbstractConstraintReader
{
	
//	private static final String COL_SQL="select f.rdb$field_name,rdb$trigger_source \n" + 
//           "from rdb$relation_constraints rc,  \n" + 
//           "     rdb$check_constraints cc, \n" + 
//           "     rdb$triggers trg, \n" + 
//           "     rdb$dependencies dep, \n" + 
//           "     rdb$relation_fields f \n" + 
//           "where rc.rdb$relation_name = ? \n" + 
//           "and   rc.rdb$constraint_type = 'CHECK' \n" + 
//           "and   rc.rdb$constraint_name = cc.rdb$constraint_name \n" + 
//           "and   cc.rdb$trigger_name = trg.rdb$trigger_name \n" + 
//           "and   dep.rdb$depended_on_name = rc.rdb$relation_name \n" + 
//           "and   dep.rdb$field_name = f.rdb$field_name \n" + 
//           "and   dep.rdb$dependent_name = trg.rdb$trigger_name \n" + 
//           "and   f.rdb$relation_name = rc.rdb$relation_name \n" + 
//           "and   trg.rdb$trigger_type = 1 \n";
	
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
