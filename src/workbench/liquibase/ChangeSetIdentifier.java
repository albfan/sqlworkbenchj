/*
 * ChangeSetId.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2010, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.liquibase;

/**
 *
 * @author Thomas Kellerer
 */
public class ChangeSetIdentifier
{
	private final String author;
	private final String id;

	/**
	 * Initialize the identifier using a combined string in the format <tt>author;id</tt>
	 * If no semicolon is present, the string is assumed to be the ID and the author to be null.
	 * @param combined
	 */
	public ChangeSetIdentifier(String combined)
	{
		if (combined == null) throw new NullPointerException("Parameter must not be null");
		int pos = combined.indexOf(';');
		if (pos == -1)
		{
			id = combined.trim();
			author = null;
		}
		else
		{
			String[] elements = combined.split(";");
			if (elements.length == 1)
			{
				id = combined.trim();
				author = null;
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
		this.author = author;
		this.id = id;
	}

	public String getAuthor()
	{
		return author;
	}

	public String getId()
	{
		return id;
	}

}
