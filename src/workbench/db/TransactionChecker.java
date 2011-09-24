/*
 * TransactionChecker.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2011, Thomas Kellerer
 * No part of this code may be reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 */
package workbench.db;

/**
 *
 * @author Thomas Kellerer
 */
public interface TransactionChecker
{
	boolean hasUncommittedChanges(WbConnection con);

	/**
	 * Dummy implementation that does no checking at all.
	 */
	public static final TransactionChecker NO_CHECK = new TransactionChecker()
	{
		/**
		 * Always returns false
		 */
		@Override
		public boolean hasUncommittedChanges(WbConnection con)
		{
			return false;
		}
	};

	/**
	 * Factory for creating a DBMS specific TransactionChecker instance.
	 *
	 * Currently only one implementation is available which takes a SQL query defined
	 * in workbench.settings to count the number of open transactions.
	 *
	 * @see DbSettings#checkOpenTransactionsQuery()
	 * @see DefaultTransactionChecker
	 */
	public static class Factory
	{
		/**
		 * Returns a TransactionChecker for the given connection.
		 *
		 * @param con the connection
		 * @return the TransactionChecker, never null
		 */
		public static TransactionChecker createChecker(WbConnection con)
		{
			if (con == null) return NO_CHECK;

			String sql = con.getDbSettings().checkOpenTransactionsQuery();

			if (sql != null && con.getDbSettings().checkOpenTransactions())
			{
				return new DefaultTransactionChecker();
			}
			return NO_CHECK;
		}
	}
}
