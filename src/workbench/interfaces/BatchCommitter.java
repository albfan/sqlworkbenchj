/*
 * BatchCommitter.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2011, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.interfaces;

/**
 *
 * @author Thomas Kellerer
 */
public interface BatchCommitter
	extends Committer
{
	void setUseBatch(boolean useBatch);
	
	void setBatchSize(int size);
	int getBatchSize();
	
	void setCommitBatch(boolean commitBatch);
	
	
}
