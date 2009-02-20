/*
 * TypeMapper.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2009, Thomas Kellerer
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

import java.util.Map;
import workbench.log.LogMgr;
import workbench.util.SqlUtil;
import workbench.util.StringUtil;
import workbench.util.Types40;

/**
 * A class to map datatypes from one DBMS to another.
 * 
 * @author support@sql-workbench.net
 */
public class TypeMapper
{
	private WbConnection targetDb;
	private Map<Integer, String> typeInfo;
	private Map<Integer, String> userMapping;
	
	/**
	 * For testing purposes only!
	 */
	TypeMapper()
	{

	}
	/**
	 * Create a TypeMapper for the DBMS specified through the target connection.
	 * @param targetConnection
	 */
	public TypeMapper(WbConnection targetConnection)
	{
		this.targetDb = targetConnection;
		this.createTypeMap();
	}

	private String findAlternateBlobType(int type)
	{
		List<Integer> allBlobTypes = new ArrayList<Integer>(4);
		
		// BLOBs are reported as BLOB, VARBINARY (Postgres), LONGVARBNARY (HSQL) and 
		// possibly as BINARY. So we need to test each of them. The order here
		// is a personal feeling which type should be preferred over others ;)
		allBlobTypes.add(Integer.valueOf(Types.BLOB));
		allBlobTypes.add(Integer.valueOf(Types.LONGVARBINARY));
		allBlobTypes.add(Integer.valueOf(Types.VARBINARY));
		allBlobTypes.add(Integer.valueOf(Types.BINARY));
		
		for (Integer blobType : allBlobTypes)
		{
			// we are looking for an alternative to the passed type
			// so ignore that one
			if (blobType == type) continue;

			String name = typeInfo.get(blobType);
			if (name != null) return name;
		}
		return null;
	}

	private String findAlternateClobType(int type)
	{
		List<Integer> allClobTypes = new ArrayList<Integer>(4);

		// BLOBs are reported as BLOB, VARBINARY (Postgres), LONGVARBNARY (HSQL) and
		// possibly as BINARY. So we need to test each of them. The order here
		// is a personal feeling which type should be preferred over others ;)
		allClobTypes.add(Integer.valueOf(Types.CLOB));
		allClobTypes.add(Integer.valueOf(Types.LONGVARCHAR));
		allClobTypes.add(Integer.valueOf(Types40.NCLOB));
		allClobTypes.add(Integer.valueOf(Types40.LONGNVARCHAR));


		for (Integer clobType : allClobTypes)
		{
			// we are looking for an alternative to the passed type
			// so ignore that one
			if (clobType == type) continue;

			String name = typeInfo.get(clobType);
			if (name != null) return name;
		}
		return null;
	}

	protected String getUserMapping(int type, int size, int digits)
	{
		if (userMapping == null) return null;
		Integer key = Integer.valueOf(type);
		String userType = userMapping.get(key);
		if (userType == null) return null;

		userType = userType.replace("$size", Integer.toString(size));
		userType = userType.replace("$digits", Integer.toString(digits));

		return userType;
	}
	
	public String getTypeName(int type, int size, int digits)
	{

		
		String userType = getUserMapping(type, size, digits);
		if (userType != null) return userType;
		
		Integer key = Integer.valueOf(type);
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
			return SqlUtil.getTypeName(type);
		}

		return this.targetDb.getMetadata().getDataTypeResolver().getSqlTypeDisplay(name, type, size, digits, -1);
	}

	private void parseTypeMap()
	{
		String mapping = targetDb.getDbSettings().getJDBCTypeMapping();
		parseTypeMap(mapping);
	}

	/**
	 * Mad protected for testing purposes
	 * 
	 * @param mapping
	 */
	protected void parseTypeMap(String mapping)
	{
		if (StringUtil.isBlank(mapping)) return;
		userMapping = new HashMap<Integer, String>();
		List<String> types = StringUtil.stringToList(mapping, ";", true, true, false, false);
		for (String type : types)
		{
			String[] def = type.split("\\:");
			if (def != null && def.length == 2)
			{
				try
				{
					Integer typeValue = Integer.parseInt(def[0]);
					LogMgr.logDebug("TypeMapp.parseTypeMap()", "Mapping JDBC Type " + SqlUtil.getTypeName(typeValue) + " to usertype: " + def[1]);
					this.userMapping.put(typeValue, def[1]);
				}
				catch (Exception e)
				{
					LogMgr.logError("TypeMapp.parseTypeMap()", "Could not parse entry: " + type, e);
				}
			}
		}
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

				Integer key = Integer.valueOf(type);
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
		parseTypeMap();
	}
}

