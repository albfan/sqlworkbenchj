/*
 * WbDataDiff.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2015, Thomas Kellerer
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
package workbench.sql.wbcommands;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Writer;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import workbench.log.LogMgr;
import workbench.resource.ResourceMgr;
import workbench.resource.Settings;

import workbench.db.ConnectionMgr;
import workbench.db.TableIdentifier;
import workbench.db.WbConnection;
import workbench.db.compare.TableDataDiff;
import workbench.db.compare.TableDeleteSync;
import workbench.db.compare.TableDiffStatus;
import workbench.db.exporter.BlobMode;
import workbench.db.importer.TableDependencySorter;

import workbench.storage.RowActionMonitor;
import workbench.storage.SqlLiteralFormatter;

import workbench.sql.SqlCommand;
import workbench.sql.StatementRunnerResult;

import workbench.util.ArgumentParser;
import workbench.util.ArgumentType;
import workbench.util.CaseInsensitiveComparator;
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
	public static final String VERB = "WbDataDiff";

	public static final String PARAM_INCLUDE_DELETE = "includeDelete";
	public static final String PARAM_IGNORE_COLS = "ignoreColumns";
	public static final String PARAM_OUTPUT_TYPE = "type";
	public static final String PARAM_ALTERNATE_KEYS = "alternateKey";
	public static final String PARAM_EXCLUDE_REAL_PK = "excludeRealPK";
	public static final String PARAM_EXCLUDE_IGNORED = "excludeIgnored";
	public static final String PARAM_SINGLE_FILE = "singleFile";

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
		cmdLine.addArgument(PARAM_ALTERNATE_KEYS, ArgumentType.Repeatable);
		cmdLine.addArgument(PARAM_EXCLUDE_REAL_PK, ArgumentType.BoolArgument);
		cmdLine.addArgument(PARAM_EXCLUDE_IGNORED, ArgumentType.BoolArgument);
		cmdLine.addArgument(PARAM_SINGLE_FILE, ArgumentType.BoolArgument);

		CommonArgs.addCheckDepsParameter(cmdLine);
		CommonArgs.addSqlDateLiteralParameter(cmdLine);
		CommonArgs.addProgressParameter(cmdLine);

		// Add common diff parameters
		params = new CommonDiffParameters(this.cmdLine, getBaseDir());
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

	protected Map<String, Set<String>> getAlternateKeys(ArgumentParser command, StatementRunnerResult result)
	{
		Map<String, Set<String>> map = new TreeMap<>(CaseInsensitiveComparator.INSTANCE);

		List<String> list = command.getList(PARAM_ALTERNATE_KEYS);
		if (CollectionUtil.isEmpty(list))
		{
			return map;
		}

		CaseInsensitiveComparator comp = new CaseInsensitiveComparator();
		comp.setIgnoreQuotes(true);

		for (String def : list)
		{
			String[] elements = def.split("=");
			if (elements != null && elements.length == 2)
			{
				List<String> l = StringUtil.stringToList(elements[1], ",", true, true, false, true);
				Set<String> cols = new TreeSet<>(comp);
				cols.addAll(l);
				map.put(elements[0], cols);
			}
			else
			{
				result.addWarning(ResourceMgr.getFormattedString("ErrIgnoringArg", def, PARAM_ALTERNATE_KEYS));
			}
		}
		return map;
	}

	@Override
	public StatementRunnerResult execute(String sql)
		throws SQLException, Exception
	{
		StatementRunnerResult result = new StatementRunnerResult();

		this.cmdLine.parse(getCommandLine(sql));

		if (cmdLine.getArgumentCount() == 0)
		{
			result.addErrorMessage(getWrongArgumentsMessage());
			return result;
		}

		if (cmdLine.hasUnknownArguments())
		{
			setUnknownMessage(result, cmdLine, getWrongArgumentsMessage());
			return result;
		}

		WbFile mainScript = evaluateFileArgument(cmdLine.getValue(CommonArgs.ARG_FILE));
		if (mainScript == null)
		{
			result.addErrorMessage(ResourceMgr.getString("ErrDataDiffNoFile"));
			result.addMessage(getWrongArgumentsMessage());
			return result;
		}

		outputDir = new WbFile(mainScript.getAbsoluteFile().getParentFile());
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
				result.addErrorMessageByKey("ErrCreateDir", outputDir.getFullPath());
				LogMgr.logError("WbDataDiff.execute()", "Could not create output directory: " + outputDir.getFullPath(), null);
				return result;
			}
		}

		if (!outputDir.exists())
		{
			result.addErrorMessageByKey("ErrOutputDirNotFound", outputDir.getFullPath());
			return result;
		}

		if (this.rowMonitor != null) this.rowMonitor.setMonitorType(RowActionMonitor.MONITOR_PLAIN);
		params.setMonitor(rowMonitor);

		WbConnection targetCon = params.getTargetConnection(currentConnection, result);
		if (!result.isSuccess()) return result;
		WbConnection sourceCon = params.getSourceConnection(currentConnection, result);

		if (sourceCon == null)
		{
			// make sure the already established target connection is closed properly
			close(targetCon);
		}

		if (!result.isSuccess()) return result;

		boolean includeDelete = cmdLine.getBoolean(PARAM_INCLUDE_DELETE, true);
		boolean checkDependencies = cmdLine.getBoolean(CommonArgs.ARG_CHECK_FK_DEPS, true);
		String nl = Settings.getInstance().getExternalEditorLineEnding();
		boolean useCDATA = cmdLine.getBoolean(WbExport.ARG_USE_CDATA, false);

		CommonDiffParameters.TableMapping mapping = params.getTables(sourceCon, targetCon);
		int tableCount = mapping.referenceTables.size();
		List<String> missing = params.getMissingReferenceTables();
		if (missing.size() > 0)
		{
			for (String table : missing)
			{
				String msg = ResourceMgr.getFormattedString("ErrRefTableNotFound", table);
				result.addWarning(msg);
			}
			result.addMessageNewLine();
		}

		missing = params.getMissingTargetTables();
		if (missing.size() > 0)
		{
			for (String table : missing)
			{
				String msg = ResourceMgr.getFormattedString("ErrTargetTableNotFound", table);
				result.addWarning(msg);
			}
			result.addMessageNewLine();
		}

		if (tableCount == 0)
		{
			LogMgr.logWarning("WbDataDiff.execute()", "No tables found.");
			result.addWarningByKey("ErrNoTablesFound");
			close(targetCon);
			close(sourceCon);
			return result;
		}

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
			result.addErrorMessage("Illegal output type: " + outputType);
			close(targetCon);
			close(sourceCon);
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

		Map<String, Set<String>> alternatekeys = getAlternateKeys(cmdLine, result);
		dataDiff.setAlternateKeys(alternatekeys);
		dataDiff.setExcludeRealPK(cmdLine.getBoolean(PARAM_EXCLUDE_REAL_PK, false));
		dataDiff.setExcludeIgnoredColumns(cmdLine.getBoolean(PARAM_EXCLUDE_IGNORED, false));

		boolean singleFile = cmdLine.getBoolean(PARAM_SINGLE_FILE, false);
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
					TableDiffStatus status = dataDiff.setTableName(refTable, targetTable);
					switch (status)
					{
						case OK:
							dataDiff.doSync();
							break;
						case ReferenceNotFound:
							result.addWarning(ResourceMgr.getFormattedString("ErrTableNotFound", refTable.getTableName()));
							break;
						case TargetNotFound:
							result.addWarning(ResourceMgr.getFormattedString("ErrTableNotFound", targetTable.getTableName()));
							break;
						case NoPK:
							result.addWarning(ResourceMgr.getFormattedString("ErrDataDiffNoPK", refTable.getTableName()));
							break;
						case ColumnMismatch:
							result.addWarning(ResourceMgr.getFormattedString("ErrDataDiffNoTableMatch", refTable.getTableName(), targetTable.getTableName()));
							break;
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
						Set<String> keys = alternatekeys.get(targetTable.getTableName());
						TableDiffStatus status = deleteSync.setTableName(refTable, targetTable, keys);
						if (status == TableDiffStatus.OK)
						{
							deleteSync.doSync();
						}
						else if (status == TableDiffStatus.NoPK)
						{
							// if the table does not have a PK, a warning was already added during the compare step
							// so there is nothing to do here.
							LogMgr.logDebug("WbDataDiff.execute()", "No delete performed for " + targetTable.getTableName() + " because not PK was found");
						}
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
			result.addErrorMessage(ExceptionUtil.getDisplay(e));
		}

		try
		{
			if (result.isSuccess() && !isCancelled)
			{
				writeOutput(mainScript, mapping, encoding, sourceCon, targetCon, checkDependencies, singleFile);
				result.addMessage(ResourceMgr.getFormattedString("MsgDataDiffSuccess", mainScript.getFullPath()));
			}
		}
		finally
		{
			close(targetCon);
			close(sourceCon);
			if (LogMgr.isDebugEnabled())
			{
				ConnectionMgr.getInstance().dumpConnections();
			}
		}

		if (this.rowMonitor != null)
		{
			this.rowMonitor.jobFinished();
		}

		return result;
	}

	private void writeOutput(WbFile mainScript, CommonDiffParameters.TableMapping mapping, String encoding, WbConnection sourceCon, WbConnection targetCon, boolean checkDependencies, boolean singleFile)
		throws IOException
	{
		String encodingParm = "-encoding='" + EncodingUtil.cleanupEncoding(encoding) + "'";
		Writer out = null;
		String nl = Settings.getInstance().getExternalEditorLineEnding();
		int tableCount = mapping.referenceTables.size();

		try
		{
			out = EncodingUtil.createWriter(mainScript, encoding, false);
			String sourceInfo = sourceCon.getDisplayString();
			String targetInfo = targetCon.getDisplayString();
			int len = sourceInfo.length();
			if (targetInfo.length() > len) len = targetInfo.length();

			String line = StringUtil.padRight("-- ", len, '*');
			line += nl;
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
				out.write(line);
				out.write("-- The following script will migrate the data in: " + nl);
				out.write("-- " + targetInfo + nl);
				out.write("-- " + nl);
				out.write("-- to match the data from: " + nl);
				out.write("-- " +  sourceInfo + nl);
				out.write("-- " + nl);
				out.write("-- Tables included:" + nl);
				for (TableIdentifier table : mapping.targetTables)
				{
					out.write("-- " + table.getTableExpression() + nl);
				}
				out.write("-- " + nl);
				out.write("-- Generated by " + ResourceMgr.TXT_PRODUCT_NAME + " at: " + StringUtil.getCurrentTimestampWithTZString() + nl);
				out.write(line);
				out.write(nl);
				if (!singleFile)
				{
					out.write("-- ----------------------" + nl);
					out.write("-- UPDATE/INSERT scripts" + nl);
					out.write("-- ----------------------" + nl);
				}
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
					out.write("    ");
          TableDataDiff.writeTableNameTag(out, "table", table);
          out.write(nl);
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
							if (singleFile)
							{
								out.write("-- -------------------" + nl);
								out.write("-- INSERTS for " + table.getFullyQualifiedName(targetCon) + nl);
								out.write("-- -------------------" + nl);
								mergeInto(out, ins, encoding, nl);
								ins.delete();
							}
							else
							{
								out.write("WbInclude -file='" + ins.getName() + "' " + encodingParm + ";"+ nl);
							}
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
							if (singleFile)
							{
								out.write("-- -------------------" + nl);
								out.write("-- UPDATES for " + table.getFullyQualifiedName(targetCon) + nl);
								out.write("-- -------------------" + nl);
								mergeInto(out, upd, encoding, nl);
								upd.delete();
							}
							else
							{
								out.write("WbInclude -file='" + upd.getName() + "' " + encodingParm + ";" + nl);
							}
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
              out.write("    ");
              TableDataDiff.writeTableNameTag(out, "table", table);
              out.write(nl);
							out.write("      <file-name type=\"delete\">" + f.getName() + "</file-name>" + nl);
							out.write("    </table>" + nl);
						}
						else
						{
							if (first)
							{
								first = false;
								out.write(nl);
								out.write("-- ---------------" + nl);
								out.write("-- DELETE " + (singleFile ? "statements" : "scripts") + nl);
								out.write("-- ---------------" + nl);
							}
							if (singleFile)
							{
								mergeInto(out, f, encoding, nl);
								f.delete();
							}
							else
							{
								out.write("WbInclude -file='" + f.getName() + "' " + encodingParm + ";" + nl);
							}
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
							out.write(nl + "-- No DELETEs for " + table.getObjectName() + " necessary" + nl);
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

	private void close(WbConnection toClose)
	{
		if (toClose != null && toClose != currentConnection)
		{
			try
			{
				toClose.disconnect();
			}
			catch (Exception e)
			{
				LogMgr.logWarning("WbDataDiff.execute()", "Could not disconnect target connection " + toClose.getId());
			}
		}
	}

	private void mergeInto(Writer out, WbFile toInclude, String encoding, String newLine)
		throws IOException
	{
		if (!toInclude.exists() || toInclude.length() == 0) return;
		BufferedReader reader = null;
		try
		{
			reader = EncodingUtil.createBufferedReader(toInclude, encoding);
			String line = reader.readLine();
			while (line != null)
			{
				if (!line.startsWith("--") && StringUtil.isNonEmpty(line))
				{
					out.write(line);
					out.write(newLine);
				}
				line = reader.readLine();
			}
			out.write(newLine);
		}
		finally
		{
			FileUtil.closeQuietely(reader);
		}
	}

	private WbFile createFilename(String type, TableIdentifier table)
	{
		if (xmlOutput)
		{
			return new WbFile(outputDir, StringUtil.makeFilename(table.getTableName() + "_$" + type + ".xml"));
		}
		return new WbFile(outputDir, StringUtil.makeFilename(table.getTableName() + "_$" + type + ".sql"));
	}

	@Override
	public boolean isWbCommand()
	{
		return true;
	}

}

