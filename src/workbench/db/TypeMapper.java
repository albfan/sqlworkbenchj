package workbench.db;

import java.util.Map;
import java.sql.Connection;
import java.util.List;
import workbench.util.SqlUtil;
import workbench.log.LogMgr;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Collections;
import java.sql.ResultSet;
import java.sql.Types;
import workbench.resource.ResourceMgr;

public class TypeMapper
{
	private WbConnection dbConn;
	private HashMap typeInfo;
	private List ignoreTypes;

	public TypeMapper(WbConnection aConnection)
	{
		this(aConnection, Collections.EMPTY_LIST);
	}

	public TypeMapper(WbConnection aConnection, List ignoreList)
	{
		this.dbConn = aConnection;
		this.ignoreTypes = ignoreList;
		this.createTypeMap();
	}

	public String getTypeName(int type, int size, int digits)
	{
		Integer key = new Integer(type);
		if (!this.typeInfo.containsKey(key)) return SqlUtil.getTypeName(type);
		TypeInfo info = (TypeInfo)this.typeInfo.get(key);

		StringBuffer result = new StringBuffer(30);
		result.append(info.name);

		// Now we need to check if the data type needs an argument.
		// I could use the "parameter" column from the driver's type info
		// result set, but it seems that not every DBMS returns the correct
		// information there, so I'm using my "own" logic to determine
		// if the data type needs an argument.
		if (type == Types.VARCHAR || type == Types.CHAR)
		{
			if (size == 0) return null;
			result.append('(');
			result.append(size);
			result.append(')');
		}
		// INTEGER's normally don't need a size argument
		else if (SqlUtil.isNumberType(type) && !SqlUtil.isIntegerType(type))
		{
			//if (size == 0) return null;
			if (size > info.max_scale)
			{
				size = info.max_scale;
			}
			if (SqlUtil.isDecimalType(type, size, digits))
			{
				result.append('(');
				result.append(size);
				result.append(',');
				result.append(digits);
				result.append(')');
			}
			else if (type != Types.INTEGER)
			{
				result.append('(');
				result.append(size);
				result.append(')');
			}
		}

		return result.toString();
	}


	private void createTypeMap()
	{
		ResultSet rs = null;
		this.typeInfo = new HashMap(27);
		try
		{
			rs = this.dbConn.getSqlConnection().getMetaData().getTypeInfo();
			while (rs.next())
			{
				String name = rs.getString(1);
				int type = rs.getInt(2);

				// we can't handle arrays anyway
				if (type == java.sql.Types.ARRAY || type == java.sql.Types.OTHER) continue;
				if (this.ignoreTypes.contains(name)) continue;

				TypeInfo info = new TypeInfo();
				info.name = name;
				info.type = type;
				info.precision = rs.getInt(3);
				info.min_scale = rs.getInt(14);
				info.max_scale = rs.getInt(15);
				Integer key = new Integer(type);
				if (this.typeInfo.containsKey(key))
				{
					LogMgr.logWarning("TypeMapper.createTypeMap()", "The mapping from JDBC type "  + SqlUtil.getTypeName(type) + " to  DB type " + name + " will be ignored. A mapping is already present.");
				}
				else
				{
					LogMgr.logInfo("TypeMapper.createTypeMap()", "Mapping JDBC type "  + SqlUtil.getTypeName(type) + " to DB type " + name);
					this.typeInfo.put(key, info);
				}
			}
		}
		catch (SQLException e)
		{
			LogMgr.logError("TypeMapper.createTypeMap()", "Error reading type info for target connection", e);
			this.typeInfo = new HashMap();
		}
		finally
		{
			try { rs.close(); } catch (Throwable th) {}
		}
	}
}

class TypeInfo
{
	String name;
	int type;
	int precision;
	int min_scale;
	int max_scale;
}