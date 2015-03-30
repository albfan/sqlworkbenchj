/*
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2016, Thomas Kellerer.
 *
 * Licensed under a modified Apache License, Version 2.0
 * that restricts the use for certain governments.
 * You may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://sql-workbench.net/manual/license.html
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * To contact the author please send an email to: support@sql-workbench.net
 */

package workbench.db.objectcache;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InvalidClassException;
import java.io.NotSerializableException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

import workbench.log.LogMgr;
import workbench.resource.GuiSettings;
import workbench.resource.Settings;

import workbench.db.ColumnIdentifier;
import workbench.db.ConnectionProfile;
import workbench.db.DependencyNode;
import workbench.db.IndexDefinition;
import workbench.db.PkDefinition;
import workbench.db.ProcedureDefinition;
import workbench.db.TableIdentifier;
import workbench.db.WbConnection;

import workbench.util.DurationNumber;
import workbench.util.WbFile;

/**
 *
 * @author Thomas Kellerer
 */
class ObjectCachePersistence
{
	private static final String OBJECTS_ENTRY = "objects.dat";
	private static final String SCHEMAS_ENTRY = "schemas.dat";
	private static final String REFERENCING_TABLES_ENTRY = "referencing_tables.data";
	private static final String REFERENCED_TABLES_ENTRY = "referenced_tables.data";
	private static final String PROCEDURES_ENTRY = "procedures.data";
	private static final String SYNONYMS_ENTRY = "synonyms.data";
	private static final String INDEX_ENTRY = "index.data";
	private static final String PK_ENTRY = "pk.data";

	void loadFromLocalFile(ObjectCache cache, WbConnection conn)
	{
		if (conn == null || cache == null) return;

		WbFile cacheFile = getCacheFile(conn);
		if (cacheFile == null || !cacheFile.exists() || cacheFile.length() == 0) return;

		String maxAgeValue = GuiSettings.getLocalStorageMaxAge();
		DurationNumber number = new DurationNumber();
		long maxAge = number.parseDefinition(maxAgeValue);

		long age = System.currentTimeMillis() - cacheFile.lastModified();
		if (age >= maxAge || age < 0)
		{
			LogMgr.logInfo("ObjectCachePersistence.loadFromLocalFile()", "Cache file " + cacheFile.getFullPath() + " was discarded because it is too old (max. age=" + maxAgeValue + ").");
			cacheFile.delete();
			return;
		}

		ZipFile zipFile = null;
		Map<TableIdentifier, List<ColumnIdentifier>> objects = null;
		Set<String> schemas = null;
		Map<TableIdentifier, List<DependencyNode>> referencedTables = null;
		Map<TableIdentifier, List<DependencyNode>> referencingTables = null;
		Map<String, List<ProcedureDefinition>> procs = null;
		Map<TableIdentifier, TableIdentifier> syns = null;
		Map<TableIdentifier, List<IndexDefinition>> indexes = null;
		Map<TableIdentifier, PkDefinition> pk = null;
		try
		{
			zipFile = new ZipFile(cacheFile);
			ZipEntry entry = zipFile.getEntry(OBJECTS_ENTRY);
			InputStream in = zipFile.getInputStream(entry);
			ObjectInputStream ois = new ObjectInputStream(in);
			objects = (Map<TableIdentifier, List<ColumnIdentifier>>)ois.readObject();

			entry = zipFile.getEntry(SCHEMAS_ENTRY);
			in = zipFile.getInputStream(entry);
			ois = new ObjectInputStream(in);
			schemas = (Set<String>)ois.readObject();

			entry = zipFile.getEntry(REFERENCED_TABLES_ENTRY);
			if (entry != null)
			{
				in = zipFile.getInputStream(entry);
				ois = new ObjectInputStream(in);
				referencedTables = (Map<TableIdentifier, List<DependencyNode>>) ois.readObject();
			}

			entry = zipFile.getEntry(REFERENCING_TABLES_ENTRY);
			if (entry != null)
			{
				in = zipFile.getInputStream(entry);
				ois = new ObjectInputStream(in);
				referencingTables = (Map<TableIdentifier, List<DependencyNode>>) ois.readObject();
			}

			entry = zipFile.getEntry(PROCEDURES_ENTRY);
			if (entry != null)
			{
				in = zipFile.getInputStream(entry);
				ois = new ObjectInputStream(in);
				procs = (Map<String, List<ProcedureDefinition>>) ois.readObject();
			}

			entry = zipFile.getEntry(SYNONYMS_ENTRY);
			if (entry != null)
			{
				in = zipFile.getInputStream(entry);
				ois = new ObjectInputStream(in);
				syns = (Map<TableIdentifier, TableIdentifier>)ois.readObject();
			}

			entry = zipFile.getEntry(INDEX_ENTRY);
			if (entry != null)
			{
				in = zipFile.getInputStream(entry);
				ois = new ObjectInputStream(in);
				indexes = (Map<TableIdentifier, List<IndexDefinition>>)ois.readObject();
			}

			entry = zipFile.getEntry(PK_ENTRY);
			if (entry != null)
			{
				in = zipFile.getInputStream(entry);
				ois = new ObjectInputStream(in);
				pk = (Map<TableIdentifier, PkDefinition>)ois.readObject();
			}

			cache.initExternally(objects, schemas, referencedTables, referencingTables, procs, syns, indexes, pk);
		}
		catch (Throwable th)
		{
			LogMgr.logError("ObjectCachePersistence.loadFromCache()", "Could not read local storage", th);
			// Something went wrong during loading. Consider the cache file corrupt and delete it
			cacheFile.delete();
		}
		finally
		{
			try
			{
				if (zipFile != null) zipFile.close();
			}
			catch (Throwable th)
			{
				LogMgr.logError("ObjectCachePersistence.loadFromCache()", "Could not close local storage file", th);
			}
		}
	}


