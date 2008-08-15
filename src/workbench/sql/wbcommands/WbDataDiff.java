/*
 * WbDataDiff.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2008, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.sql.wbcommands;

import java.io.Writer;
import java.sql.SQLException;
import java.util.List;
import workbench.db.TableIdentifier;
import workbench.db.WbConnection;
import workbench.db.compare.TableDataDiff;
import workbench.db.compare.TableDeleteSync;
import workbench.db.exporter.BlobMode;
import workbench.db.importer.TableDependencySorter;
import workbench.log.LogMgr;
import workbench.resource.ResourceMgr;
import workbench.resource.Settings;
import workbench.sql.SqlCommand;
import workbench.sql.StatementRunnerResult;
import workbench.storage.RowActionMonitor;
import workbench.storage.SqlLiteralFormatter;
import workbench.util.ArgumentParser;
import workbench.util.ArgumentType;
import workbench.util.EncodingUtil;
import workbench.util.ExceptionUtil;
import workbench.util.FileUtil;
import workbench.util.StringUtil;
import workbench.util.WbFile;

/**
 * Compare the data of one or more tables and generate SQL scripts to migrate
 * the target data to match the reference data.
 * 
 * This is esentiall the SQL "front end" for a TableDataDiff 
 * 
 * @see workbench.db.compare.TableDataDiff
 * @see workbench.db.compare.TableDeleteSync
 * 
 * @author support@sql-workbench.net
 */
