/*
 * DefaultTextImportOptions.java
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

import workbench.resource.Settings;
import workbench.util.QuoteEscapeType;

/**
 * @author Thomas Kellerer
 */
public class DefaultTextImportOptions
	implements TextImportOptions
{

	private String delimiter;
	private String quoteChar;
	private boolean containsHeader = true;

	public DefaultTextImportOptions(String delim, String quote)
	{
		this.delimiter = delim;
		this.quoteChar = quote;
	}

	@Override
	public String getTextDelimiter()
	{
		return delimiter;
	}

	@Override
	public boolean getContainsHeader()
	{
		return containsHeader;
	}

	@Override
	public boolean getQuoteAlways()
	{
		return false;
	}

	@Override
	public String getTextQuoteChar()
	{
		return quoteChar;
	}

	@Override
	public QuoteEscapeType getQuoteEscaping()
	{
		return QuoteEscapeType.none;
	}

	@Override
	public boolean getDecode()
	{
		return false;
	}

	@Override
	public String getDecimalChar()
	{
		return Settings.getInstance().getDecimalSymbol();
	}

	@Override
	public void setTextDelimiter(String delim)
	{
	}

	@Override
	public void setContainsHeader(boolean flag)
	{
		containsHeader = flag;
	}

	@Override
	public void setTextQuoteChar(String quote)
	{
	}

	@Override
	public void setDecode(boolean flag)
	{
	}

	@Override
	public void setDecimalChar(String s)
	{
	}

	@Override
	public String getNullString()
	{
		return null;
	}

	@Override
	public void setNullString(String nullString)
	{
	}

}