	void deleteCacheFile(String jdbcUrl, String userName)
	{
		WbFile cacheFile = getCacheFile(jdbcUrl, userName);
		try
		{
			if (cacheFile.delete())
			{
				LogMgr.logInfo("ObjectCachePersistence.deleteCacheFile()", "Deleted local storage file: " + cacheFile.getFullPath());
			}
		}
		catch (Throwable th)
		{
		}
	}

	void saveToLocalFile(ObjectCache objectCache, WbConnection conn)
	{
		if (conn == null || objectCache == null) return;

		WbFile cacheFile = getCacheFile(conn);

		OutputStream out = null;
		ZipOutputStream zout = null;

		try
		{
			out = new FileOutputStream(cacheFile);
			zout = new ZipOutputStream(out);
			zout.setLevel(9);

			Map<TableIdentifier, List<ColumnIdentifier>> objects = objectCache.getObjects();
			writeObject(zout, OBJECTS_ENTRY, objects);

			Set<String> schemas = objectCache.getSchemasInCache();
			writeObject(zout, SCHEMAS_ENTRY, schemas);

			Map<TableIdentifier, List<DependencyNode>> referencedTables = objectCache.getReferencedTables();
			writeObject(zout, REFERENCED_TABLES_ENTRY, referencedTables);

			Map<TableIdentifier, List<DependencyNode>> referencingTables = objectCache.getReferencingTables();
			writeObject(zout, REFERENCING_TABLES_ENTRY, referencingTables);

			Map<String, List<ProcedureDefinition>> procs = objectCache.getProcedures();
			writeObject(zout, PROCEDURES_ENTRY, procs);

			Map<TableIdentifier, TableIdentifier> syns = objectCache.getSynonyms();
			writeObject(zout, SYNONYMS_ENTRY, syns);

			Map<TableIdentifier, List<IndexDefinition>> indexes = objectCache.getIndexes();
			writeObject(zout, INDEX_ENTRY, indexes);

			Map<TableIdentifier, PkDefinition> pkMap = objectCache.getPKMap();
			writeObject(zout, PK_ENTRY, pkMap);
		}
		catch (Exception ex)
		{
			LogMgr.logError("ObjectCachePersistence.saveCache()", "Could not write cache file", ex);
			cacheFile.delete();
		}
		finally
		{
			try
			{
				zout.close();
			}
			catch (Exception io)
			{
				LogMgr.logError("ObjectCachePersistence.saveCache()", "Could not close ZIP archive", io);
				cacheFile.delete();
			}
		}
		LogMgr.logDebug("ObjectCachePersistence.saveCache()", "Current object cache written to: " + cacheFile.getFullPath());
	}

	private void writeObject(ZipOutputStream zout, String filename, Object toWrite)
		throws IOException
	{
		try
		{
			zout.putNextEntry(new ZipEntry(filename));
			ObjectOutputStream oos = new ObjectOutputStream(zout);
			oos.writeObject(toWrite);
			zout.closeEntry();
		}
		catch (InvalidClassException | NotSerializableException ice)
		{
			LogMgr.logError("ObjectCachePersistence.writeObject()", "Could not write cache entry: " + filename, ice);
		}
	}

	private WbFile getCacheFile(WbConnection dbConnection)
	{
		return getCacheFile(dbConnection.getUrl(), dbConnection.getDisplayUser());
	}

	private WbFile getCacheFile(String jdbcUrl, String userName)
	{
		String fileName = ConnectionProfile.makeFilename(jdbcUrl, userName);
		WbFile configDir = new WbFile(Settings.getInstance().getConfigDir());
		String cacheDirName = Settings.getInstance().getProperty(GuiSettings.PROP_LOCAL_OBJECT_CACHE_DIR, ".cache");
		WbFile cDir = new WbFile(cacheDirName);
		WbFile cacheDir = null;

		if (cDir.isAbsolute())
		{
			cacheDir = cDir;
		}
		else
		{

			cacheDir = new WbFile(configDir, cacheDirName);
		}

		if (!cacheDir.exists())
		{
			if (cacheDir.mkdirs())
			{
				LogMgr.logInfo("ObjectCachePersistence.getCacheFile()", "Created cache directory for local cache storage: " + cacheDir.getFullPath());
			}
			else
			{
				LogMgr.logWarning("ObjectCachePersistence.getCacheFile()", "Could not create cache directory \"" + cacheDir.getFullPath() + "\". Using config directory: " + configDir.getFullPath());
				cacheDir = configDir;
			}
		}
		return new WbFile(cacheDir, fileName + ".wbcache");
	}
}