public class WbDataDiff 
	extends SqlCommand
{
	public static final String VERB = "WBDATADIFF";
	
	public static final String PARAM_INCLUDE_DELETE = "includeDelete";
	public static final String PARAM_IGNORE_COLS = "ignoreColumns";

	private WbFile outputDir;
	private WbFile mainScript;
	private TableDataDiff dataDiff;
	private TableDeleteSync deleteSync;
	
	public WbDataDiff()
	{
		cmdLine = new ArgumentParser();
		cmdLine.addArgument(PARAM_INCLUDE_DELETE, ArgumentType.BoolArgument);
		cmdLine.addArgument(WbExport.ARG_CREATE_OUTPUTDIR);
		cmdLine.addArgument(PARAM_IGNORE_COLS);
		cmdLine.addArgument(WbExport.ARG_BLOB_TYPE, BlobMode.getTypes());
		
		CommonArgs.addCheckDepsParameter(cmdLine);
		CommonArgs.addSqlDateLiteralParameter(cmdLine);
		CommonArgs.addProgressParameter(cmdLine);
		
		// Add common diff parameters
		new CommonDiffParameters(this.cmdLine);
	}

	public String getVerb() { return VERB; }
	protected boolean isConnectionRequired() { return false; }

	private String getWrongArgumentsMessage()
	{
		String msg = ResourceMgr.getString("ErrDataDiffWrongParms");
		msg = msg.replace("%date_literal_default%", Settings.getInstance().getDefaultDiffDateLiteralType());
		msg = msg.replace("%encoding_default%", Settings.getInstance().getDefaultEncoding());
		return msg;
	}

	@Override
	public void cancel()
		throws SQLException
	{
		super.cancel();
		if (this.dataDiff != null) this.dataDiff.cancel();
		if (this.deleteSync != null) this.deleteSync.cancel();
	}

	@Override
	public StatementRunnerResult execute(String sql)
		throws SQLException, Exception
	{
		StatementRunnerResult result = new StatementRunnerResult();
		CommonDiffParameters params = new CommonDiffParameters(this.cmdLine);
		this.cmdLine.parse(getCommandLine(sql));

		if (cmdLine.getArgumentCount() == 0)
		{
			result.addMessage(getWrongArgumentsMessage());
			result.setFailure();
			return result;
		}

		if (cmdLine.hasUnknownArguments())
		{
			setUnknownMessage(result, cmdLine, getWrongArgumentsMessage());
			return result;
		}
		
		mainScript = evaluateFileArgument(cmdLine.getValue(CommonDiffParameters.PARAM_FILENAME));
		if (mainScript == null)
		{
			result.setFailure();
			result.addMessage(ResourceMgr.getString("ErrDataDiffNoFile"));
			result.addMessage(getWrongArgumentsMessage());
			return result;
		}
		
		outputDir = new WbFile(mainScript.getParentFile());
		String encoding = cmdLine.getValue(CommonDiffParameters.PARAM_ENCODING);
		if (encoding == null) 
		{
			encoding = Settings.getInstance().getDefaultEncoding();
		}
		
		boolean createDir = cmdLine.getBoolean(WbExport.ARG_CREATE_OUTPUTDIR, false);
		String literalType = cmdLine.getValue(CommonArgs.ARG_DATE_LITERAL_TYPE);
		if (literalType == null) literalType = SqlLiteralFormatter.JDBC_DATE_LITERAL_TYPE;
		
		if (createDir && !outputDir.exists())
		{
			boolean created = outputDir.mkdirs();
			if (created)
			{
				result.addMessage(ResourceMgr.getFormattedString("MsgDirCreated", outputDir.getFullPath()));
			}
			else
			{
				result.addMessage(ResourceMgr.getFormattedString("ErrCreateDir", outputDir.getFullPath()));
				result.setFailure();
				LogMgr.logError("WbDataDiff.execute()", "Could not create output directory!", null);
				return result;
			}
		}
		
		if (!outputDir.exists())
		{
			result.addMessage(ResourceMgr.getFormattedString("ErrOutputDirNotFound", outputDir.getFullPath()));
			result.setFailure();
			return result;
		}
		
		if (this.rowMonitor != null) this.rowMonitor.setMonitorType(RowActionMonitor.MONITOR_PLAIN);
		params.setMonitor(rowMonitor);
		
		WbConnection targetCon = params.getTargetConnection(currentConnection, result);
		if (!result.isSuccess()) return result;
		WbConnection sourceCon = params.getSourceConnection(currentConnection, result);

		if (sourceCon == null && targetCon != null && targetCon != currentConnection)
		{
			try
			{
				targetCon.disconnect();
			}
			catch (Exception th)
			{
			}
		}
		if (!result.isSuccess()) return result;

		boolean includeDelete = cmdLine.getBoolean(PARAM_INCLUDE_DELETE, true);
		boolean checkDependencies = cmdLine.getBoolean(CommonArgs.ARG_CHECK_FK_DEPS, true);
		String nl = Settings.getInstance().getExternalEditorLineEnding();
		
		CommonDiffParameters.TableMapping mapping = params.getTables(sourceCon, targetCon);
		int tableCount = mapping.referenceTables.size();
		dataDiff = new TableDataDiff(sourceCon, targetCon);
		dataDiff.setSqlDateLiteralType(literalType);
		
		deleteSync = new TableDeleteSync(targetCon, sourceCon);
		deleteSync.setRowMonitor(rowMonitor);
		dataDiff.setRowMonitor(rowMonitor);
		
		CommonArgs.setProgressInterval(dataDiff, cmdLine);
		CommonArgs.setProgressInterval(deleteSync, cmdLine);

		List<String> ignoreColumns = CommonArgs.getListArgument(cmdLine, PARAM_IGNORE_COLS);
		dataDiff.setColumnsToIgnore(ignoreColumns);
		
		try
		{
			for (int i=0; i < tableCount; i++)
			{
				TableIdentifier refTable = mapping.referenceTables.get(i);
				TableIdentifier targetTable = mapping.targetTables.get(i);

				WbFile updateFile = createFilename("update", targetTable);
				WbFile insertFile = createFilename("insert", targetTable);
				Writer updates = EncodingUtil.createWriter(updateFile, encoding, false);
				Writer inserts = EncodingUtil.createWriter(insertFile, encoding, false);
				try
				{
					dataDiff.setOutputWriters(updates, inserts, nl);
					dataDiff.setTableName(refTable, targetTable);
					dataDiff.doSync();
				}
				finally
				{
					FileUtil.closeQuitely(updates);
					FileUtil.closeQuitely(inserts);
				}
				
				if (includeDelete && !this.isCancelled)
				{
					WbFile deleteFile = createFilename("delete", targetTable);
					Writer deleteOut = EncodingUtil.createWriter(deleteFile, encoding, false);
					try
					{
						deleteSync.setOutputWriter(deleteOut);
						deleteSync.setTableName(refTable, targetTable);
						deleteSync.doSync();
					}
					finally
					{
						FileUtil.closeQuitely(deleteOut);
					}
				}
			}
		}
		catch (Exception e)
		{
			LogMgr.logError("WbDataDiff.execute()", "Error during diff", e);
			result.addMessage(ExceptionUtil.getDisplay(e));
			result.setFailure();
		}
		
		if (result.isSuccess() && !isCancelled)
		{
			String encodingParm = "-encoding='" + EncodingUtil.cleanupEncoding(encoding) + "'";
			Writer out = null;
			try
			{
				out = EncodingUtil.createWriter(mainScript, encoding, false);
				String sourceInfo = sourceCon.getDisplayString();
				String targetInfo = targetCon.getDisplayString();
				int len = sourceInfo.length();
				if (targetInfo.length() > len) len = targetInfo.length();
				
				StringBuffer line = new StringBuffer(len);
				line.append("-- ");
				for (int i=0; i < len; i++) line.append('*');
				line.append(nl);
				out.write(line.toString());
				out.write("-- The following script will migrate the data in: " + nl);
				out.write("-- " + targetInfo + nl);
				out.write("--" + nl);
				out.write("-- to match the data from: " + nl);
				out.write("-- " +  sourceInfo + nl);
				out.write("--" + nl);
				out.write("-- Generated by " + ResourceMgr.TXT_PRODUCT_NAME + " at: " + StringUtil.getCurrentTimestampWithTZString() + nl);
				out.write(line.toString());
				out.write(nl);
				out.write("------------------------" + nl);
				out.write("-- UPDATE/INSERT scripts" + nl);
				out.write("------------------------" + nl);
				
				TableDependencySorter sorter = new TableDependencySorter(targetCon);
				if (checkDependencies)
				{
					sorter.sortForInsert(mapping.targetTables);
				}
				
				int count = 0;
				
				for (TableIdentifier table : mapping.targetTables)
				{
					WbFile ins = createFilename("insert", table);
					if (ins.exists())
					{
						if (ins.length() > 0)
						{
							out.write("WbInclude -file='" + ins.getName() + "' " + encodingParm + ";"+ nl);
							count ++;
						}
						else
						{
							ins.delete();
						}
					}
					WbFile upd = createFilename("update", table);
					if (upd.exists())
					{
						if (upd.length() > 0)
						{
							out.write("WbInclude -file='" + upd.getName() + "' " + encodingParm + ";" + nl);
							count ++;
						}
						else
						{
							upd.delete();
						}
					}
				}
				
				if (count > 0) out.write(nl + "COMMIT;" + nl);
				count = 0;
				
				if (checkDependencies)
				{
					sorter.sortForDelete(mapping.targetTables, false);
				}
				
				boolean first = true;
				
				for (TableIdentifier table : mapping.targetTables)
				{
					WbFile f = createFilename("delete", table);
					if (f.exists())
					{
						if (f.length() > 0)
						{
							if (first)
							{
								first = false;
								out.write(nl);
								out.write("-----------------" + nl);
								out.write("-- DELETE scripts" + nl);
								out.write("-----------------" + nl);
							}
							out.write("WbInclude -file='" + f.getName() + "' " + encodingParm + ";" + nl);
							count ++;
						}
						else
						{
							f.delete();
						}
					}
				}
				if (count > 0) out.write(nl + "COMMIT;" + nl);
			}
			finally
			{
				FileUtil.closeQuitely(out);
			}
		}
		
		if (result.isSuccess() && !isCancelled)
		{
			result.addMessage(ResourceMgr.getFormattedString("MsgDataDiffSuccess", this.mainScript.getFullPath()));
		}
		return result;
	}


	private WbFile createFilename(String type, TableIdentifier table)
	{
		WbFile f = new WbFile(outputDir, StringUtil.makeFilename(table.getTableName() + "_$" + type + ".sql"));
		return f;
	}
}
