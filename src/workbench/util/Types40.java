/*
 * Jdbc40Types.java
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
 * java.sql.Types for JDBC 4.0 copied from the Java 6 source code, so I don't have
 * to hardcode the actual values in several places, but still can check for
 * the new datatypes without having to compile agains a Java 6 JDK
 *
 * @author support@sql-workbench.net
 */
public class Types40
{
    public final static int ROWID = -8;
    public static final int NCHAR = -15;
    public static final int NVARCHAR = -9;
    public static final int LONGNVARCHAR = -16;
    public static final int NCLOB = 2011;
    public static final int SQLXML = 2009;
}
