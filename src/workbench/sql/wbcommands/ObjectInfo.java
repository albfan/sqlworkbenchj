/*
 * ObjectInfo.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2012, Thomas Kellerer
 * No part of this code may be reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.sql.wbcommands;

import java.sql.SQLException;
import java.util.List;
import workbench.db.*;
import workbench.log.LogMgr;
import workbench.resource.ResourceMgr;
import workbench.sql.StatementRunnerResult;
import workbench.storage.ColumnRemover;
import workbench.storage.DataStore;
import workbench.util.StringUtil;

/**
 * Retrieves information about a database object.
 * <br>
 * Only the object name is necessary. This class will then search the database
 * for a databse object in the following order:
 * <ol>
 *		<li>Tables and Views</li>
 *		<li>Synonyms</li>
 *		<li>Sequences</li>
 *		<li>Procedures (and functions)</li>
 * </ol>
 *
 * @author Thomas Kellerer
 */
public class ObjectInfo
{

	public ObjectInfo()
	{
	}

	/**
	 * Tries to find the definition of the object identified by the given name.
	 *
	 * If it's a TABLE (or something similar) the result will contain several
	 * DataStores that show the definition of the object.
	 *
	 * If no "selectable" object with that name can be found, the method will first
	 * check if a procedure with that name exists, otherwise it checks for a trigger.
	 *
	 * For a procedure or trigger, the source code will be returned as the message
	 * of the result
	 *
	 * @param connection the database connection
	 * @param objectName the object name to test
	 * @param includeDependencies if true dependent objects (e.g. indexes, constraints) are retrieved as well
	 * @return a StatementRunnerResult with DataStores that contain the definion or the
	 *  source SQL of the object in the message of the result object
	 * @throws SQLException
	 */
	public StatementRunnerResult getObjectInfo(WbConnection connection, String objectName, boolean includeDependencies)
		throws SQLException
	{
		StatementRunnerResult result = new StatementRunnerResult();

		TableIdentifier tbl = new TableIdentifier(objectName);
		tbl.adjustCase(connection);

		boolean searchAllSchemas = connection.getDbSettings().getSearchAllSchemas();
		boolean showSchema = false;
		TableIdentifier toDescribe = null;
		List<String> searchPath = DbSearchPath.Factory.getSearchPathHandler(connection).getSearchPath(connection, null);
		if (tbl.getSchema() == null && !searchPath.isEmpty())
		{
			for (String schema : searchPath)
			{
				TableIdentifier tb = tbl.createCopy();
				tb.setSchema(schema);
				toDescribe = connection.getMetadata().findObject(tb, true, searchAllSchemas);
				if (toDescribe != null) break;
			}
		}
		else
		{
			toDescribe = connection.getMetadata().findObject(tbl, true, searchAllSchemas);
		}

		DbSettings dbs = connection.getDbSettings();
		TableIdentifier synonymTarget = null;
		if (toDescribe != null && dbs.isSynonymType(toDescribe.getType()))
		{
			try
			{
				synonymTarget = connection.getMetadata().getSynonymTable(toDescribe);
				if (synonymTarget != null)
				{
					String msg = "--------[ " + StringUtil.capitalize(toDescribe.getType()) + ": " + toDescribe.getTableName() + " ]--------\n" +
							toDescribe.getTableExpression() + " --> " +
							synonymTarget.getTableExpression() + " (" +
							synonymTarget.getObjectType() + ")";

					result.addMessage(msg + "\n");
					result.setSourceCommand(msg);
				}
				toDescribe = synonymTarget;
			}
			catch (Exception e)
			{
				LogMgr.logError("ObjectInfo.getObjectInfo()", "Error retrieving synonym table", e);
			}
		}

		if (toDescribe != null && "SEQUENCE".equals(toDescribe.getType()))
		{
			try
			{
				SequenceReader seqReader = connection.getMetadata().getSequenceReader();
				if (seqReader != null)
				{
					String schema = toDescribe.getSchema();
					String name = toDescribe.getObjectName();
					String catalog = toDescribe.getCatalog();
					SequenceDefinition seq = seqReader.getSequenceDefinition(catalog, schema, name);
					if (seq != null)
					{
						DataStore ds = seq.getRawDefinition();
						ds.setResultName(seq.getObjectType() + ": " + seq.getObjectName());
						result.addDataStore(ds);

						CharSequence source = seq.getSource();
						if (source == null)
						{
							// source was not build by the reader during initial retrieval
							source = seq.getSource(connection);
						}

						if (source != null)
						{
							String src = source.toString();
							result.addMessage("--------[ " + StringUtil.capitalize(seq.getObjectType()) + ": " + seq.getObjectName() + " ]--------");
							result.addMessage(src);
							result.addMessage("--------");
							if (StringUtil.isBlank(result.getSourceCommand()))
							{
								result.setSourceCommand(StringUtil.getMaxSubstring(src, 350, "..."));
							}
						}
						return result;
					}
				}
			}
			catch (Exception e)
			{
				LogMgr.logError("ObjectInfo.getObjectInfo()", "Error retrieving sequences", e);
			}
		}

		if (toDescribe == null)
		{
			try
			{
				// No table or something similar found, try to find a procedure with that name
				ProcedureReader reader = connection.getMetadata().getProcedureReader();
				List<ProcedureDefinition> procs = reader.getProcedureList(tbl.getCatalog(), tbl.getSchema(), tbl.getObjectName());

				if (procs.size() == 1)
				{
					ProcedureDefinition def = procs.get(0);
					CharSequence source = def.getSource(connection);
					result.addMessage("--------[ " + StringUtil.capitalize(def.getObjectType())  + ": " + def.getObjectExpression(connection) + " ]--------");
					result.addMessage(source);
					result.addMessage("--------");
					result.setSuccess();
					return result;
				}
			}
			catch (Exception e)
			{
				LogMgr.logError("ObjectInfo.getObjectInfo()", "Error retrieving procedures", e);
			}

			try
			{
				// No procedure found, try to find a trigger.
				TriggerReader trgReader = TriggerReaderFactory.createReader(connection);
				TriggerDefinition trg = trgReader.findTrigger(tbl.getCatalog(), tbl.getSchema(), tbl.getObjectName());
				String source = null;
				if (trg != null)
				{
					source = trgReader.getTriggerSource(trg, true);
				}
				if (StringUtil.isNonBlank(source))
				{
					result.addMessage("--------[ Trigger: " + tbl.getObjectName() + " ]--------");
					result.addMessage(source);
					result.addMessage("--------");
					result.setSuccess();
					return result;
				}
			}
			catch (Exception e)
			{
				LogMgr.logError("ObjectInfo.getObjectInfo()", "Error retrieving triggers", e);
			}
		}

		if (toDescribe == null)
		{
			// No table, view, procedure, trigger or something similar found
			result.setFailure();
			String msg = ResourceMgr.getFormattedString("ErrTableOrViewNotFound", objectName);
			result.addMessage(msg);
			return result;
		}

		DataStore details = null;
		if (connection.getMetadata().objectTypeCanContainData(toDescribe.getType()))
		{
			TableDefinition def = connection.getMetadata().getTableDefinition(toDescribe);
			connection.getObjectCache().addTable(def);
			details = new TableColumnsDatastore(def);
		}
		else
		{
			details = connection.getMetadata().getObjectDetails(toDescribe);
		}
		boolean isExtended = connection.getMetadata().isExtendedObject(toDescribe);

		CharSequence source = null;
		String displayName = null;

		if (synonymTarget != null && dbs.isViewType(synonymTarget.getType()))
		{
			source = connection.getMetadata().getViewReader().getExtendedViewSource(synonymTarget, false);
			displayName = synonymTarget.getTableExpression();
		}
		else if (dbs.isViewType(toDescribe.getType()))
		{
			TableDefinition def = connection.getMetadata().getTableDefinition(toDescribe);
			source = connection.getMetadata().getViewReader().getExtendedViewSource(def, false, false);
			displayName = showSchema ? def.getTable().getTableExpression() : def.getTable().getTableExpression(connection);
		}
		else if (isExtended)
		{
			source = connection.getMetadata().getObjectSource(toDescribe);
			displayName = toDescribe.getObjectName();
		}

		ColumnRemover remover = new ColumnRemover(details);
		DataStore cols = remover.removeColumnsByName(TableColumnsDatastore.JAVA_SQL_TYPE_COL_NAME, "SCALE/SIZE", "PRECISION");
		cols.setResultName(showSchema ? toDescribe.getTableExpression() : toDescribe.getTableExpression(connection));
		result.setSuccess();
		result.addDataStore(cols);

		if (source != null)
		{
			result.addMessage("\n--------[ " + StringUtil.capitalize(toDescribe.getObjectType()) + ": " +  displayName + " ]--------");
			result.addMessage(source.toString().trim());
			result.addMessage("--------");
			result.setSourceCommand(StringUtil.getMaxSubstring(source.toString(), 350, " ... "));
		}
		else if (toDescribe != null && toDescribe.getType().indexOf("TABLE") > -1 && includeDependencies)
		{
			try
			{
				IndexReader idxReader = connection.getMetadata().getIndexReader();
				DataStore index = idxReader != null ? idxReader.getTableIndexInformation(toDescribe) : null;
				if (index != null && index.getRowCount() > 0)
				{
					index.setResultName(toDescribe.getTableName() +  " - " + ResourceMgr.getString("TxtDbExplorerIndexes"));
					result.addDataStore(index);
				}
			}
			catch (Exception e)
			{
				LogMgr.logError("ObjectInfo.getObjectInfo()", "Error retrieving index info for " + toDescribe, e);
			}

			try
			{
				TriggerReader trgReader = TriggerReaderFactory.createReader(connection);
				DataStore triggers = trgReader != null ? trgReader.getTableTriggers(toDescribe) : null;
				if (triggers != null && triggers.getRowCount() > 0)
				{
					triggers.setResultName(toDescribe.getTableName() +  " - " + ResourceMgr.getString("TxtDbExplorerTriggers"));
					result.addDataStore(triggers);
				}
			}
			catch (Exception e)
			{
				LogMgr.logError("ObjectInfo.getObjectInfo()", "Error retrieving triggers for " + toDescribe, e);
			}

			if (connection.getDbSettings().objectInfoWithFK())
			{
				try
				{
					FKHandler fk = FKHandlerFactory.createInstance(connection);
					DataStore references = fk.getForeignKeys(toDescribe, false);
					if (references.getRowCount() > 0)
					{
						references.setResultName(toDescribe.getTableName() +  " - " + ResourceMgr.getString("TxtDbExplorerFkColumns"));
						result.addDataStore(references);
					}
					DataStore referencedBy = fk.getReferencedBy(toDescribe);
					if (referencedBy.getRowCount() > 0)
					{
						referencedBy.setResultName(toDescribe.getTableName() +  " - " + ResourceMgr.getString("TxtDbExplorerReferencedColumns"));
						result.addDataStore(referencedBy);
					}
				}
				catch (Exception e)
				{
					LogMgr.logError("ObjectInfo.getObjectInfo()", "Error retrieving foreign keys for " + toDescribe, e);
				}
			}
		}

		return result;
	}

}
