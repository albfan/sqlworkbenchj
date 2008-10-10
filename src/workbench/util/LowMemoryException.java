/*
 * LowMemoryException.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2008, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.util;

import workbench.resource.ResourceMgr;

/**
 * @author support@sql-workbench.net
 */
public class LowMemoryException 
	extends java.lang.RuntimeException
{

	public LowMemoryException()
	{
		super();
	}

	@Override
	public String getMessage()
	{
		return ResourceMgr.getString("MsgLowMemoryError");
	}

}
