/*
 * GenericDiffLoader.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2016, Thomas Kellerer
 *
 * Licensed under a modified Apache License, Version 2.0
 * that restricts the use for certain governments.
 * You may not use this file except in compliance with the License.
 * You may obtain a copy of the License at.
 *
 *     http://sql-workbench.net/manual/license.html
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
package workbench.db.diff;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import workbench.log.LogMgr;
import workbench.resource.ResourceMgr;

import workbench.db.ComparableDbObject;
import workbench.db.DbMetadata;
import workbench.db.DbObject;
import workbench.db.TableIdentifier;
import workbench.db.WbConnection;

import workbench.storage.RowActionMonitor;

import workbench.util.StringUtil;

/**
 *
 * @author Thomas Kellerer
 */
public class GenericDiffLoader
{
	private WbConnection referenceDb;
	private WbConnection targetDb;
	private String referenceSchema;
	private String targetSchema;
	private List<ObjectDiff> objects;
	private String[] objectTypes;
	private RowActionMonitor progressMonitor;
	private boolean cancelProcessing;

	public GenericDiffLoader(WbConnection reference, WbConnection target, String refSchema, String targetSchema, String[] types)
	{
		this.referenceDb = reference;
		this.targetDb = target;
		this.referenceSchema = refSchema;
		this.targetSchema = targetSchema;
		this.objectTypes = types;
	}

	public void setProgressMonitor(RowActionMonitor monitor)
	{
		this.progressMonitor = monitor;
	}

	public int getObjectCount()
	{
		if (objects == null) return 0;
		return objects.size();
	}

	public StringBuilder getMigrateTargetXml(StringBuilder indent)
	{
		if (this.objects == null)
		{
			loadObjects();
		}
		StringBuilder result = new StringBuilder(objects.size() * 50);
		for (ObjectDiff diff : objects)
		{
			diff.setIndent(indent);
			StringBuilder xml = diff.getMigrateTargetXml(referenceDb, targetDb);
			result.append(xml);
			result.append('\n');
		}
		return result;
	}

	public void cancel()
	{
		this.cancelProcessing = true;
	}

	public void loadObjects()
	{
		cancelProcessing = false;
		objects = new ArrayList<>();
		String msg = ResourceMgr.getString("MsgProcessObject") + ": ";

		try
		{
			List<TableIdentifier> refObjects = referenceDb.getMetadata().getObjectList(referenceSchema, objectTypes);
			List<TableIdentifier> targetObjects = targetDb.getMetadata().getObjectList(targetSchema, objectTypes);

			if (this.progressMonitor != null)
			{
				this.progressMonitor.setMonitorType(RowActionMonitor.MONITOR_PLAIN);
			}

			for (TableIdentifier tbl : refObjects)
			{
				if (cancelProcessing) break;

				if (this.progressMonitor != null)
				{
					this.progressMonitor.setCurrentObject(msg + tbl.getObjectName(), -1, -1);
				}

				DbObject ref = referenceDb.getMetadata().getObjectDefinition(tbl);

				if (ref instanceof ComparableDbObject)
				{
					ComparableDbObject refObj = (ComparableDbObject)ref;
					ComparableDbObject tgObj = null;
					TableIdentifier tgTbl = findInList(targetObjects, ref);
					if (tgTbl != null)
					{
						DbObject details = targetDb.getMetadata().getObjectDefinition(tgTbl);
						if (details instanceof ComparableDbObject)
						{
							tgObj = (ComparableDbObject)details;
						}
					}
					ObjectDiff diff = new ObjectDiff(refObj, tgObj, targetSchema);
					if (diff.isDifferent(referenceDb, targetDb))
					{
						objects.add(diff);
					}
				}
			}
			if (cancelProcessing) return;

			for (TableIdentifier tg : targetObjects)
			{
				if (cancelProcessing) break;

				TableIdentifier ref = findInList(refObjects, tg);
				if (ref == null)
				{
					DbObject details = targetDb.getMetadata().getObjectDefinition(tg);
					if (details instanceof ComparableDbObject)
					{
						// a null reference object means the target object needs to be dropped
						objects.add(new ObjectDiff(null, (ComparableDbObject)details, targetSchema));
					}
				}
			}
		}
		catch (SQLException sql)
		{
			LogMgr.logError("GenericDiffLoader.loadObjects()", "Could not load database objects", sql);
		}
	}

	private TableIdentifier findInList(List<TableIdentifier> objects, DbObject toFind)
	{
		if (objects == null) return null;
		if (toFind == null) return null;

		for (TableIdentifier tbl : objects)
		{
			if (StringUtil.equalStringIgnoreCase(toFind.getObjectName(), tbl.getObjectName())) return tbl;
		}
		return null;
	}

	public List<DbObject> getObjectDefinitions(WbConnection conn, String schema, String[] types)
		throws SQLException
	{
		if (conn == null) return Collections.emptyList();
		DbMetadata meta = conn.getMetadata();
		if (meta == null) return Collections.emptyList();

		List<TableIdentifier> objList = meta.getObjectList(schema, types);
		int count = objList.size();

		List<DbObject> dbos = new ArrayList<>(count);
		for (TableIdentifier tbl : objList)
		{
			DbObject dbo = meta.getObjectDefinition(tbl);
			dbos.add(dbo);
		}
		return dbos;
	}
}
