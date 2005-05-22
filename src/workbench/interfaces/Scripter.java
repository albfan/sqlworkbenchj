/*
 * Scripter.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2005, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.interfaces;

/**
 *
 * @author  thomas
 */
public interface Scripter
{
	String getScript();
	void generateScript();
	void setProgressMonitor(ScriptGenerationMonitor monitor);
	void cancel();
	boolean isCancelled();
}
