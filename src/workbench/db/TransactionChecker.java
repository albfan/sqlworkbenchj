/*
 * TransactionChecker.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2013, Thomas Kellerer
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at.
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.db;

import java.util.Set;
import java.util.TreeSet;

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
	TransactionChecker NO_CHECK = new TransactionChecker()
	{
		/**
		 * Always returns false.
		 * @return false
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
	class Factory
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
			DbSettings db = con.getDbSettings();
			if (db == null) return NO_CHECK;

			String sql = db.checkOpenTransactionsQuery();

			if (sql != null && con.getProfile().getDetectOpenTransaction())
			{
				return new DefaultTransactionChecker(sql);
			}
			return NO_CHECK;
		}

		/**
		 * Checks if the given DriverClass supports detecting uncommitted changes.
		 *
		 * This is used to hide/show the profile checkbox in the connection dialog.
		 * Before a connection is made no DbSettings are available, therefor
		 * the presence of the configured SQL statement cannot be checked.
		 *
		 * @param driverClass the driver to check
		 * @return true if detecting uncommitted changes is supported.
		 */
		public static boolean supportsTransactionCheck(String driverClass)
		{
			if (StringUtil.isBlank(driverClass)) return false;
			Set<String> drivers = new TreeSet<String>();
			drivers.addAll(Settings.getInstance().getListProperty("workbench.db.drivers.opentransaction.check.builtin", false));
			drivers.addAll(Settings.getInstance().getListProperty("workbench.db.drivers.opentransaction.check", false));
			return drivers.contains(driverClass);
		}
	}
}
