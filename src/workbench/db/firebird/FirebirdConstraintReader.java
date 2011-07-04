/*
 * FirebirdConstraintReader.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2011, Thomas Kellerer
 * No part of this code may be reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.db.firebird;

import workbench.db.AbstractConstraintReader;

/**
 * An implementation of {@link AbstractConstraintReader} for the
 * <a href="http://www.firebirdsql.org">Firebird</a> database server
 * @author  Thomas Kellerer
 */
public class FirebirdConstraintReader
	extends AbstractConstraintReader
{

	private final String TABLE_SQL =
		 "select trim(cc.rdb$constraint_name), trg.rdb$trigger_source " +
		 "from rdb$relation_constraints rc,  \n" +
		 "     rdb$check_constraints cc, \n" +
		 "     rdb$triggers trg \n" +
		 "where rc.rdb$relation_name = ? \n" +
		 "and   rc.rdb$constraint_type = 'CHECK' \n" +
		 "and   rc.rdb$constraint_name = cc.rdb$constraint_name \n" +
		 "and   cc.rdb$trigger_name = trg.rdb$trigger_name \n" +
		 "and   trg.rdb$trigger_type = 1 \n";

	@Override
	public String getColumnConstraintSql()
	{
		return null;
	}

	@Override
	public String getTableConstraintSql()
	{
		return TABLE_SQL;
	}
}
