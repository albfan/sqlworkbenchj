/*
 * DbObjectCacheFactory.java
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
package workbench.db.objectcache;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import workbench.log.LogMgr;
import workbench.resource.GuiSettings;

import workbench.db.ConnectionProfile;
import workbench.db.WbConnection;
import workbench.util.CollectionUtil;

/**
 * A factory for DbObjectCache instances.
 *
 * For each unique JDBC URL (including the username) one instance of the cache will be maintained.
 *
 * @author Thomas Kellerer
 */
public class DbObjectCacheFactory
	implements PropertyChangeListener
{
	public static final long CACHE_VERSION_UID = 1L;

	private final Object lock = new Object();

	private final Map<String, ObjectCache> caches = new HashMap<>();
	private final Map<String, Set<String>> refCounter = new HashMap<>();

	/**
	 * Thread safe singleton-instance
	 */
	protected static class LazyInstanceHolder
	{
		protected static final DbObjectCacheFactory instance = new DbObjectCacheFactory();
	}

	public static DbObjectCacheFactory getInstance()
	{
		return LazyInstanceHolder.instance;
	}

	private DbObjectCacheFactory()
	{
	}

	private void loadCache(ObjectCache cache, WbConnection connection)
	{
		if (cache == null || connection == null) return;
		if (useLocalCacheStorage(connection))
		{
			synchronized (lock)
			{
				ObjectCachePersistence persistence = new ObjectCachePersistence();
				persistence.loadFromLocalFile(cache, connection);
			}
		}
	}

	private void saveCache(ObjectCache cache, WbConnection connection)
	{
		if (useLocalCacheStorage(connection))
		{
			ObjectCachePersistence persistence = new ObjectCachePersistence();
			persistence.saveToLocalFile(cache, connection);
		}
	}

	private boolean useLocalCacheStorage(WbConnection connection)
	{
		if (connection == null) return false;
		ObjectCacheStorage storage = GuiSettings.getLocalStorageForObjectCache();

		switch (storage)
		{
			case always:
				return true;
			case never:
				return false;
			case profile:
				ConnectionProfile profile = connection.getProfile();
				if (profile != null)
				{
					return profile.getStoreCacheLocally();
				}
			default:
				return false;
		}
	}

	public DbObjectCache getCache(WbConnection connection)
	{
		if (connection == null) return null;
		String key = makeKey(connection);

		synchronized (lock)
		{
			ObjectCache cache = caches.get(key);
			if (cache == null)
			{
				LogMgr.logDebug("DbObjectCacheFactory.getCache()", "Creating new cache for: " + key);
				cache = new ObjectCache(connection);
				caches.put(key, cache);
			}
			DbObjectCache result = new DbObjectCache(cache, connection);
			connection.addChangeListener(this);
			boolean isUsed = isCacheInUse(key);
			if (!isUsed)
			{
        // first time used, load the local storage
				loadCache(cache, connection);
			}
      increaseRefCount(key, connection.getId());
			return result;
		}
	}

	private int decreaseRefCount(String key, String connectionId)
	{
		Set<String> ids = refCounter.get(key);
		if (ids == null)
		{
			return 0;
		}
		ids.remove(connectionId);
		return ids.size();
	}

	private void increaseRefCount(String key, String connectionId)
	{
		Set<String> ids = refCounter.get(key);
		if (ids == null)
		{
			ids = new HashSet<>();
			refCounter.put(key, ids);
		}
		ids.add(connectionId);
	}

	private boolean isCacheInUse(String key)
	{
		Set<String> ids = refCounter.get(key);
    return CollectionUtil.isNonEmpty(ids);
	}

	private String makeKey(WbConnection connection)
	{
		return connection.getProfile().getLoginUser()+ "@" + connection.getProfile().getUrl();
	}

	/**
	 * Notification about the state of the connection. If the connection
	 * is closed, we can dispose the object cache
	 */
	@Override
	public void propertyChange(PropertyChangeEvent evt)
	{
		if (WbConnection.PROP_CONNECTION_STATE.equals(evt.getPropertyName()) &&
			  WbConnection.CONNECTION_CLOSED.equals(evt.getNewValue()))
		{
			WbConnection conn = (WbConnection)evt.getSource();
			synchronized (lock)
			{
				String key = makeKey(conn);

				ObjectCache cache = caches.get(key);
				int refCount = decreaseRefCount(key, conn.getId());
				LogMgr.logDebug("DbObjectCacheFactory.propertyChange()", "Connection with key=" + key + " was closed. Reference count for this cache is: " + refCount);
				if (cache != null && refCount <= 0)
				{
					saveCache(cache, conn);
					cache.clear();
					caches.remove(key);
					LogMgr.logDebug("DbObjectCacheFactory.propertyChange()", "Removed cache for key=" + key);
				}
				conn.removeChangeListener(this);
			}
		}
	}

	public void clear()
	{
		synchronized(lock)
		{
			for (ObjectCache cache : caches.values())
			{
				cache.clear();
			}
			caches.clear();
		}
	}
}
