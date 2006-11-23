/*
 * PostgresProcedureReader.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2006, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.db.postgres;

import java.sql.DatabaseMetaData;
import workbench.db.DbMetadata;
import workbench.db.JdbcProcedureReader;
import workbench.db.ProcedureReader;
import workbench.resource.Settings;
import workbench.storage.DataStore;
import workbench.util.StringUtil;

/**
 * @author  support@sql-workbench.net
 */
public class PostgresProcedureReader
	extends JdbcProcedureReader
{
	public PostgresProcedureReader(DbMetadata meta)
	{
		super(meta);
	}
	
	public StringBuffer getProcedureHeader(String aCatalog, String aSchema, String aProcname, int procType)
	{
		StringBuffer source = new StringBuffer();
		
		String nl = Settings.getInstance().getInternalEditorLineEnding();
		
		try
		{
			DataStore ds = this.getProcedureColumns(aCatalog, aSchema, aProcname);
			source.append("CREATE OR REPLACE ");
			
			if (procType == DatabaseMetaData.procedureReturnsResult) source.append("FUNCTION ");
			else source.append("PROCEDURE ");
			
			source.append(aProcname);
			source.append(" (");
			String retType = null;
			int count = ds.getRowCount();
			int added = 0;
			for (int i=0; i < count; i++)
			{
				String vartype = ds.getValueAsString(i,ProcedureReader.COLUMN_IDX_PROC_COLUMNS_DATA_TYPE);
				String ret = ds.getValueAsString(i,ProcedureReader.COLUMN_IDX_PROC_COLUMNS_RESULT_TYPE);
				if ("RETURN".equals(ret))
				{
					retType = vartype;
				}
				else
				{
					if (added > 0) source.append(',');
					source.append(vartype);
					added ++;
				}
			}
			source.append(')');
			source.append(nl + "RETURNS ");
			source.append(retType);
			source.append(nl + "AS" + nl);
		}
		catch (Exception e)
		{
			source = StringUtil.emptyBuffer();
		}
		return source;
	}

}
