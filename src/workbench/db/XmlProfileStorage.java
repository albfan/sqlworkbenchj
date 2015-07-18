/*
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2015 Thomas Kellerer.
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
package workbench.db;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import workbench.log.LogMgr;

import workbench.util.WbPersistence;

/**
 *
 * @author Thomas Kellerer
 */
public class XmlProfileStorage
  implements ProfileStorage
{

  @Override
  public List<ConnectionProfile> readProfiles(String filename)
  {
    long start = System.currentTimeMillis();
    LogMgr.logTrace("XmlProfileStorage.readProfiles()", "readProfiles() called at " + start + " from " + Thread.currentThread().getName());

    Object result = null;
    try
    {
      LogMgr.logInfo("XmlProfileStorage.readProfiles()", "Loading connection profiles from " + filename);
      WbPersistence reader = new WbPersistence(filename);
      result = reader.readObject();
    }
    catch (FileNotFoundException fne)
    {
      LogMgr.logDebug("XmlProfileStorage.readProfiles()", filename + " not found. Creating new one.");
      result = null;
    }
    catch (Exception e)
    {
      LogMgr.logError("XmlProfileStorage.readProfiles()", "Error when reading connection profiles from " + filename, e);
      result = null;
    }

    List<ConnectionProfile> profiles = new ArrayList<>();

    if (result instanceof Collection)
    {
      Collection c = (Collection)result;
      profiles.addAll(c);
    }
    else if (result instanceof Object[])
    {
      // This is to support the very first version of the profile storage
      // probably obsolete by know, but you never know...
      Object[] l = (Object[])result;
      for (Object prof : l)
      {
        profiles.add((ConnectionProfile)prof);
      }
    }
    long end = System.currentTimeMillis();
    long duration = end - start;
    LogMgr.logTrace("XmlProfileStorage.readProfiles()", "readProfiles() finished at " + end + " (" + duration + "ms) in " + Thread.currentThread().getName());
    return profiles;
  }

  @Override
  public void saveProfiles(List<ConnectionProfile> profiles, String filename)
  {
    WbPersistence writer = new WbPersistence(filename);
    try
    {
      writer.writeObject(profiles);
    }
    catch (IOException e)
    {
      LogMgr.logError("XmlProfileStorage.saveProfiles()", "Error saving profiles to: " + filename, e);
    }
  }

}
