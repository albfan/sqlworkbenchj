/*
 * WbNullCipher.java
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
 * @author  Thomas Kellerer
 */
public class WbNullCipher
	implements WbCipher
{
	public String decryptString(String aValue) { return aValue; }
	public String encryptString(String aValue) { return aValue; }
}
