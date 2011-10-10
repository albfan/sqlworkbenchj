/*
 * TransactionChecker.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2012, Thomas Kellerer
 * No part of this code may be reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 */
package workbench.db;

import java.util.List;
import workbench.resource.Settings;
import workbench.util.StringUtil;

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
	 * @see ConnectionProfile#getDetectOpenTransaction()
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

			if (sql != null && con.getProfile().getDetectOpenTransaction())
			{
				return new DefaultTransactionChecker();
			}
			return NO_CHECK;
		}

		/**
		 * Checks if the given DriverClass supports detecting uncommitted changes.
		 *
		 * @param driverClass the driver to check
		 * @return true if detecting uncommitted changes is supported.
		 */
		public static boolean supportsTransactionCheck(String driverClass)
		{
			if (StringUtil.isBlank(driverClass)) return false;
			List<String> drivers = Settings.getInstance().getListProperty("workbench.db.drivers.opentransaction.check", false);
			return drivers.contains(driverClass);
		}
	}
}
