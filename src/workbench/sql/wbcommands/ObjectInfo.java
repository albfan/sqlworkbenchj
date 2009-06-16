/*
 * ObjectInfo.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2009, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.sql.wbcommands;

import java.sql.DatabaseMetaData;
import java.sql.SQLException;
import workbench.db.DbSettings;
import workbench.db.FKHandler;
import workbench.db.ProcedureDefinition;
import workbench.db.ProcedureReader;
import workbench.db.TableColumnsDatastore;
import workbench.db.TableDefinition;
import workbench.db.TableIdentifier;
import workbench.db.TriggerReader;
import workbench.db.WbConnection;
import workbench.resource.ResourceMgr;
import workbench.sql.StatementRunnerResult;
import workbench.storage.ColumnRemover;
import workbench.storage.DataStore;
import workbench.util.StringUtil;

/**
 * Retrieves information about a database object.
 *
 * @author support@sql-workbench.net
 */
public class ObjectInfo
{

	public ObjectInfo()
	{
	}

	/**
	 * Tries to find the definition of the object identified by the given name.
	 *
	 * If it's a TABLE (or something similar) the reuslt will contain several
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

		TableIdentifier toDescribe = connection.getMetadata().findSelectableObject(tbl);

		if (toDescribe == null)
		{
			// No table or something similar found, try to find a procedure with that name
			ProcedureReader reader = connection.getMetadata().getProcedureReader();
			tbl.adjustCase(connection);
			ProcedureDefinition def = new ProcedureDefinition(tbl.getCatalog(), tbl.getSchema(), tbl.getObjectName(), DatabaseMetaData.procedureResultUnknown);
			if (reader.procedureExists(def))
			{
				CharSequence source = def.getSource(connection);
				result.addMessage("--------[ " + def.getObjectType()  + ": " + def.getObjectExpression(connection) + " ]--------");
				result.addMessage(source);
				result.addMessage("--------");
				result.setSuccess();
				return result;
			}

			// No procedure found, try to find a trigger.
			TriggerReader trgReader = new TriggerReader(connection);
			String source = trgReader.getTriggerSource(tbl.getCatalog(), tbl.getSchema(), tbl.getObjectName());
			if (StringUtil.isNonBlank(source))
			{
				result.addMessage("--------[ TRIGGER: " + tbl.getObjectName() + " ]--------");
				result.addMessage(source);
				result.addMessage("--------");
				result.setSuccess();
				return result;
			}
		}

		if (toDescribe == null)
		{
			// No table, view, procedure, trigger or something similar found
			result.setFailure();
			String msg = ResourceMgr.getString("ErrTableOrViewNotFound");
			msg = msg.replace("%name%", objectName);
			result.addMessage(msg);
			return result;
		}

		TableDefinition def = connection.getMetadata().getTableDefinition(toDescribe);
		DataStore ds = new TableColumnsDatastore(def);

		DbSettings dbs = connection.getDbSettings();

		if (dbs.isSynonymType(toDescribe.getType()))
		{
			TableIdentifier target = connection.getMetadata().getSynonymTable(toDescribe);
			if (target != null)
			{
				result.addMessage(toDescribe.getTableExpression(connection) + " --> " + target.getTableExpression(connection));
			}
		}

		CharSequence viewSource = null;
		if (dbs.isViewType(toDescribe.getType()))
		{
			viewSource = connection.getMetadata().getViewReader().getExtendedViewSource(def, false, false);
		}

		ColumnRemover remover = new ColumnRemover(ds);
		DataStore cols = remover.removeColumnsByName("java.sql.Types", "SCALE/SIZE", "PRECISION");
		cols.setResultName(toDescribe.getTableName());
		result.setSuccess();
		result.addDataStore(cols);

		if (viewSource != null)
		{
			result.addMessage("--------[ " + toDescribe.getObjectType()  + ": " + toDescribe.getObjectExpression(connection) + " ]--------");
			result.addMessage(viewSource);
			result.addMessage("--------");
		}
		else if (toDescribe.getType().indexOf("TABLE") > -1 && includeDependencies)
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
