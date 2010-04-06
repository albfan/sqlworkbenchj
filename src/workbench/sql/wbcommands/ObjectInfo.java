/*
 * ObjectInfo.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2010, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.sql.wbcommands;

import java.sql.SQLException;
import java.util.List;
import workbench.db.DbObject;
import workbench.db.DbSettings;
import workbench.db.FKHandler;
import workbench.db.ProcedureDefinition;
import workbench.db.ProcedureReader;
import workbench.db.SequenceDefinition;
import workbench.db.SequenceReader;
import workbench.db.TableColumnsDatastore;
import workbench.db.TableDefinition;
import workbench.db.TableIdentifier;
import workbench.db.TriggerDefinition;
import workbench.db.TriggerReader;
import workbench.db.WbConnection;
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
	 * @param includeDependencies if true dependen objects (e.g. indexes, constraints) are retrieved as well
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

		TableIdentifier toDescribe = connection.getMetadata().findObject(tbl);
		if (toDescribe == null)
		{
			// try again with the same case as entered by the user
			toDescribe = connection.getMetadata().findObject(tbl, false);
		}

		DbSettings dbs = connection.getDbSettings();
		TableIdentifier synonymTarget = null;
		if (toDescribe != null && dbs.isSynonymType(toDescribe.getType()))
		{
			synonymTarget = connection.getMetadata().getSynonymTable(toDescribe);
			if (synonymTarget != null)
			{
				String msg = 
					"--------[ " + StringUtil.capitalize(toDescribe.getType()) + ": " + toDescribe.getTableName() + " ]--------\n" +
					toDescribe.getObjectExpression(connection) + " --> " +
					synonymTarget.getTableExpression(connection) + " (" +
					synonymTarget.getObjectType() + ")";

				result.addMessage(msg + "\n");
				result.setSourceCommand(msg);
			}
			toDescribe = synonymTarget;
		}

		if (toDescribe != null || "SEQUENCE".equals(toDescribe.getType()))
		{
			SequenceReader seqReader = connection.getMetadata().getSequenceReader();
			if (seqReader != null)
			{
				String schema = (toDescribe == null ? tbl.getSchema() : toDescribe.getSchema());
				String name = (toDescribe == null ? tbl.getObjectName() : toDescribe.getObjectName());
				SequenceDefinition seq = seqReader.getSequenceDefinition(schema, name);
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

			// No procedure found, try to find a trigger.
			TriggerReader trgReader = new TriggerReader(connection);
			TriggerDefinition trg = trgReader.findTrigger(tbl.getCatalog(), tbl.getSchema(), tbl.getObjectName());
			String source = null;
			if (trg != null)
			{
				source = trgReader.getTriggerSource(trg);
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

		DbObject extendedObject = null;
		if (toDescribe == null)
		{
			extendedObject = connection.getMetadata().getObjectDefinition(new TableIdentifier(objectName));
		}

		if (toDescribe == null && extendedObject == null)
		{
			// No table, view, procedure, trigger or something similar found
			result.setFailure();
			String msg = ResourceMgr.getFormattedString("ErrTableOrViewNotFound", objectName);
			result.addMessage(msg);
			return result;
		}

		DataStore details = null;
		boolean isExtended = false;
		if (extendedObject != null || connection.getMetadata().isExtendedObject(toDescribe))
		{
			isExtended = true;
			details = connection.getMetadata().getExtendedObjectDetails(extendedObject == null ? toDescribe : extendedObject);
		}
		else
		{
			details = connection.getMetadata().getObjectDetails(toDescribe);
		}

		CharSequence source = null;
		String displayName = null;
		String displayType = "View";

		if (synonymTarget != null && dbs.isViewType(synonymTarget.getType()))
		{
			source = connection.getMetadata().getViewReader().getExtendedViewSource(synonymTarget, false);
			displayName = synonymTarget.getObjectExpression(connection);
		}
		else if (toDescribe != null && dbs.isViewType(toDescribe.getType()))
		{
			TableDefinition def = connection.getMetadata().getTableDefinition(toDescribe);
			source = connection.getMetadata().getViewReader().getExtendedViewSource(def, false, false);
			displayName = def.getTable().getObjectExpression(connection);
		}
		else if (isExtended)
		{
			source = connection.getMetadata().getObjectSource(extendedObject == null ? toDescribe : extendedObject);
			displayType = extendedObject == null ? toDescribe.getType() : extendedObject.getObjectType();
			displayName = extendedObject == null ? toDescribe.getObjectName() : extendedObject.getObjectName();
		}

		ColumnRemover remover = new ColumnRemover(details);
		DataStore cols = remover.removeColumnsByName(TableColumnsDatastore.JAVA_SQL_TYPE_COL_NAME, "SCALE/SIZE", "PRECISION");
		cols.setResultName(toDescribe.getTableName());
		result.setSuccess();
		result.addDataStore(cols);

		if (source != null)
		{
			result.addMessage("\n--------[ " + displayType + ": " +  displayName + " ]--------");
			result.addMessage(source.toString().trim());
			result.addMessage("--------");
			result.setSourceCommand(StringUtil.getMaxSubstring(source.toString(), 350, " ... "));
		}
		else if (toDescribe != null && toDescribe.getType().indexOf("TABLE") > -1 && includeDependencies)
		{
			DataStore index = connection.getMetadata().getIndexReader().getTableIndexInformation(toDescribe);
			if (index.getRowCount() > 0)
			{
				index.setResultName(toDescribe.getTableName() +  " - " + ResourceMgr.getString("TxtDbExplorerIndexes"));
				result.addDataStore(index);
			}

			TriggerReader trgReader = new TriggerReader(connection);
			DataStore triggers = trgReader.getTableTriggers(toDescribe);
			if (triggers != null && triggers.getRowCount() > 0)
			{
				triggers.setResultName(toDescribe.getTableName() +  " - " + ResourceMgr.getString("TxtDbExplorerTriggers"));
				result.addDataStore(triggers);
			}

			FKHandler fk = new FKHandler(connection);
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

		return result;
	}

}
