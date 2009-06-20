/*
 * PGTypeLookup.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2009, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.db.postgres;

import java.util.HashMap;
import java.util.Map;

/**
 *
 * @author support@sql-workbench.net
 */
public class PGTypeLookup
{
	private Map<Integer, PGType> oidToTypeMap;
	private Map<String, PGType> rawTypeMap = new HashMap<String, PGType>();
	
	public PGTypeLookup(Map<Integer, PGType> oidMap)
	{
		oidToTypeMap = oidMap;
	}

	public PGType getTypeFromOID(int oid)
	{
		return oidToTypeMap.get(oid);
	}

	public PGType getTypeEntry(String rawType)
	{
		PGType result = rawTypeMap.get(rawType);
		if (result == null)
		{
			for (PGType typ : oidToTypeMap.values())
			{
				if (typ.rawType.equals(rawType))
				{
					rawTypeMap.put(rawType, typ);
					result = typ;
					break;
				}
			}
		}
		return result;
	}
}
