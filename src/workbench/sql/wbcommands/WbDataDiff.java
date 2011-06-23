/*
 * WbDataDiff.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2011, Thomas Kellerer
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
import workbench.util.CollectionUtil;
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
 * @author Thomas Kellerer
 */
public class WbDataDiff
	extends SqlCommand
{
	public static final String VERB = "WBDATADIFF";

	public static final String PARAM_INCLUDE_DELETE = "includeDelete";
	public static final String PARAM_IGNORE_COLS = "ignoreColumns";
	public static final String PARAM_OUTPUT_TYPE = "type";

	private WbFile outputDir;
	private TableDataDiff dataDiff;
	private TableDeleteSync deleteSync;
	private boolean xmlOutput;
	private CommonDiffParameters params;
	public WbDataDiff()
	{
		super();
		cmdLine = new ArgumentParser();
		cmdLine.addArgument(PARAM_INCLUDE_DELETE, ArgumentType.BoolArgument);
		cmdLine.addArgument(WbExport.ARG_CREATE_OUTPUTDIR);
		cmdLine.addArgument(PARAM_IGNORE_COLS);
		cmdLine.addArgument(PARAM_OUTPUT_TYPE, CollectionUtil.arrayList("sql", "xml"));
		cmdLine.addArgument(WbExport.ARG_BLOB_TYPE, BlobMode.getTypes());
		cmdLine.addArgument(WbExport.ARG_USE_CDATA, ArgumentType.BoolArgument);

		CommonArgs.addCheckDepsParameter(cmdLine);
		CommonArgs.addSqlDateLiteralParameter(cmdLine);
		CommonArgs.addProgressParameter(cmdLine);

		// Add common diff parameters
		params = new CommonDiffParameters(this.cmdLine);
	}

	@Override
	public String getVerb()
	{
		return VERB;
	}

	@Override
	protected boolean isConnectionRequired()
	{
		return false;
	}

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

		WbFile mainScript = evaluateFileArgument(cmdLine.getValue(CommonDiffParameters.PARAM_FILENAME));
		if (mainScript == null)
		{
			result.setFailure();
			result.addMessage(ResourceMgr.getString("ErrDataDiffNoFile"));
			result.addMessage(getWrongArgumentsMessage());
			return result;
		}

		outputDir = new WbFile(mainScript.getParentFile());
		String encoding = cmdLine.getValue(CommonArgs.ARG_ENCODING);
		if (encoding == null)
		{
			encoding = Settings.getInstance().getDefaultEncoding();
		}
		encoding = EncodingUtil.cleanupEncoding(encoding);

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
		boolean useCDATA = cmdLine.getBoolean(WbExport.ARG_USE_CDATA, false);

		CommonDiffParameters.TableMapping mapping = params.getTables(sourceCon, targetCon);
		int tableCount = mapping.referenceTables.size();
		dataDiff = new TableDataDiff(sourceCon, targetCon);
		dataDiff.setSqlDateLiteralType(literalType);

		String outputType = cmdLine.getValue(PARAM_OUTPUT_TYPE);
		if (StringUtil.isBlank(outputType)) outputType = "sql";
		xmlOutput = false;
		if ("xml".equalsIgnoreCase(outputType))
		{
			dataDiff.setTypeXml(useCDATA);
			xmlOutput = true;
		}
		else if ("sql".equalsIgnoreCase(outputType))
		{
			dataDiff.setTypeSql();
		}
		else
		{
			result.addMessage("Illegal output type: " + outputType);
			result.setFailure();
			return result;
		}


		String blobtype = cmdLine.getValue(WbExport.ARG_BLOB_TYPE);
		if (StringUtil.isNonBlank(blobtype))
		{
			dataDiff.setBlobMode(blobtype);
		}

		dataDiff.setRowMonitor(rowMonitor);

		if (rowMonitor != null)
		{
			rowMonitor.setMonitorType(RowActionMonitor.MONITOR_PROCESS_TABLE);
		}

		CommonArgs.setProgressInterval(dataDiff, cmdLine);

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
					dataDiff.setOutputWriters(updates, inserts, nl, encoding);
					dataDiff.setBaseDir(outputDir);
					if (dataDiff.setTableName(refTable, targetTable))
					{
						dataDiff.doSync();
					}
					else
					{
						result.addMessage(ResourceMgr.getFormattedString("ErrDataDiffNoTableMatch", refTable.getTableName(), targetTable.getTableName()));
						result.setWarning(true);
					}
				}
				finally
				{
					FileUtil.closeQuietely(updates);
					FileUtil.closeQuietely(inserts);
				}

				if (includeDelete && !this.isCancelled)
				{
					WbFile deleteFile = createFilename("delete", targetTable);
					Writer deleteOut = EncodingUtil.createWriter(deleteFile, encoding, false);
					try
					{
						deleteSync = new TableDeleteSync(targetCon, sourceCon);
						CommonArgs.setProgressInterval(deleteSync, cmdLine);

						deleteSync.setRowMonitor(rowMonitor);
						deleteSync.setOutputWriter(deleteOut, nl, encoding);
						if ("xml".equalsIgnoreCase(outputType))
						{
							deleteSync.setTypeXml(useCDATA);
						}
						else if ("sql".equalsIgnoreCase(outputType))
						{
							deleteSync.setTypeSql();
						}
						deleteSync.setTableName(refTable, targetTable);
						deleteSync.doSync();
					}
					finally
					{
						FileUtil.closeQuietely(deleteOut);
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
				if (xmlOutput)
				{
					out.write("<?xml version=\"1.0\" encoding=\"" + encoding + "\"?>" + nl);
					out.write("<!-- " + nl);
					out.write("  ** Generated by " + ResourceMgr.TXT_PRODUCT_NAME + " at: " + StringUtil.getCurrentTimestampWithTZString() + " **" + nl);
					out.write(nl);
					out.write("  The following XML files describe the diff result to migrate the data in" + nl);
					out.write("  " + targetInfo + nl);
					out.write("  to match the data from" + nl);
					out.write("  " +  sourceInfo + nl);
					out.write("-->" + nl);
					out.write(nl);
					out.write("<data-diff>" + nl);
					out.write("  <summary>" + nl);

					for (int i=0; i < tableCount; i++)
					{
						TableIdentifier refTable = mapping.referenceTables.get(i);
						TableIdentifier targetTable = mapping.targetTables.get(i);
						out.write("    <mapping>" + nl);
						out.write("      <reference-table>" + refTable.getFullyQualifiedName(sourceCon) + "</reference-table>" + nl);
						out.write("      <target-table>" + targetTable.getFullyQualifiedName(targetCon) + "</target-table>" + nl);
						out.write("    </mapping>" + nl);
					}
					out.write("  </summary>" + nl + nl);
					out.write("  <files>" + nl);
					if (checkDependencies)
					{
						out.write("    <!-- UPDATE/INSERT migrations are sorted according to their foreign key relationship --> " + nl);
					}
				}
				else
				{
					out.write(line.toString());
					out.write("-- The following script will migrate the data in: " + nl);
					out.write("-- " + targetInfo + nl);
					out.write("--" + nl);
					out.write("-- to match the data from: " + nl);
					out.write("-- " +  sourceInfo + nl);
					out.write("--" + nl);
					out.write("-- Tables included:" + nl);
					for (TableIdentifier table : mapping.targetTables)
					{
						out.write("-- " + table.getTableExpression() + nl);
					}
					out.write("--" + nl);
					out.write("-- Generated by " + ResourceMgr.TXT_PRODUCT_NAME + " at: " + StringUtil.getCurrentTimestampWithTZString() + nl);
					out.write(line.toString());
					out.write(nl);
					out.write("------------------------" + nl);
					out.write("-- UPDATE/INSERT scripts" + nl);
					out.write("------------------------" + nl);
				}

				TableDependencySorter sorter = new TableDependencySorter(targetCon);
				if (checkDependencies)
				{
					if (this.rowMonitor != null && mapping.targetTables.size() > 1)
					{
						rowMonitor.setMonitorType(RowActionMonitor.MONITOR_PLAIN);
						rowMonitor.setCurrentObject(ResourceMgr.getString("MsgDataDiffSortInsert"), -1, -1);
					}
					sorter.sortForInsert(mapping.targetTables);
				}

				int count = 0;

				for (TableIdentifier table : mapping.targetTables)
				{
					WbFile ins = createFilename("insert", table);
					WbFile upd = createFilename("update", table);

					if (xmlOutput)
					{
						out.write("    <table name=\"" + table.getFullyQualifiedName(targetCon) + "\">" + nl);
						if (ins.exists())
						{
							if (ins.length() > 0)
							{
								out.write("      <file-name type=\"insert\">" + ins.getName() + "</file-name>" + nl);
							}
							else
							{
								ins.delete();
								out.write("      <!-- No INSERTs for " + table.getObjectName() + " necessary -->" + nl);
							}
						}
						if (upd.exists())
						{
							if (upd.length() > 0)
							{
								out.write("      <file-name type=\"update\">" + upd.getName() + "</file-name>" + nl);
							}
							else
							{
								upd.delete();
								out.write("      <!-- No UPDATEs for " + table.getObjectName() + " necessary -->" + nl);
							}

						}
						out.write("    </table>" + nl);
					}
					else
					{
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
								out.write("-- No INSERTs for " + table.getObjectName() + " necessary" + nl);
							}
						}
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
								out.write("-- No UPDATEs for " + table.getObjectName() + " necessary" + nl);
							}
						}
					}
				}

				if (count > 0 && !xmlOutput) out.write(nl + "COMMIT;" + nl);
				count = 0;

				if (checkDependencies  && mapping.targetTables.size() > 1)
				{
					if (this.rowMonitor != null)
					{
						rowMonitor.setMonitorType(RowActionMonitor.MONITOR_PLAIN);
						rowMonitor.setCurrentObject(ResourceMgr.getString("MsgDataDiffSortDelete"), -1, -1);
					}
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
							if (xmlOutput)
							{
								if (first)
								{
									first = false;
									if (checkDependencies)
									{
										out.write(nl + "    <!-- DELETE migrations are sorted according to their foreign key relationship --> " + nl);
									}
								}
								out.write("    <table name=\"" + table.getTableExpression(targetCon) + "\">" + nl);
								out.write("      <file-name type=\"delete\">" + f.getName() + "</file-name>" + nl);
								out.write("    </table>" + nl);
							}
							else
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
							}
							count ++;
						}
						else
						{
							if (xmlOutput)
							{
								out.write("    <!-- No DELETEs for " + table.getObjectName() + " necessary -->" + nl);
							}
							else
							{
								out.write("-- No DELETEs for " + table.getObjectName() + " necessary" + nl);
							}
							f.delete();
						}
					}
				}
				if (xmlOutput)
				{
					out.write("  </files>" + nl);
					out.write("</data-diff>" + nl);
				}
				else if (count > 0 )
				{
					out.write(nl + "COMMIT;" + nl);
				}

			}
			finally
			{
				FileUtil.closeQuietely(out);
			}
		}

		if (this.rowMonitor != null)
		{
			this.rowMonitor.jobFinished();
		}

		if (result.isSuccess() && !isCancelled)
		{
			result.addMessage(ResourceMgr.getFormattedString("MsgDataDiffSuccess", mainScript.getFullPath()));
		}
		return result;
	}


	private WbFile createFilename(String type, TableIdentifier table)
	{
		if (xmlOutput)
		{
			return new WbFile(outputDir, StringUtil.makeFilename(table.getTableName() + "_$" + type + ".xml"));
		}
		return new WbFile(outputDir, StringUtil.makeFilename(table.getTableName() + "_$" + type + ".sql"));
	}
}
