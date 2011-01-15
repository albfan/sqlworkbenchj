/*
 * ParameterDefinition.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2011, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.sql.preparedstatement;

import java.sql.ParameterMetaData;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import workbench.log.LogMgr;
import workbench.util.NumberStringCache;
import workbench.util.SqlUtil;
import workbench.util.ValueConverter;

/**
 * The defintion for a single parameter in StatementParameters
 *
 * @see StatementParameters
 * @author  Thomas Kellerer
 */
public class ParameterDefinition
{
	private final ValueConverter converter = new ValueConverter();
	private int dataType;
	private int parameterIndex;
	private String parameterName;

	/**
	 * Parameter mode according to 
	 * ParameterMetaData.parameterModeOut
	 * ParameterMetaData.parameterModeIn
	 * ParameterMetaData.parameterModeInOut;
	 */
	private int parameterMode = ParameterMetaData.parameterModeUnknown;

	private boolean valueValid = false;
	private Object value = null;

	public ParameterDefinition(int index, int type)
	{
		this.parameterIndex = index;
		this.dataType = type;
	}

	public int getIndex()
	{
		return this.parameterIndex;
	}

	public int getType()
	{
		return this.dataType;
	}

	public boolean isValueValid(String v)
	{
		try
		{
			converter.convertValue(v, this.dataType);
			return true;
		}
		catch (Exception e)
		{
			return false;
		}
	}

	public void setParameterName(String parm)
	{
		this.parameterName = parm;
	}

	public String getParameterName()
	{
		if (parameterName == null) return NumberStringCache.getNumberString(this.parameterIndex);
		return parameterName;
	}

	public boolean setValue(String v)
	{
		try
		{
			this.value = converter.convertValue(v, this.dataType);
			this.valueValid = true;
		}
		catch (Exception e)
		{
			this.valueValid = false;
			LogMgr.logError("ParameterDefinition.setValue()", "Error applying value " + v + " for type " + SqlUtil.getTypeName(this.dataType), e);
		}
		return this.valueValid;
	}

	public Object getValue()
	{
		return this.value;
	}

	public void setStatementValue(PreparedStatement stmt)
		throws IllegalStateException, SQLException
	{
		if (!this.valueValid) throw new IllegalStateException("No valid value defined for parameter #" + this.parameterIndex);
		stmt.setObject(this.parameterIndex, this.value);
	}

	public static void sortByIndex(List<ParameterDefinition> parameters)
	{
		Comparator<ParameterDefinition> comp = new Comparator<ParameterDefinition>()
		{
			public int compare(ParameterDefinition p1, ParameterDefinition p2)
			{
				return p1.parameterIndex - p2.parameterIndex;
			}
		};
		Collections.sort(parameters, comp);
	}

	public String toString()
	{
		return this.parameterName + "=" + this.value + "(" + parameterIndex + ")";
	}

	public boolean isOutParameter()
	{
		return parameterMode == ParameterMetaData.parameterModeOut || parameterMode == ParameterMetaData.parameterModeInOut;
	}

	public int getParameterMode()
	{
		return parameterMode;
	}
	
	public void setParameterMode(int mode)
	{
		if (mode != ParameterMetaData.parameterModeOut && 
			  mode != ParameterMetaData.parameterModeIn &&
				mode != ParameterMetaData.parameterModeInOut && 
				mode != ParameterMetaData.parameterModeUnknown)
		{
			throw new IllegalArgumentException("Incorrect parameter mode specified!");
		}
		this.parameterMode = mode;
	}
}
