/*
 * DataType.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2010, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.db.report;
import java.util.ArrayList;
import java.util.List;
import workbench.db.ColumnIdentifier;
import workbench.util.SqlUtil;
import static workbench.util.SqlUtil.*;

/**
 * @author Thomas Kellerer
 */
public class DataType 
{
	private String dbmsName;
	private String typeName;
	private int id = -1;
	private int typeGroupId = 0;
	private List<String> parameters = new ArrayList<String>(2);

	public static DataType getDataType(ColumnIdentifier column)
	{
		int jdbcType = column.getDataType();
		int scale = column.getColumnSize();
		int precision = column.getDecimalDigits();
		String dbmsName = column.getDbmsType();
		int pos = dbmsName.indexOf("(");
		if (pos > -1)
		{
			dbmsName = dbmsName.substring(0, pos);
		}
		
		if (isBlobType(jdbcType) || isClobType(jdbcType) ||
			  isDateType(jdbcType) || isIntegerType(jdbcType))
		{
			return new DataType(dbmsName, jdbcType);
		}
		if (isDecimalType(jdbcType, scale, precision))
		{
			return new DataType(dbmsName, jdbcType, "Length", "Decimals");
		}
		if (isCharacterType(jdbcType))
		{
			return new DataType(dbmsName, jdbcType, "Size");
		}
		return new DataType(dbmsName, jdbcType);
	}
	
	private DataType(String name, int jdbcType)
	{
		this(name, jdbcType, null, null);
	}
	
	private DataType(String name, int jdbcType, String param1)
	{
		this(name, jdbcType, param1, null);
	}
	
	private DataType(String name, int jdbcType, String param1, String param2)
	{
		this.dbmsName = name;
		this.typeName = SqlUtil.getTypeName(jdbcType);
		if (param1 != null) parameters.add(param1);
		if (param2 != null) parameters.add(param2);
	}

	public void setTypeGroupId(int id)
	{
		this.typeGroupId = id;
	}
	
	public int getTypeGroupId()
	{
		return typeGroupId;
	}
	
	public void setId(int i)
	{
		if (this.id == -1) this.id = i;
	}
	
	public int getId()
	{
		return this.id;
	}
	
	public String getTypeName()
	{
		return this.typeName;
	}
	
	public String getDbmsTypeName()
	{
		return this.dbmsName;
	}
	
	public List<String> getParameters()
	{
		return this.parameters;
	}

	@Override
	public boolean equals(Object obj)
	{
		if (obj == null)
		{
			return false;
		}
		if (getClass() != obj.getClass())
		{
			return false;
		}
		final DataType other = (DataType)obj;
		if (this.typeName != other.typeName && (this.typeName == null || !this.typeName.equals(other.typeName)))
		{
			return false;
		}
		if (this.parameters != other.parameters && (this.parameters == null || !this.parameters.equals(other.parameters)))
		{
			return false;
		}
		return true;
	}

	@Override
	public int hashCode()
	{
		int hash = 7;
		hash = 37 * hash + (this.typeName != null ? this.typeName.hashCode() : 0);
		hash = 37 * hash + (this.parameters != null ? this.parameters.hashCode() : 0);
		return hash;
	}

	
}
