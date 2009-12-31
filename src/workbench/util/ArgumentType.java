/*
 * ArgumentType.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2010, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.util;

/**
 * Available argument types for the ArgumentParser
 * 
 * @author Thomas Kellerer
 */
public enum ArgumentType
{
	StringArgument,
	/**
	 * Defines a boolean parameter. If this is set
	 * the automcompletion for this parameter will
	 * show true and false as possible values
	 */
	BoolArgument,

	IntegerArgument,
	/**
	 * A parameter that selects tables. If this is set
	 * the autocompletion for this parameter will
	 * show a table list
	 */
	TableArgument,

	/**
	 * A parameter that accepts a fixed set of values. The
	 * values can be registered using ArgumentParser.addArgument(String, List)
	 * @see ArgumentParser#addArgument(java.lang.String, java.util.List) 
	 */
	ListArgument,

	/**
	 * A parameter that selects a connection profile. If this is defined
	 * the autocompletion for this parameter will show all currently
	 * defined connection profiles
	 */
	ProfileArgument,
	Deprecated;
}
