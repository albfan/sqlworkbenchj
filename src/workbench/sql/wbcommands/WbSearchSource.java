/*
 * WbSearchSource
 *
 *  This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 *  Copyright 2002-2009, Thomas Kellerer
 *  No part of this code maybe reused without the permission of the author
 *
 *  To contact the author please send an email to: support@sql-workbench.net
 */
package workbench.sql.wbcommands;

import java.sql.SQLException;
import java.sql.Types;
import java.util.List;
import workbench.db.DbObject;
import workbench.db.search.ObjectSourceSearcher;
import workbench.resource.ResourceMgr;
import workbench.sql.SqlCommand;
import workbench.sql.StatementRunnerResult;
import workbench.storage.DataStore;
import workbench.util.ArgumentParser;
import workbench.util.ArgumentType;
import workbench.util.CollectionUtil;

/**
 *
 * @author Thomas Kellerer
 */
public class WbSearchSource
	extends SqlCommand
{
	public static final String VERB = "WBSEARCHSOURCE";
	public static final String PARAM_TYPES = "types";
	public static final String PARAM_SCHEMAS = "schemas";
	public static final String PARAM_NAMES = "objects";

	public static final String PARAM_SEARCH_EXP = "searchValues";
	public static final String PARAM_MATCHALL = "matchAll";
	public static final String PARAM_CASE_SENSITIVE = "caseSensitive";

	private ObjectSourceSearcher searcher;
	
	public WbSearchSource()
	{
		super();
		this.isUpdatingCommand = false;

		cmdLine = new ArgumentParser();
		cmdLine.addArgument(PARAM_TYPES);
		cmdLine.addArgument(PARAM_SCHEMAS);
		cmdLine.addArgument(PARAM_NAMES);
		cmdLine.addArgument(PARAM_SEARCH_EXP);
		cmdLine.addArgument(PARAM_MATCHALL, ArgumentType.BoolArgument);
		cmdLine.addArgument(PARAM_CASE_SENSITIVE, ArgumentType.BoolArgument);
	}

	@Override
	public String getVerb()
	{
		return VERB;
	}

	@Override
	public StatementRunnerResult execute(String sql)
		throws SQLException
	{
		StatementRunnerResult result = new StatementRunnerResult();
		String args = getCommandLine(sql);
		cmdLine.parse(args);

		if (cmdLine.hasUnknownArguments())
		{
			setUnknownMessage(result, cmdLine, ResourceMgr.getString("ErrSrcSearchWrongParameters"));
			return result;
		}
		
		List<String> values = cmdLine.getListValue(PARAM_SEARCH_EXP);
		if (CollectionUtil.isEmpty(values))
		{
			result.addMessage(ResourceMgr.getString("ErrScrSearchValueReq"));
			result.addMessage(ResourceMgr.getString("ErrSrcSearchWrongParameters"));
			result.setFailure();
			return result;
		}
		
		boolean matchAll = cmdLine.getBoolean(PARAM_MATCHALL, false);
		boolean caseSensitive = cmdLine.getBoolean(PARAM_CASE_SENSITIVE, false);
		
		List<String> schemas = cmdLine.getListValue(PARAM_SCHEMAS);
		for (int i=0; i < schemas.size(); i++)
		{
			String s = currentConnection.getMetadata().adjustSchemaNameCase(schemas.get(i));
			schemas.set(i, s);
		}
		List<String> types = cmdLine.getListValue(PARAM_TYPES);

		List<String> names = cmdLine.getListValue(PARAM_NAMES);
		for (int i=0; i < names.size(); i++)
		{
			String n = currentConnection.getMetadata().adjustObjectnameCase(names.get(i));
			names.set(i, n);
		}

		searcher = new ObjectSourceSearcher(this.currentConnection);
		searcher.setRowMonitor(this.rowMonitor);
		searcher.setSchemasToSearch(schemas);
		searcher.setTypesToSearch(types);
		searcher.setNamesToSearch(names);

		List<DbObject> found = searcher.searchObjects(values, matchAll, caseSensitive);
		String[] cols = new String[] { "NAME", "TYPE", "SOURCE" };
		int[] colTypes = new int[] { Types.VARCHAR, Types.VARCHAR, Types.CLOB };
		int[] colSizes = new int[] { 30, 30, 50 };
		DataStore ds = new DataStore(cols, colTypes, colSizes);
		for (DbObject object : found)
		{
			int row = ds.addRow();
			String name = null;
			if (schemas.size() > 0)
			{
				name = object.getObjectExpression(currentConnection);
			}
			else
			{
				name = object.getObjectName();
			}

			ds.setValue(row, 0, name);
			ds.setValue(row, 1, object.getObjectType());
			ds.setValue(row, 2, object.getSource(currentConnection));
		}
		ds.resetStatus();
		result.addDataStore(ds);
		result.setSuccess();
		
		return result;
	}

	@Override
	public void cancel()
		throws SQLException
	{
		super.cancel();
		if (searcher != null)
		{
			searcher.cancelSearch();
		}
	}

	@Override
	public void done()
	{
		super.done();
		searcher = null;
	}

}
