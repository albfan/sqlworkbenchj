/*
 * CommandMapper.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.sql;

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import workbench.db.DbMetadata;
import workbench.db.WbConnection;
import workbench.log.LogMgr;
import workbench.resource.Settings;
import workbench.sql.commands.AlterSessionCommand;
import workbench.sql.commands.DdlCommand;
import workbench.sql.commands.IgnoredCommand;
import workbench.sql.commands.SelectCommand;
import workbench.sql.commands.SetCommand;
import workbench.sql.commands.SingleVerbCommand;
import workbench.sql.commands.UpdatingCommand;
import workbench.sql.commands.UseCommand;
import workbench.sql.wbcommands.CommandTester;
import workbench.sql.wbcommands.WbCall;
import workbench.sql.wbcommands.WbConfirm;
import workbench.sql.wbcommands.WbConnect;
import workbench.sql.wbcommands.WbCopy;
import workbench.sql.wbcommands.WbDataDiff;
import workbench.sql.wbcommands.WbDefinePk;
import workbench.sql.wbcommands.WbDefineVar;
import workbench.sql.wbcommands.WbDescribeObject;
import workbench.sql.wbcommands.WbDisableOraOutput;
import workbench.sql.wbcommands.WbEnableOraOutput;
import workbench.sql.wbcommands.WbEndBatch;
import workbench.sql.wbcommands.WbExport;
import workbench.sql.wbcommands.WbFeedback;
import workbench.sql.wbcommands.WbFetchSize;
import workbench.sql.wbcommands.WbHelp;
import workbench.sql.wbcommands.WbHideWarnings;
import workbench.sql.wbcommands.WbImport;
import workbench.sql.wbcommands.WbInclude;
import workbench.sql.wbcommands.WbListCatalogs;
import workbench.sql.wbcommands.WbListPkDef;
import workbench.sql.wbcommands.WbListProcedures;
import workbench.sql.wbcommands.WbListTables;
import workbench.sql.wbcommands.WbListTriggers;
import workbench.sql.wbcommands.WbListVars;
import workbench.sql.wbcommands.WbLoadPkMapping;
import workbench.sql.wbcommands.WbProcSource;
import workbench.sql.wbcommands.WbRemoveVar;
import workbench.sql.wbcommands.WbSavePkMapping;
import workbench.sql.wbcommands.WbSchemaDiff;
import workbench.sql.wbcommands.WbSchemaReport;
import workbench.sql.wbcommands.WbGrepSource;
import workbench.sql.wbcommands.WbGrepData;
import workbench.sql.wbcommands.WbRunLB;
import workbench.sql.wbcommands.WbListSchemas;
import workbench.sql.wbcommands.WbMode;
import workbench.sql.wbcommands.WbSelectBlob;
import workbench.sql.wbcommands.WbStartBatch;
import workbench.sql.wbcommands.WbTriggerSource;
import workbench.sql.wbcommands.WbXslt;
import workbench.sql.wbcommands.console.WbAbout;
import workbench.util.CaseInsensitiveComparator;
import workbench.util.CollectionUtil;
import workbench.util.SqlUtil;
import workbench.util.StringUtil;

/**
 * @author Thomas Kellerer
 */
public class CommandMapper
{
	private Map<String, SqlCommand> cmdDispatch;
	private List<String> dbSpecificCommands;
	private Set<String> passThrough = CollectionUtil.caseInsensitiveSet();
	private boolean supportsSelectInto;
	private DbMetadata metaData;
	private boolean allowAbbreviated;

