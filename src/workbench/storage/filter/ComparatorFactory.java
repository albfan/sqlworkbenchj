/*
 * FilterComparatorFactory.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2005, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.storage.filter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author support@sql-workbench.net
 */
public class ComparatorFactory
{
	ColumnComparator greaterOrEqual;
	ColumnComparator lessOrEqual;
	ColumnComparator greaterThan;
	ColumnComparator lessThan;
	ColumnComparator equalString;
	ColumnComparator equalNumber;
	ColumnComparator regex;
	ColumnComparator startsWith;
	ColumnComparator contains;
	
	public ComparatorFactory()
	{
	}
	
	public ColumnComparator getStringEqualsComparator()
	{
		if (this.equalString == null) equalString = new StringEqualsComparator();
		return equalString;
	}

	public ColumnComparator getNumberEqualsComparator()
	{
		if (this.equalNumber == null) equalNumber = new NumberEqualsComparator();
		return equalNumber;
	}
	
	public ColumnComparator getContainsComparator()
	{
		if (this.contains == null) contains = new ContainsComparator();
		return contains;
	}
	
	public ColumnComparator getStartsWithComparator()
	{
		if (this.startsWith == null) startsWith = new StartsWithComparator();
		return startsWith;
	}
	
	public ColumnComparator getLessThanComparator()
	{
		if (this.lessThan == null) lessThan = new LessThanComparator();
		return lessThan;
	}
	
	public ColumnComparator getGreaterThanComparator()
	{
		if (this.greaterThan == null) greaterThan = new GreaterThanComparator();
		return greaterThan;
	}
	
	public ColumnComparator getGreaterOrEqualComparator()
	{
		if (this.greaterOrEqual == null) greaterOrEqual = new GreaterOrEqualComparator();
		return greaterOrEqual;
	}
	
	public ColumnComparator getLessOrEqualComparator()
	{
		if (this.lessOrEqual == null) lessOrEqual = new LessOrEqualComparator();
		return lessOrEqual;
	}
	
	public ColumnComparator getRegExComparator()
	{
		if (regex == null) regex = new RegExComparator();
		return regex;
	}
	
	public ColumnComparator[] getAvailableComparators()
	{
		ColumnComparator[] result = new ColumnComparator[]
		{
			getStringEqualsComparator(), 
			getNumberEqualsComparator(),
			getLessThanComparator(),
			getLessOrEqualComparator(),
			getGreaterThanComparator(),
			getGreaterOrEqualComparator(),
			getStartsWithComparator(),
			getContainsComparator(),
			getRegExComparator()
		};
		return result;
	}
	
}

