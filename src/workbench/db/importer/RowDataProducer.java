/*
 * RowDataProducer.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2004, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: info@sql-workbench.net
 *
 */
package workbench.db.importer;


/**
 *
 * @author  info@sql-workbench.net
 */
public interface RowDataProducer
{
	void setReceiver(RowDataReceiver receiver);
	void start() throws Exception;
	void cancel();
	String getMessages();
	void setAbortOnError(boolean flag);
}