	public CommandMapper()
	{
		cmdDispatch = new TreeMap<String, SqlCommand>(CaseInsensitiveComparator.INSTANCE);
		cmdDispatch.put("*", new SqlCommand());

		// Workbench specific commands
		addCommand(new WbListTables());
		addCommand(new WbListProcedures());
		addCommand(new WbDefineVar());
		addCommand(new WbEnableOraOutput());
		addCommand(new WbDisableOraOutput());
		addCommand(new WbStartBatch());
		addCommand(new WbEndBatch());
		addCommand(new WbXslt());
		addCommand(new WbRemoveVar());
		addCommand(new WbListVars());
		addCommand(new WbExport());
		addCommand(new WbImport());
		addCommand(new WbCopy());
		addCommand(new WbSchemaReport());
		addCommand(new WbSchemaDiff());
		addCommand(new WbDataDiff());
		addCommand(new WbFeedback());
		addCommand(new WbDefinePk());
		addCommand(new WbListPkDef());
		addCommand(new WbLoadPkMapping());
		addCommand(new WbSavePkMapping());
		addCommand(new WbConfirm());
		addCommand(new WbCall());
		addCommand(new WbConnect());
		addCommand(new WbInclude());
		addCommand(new WbListCatalogs());
		addCommand(new WbListSchemas());
		addCommand(new WbHelp());
		addCommand(new WbSelectBlob());
		addCommand(new WbHideWarnings());
		addCommand(new WbProcSource());
		addCommand(new WbListTriggers());
		addCommand(new WbTriggerSource());
		addCommand(new WbDescribeObject());
		addCommand(new WbGrepSource());
		addCommand(new WbGrepData());
		addCommand(new WbMode());
		addCommand(new WbFetchSize());
		addCommand(new WbAbout());
		addCommand(new WbRunLB());

		// Wrappers for standard SQL statements
		addCommand(SingleVerbCommand.COMMIT);
		addCommand(SingleVerbCommand.ROLLBACK);

		addCommand(UpdatingCommand.DELETE);
		addCommand(UpdatingCommand.INSERT);
		addCommand(UpdatingCommand.UPDATE);
		addCommand(UpdatingCommand.TRUNCATE);

		addCommand(new SetCommand());
		addCommand(new SelectCommand());

		for (DdlCommand cmd : DdlCommand.DDL_COMMANDS)
		{
			addCommand(cmd);
		}
		this.cmdDispatch.put("CREATE OR REPLACE", DdlCommand.CREATE);

		this.dbSpecificCommands = new LinkedList<String>();
		this.allowAbbreviated = Settings.getInstance().getBoolProperty("workbench.sql.allow.abbreviation", false);
	}

	public Collection<String> getAllWbCommands()
	{
		Collection<SqlCommand> commands = cmdDispatch.values();
		TreeSet<String> result = new TreeSet<String>();
		CommandTester tester = new CommandTester();
		for (SqlCommand cmd : commands)
		{
			String verb = cmd.getVerb();
			if (tester.isWbCommand(verb))
			{
				result.add(tester.formatVerb(verb));
			}
		}
		return result;
	}

	/**
	 * Add a new command definition during runtime.
	 */
	public void addCommand(SqlCommand command)
	{
		cmdDispatch.put(command.getVerb(), command);
		String longVerb = command.getAlternateVerb();
		if (longVerb != null)
		{
			cmdDispatch.put(longVerb, command);
		}
	}

