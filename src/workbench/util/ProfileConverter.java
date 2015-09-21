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
package workbench.util;

import java.util.List;

import workbench.db.ConnectionProfile;
import workbench.db.IniProfileStorage;
import workbench.db.ProfileStorage;
import workbench.db.XmlProfileStorage;

/**
 *
 * @author Thomas Kellerer
 */
public class ProfileConverter
{
  public static void main(String[] args)
  {
    if (args.length != 1)
    {
      System.err.println("Usage: ProfileConverter inputfile");
      System.exit(1);
    }

    WbFile in = new WbFile(args[0]);

    if (!in.exists())
    {
      System.out.println("File " + in.getFullPath() + " not found!");
      System.exit(2);
    }

    WbFile out = null;
    ProfileStorage reader = null;
    ProfileStorage writer = null;

    if (in.getExtension().equalsIgnoreCase(IniProfileStorage.EXTENSION))
    {
      out = new WbFile(in.getParentFile(), XmlProfileStorage.DEFAULT_FILE_NAME);
      reader = new IniProfileStorage();
      writer = new XmlProfileStorage();
    }
    else
    {
      out = new WbFile(in.getParentFile(), IniProfileStorage.DEFAULT_FILE_NAME);
      reader = new XmlProfileStorage();
      writer = new IniProfileStorage();
    }

    if (out.exists())
    {
      out.makeBackup();
    }

    System.out.println("Converting " + in.getFullPath() + " to " + out.getFullPath());

    List<ConnectionProfile> profiles = reader.readProfiles(in.getFullPath());
    writer.saveProfiles(profiles, out.getFullPath());
  }

}
