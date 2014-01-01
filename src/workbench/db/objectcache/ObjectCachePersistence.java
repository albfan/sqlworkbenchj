/*
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2014, Thomas Kellerer.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
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
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

import workbench.log.LogMgr;
import workbench.resource.GuiSettings;
import workbench.resource.Settings;

import workbench.db.ColumnIdentifier;
import workbench.db.DependencyNode;
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
	private static final String PROP_MAXAGE = GuiSettings.PROP_OBJECT_CACHE_LOCAL_STORAGE + ".maxage";
	private static final String PROP_CACHEDIR = GuiSettings.PROP_OBJECT_CACHE_LOCAL_STORAGE + ".cachedir";

	private static final String OBJECTS_ENTRY = "objects.dat";
	private static final String SCHEMAS_ENTRY = "schemas.dat";
	private static final String REFERENCING_TABLES_ENTRY = "referencing_tables.data";
	private static final String REFERENCED_TABLES_ENTRY = "referenced_tables.data";

	void loadFromLocalFile(ObjectCache cache, WbConnection conn)
	{
		if (conn == null || cache == null) return;

		WbFile cacheFile = getCacheFile(conn);
		if (cacheFile == null || !cacheFile.exists()) return;

		String maxAgeValue = Settings.getInstance().getProperty(PROP_MAXAGE, "5d");
		DurationNumber number = new DurationNumber();
		long maxAge = number.parseDefinition(maxAgeValue);

		long age = System.currentTimeMillis() - cacheFile.lastModified();
		if (age >= maxAge || age < 0)
		{
			LogMgr.logInfo("ObjectCachePersistence.loadFromLocalFile()", "Cache file " + cacheFile.getFullPath() + " was discarded because it is too old (max. age=" + maxAgeValue + ").");
			cacheFile.delete();
		}

		ZipFile zipFile = null;
		Map<TableIdentifier, List<ColumnIdentifier>> objects = null;
		Set<String> schemas = null;
		Map<TableIdentifier, List<DependencyNode>> referencedTables = null;
		Map<TableIdentifier, List<DependencyNode>> referencingTables = null;

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
			cache.initExternally(objects, schemas, referencedTables, referencingTables);
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
				zipFile.close();
			}
			catch (Throwable th)
			{
				LogMgr.logError("ObjectCachePersistence.loadFromCache()", "Could not close local storage file", th);
			}
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
			zout.putNextEntry(new ZipEntry(OBJECTS_ENTRY));
			ObjectOutputStream oos = new ObjectOutputStream(zout);
			oos.writeObject(objects);
			zout.closeEntry();

			Set<String> schemas = objectCache.getSchemasInCache();
			zout.putNextEntry(new ZipEntry(SCHEMAS_ENTRY));
			oos = new ObjectOutputStream(zout);
			oos.writeObject(schemas);
			zout.closeEntry();

			Map<TableIdentifier, List<DependencyNode>> referencedTables = objectCache.getReferencedTables();
			zout.putNextEntry(new ZipEntry(REFERENCED_TABLES_ENTRY));
			oos = new ObjectOutputStream(zout);
			oos.writeObject(referencedTables);
			zout.closeEntry();

			Map<TableIdentifier, List<DependencyNode>> referencingTables = objectCache.getReferencingTables();
			zout.putNextEntry(new ZipEntry(REFERENCING_TABLES_ENTRY));
			oos = new ObjectOutputStream(zout);
			oos.writeObject(referencingTables);
			zout.closeEntry();
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

	private WbFile getCacheFile(WbConnection dbConnection)
	{
		Pattern invalidChars = Pattern.compile("[^a-zA-Z0-9$]+");
		Matcher urlMatcher = invalidChars.matcher(dbConnection.getUrl());
		String url = urlMatcher.replaceAll("_");

		// remove the jdbc_ prefix, it is not needed
		url = url.substring(5);

		Matcher userMatcher = invalidChars.matcher(dbConnection.getDisplayUser());
		String user = userMatcher.replaceAll("_");

		WbFile configDir = new WbFile(Settings.getInstance().getConfigDir());
		String cacheDirName = Settings.getInstance().getProperty(PROP_CACHEDIR, "cache");
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
		return new WbFile(cacheDir, user + "@" + url + ".wbcache");
	}
}
