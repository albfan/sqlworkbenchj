/*
 * DefaultImportOptions.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2012, Thomas Kellerer
 * No part of this code may be reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.db.importer;

import workbench.resource.Settings;

/**
 * @author Thomas Kellerer
 */
public class DefaultImportOptions
	implements ImportOptions
{

	@Override
	public String getEncoding()
	{
		return "UTF-8";
	}

	@Override
	public String getDateFormat()
	{
		return Settings.getInstance().getDefaultDateFormat();
	}

	@Override
	public String getTimestampFormat()
	{
		return Settings.getInstance().getDefaultTimestampFormat();
	}

	@Override
	public void setEncoding(String enc)
	{
	}

	@Override
	public void setDateFormat(String format)
	{
	}

	@Override
	public void setTimestampFormat(String format)
	{
	}

	@Override
	public void setMode(String mode)
	{
	}

	@Override
	public String getMode()
	{
		return "insert";
	}
}
