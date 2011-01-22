/*
 * XmlTableNameResolver.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2011, Thomas Kellerer
 * No part of this code may be reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.db.importer;

import workbench.log.LogMgr;
import workbench.util.WbFile;

/**
 *
 * @author Thomas Kellerer
 */
public class XmlTableNameResolver
	implements TablenameResolver
{
	private String encoding;

	public XmlTableNameResolver(String enc)
	{
		this.encoding = enc;
	}

	public String getTableName(WbFile f)
	{
		String tablename = f.getFileName();
		ImportFileHandler handler = new ImportFileHandler();
		try
		{
			handler.setMainFile(f, this.encoding);
			XmlTableDefinitionParser parser = new XmlTableDefinitionParser(handler);
			tablename = parser.getTableName();
		}
		catch (Exception ex)
		{
			LogMgr.logError("XmlTableNameResolver.getTableName()", "Error retrieving table name", ex);
		}
		finally
		{
			handler.done();
		}
		return tablename;
	}

}
