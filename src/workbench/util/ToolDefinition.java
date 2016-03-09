/*
 * ToolDefinition.java
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
package workbench.util;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Thomas Kellerer
 */
public class ToolDefinition
{
  private String exePath;
  private String name;
  private String parameters;

  public ToolDefinition()
  {
  }

  public ToolDefinition(String exe, String params, String name)
  {
    setExecutablePath(exe);
    setName(name);
    setParameters(params);
  }

  /**
   * Return any parameter that should be passed to the application when starting it.
   */
  public String getParameters()
  {
    return parameters;
  }

  public final void setParameters(String parameters)
  {
    this.parameters = parameters;
  }

  public String getName()
  {
    return name;
  }

  public final void setName(String appName)
  {
    this.name = appName;
  }

  /**
   * The path to the executable for this tool.
   * Parameters are not part of this.
   *
   * @see #getParameters()
   */
  public String getExecutablePath()
  {
    return exePath;
  }

  /**
   * The command line that should be used to run the external tool. This must not
   * include parameters for the application. They have to be defined through setParameters()
   *
   * The method is invoked through reflection from the {@link workbench.gui.settings.ToolDefinitionPanel}
   * by a {@link workbench.gui.components.StringPropertyEditor}
   *
   * @param path
   * @see #setParameters(java.lang.String)
   */
  public final void setExecutablePath(String path)
  {
    this.exePath = path;
  }

  @Override
  public String toString()
  {
    return getName();
  }

  /**
   * Returns a File object to the executable of this tool
   *
   * @see #getExecutablePath()
   * @see #getParameters()
   */
  public WbFile getExecutable()
  {
    if (this.exePath == null) return null;
    return new WbFile(exePath);
  }

  /**
   * Starts the executable passing any possible arguments to it.
   * If this tool has parameters defined, they will occur before the arguments passed to this method.
   *
   * @param arg additional arguments for the application
   *
   * @throws IOException
   */
  public void runApplication(String arg)
    throws IOException
  {
    List<String> appDef = getComandElements();
    String[] cmd = new String[appDef.size() + 1];
    for (int i = 0; i < appDef.size(); i++)
    {
      cmd[i] = appDef.get(i);
    }
    cmd[appDef.size()] = arg;
    Runtime.getRuntime().exec(cmd, null);
  }

  /**
   * Check if the given executable actually exists.
   * This is equivalent to <code>getExecutable().exists()</code>
   * @return true if File.exists() returns true
   */
  public boolean executableExists()
  {
    WbFile f = getExecutable();
    return f.exists();
  }

  private List<String> getComandElements()
  {
    List<String> result = new ArrayList<>(2);
    result.add(this.exePath);
    if (StringUtil.isNonBlank(parameters))
    {
      result.add(this.parameters);
    }
    return result;
  }

  @Override
  public boolean equals(Object other)
  {
    if (this.name == null) return false;
    if (other instanceof ToolDefinition)
    {
      ToolDefinition t = (ToolDefinition)other;
      return this.name.equals(t.getName());
    }
    else if (other instanceof String)
    {
      return this.name.equals((String)other);
    }
    return false;
  }

  @Override
  public int hashCode()
  {
    int hash = 7;
    hash = 43 * hash + (this.name != null ? this.name.hashCode() : 0);
    return hash;
  }
}
