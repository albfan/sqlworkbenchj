/*
 * WbData
 * 
 *  This file is part of SQL Workbench/J, http://www.sql-workbench.net
 * 
 *  Copyright 2002-2009, Thomas Kellerer
 *  No part of this code maybe reused without the permission of the author
 * 
 *  To contact the author please send an email to: support@sql-workbench.net
 */
package workbench.db.derby;

import java.io.Serializable;

/**
 *
 * @author Thomas Kellerer
 */
public class WbData
	implements Serializable
{

	public WbData()
	{
	}

	public String toString()
	{
		return "MyDATA";
	}
}
