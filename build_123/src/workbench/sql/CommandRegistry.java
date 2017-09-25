/*
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2017, Thomas Kellerer.
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
package workbench.sql;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import workbench.interfaces.ToolWindow;
import workbench.log.LogMgr;

import workbench.util.ClassFinder;
import workbench.util.StringUtil;

/**
 *
 * @author Thomas Kellerer
 */
public class CommandRegistry
{
  private static final String PACKAGE_NAME = "workbench.extensions";
  private final List<Class> commands = new ArrayList<>();
  private final List<Class> guiExtensions = new ArrayList<>();
  private List<String> verbs;

  /**
   * Thread safe singleton-instance
   */
  protected static class LazyInstanceHolder
  {
    protected static final CommandRegistry instance = new CommandRegistry();
  }

  public static CommandRegistry getInstance()
  {
    return LazyInstanceHolder.instance;
  }

  private CommandRegistry()
  {
  }

  public synchronized List<String> getVerbs()
  {
    if (verbs == null)
    {
      initVerbs();
    }
    return Collections.unmodifiableList(verbs);
  }

  private void initVerbs()
  {
    List<SqlCommand> cmdList = getCommands();
    verbs = new ArrayList<>(cmdList.size());
    for (SqlCommand cmd : cmdList)
    {
      verbs.addAll(cmd.getAllVerbs());
    }
  }

  public List<SqlCommand> getCommands()
  {
    List<SqlCommand> result = new ArrayList<>(commands.size());
    for (Class clz : commands)
    {
      try
      {
        SqlCommand cmd = (SqlCommand)clz.newInstance();
        result.add(cmd);
      }
      catch (Throwable th)
      {
        LogMgr.logError("CommandRegistry.getCommands()", "Could not create instance of: " + clz.getCanonicalName(), th);
      }
    }
    return result;
  }

  public synchronized void scanForExtensions()
  {
    long start = System.currentTimeMillis();
    commands.clear();
    try
    {
      List<Class> classes = ClassFinder.getClasses(PACKAGE_NAME);
      for (Class cls : classes)
      {
        LogMgr.logDebug("CommandRegistry.scanForExtensions()", "Found class " + cls.getName());
        if (SqlCommand.class.isAssignableFrom(cls))
        {
          commands.add(cls);
        }
      }
      long duration = System.currentTimeMillis() - start;
      LogMgr.logDebug("CommandRegistry.scanForExtensions()", "Found " + commands.size() + " commands in " + duration + "ms");
    }
    catch (Exception ex)
    {
      LogMgr.logWarning("CommandRegistry.scanForExtensions", "Error when scanning for exentensions", ex);
    }
  }

  public ToolWindow getGuiExtension(String name)
  {
    if (StringUtil.isEmptyString(name))
    {
      return null;
    }
    else if (!name.startsWith(PACKAGE_NAME))
    {
      name = PACKAGE_NAME + "." + name;
    }

    ToolWindow gui = null;
    for (Class clz : guiExtensions)
    {
      try
      {
        if (name.equals(clz.getCanonicalName()))
        {
          gui = (ToolWindow)clz.newInstance();
          break;
        }
      }
      catch (Throwable th)
      {
        LogMgr.logError("CommandRegistry.getGuiExtension()", "Could not create instance of: " + clz.getCanonicalName(), th);
      }
    }
    return gui;
  }

  public synchronized void scanForGuiExtensions()
  {
    long start = System.currentTimeMillis();
    guiExtensions.clear();
    try
    {
      List<Class> classes = ClassFinder.getClasses(PACKAGE_NAME);
      for (Class cls : classes)
      {
        LogMgr.logDebug("CommandRegistry.scanForGuiExtensions()", "Found class " + cls.getName());
        if (ToolWindow.class.isAssignableFrom(cls))
        {
          guiExtensions.add(cls);
        }
      }
      long duration = System.currentTimeMillis() - start;
      LogMgr.logDebug("CommandRegistry.scanForGuiExtensions()", "Found " + guiExtensions.size() + " commands in " + duration + "ms");
    }
    catch (Exception ex)
    {
      LogMgr.logWarning("CommandRegistry.scanForGuiExtensions", "Error when scanning for exentensions", ex);
    }
  }

}
