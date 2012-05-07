/*
 * OracleMergeGenerator.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2012, Thomas Kellerer
 * No part of this code may be reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 */
package workbench.storage;

import java.util.List;

/**
 *
 * @author Thomas Kellerer
 */
public class OracleMergeGenerator
	implements MergeGenerator
{
	@Override
	public List<String> generateMerge(DataStore data, int[] rows, int chunkSize)
	{
		throw new UnsupportedOperationException("Not supported yet.");
	}

}
