/*
 * ObjectResultListDataStore
 * 
 *  This file is part of SQL Workbench/J, http://www.sql-workbench.net
 * 
 *  Copyright 2002-2009, Thomas Kellerer
 *  No part of this code maybe reused without the permission of the author
 * 
 *  To contact the author please send an email to: support@sql-workbench.net
 */
package workbench.sql.wbcommands;

import java.sql.SQLException;
import java.sql.Types;
import java.util.List;
import workbench.db.DbObject;
import workbench.db.WbConnection;
import workbench.storage.DataStore;

/**
 *
 * @author Thomas Kellerer
 */
public class ObjectResultListDataStore
	extends DataStore
{
	public static final int COL_IDX_OBJECT_NAME = 0;
	public static final int COL_IDX_OBJECT_TYPE = 1;
	public static final int COL_IDX_SOURCE = 2;
	
	private static final String[] colnames = new String[] { "NAME", "TYPE", "SOURCE" };
	private static final int[] colTypes = new int[] { Types.VARCHAR, Types.VARCHAR, Types.CLOB };
	private static final int[] colSizes = new int[] { 30, 30, 50 };

	public ObjectResultListDataStore()
	{
		super(colnames, colTypes, colSizes);
	}
	
	public ObjectResultListDataStore(WbConnection con, List<DbObject> resultList, boolean showFullname)
		throws SQLException
	{
		super(colnames, colTypes, colSizes);
		setResultList(con, resultList, showFullname);
	}

	public void setResultList(WbConnection con, List<DbObject> resultList, boolean showFullname)
		throws SQLException
	{
		for (DbObject object : resultList)
		{
			int row = addRow();
			String name = null;
			if (showFullname)
			{
				name = object.getFullyQualifiedName(con);
			}
			else
			{
				name = object.getObjectName();
			}
			setValue(row, COL_IDX_OBJECT_NAME, name);
			setValue(row, COL_IDX_OBJECT_TYPE, object.getObjectType());
			setValue(row, COL_IDX_SOURCE, object.getSource(con));
		}
		resetStatus();
	}
	
}
