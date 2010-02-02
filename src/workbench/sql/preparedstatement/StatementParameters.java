/*
 * StatementParameters.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2010, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.sql.preparedstatement;

import java.sql.ParameterMetaData;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.List;
import workbench.db.WbConnection;
import workbench.log.LogMgr;
import workbench.util.CollectionUtil;
import workbench.util.SqlUtil;

/**
 * A class to store the parameters for a PreparedStatement
 * @author  Thomas Kellerer
 */
public class StatementParameters
{
	private List<ParameterDefinition> parameterList;

	public StatementParameters(String sql, WbConnection conn)
		throws SQLException
	{
		PreparedStatement pstmt = null;
		try
		{
			pstmt = conn.getSqlConnection().prepareStatement(sql);
			ParameterMetaData meta = pstmt.getParameterMetaData();
			int parameterCount = meta.getParameterCount();
			if (parameterCount > 0)
			{
				parameterList = CollectionUtil.sizedArrayList(parameterCount);

				for (int i=1; i <= parameterCount; i++)
				{
					int type = meta.getParameterType(i);
					ParameterDefinition def = new ParameterDefinition(i, type);
					parameterList.add(def);
				}
			}
		}
		catch (Exception e)
		{
			LogMgr.logError("StatementParameter.<init>", "Error when checking parameters", e);
			if (e instanceof SQLException) throw (SQLException)e;
			else throw new SQLException("Error retrieving statement parameters: " + e.getClass().getName());
		}
		finally
		{
			SqlUtil.closeStatement(pstmt);
		}
	}

	public StatementParameters(List<ParameterDefinition> params)
	{
		parameterList = CollectionUtil.sizedArrayList(params.size());
		parameterList.addAll(params);
	}

	public String getParameterName(int index)
	{
		return this.parameterList.get(index).getParameterName();
	}

	public int getParameterType(int index)
	{
		return this.parameterList.get(index).getType();
	}

	public Object getParameterValue(int index)
	{
		return this.parameterList.get(index).getValue();
	}

	public void applyParameter(PreparedStatement pstmt)
		throws SQLException
	{
		for (ParameterDefinition param : parameterList)
		{
			param.setStatementValue(pstmt);
		}
	}

	public boolean isValueValid(int index, String value)
	{
		return this.parameterList.get(index).isValueValid(value);
	}

	public void setParameterValue(int index, String value)
	{
		this.parameterList.get(index).setValue(value);
	}

	public int getParameterCount()
	{
		if (parameterList == null) return 0;
		return parameterList.size();
	}

	public boolean hasParameter()
	{
		return getParameterCount() > 0;
	}
}

