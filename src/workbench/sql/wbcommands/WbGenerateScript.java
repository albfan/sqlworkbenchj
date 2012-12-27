/*
 * WbGenerateScript.java
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
package workbench.sql.wbcommands;

import java.io.IOException;
import java.io.Writer;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import workbench.db.DbObject;
import workbench.db.ObjectScripter;
import workbench.db.ProcedureDefinition;
import workbench.db.ProcedureReader;
import workbench.db.TableIdentifier;
import workbench.db.TriggerDefinition;
import workbench.db.TriggerReader;
import workbench.db.TriggerReaderFactory;
import workbench.interfaces.ScriptGenerationMonitor;
import workbench.log.LogMgr;
import workbench.resource.ResourceMgr;
import workbench.sql.SqlCommand;
import workbench.sql.StatementRunnerResult;
import workbench.storage.RowActionMonitor;
import workbench.util.ArgumentParser;
import workbench.util.ArgumentType;
import workbench.util.CollectionUtil;
import workbench.util.EncodingUtil;
import workbench.util.FileUtil;
import workbench.util.StringUtil;
import workbench.util.WbFile;

/**
 *
 * @author Thomas Kellerer
 */
public class WbGenerateScript
	extends SqlCommand
	implements ScriptGenerationMonitor
{

	public static final String VERB = "WBGENERATESCRIPT";
	private ObjectScripter scripter;
	private int currentObject;
	private int totalObjects;

	public WbGenerateScript()
	{
		super();
		this.isUpdatingCommand = false;

		cmdLine = new ArgumentParser();
		cmdLine.addArgument(CommonArgs.ARG_TYPES, ArgumentType.ObjectTypeArgument);
		cmdLine.addArgument(CommonArgs.ARG_SCHEMAS, ArgumentType.SchemaArgument);
		cmdLine.addArgument(CommonArgs.ARG_OBJECTS, ArgumentType.TableArgument);
		cmdLine.addArgument(WbSchemaReport.PARAM_INCLUDE_PROCS, ArgumentType.BoolArgument);
		cmdLine.addArgument(WbSchemaReport.PARAM_INCLUDE_TRIGGERS, ArgumentType.BoolArgument);
		cmdLine.addArgument("useSeparator", ArgumentType.BoolArgument);
		cmdLine.addArgument("file", ArgumentType.StringArgument);
	}

	@Override
	public StatementRunnerResult execute(String sql)
		throws SQLException
	{
		StatementRunnerResult result = new StatementRunnerResult();
		String args = getCommandLine(sql);
		cmdLine.parse(args);

		List<String> schemas = null;
		List<String> types = null;
		List<String> names = null;

		if (!cmdLine.hasArguments())
		{
			names = StringUtil.stringToList(args, " ");
		}
		else
		{
			if (cmdLine.hasUnknownArguments())
			{
				setUnknownMessage(result, cmdLine, ResourceMgr.getString("ErrGenScriptWrongParam"));
				return result;
			}
			names = cmdLine.getListValue(CommonArgs.ARG_OBJECTS);
			schemas = cmdLine.getListValue(CommonArgs.ARG_SCHEMAS);
			types = cmdLine.getListValue(CommonArgs.ARG_TYPES);
		}

		List<DbObject> objects = new ArrayList<DbObject>();

		if (CollectionUtil.isEmpty(schemas))
		{
			schemas = CollectionUtil.arrayList(currentConnection.getCurrentSchema());
		}

		if (CollectionUtil.isNonEmpty(names))
		{
			for (String table : names)
			{
				String tname = currentConnection.getMetadata().adjustObjectnameCase(table);
				for (String schema : schemas)
				{
					if (isCancelled) break;

					List<TableIdentifier> elements = currentConnection.getMetadata().getObjectList(tname, schema, null);
					objects.addAll(elements);
				}
				if (isCancelled) break;
			}
		}
		else if (CollectionUtil.isNonEmpty(types))
		{
			String[] typeNames = types.toArray(new String[0]);
			for (String schema : schemas)
			{
				if (isCancelled) break;
				List<TableIdentifier> elements = currentConnection.getMetadata().getObjectList(schema, typeNames);
				objects.addAll(elements);
			}
		}
		else
		{
			for (String schema : schemas)
			{
				if (isCancelled) break;
				List<TableIdentifier> elements = currentConnection.getMetadata().getObjectList(schema, null);
				objects.addAll(elements);
			}
		}

		if (cmdLine.getBoolean(WbSchemaReport.PARAM_INCLUDE_PROCS, false))
		{
			ProcedureReader reader = currentConnection.getMetadata().getProcedureReader();
			for (String schema : schemas)
			{
				if (isCancelled) break;
				List<ProcedureDefinition> procs = reader.getProcedureList(null, schema, null);
				objects.addAll(procs);
			}
		}

		if (cmdLine.getBoolean(WbSchemaReport.PARAM_INCLUDE_TRIGGERS, false))
		{
			TriggerReader reader = TriggerReaderFactory.createReader(currentConnection);
			for (String schema : schemas)
			{
				if (isCancelled) break;
				List<TriggerDefinition> triggers = reader.getTriggerList(null, schema, null);
				objects.addAll(triggers);
			}
		}

		if (isCancelled)
		{
			result.setWarning(true);
			return result;
		}

		WbFile output = evaluateFileArgument(cmdLine.getValue("file"));

		totalObjects = objects.size();
		currentObject = 0;
		scripter = new ObjectScripter(objects, currentConnection);
		scripter.setUseSeparator(cmdLine.getBoolean("useSeparator", false));
		if (this.rowMonitor != null)
		{
			rowMonitor.saveCurrentType("genscript");
			rowMonitor.setMonitorType(RowActionMonitor.MONITOR_PROCESS_TABLE);
		}
		scripter.setProgressMonitor(this);
		try
		{
			scripter.generateScript();
		}
		finally
		{
			rowMonitor.restoreType("genscript");
			rowMonitor.jobFinished();
		}

		if (isCancelled)
		{
			result.setWarning(true);
			return result;
		}

		result.setSuccess();

		if (output != null)
		{
			Writer writer = null;
			try
			{
				writer = EncodingUtil.createWriter(output, EncodingUtil.getDefaultEncoding(), false);
				writer.write(scripter.getScript());
				result.addMessage(ResourceMgr.getFormattedString("MsgScriptWritten", output.getAbsolutePath()));
			}
			catch (IOException io)
			{
				LogMgr.logError("WbGenerateScript.execute()", "Could not write outputfile", io);
				result.setFailure();
				result.addMessage(io.getLocalizedMessage());
			}
			finally
			{
				FileUtil.closeQuietely(writer);
			}
		}
		else
		{
			result.addMessage(scripter.getScript());
		}
		return result;
	}

	@Override
	public void cancel()
		throws SQLException
	{
		super.cancel();
		if (scripter != null)
		{
			scripter.cancel();
		}
	}

	@Override
	public String getVerb()
	{
		return VERB;
	}

	@Override
	public void done()
	{
		totalObjects = 0;
		currentObject = 0;
		scripter = null;
	}

	@Override
	public void setCurrentObject(String anObject)
	{
		if (this.rowMonitor != null)
		{
			if (anObject.indexOf(' ') > -1)
			{
				try
				{
					rowMonitor.saveCurrentType("gen2");
					rowMonitor.setMonitorType(RowActionMonitor.MONITOR_PLAIN);
					rowMonitor.setCurrentObject(anObject, currentObject, totalObjects);
				}
				finally
				{
					rowMonitor.restoreType("gen2");
				}
			}
			else
			{
				currentObject ++;
				rowMonitor.setCurrentObject(anObject, currentObject, totalObjects);
			}
		}
	}


}
