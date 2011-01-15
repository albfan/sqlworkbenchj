/*
 * ColumnComparator.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2011, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.storage.filter;

/**
 * @author Thomas Kellerer
 */
public interface ColumnComparator
{
	/**
	 * Return a String representation to be displayed in the UI
	 */
	String getOperator();

	/**
	 * Return a human redeable (localized) name of this operator
	 */
	String getDescription();
	
	/**
	 * Evaluate this ColumnComparator.
	 *
	 * reference is the value entered by the user in the filter definition.
	 * value is the actual value against which the reference should be 
	 * compared. 
	 * Comparators with needsValue() == false should ignore the reference
	 * value
	 *
	 * @param reference The "filter" definition
	 * @param value the value to test
	 * @param ignoreCase if true, comparison should be done case-insesitive it the filter supports it
	 */
	boolean evaluate(Object reference, Object value, boolean ignoreCase);
	
	/**
	 * Check if this comparator can be applied to the given class
	 */
	boolean supportsType(Class valueClass);
	
	/**
	 * Should return true if this comparator supports the ignoreCase
	 * parameter in the {@link #evaluate(Object, Object, boolean)} method.
	 * The value returned by this method will be used when building the GUI
	 * for defining a filter.
	 */
	boolean supportsIgnoreCase();
	
	/**
	 * If this filter does not need a reference value (e.g. for IS NULL)
	 * this method should return true. In that case, the reference
	 * value passed into the {@link #evaluate(Object, Object, boolean)} method
	 * has to be ignored by the filter.
	 */
	boolean needsValue();
	
	String getValueExpression(Object value);
	
	/**
	 *	Validate the input whether this comparator
	 * can actually deal with the value
	 */
	boolean validateInput(Object input);
	
	boolean comparesEquality();
}
