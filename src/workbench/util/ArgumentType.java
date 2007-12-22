/*
 * ArgumentType.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2008, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.util;

/**
 * Available argument types for the ArgumentParser
 * 
 * @author support@sql-workbench.net
 */
public enum ArgumentType
{
	StringArgument, 
	BoolArgument, 
	IntegerArgument, 
	TableArgument, 
	ListArgument,
	ProfileArgument,
	Deprecated;
}
