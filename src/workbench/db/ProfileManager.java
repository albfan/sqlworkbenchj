/*
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2016 Thomas Kellerer.
 *
 * Licensed under a modified Apache License, Version 2.0 (the "License")
 * that restricts the use for certain governments.
 * You may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.sql-workbench.net/manual/license.html
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

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import workbench.log.LogMgr;
import workbench.resource.Settings;

import workbench.gui.profiles.ProfileKey;

import workbench.util.CaseInsensitiveComparator;
import workbench.util.WbFile;


/**
 *
 * @author Thomas Kellerer
 */
public class ProfileManager
{
  private boolean loaded = false;
  private boolean profilesDeleted = false;
  private List<ConnectionProfile> profiles = new ArrayList<>();
  private WbFile currentFile;

  public ProfileManager(String filename)
  {
    currentFile = new WbFile(filename);
  }

  public ProfileManager(File file)
  {
    currentFile = new WbFile(file);
  }

  /**
   * Find a connection profile identified by the given key.
   *
   * @param key the key of the profile
   * @return a connection profile with that name or null if none was found.
   */
  public ConnectionProfile getProfile(ProfileKey key)
  {
    if (key == null) return null;
    return findProfile(profiles, key);
  }

  /**
   * Return a list with profile keys that can be displayed to the user.
   *
   * @return all profiles keys (sorted).
   */
  public List<String> getProfileKeys()
  {
    List<String> result = new ArrayList(profiles.size());
    for (ConnectionProfile profile : profiles)
    {
      result.add(profile.getKey().toString());
    }
    result.sort(CaseInsensitiveComparator.INSTANCE);
    return result;
  }


  public List<ConnectionProfile> getProfiles()
  {
    return Collections.unmodifiableList(this.profiles);
  }

  public void ensureLoaded()
  {
    if (loaded == false)
    {
      readProfiles();
    }
  }

  public void load()
  {
    readProfiles();
  }

  /**
   * Save the connectioin profiles to an external file.
   *
   * This will also resetChangedFlags the changed flag for any modified or new
 profiles. The name of the file defaults to <tt>WbProfiles.xml</tt>, but
   * can be defined in the configuration properties.
   *
   * @see workbench.resource.Settings#getProfileStorage()
   * @see #getFile()
   * @see ProfileStorage#saveProfiles(java.util.List, workbench.util.WbFile)
   */
  public void save()
  {
    if (Settings.getInstance().getCreateProfileBackup())
    {
      Settings.createBackup(getFile());
    }
    ProfileStorage handler = getStorageHandler();
    handler.saveProfiles(profiles, getFile());
    resetChangedFlags();
  }


  private void readProfiles()
  {
    ProfileStorage reader = getStorageHandler();
    WbFile f = getFile();

    if (f.exists())
    {
      long start = System.currentTimeMillis();
      LogMgr.logTrace("ProfileManager.readProfiles()", "readProfiles() called at " + start + " from " + Thread.currentThread().getName());

      profiles = reader.readProfiles(f);

      long duration = System.currentTimeMillis() - start;
      LogMgr.logDebug("ProfileManager.readProfiles()", profiles.size() + " profiles loaded in " + duration + "ms");
    }
    else
    {
      LogMgr.logDebug("ProfileManager.readProfiles()", f + " not found. Creating new one.");
    }

    if (profiles == null)
    {
      // first time start, or empty config dir
      profiles = new ArrayList<>();
    }

    resetChangedFlags();
    loaded = true;
  }


  /**
   * Reset the changed status on the profiles.
   *
   * Called after saving the profiles.
   */
  private void resetChangedFlags()
  {
    for (ConnectionProfile profile : this.profiles)
    {
      profile.resetChangedFlags();
    }
    profilesDeleted = false;
  }

  public void reset(String newStorageFile)
  {
    LogMgr.logDebug("ProfileManager.reset()", "Using new profile storage: " + newStorageFile);
    loaded = false;
    profiles.clear();
    profilesDeleted = false;
    currentFile = new WbFile(newStorageFile);
  }

  public String getProfilesPath()
  {
    return getFile().getFullPath();
  }

  public WbFile getFile()
  {
    return currentFile;
  }

  private ProfileStorage getStorageHandler()
  {
    return ProfileStorage.Factory.getStorageHandler(getFile());
  }

  public boolean isLoaded()
  {
    return loaded;
  }

  /**
   * Returns true if any of the profile definitions has changed.
   * (Or if a profile has been deleted or added)
   *
   * @return true if at least one profile has been changed, deleted or added
   */
  public boolean profilesAreModified()
  {
    if (profiles == null) return false;
    if (profilesDeleted) return true;

    for (ConnectionProfile profile : this.profiles)
    {
      if (profile.isChanged())
      {
        return true;
      }
    }
    return false;
  }

  public void applyProfiles(List<ConnectionProfile> newProfiles)
  {
    if (newProfiles == null) return;

    for (ConnectionProfile profile : profiles)
    {
      if (!newProfiles.contains(profile))
      {
        profilesDeleted = true;
        break;
      }
    }

    this.profiles.clear();

    for (ConnectionProfile profile : newProfiles)
    {
      this.profiles.add(profile.createStatefulCopy());
    }
  }

  public void addProfile(ConnectionProfile aProfile)
  {
    this.profiles.remove(aProfile);
    this.profiles.add(aProfile);
  }

  public void removeProfile(ConnectionProfile aProfile)
  {
    this.profiles.remove(aProfile);

    // deleting a new profile should not change the status to "modified"
    if (!aProfile.isNew())
    {
      this.profilesDeleted = true;
    }
  }

  public static ConnectionProfile findProfile(List<ConnectionProfile> list, ProfileKey key)
  {
    if (key == null) return null;
    if (list == null) return null;

    String name = key.getName();
    String group = key.getGroup();

    ConnectionProfile firstMatch = null;
    for (ConnectionProfile prof : list)
    {
      if (name.equalsIgnoreCase(prof.getName().trim()))
      {
        if (firstMatch == null) firstMatch = prof;
        if (group == null)
        {
          return prof;
        }
        else if (group.equalsIgnoreCase(prof.getGroup().trim()))
        {
          return prof;
        }
      }
    }
    return firstMatch;
  }

}
