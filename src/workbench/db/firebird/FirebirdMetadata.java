/*
 * FirebirdMetadata.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2005, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: info@sql-workbench.net
 *
 */
package workbench.db.firebird;

import workbench.db.DbMetadata;
import workbench.db.ProcedureReader;
import workbench.storage.DataStore;
import workbench.util.StrBuffer;

/**
 * @author  info@sql-workbench.net
 */
public class FirebirdMetadata
	implements ProcedureReader
{
	private DbMetadata metaData;
	public FirebirdMetadata(DbMetadata meta)
	{
		this.metaData = meta;
	}
	
	public StrBuffer getProcedureHeader(String aCatalog, String aSchema, String aProcname)
	{
		StrBuffer source = new StrBuffer();
		try
		{
			DataStore ds = this.metaData.getProcedureColumns(aCatalog, aSchema, aProcname);
			source.append("CREATE PROCEDURE ");
			source.append(aProcname);
			String retType = null;
			int count = ds.getRowCount();
			int added = 0;
			for (int i=0; i < count; i++)
			{
				String vartype = ds.getValueAsString(i, DbMetadata.COLUMN_IDX_PROC_COLUMNS_DATA_TYPE);
				String name = ds.getValueAsString(i, DbMetadata.COLUMN_IDX_PROC_COLUMNS_COL_NAME);
				String ret = ds.getValueAsString(i, DbMetadata.COLUMN_IDX_PROC_COLUMNS_RESULT_TYPE);
				if ("OUT".equals(ret))
				{
					retType = "(" + name + " " + vartype + ")";
				}
				else
				{
					if (added > 0)
					{
						source.append(",");
					}
					else
					{
						source.append(" (");
					}
					source.append(name);
					source.append(" ");
					source.append(vartype);
					added ++;
				}
			}
			if (added > 0) source.append(")");
			if (retType != null)
			{
				source.append("\nRETURNS ");
				source.append(retType);
			}
			source.append("\nAS");
		}
		catch (Exception e)
		{
			source = new StrBuffer();
		}
		return source;
	}

}