	/**
	 * Initialize the CommandMapper with a database connection.
	 * This will add DBMS specific commands to the internal dispatch.
	 *
	 * This method can be called multiple times.
	 */
	public void setConnection(WbConnection aConn)
	{
		this.cmdDispatch.keySet().removeAll(dbSpecificCommands);
		this.dbSpecificCommands.clear();
		this.supportsSelectInto = false;

		if (aConn == null) return;

		this.metaData = aConn.getMetadata();

		if (metaData == null)
		{
			LogMgr.logError("CommandMapper.setConnection()","Received connection without metaData!", null);
			return;
		}

		if (metaData.isOracle())
		{
			SqlCommand wbcall = this.cmdDispatch.get(WbCall.VERB);

			this.cmdDispatch.put(WbCall.EXEC_VERB_LONG, wbcall);
			this.cmdDispatch.put(WbCall.EXEC_VERB_SHORT, wbcall);

			AlterSessionCommand alter = new AlterSessionCommand();
			this.cmdDispatch.put(alter.getVerb(), alter);

			WbFeedback echo = new WbFeedback("ECHO");
			this.cmdDispatch.put(echo.getVerb(), echo);

			this.dbSpecificCommands.add(alter.getVerb());
			this.dbSpecificCommands.add(WbCall.EXEC_VERB_LONG);
			this.dbSpecificCommands.add(WbCall.EXEC_VERB_SHORT);
			this.dbSpecificCommands.add(echo.getVerb());
		}
		else if (metaData.isSqlServer() || metaData.isMySql() || metaData.supportsCatalogs())
		{
			UseCommand cmd = new UseCommand();
			this.cmdDispatch.put(cmd.getVerb(), cmd);
			this.dbSpecificCommands.add(cmd.getVerb());
		}
		else if (metaData.isFirebird())
		{
			this.cmdDispatch.put(DdlCommand.RECREATE.getVerb(), DdlCommand.RECREATE);
			this.dbSpecificCommands.add(DdlCommand.RECREATE.getVerb());
		}

		if (metaData.getDbSettings().useWbProcedureCall())
		{
			SqlCommand wbcall = this.cmdDispatch.get(WbCall.VERB);
			this.cmdDispatch.put("CALL", wbcall);
			this.dbSpecificCommands.add("CALL");
		}

		List<String> verbs = Settings.getInstance().getListProperty("workbench.db.ignore." + metaData.getDbId(), false, "");
		for (String verb : verbs)
		{
			if (verb == null) continue;
			IgnoredCommand cmd = new IgnoredCommand(verb);
			this.cmdDispatch.put(verb, cmd);
			this.dbSpecificCommands.add(verb);
		}

		List<String> passVerbs = Settings.getInstance().getListProperty("workbench.db." + metaData.getDbId() + ".passthrough", false, "");
		passThrough.clear();
		if (passVerbs != null)
		{
			for (String v : passVerbs)
			{
				passThrough.add(v);
			}

		}

		// this is stored in an instance variable for performance
		// reasons, so we can skip the call to isSelectIntoNewTable() in
		// getCommandToUse()
		// For a single call this doesn't matter, but when executing
		// huge scripts the repeated call to getCommandToUse should
		// be as quick as possible
		this.supportsSelectInto = metaData.supportsSelectIntoNewTable();
	}

	/**
	 * Check for a SELECT ... INTO syntax for Informix which actually
	 * creates a table. In that case we will simply pretend it's a
	 * CREATE statement.
	 * In all other casese, the approriate SqlCommand from commanDispatch will be used
	 * This is made public in order to be accessible from a JUnit test
	 *
	 * @param sql the statement to be executed
	 * @return the instance of SqlCommand to be used to run the sql, or null if the
	 * given sql is empty or contains comments only
	 */
	public SqlCommand getCommandToUse(String sql)
	{
		SqlCommand cmd = null;
		String verb = SqlUtil.getSqlVerb(sql);
		if (StringUtil.isEmptyString(verb)) return null;

		if (this.supportsSelectInto && "SELECT".equals(verb) && this.metaData != null && this.metaData.isSelectIntoNewTable(sql))
		{
			LogMgr.logDebug("CommandMapper.getCommandToUse()", "Found 'SELECT ... INTO new_table'");
			// use the generic SqlCommand implementation for this and not the SelectCommand
			cmd = this.cmdDispatch.get("*");
		}
		// checking for the collection size before checking for the presence
		// is a bit faster because of the hashing that is necessary to look up
		// the entry. Again this doesn't matter for a single command, but when
		// running a large script this does make a difference
		else if (passThrough.size() > 0 && passThrough.contains(verb))
		{
			cmd = this.cmdDispatch.get("*");
		}
		else
		{
			cmd = this.cmdDispatch.get(verb);
		}

		if (cmd == null && allowAbbreviated)
		{
			CommandTester tester = new CommandTester();
			Set<String> verbs = cmdDispatch.keySet();
			int found = 0;
			String lastVerb = null;
			String lverb = verb.toLowerCase();
			for (String toTest : verbs)
			{
				if (tester.isWbCommand(toTest))
				{
					if (toTest.toLowerCase().startsWith(lverb))
					{
						lastVerb = toTest;
						found ++;
					}
				}
			}
			if (found == 1)
			{
				LogMgr.logDebug("CommandMapper.getCommandToUse()", "Found workbench command " + lastVerb + " for abbreviation " + verb);
				cmd = cmdDispatch.get(lastVerb);
			}
		}

		if (cmd == null)
		{
			cmd = this.cmdDispatch.get("*");
		}
		return cmd;
	}

}
