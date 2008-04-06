/*
 * TypeMapper.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2008, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.db;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import workbench.log.LogMgr;
import workbench.util.SqlUtil;

/**
 * A class to map datatypes from one DBMS to another.
 * 
 * @author support@sql-workbench.net
 */
public class TypeMapper
{
	private WbConnection targetDb;
	private HashMap<Integer, String> typeInfo;
	private List<String> ignoreTypes;

	/**
	 * Create a TypeMapper for the DBMS specified through the target connection.
	 * @param targetConnection
	 */
	public TypeMapper(WbConnection targetConnection)
	{
		this.targetDb = targetConnection;
		this.ignoreTypes = targetConnection.getDbSettings().getDataTypesToIgnore();
		this.createTypeMap();
	}

	private String findAlternateBlobType(int type)
	{
		List<Integer> allBlobTypes = new ArrayList<Integer>(4);
		
		// BLOBs are reported as BLOB, VARBINARY (Postgres), LONGVARBNARY (HSQL) and 
		// possibly as BINARY. So we need to test each of them. The order here
		// is a personal feeling which type should be preferred over others ;)
		if (type != Types.BLOB) allBlobTypes.add(new Integer(Types.BLOB));
		if (type != Types.LONGVARBINARY) allBlobTypes.add(new Integer(Types.LONGVARBINARY));
		if (type != Types.VARBINARY) allBlobTypes.add(new Integer(Types.VARBINARY));
		if (type != Types.BINARY) allBlobTypes.add(new Integer(Types.BINARY));
		
		for (Integer blobType : allBlobTypes)
		{
			String name = typeInfo.get(blobType);
			if (name != null) return name;
		}
		return null;
	}

	private String findAlternateClobType(int type)
	{
		String name = null;
		
		// CLOBs can either be reported as LONVARCHAR or CLOB
		if (type == Types.CLOB)
		{
			name = typeInfo.get(new Integer(Types.LONGVARCHAR));
		}
		else 
		{
			name = typeInfo.get(new Integer(Types.CLOB));
		}
		return name;
	}
	
	public String getTypeName(int type, int size, int digits)
	{
		Integer key = new Integer(type);
		String name = this.typeInfo.get(key);
		
		// BLOBs and CLOBs are mapped to different types in different
		// DBMS and not all are using the same java.sql.Types value
		// this code tries to find a mapping even if the "desired" input
		// type was not part of the type info returned by the driver
		if (name == null)
		{
			if (SqlUtil.isBlobType(type))
			{
				name = findAlternateBlobType(type);
			}
			else if (SqlUtil.isClobType(type))
			{
				name = findAlternateClobType(type);
			}
			if (name != null)
			{
				LogMgr.logInfo("TypeMapper.getTypeName()", "Could not find a direct mapping for java.sql.Types." + SqlUtil.getTypeName(type) + ", using DBMS type: " + name);
			}
		}
		
		if (name == null)
		{
			if (!this.typeInfo.containsKey(key)) return SqlUtil.getTypeName(type);
		}

		return this.targetDb.getMetadata().getDataTypeResolver().getSqlTypeDisplay(name, type, size, digits, -1);
	}


	private void createTypeMap()
	{
		ResultSet rs = null;
		this.typeInfo = new HashMap<Integer, String>(27);
		try
		{
			rs = this.targetDb.getSqlConnection().getMetaData().getTypeInfo();
			while (rs.next())
			{
				String name = rs.getString(1);
				int type = rs.getInt(2);

				// we can't handle arrays anyway
				if (type == java.sql.Types.ARRAY || type == java.sql.Types.OTHER) continue;
				if (this.ignoreTypes.contains(name)) continue;

				Integer key = new Integer(type);
				if (this.typeInfo.containsKey(key))
				{
					LogMgr.logWarning("TypeMapper.createTypeMap()", "The mapping from JDBC type "  + SqlUtil.getTypeName(type) + " to  DB type " + name + " will be ignored. A mapping is already present.");
				}
				else
				{
					LogMgr.logInfo("TypeMapper.createTypeMap()", "Mapping JDBC type "  + SqlUtil.getTypeName(type) + " to DB type " + name);
					this.typeInfo.put(key, name);
				}
			}
		}
		catch (SQLException e)
		{
			LogMgr.logError("TypeMapper.createTypeMap()", "Error reading type info for target connection", e);
			this.typeInfo = new HashMap<Integer, String>();
		}
		finally
		{
			SqlUtil.closeResult(rs);
		}
	}
}

