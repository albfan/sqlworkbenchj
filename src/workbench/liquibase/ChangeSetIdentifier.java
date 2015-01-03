/*
 * ChangeSetIdentifier.java
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
package workbench.liquibase;

import workbench.util.StringUtil;

/**
 *
 * @author Thomas Kellerer
 */
public class ChangeSetIdentifier
{
	private final String author;
	private final String id;
	private String comment;

	/**
	 * Initialize the identifier using a combined string in the format <tt>author::id</tt>.
	 *
	 * If no double colon is present, the string is assumed to be the ID and the author to be null.
	 * @param combined
	 */
	public ChangeSetIdentifier(String combined)
	{
		if (combined == null) throw new NullPointerException("Parameter must not be null");
		int pos = combined.indexOf("::");
		if (pos == -1)
		{
			id = combined.trim();
			author = "*";
		}
		else
		{
			String[] elements = combined.split("::");
			if (elements.length == 1)
			{
				id = combined.trim();
				author = "*";
			}
			else
			{
				author = elements[0].trim();
				id = elements[1].trim();
			}
		}
	}

	public ChangeSetIdentifier(String author, String id)
	{
		this.author = author == null ? "*" : author.trim();
		this.id = id == null ? "*" : id.trim();
	}

	public String getComment()
	{
		return comment;
	}
	
	public void setComment(String comment)
	{
		this.comment = comment;
	}

	public String getAuthor()
	{
		return author;
	}

	public String getId()
	{
		return id;
	}

	private boolean isWildcard(String value)
	{
		return value == null || "*".equals(value);
	}

	public boolean isEqualTo(ChangeSetIdentifier other)
	{
		if (other == null) return false;
		boolean authorsEqual = isWildcard(this.author) || isWildcard(other.author) || StringUtil.equalString(this.author, other.author);
		boolean idsEqual = isWildcard(this.id) || isWildcard(other.id) || StringUtil.equalString(this.id, other.id);
		return authorsEqual && idsEqual;
	}

	@Override
	public String toString()
	{
		StringBuilder result = new StringBuilder(20);
		result.append(author == null ? "*" : author);
		result.append("::");
		result.append(id == null ? "*" : id);
		return result.toString();
	}
}
