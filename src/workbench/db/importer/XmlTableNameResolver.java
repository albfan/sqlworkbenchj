/*
 * XmlTableNameResolver.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2015, Thomas Kellerer
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at.
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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

	@Override
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
