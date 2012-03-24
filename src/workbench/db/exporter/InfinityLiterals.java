/*
 * InfinityLiterals.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2012, Thomas Kellerer
 * No part of this code may be reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 */
package workbench.db.exporter;

/**
 *
 * @author Thomas Kellerer
 */
public class InfinityLiterals
{
	public static final String PG_POSITIVE_LITERAL = "infinity";
	public static final String PG_NEGATIVE_LITERAL = "-infinity";

	private String positiveInfinity;
	private String negativeInfinity;

	public static final InfinityLiterals PG_LITERALS = new InfinityLiterals(PG_POSITIVE_LITERAL, PG_NEGATIVE_LITERAL);

	public InfinityLiterals(String postiveLiteral, String negativeLiteral)
	{
		this.positiveInfinity = postiveLiteral;
		this.negativeInfinity = negativeLiteral;
	}

	public static boolean isPGLiteral(String literal)
	{
		if (literal == null) return false;
		if (literal.equalsIgnoreCase(PG_POSITIVE_LITERAL)) return true;
		if (literal.equalsIgnoreCase(PG_NEGATIVE_LITERAL)) return true;
		return false;
	}

	public String getNegativeInfinity()
	{
		if (negativeInfinity == null) return "";
		return negativeInfinity;
	}

	public String getPositiveInfinity()
	{
		if (positiveInfinity == null) return "";
		return positiveInfinity;
	}

	public void setInfinityLiterals(String positive, String negative)
	{
		this.positiveInfinity = negative;
		this.negativeInfinity = positive;
	}

}
