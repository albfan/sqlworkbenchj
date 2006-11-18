/*
 * PackageParser.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2006, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.db.oracle;

import java.io.IOException;
import workbench.sql.formatter.SQLLexer;
import workbench.sql.formatter.SQLToken;

/**
 * @author support@sql-workbench.net
 */
public class OraclePackageParser
{
	private String packageDeclaration;
	private String packageBody;
	private String packageName;
	
	public OraclePackageParser(String sql)
	{
		try
		{
			parse(sql);
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
	}
	
	public String getPackageDeclaration()
	{
		return this.packageDeclaration;
	}
	
	public String getPackageBody()
	{
		return this.packageBody;
	}

	public String getPackageName()
	{
		return this.packageName;
	}
	
	private void parse(String sql)
		throws IOException
	{
		SQLLexer lexer = new SQLLexer(sql);
		SQLToken t = lexer.getNextToken(false, false);
		
		int defBegin = -1;
		int defEnd = -1;
		int bodyBegin = -1;
		int bodyEnd = -1;
		int lastCreateStart = -1;
		
		while (t != null)
		{
			String text = t.getContents();
			
			if (isCreate(text))
			{
				lastCreateStart = t.getCharBegin();
			}
			else if (text.equals("PACKAGE"))
			{
				defBegin = lastCreateStart;
				t = lexer.getNextToken(false, false);
				if (t == null) continue;
				
				if (t.isIdentifier())
				{
					this.packageName = t.getContents();
					t = findEnd(lexer, this.packageName);
					if (t != null)
					{
						defEnd = t.getCharEnd();
					}
				}
			}
			else if (text.equals("PACKAGE BODY"))
			{
				bodyBegin = lastCreateStart;
				
				t = lexer.getNextToken(false, false);
				if (t == null) continue;
				
				String name = t.getContents();
				t = findEnd(lexer, name);
				if (t != null)
				{
					bodyEnd = t.getCharEnd();
					break;
				}
			}
			t = lexer.getNextToken(false, false);
		}
		if (defBegin > -1 && defEnd > defBegin)
		{
			this.packageDeclaration = sql.substring(defBegin, defEnd);
		}
		if (bodyBegin > -1 && bodyEnd > bodyBegin)
		{
			this.packageBody = sql.substring(bodyBegin, bodyEnd);
		}
	}
	
	private boolean isCreate(String text)
	{
		return text.equals("CREATE") || text.equals("CREATE OR REPLACE");
	}
	
	private SQLToken findEnd(SQLLexer lexer, String name)
		throws IOException
	{
		SQLToken t = lexer.getNextToken(false, false);
		boolean lastWasEnd = false;
		
		while (t != null)
		{
			if (t.getContents().equalsIgnoreCase("END"))
			{
				lastWasEnd = true;
			}
			else if (lastWasEnd && name.equalsIgnoreCase(t.getContents()))
			{
				SQLToken t2 = lexer.getNextToken(false, false);
				if (t2 != null) return t2;
				else return t;
			}
			else
			{
				lastWasEnd = false;
			}
			t = lexer.getNextToken(false, false);
		}
		return null;
	}
	
}
