/*
 * ParameterDefinition.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2008, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.sql.preparedstatement;

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
 * @author  support@sql-workbench.net
 */
public class ParameterDefinition
{
	private final ValueConverter converter = new ValueConverter();
	private int type;
	private int index;
	private String name;

	private boolean valueValid = false;
	private Object value = null;
	
	public ParameterDefinition(int index, int type)
	{
		this.index = index;
		this.type = type;
	}
	
	public int getIndex() { return this.index; }
	public int getType() { return this.type; }
	
	public boolean isValueValid(String v)
	{
		try
		{
			converter.convertValue(v, this.type);
			return true;
		}
		catch (Exception e)
		{
			return false;
		}
	}

	public void setParameterName(String parm)
	{
		this.name = parm;
	}

	public String getParameterName()
	{
		if (name == null) return NumberStringCache.getNumberString(this.index);
		return name;
	}

	public boolean setValue(String v)
	{
		try
		{
			this.value = converter.convertValue(v, this.type);
			this.valueValid = true;
		}
		catch (Exception e)
		{
			this.valueValid = false;
			LogMgr.logError("ParameterDefinition.setValue()", "Error applying value " + v + " for type " + SqlUtil.getTypeName(this.type), e);
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
		if (!this.valueValid) throw new IllegalStateException("No valid value defined for parameter " + this.index);
		stmt.setObject(this.index, this.value);
	}

	public static void sortByIndex(List<ParameterDefinition> parameters)
	{
		Comparator<ParameterDefinition> comp = new Comparator<ParameterDefinition>()
		{
			public int compare(ParameterDefinition p1, ParameterDefinition p2)
			{
				return p1.index - p2.index;
			}
		};
		Collections.sort(parameters, comp);
	}
}
