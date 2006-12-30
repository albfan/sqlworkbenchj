/*
 * OutputFactory.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2007, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.util;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.io.Writer;

/**
 * 
 * @author support@sql-workbench.net
 */
public interface OutputFactory
{
	OutputStream createOutputStream(File output) throws IOException;
	Writer createWriter(File output, String encoding) throws IOException;
	void done() throws IOException;
}
