/*
 * ArgumentParser.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2017, Thomas Kellerer
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

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import workbench.log.LogMgr;

import workbench.sql.wbcommands.CommonArgs;

/**
 *
 * @author  Thomas Kellerer
 */
public class ArgumentParser
{
  private static final String ARG_PRESENT = "$WB$__ARG_PRESENT__$WB$";

  // Maps the argument to the supplied value
  protected Map<String, Object> arguments;

  // Maps a registered argument to the argument type
  private Map<String, ArgumentType> argTypes;

  private List<String> unknownParameters = new ArrayList<>();
  private Set<String> deprecatedParameters = CollectionUtil.caseInsensitiveSet();

  // Stores the allowed values for a parameter
  private Map<String, Collection<ArgumentValue>> allowedValues;
  private int argCount = 0;
  private boolean needSwitch = true;
  private String nonArguments;
  private final Set<ArgumentType> repeatableTypes = EnumSet.of(ArgumentType.Repeatable, ArgumentType.RepeatableValue);

  public ArgumentParser()
  {
    arguments = new TreeMap<>(CaseInsensitiveComparator.INSTANCE);
    argTypes = new TreeMap<>(CaseInsensitiveComparator.INSTANCE);
    allowedValues = new TreeMap<>(CaseInsensitiveComparator.INSTANCE);
    addArgument(CommonArgs.ARG_HELP, ArgumentType.BoolSwitch);
  }

  public ArgumentParser(boolean parameterSwitchNeeded)
  {
    this();
    this.needSwitch = parameterSwitchNeeded;
  }

  public boolean needsSwitch()
  {
    return needSwitch;
  }

  public boolean isAllowedValue(String key, String value)
  {
    StringArgumentValue argValue = new StringArgumentValue(value);
    Collection<ArgumentValue> allowed = getAllowedValues(key);
    if (allowed == null)
    {
      return false;
    }
    return allowed.contains(argValue);
  }

  public Collection<ArgumentValue> getAllowedValues(String key)
  {
    return allowedValues.get(key);
  }

  public boolean hasValidValue(String parameter)
  {
    String value = getValue(parameter);
    return isAllowedValue(parameter, value);
  }

  /**
   * Register an enum as an argument.
   *
   * The elements of the enum are registered as valid argument parameters.
   *
   * @param key the argument name
   * @param enumClass  the enum's class
   *
   * @see #addArgument(java.lang.String, java.util.List)
   * @see #getEnumValue(java.lang.String, java.lang.Enum, java.lang.Class)
   */
  public <T extends Enum<T>> void addArgument(String key, Class<T> enumClass)
  {
    T[] values = enumClass.getEnumConstants();
    List<String> names = new ArrayList<>(values.length);
    for (T value : values)
    {
      names.add(value.toString());
    }
    addArgument(key, names);
  }

  /**
   * Register an argument with a predefined list of values.
   *
   * The list of allowed values can be used for autocompletion in the frontend.
   *
   * @param key the argument name
   * @param values  the allowed values
   *
   * @see #isAllowedValue(java.lang.String, java.lang.String)
   * @see #getAllowedValues(java.lang.String)
   */
  public void addArgument(String key, List<String> values)
  {
    addArgument(key, ArgumentType.ListArgument);
    if (values == null) return;
    Collection<ArgumentValue> v = new TreeSet<>(ArgumentValue.COMPARATOR);
    for (String value : values)
    {
      v.add(new StringArgumentValue(value));
    }
    allowedValues.put(key, v);
  }

  public void addArgumentWithValues(String key, List<? extends ArgumentValue> values)
  {
    addArgument(key, ArgumentType.ListArgument);
    if (values == null) return;
    Collection<ArgumentValue> v = new TreeSet<>();
    v.addAll(values);
    allowedValues.put(key, v);
  }

  public void addArgument(String key)
  {
    addArgument(key, ArgumentType.StringArgument);
  }

  public void addArgument(String key, ArgumentType type)
  {
    if (key == null) throw new NullPointerException("Key may not be null");

    this.arguments.put(key, null);
    this.argTypes.put(key, type);
  }

  public void addDeprecatedArgument(String key, ArgumentType type)
  {
    if (key == null) throw new NullPointerException("Key may not be null");

    this.arguments.put(key, null);
    this.argTypes.put(key, type);
    deprecatedParameters.add(key);
  }

  public void parseProperties(File propFile)
    throws IOException
  {
    Reader in = EncodingUtil.createReader(propFile, null);
    List<String> lines = FileUtil.getLines(new BufferedReader(in));
    parse(lines);
  }

  public void parse(String[] args)
  {
    reset();
    if (args == null) return;

    StringBuilder line = new StringBuilder(args.length * 20);
    for (String arg : args)
    {
      line.append(arg);
      line.append(' ');
    }
    parse(line.toString());
  }

  public void parse(String cmdLine)
  {
    reset();
    if (cmdLine == null) return;

    WbStringTokenizer tok;
    if (needSwitch)
    {
      tok = new WbStringTokenizer('-', "\"'", true);
      tok.setDelimiterNeedsWhitspace(true);
    }
    else
    {
      tok = new WbStringTokenizer(' ', "\"'", true);
      tok.setDelimiterNeedsWhitspace(false);
    }
    tok.setSourceString(cmdLine.trim());
    List<String> entries = tok.getAllTokens();
    parse(entries);
  }

  public String getNonArguments()
  {
    return nonArguments;
  }

  private void appendNonArg(String value)
  {
    if (nonArguments.length() > 0)
    {
      nonArguments += " ";
    }
    nonArguments += value.trim();
  }

  protected void parse(List<String> entries)
  {
    this.nonArguments = "";
    Pattern equals = Pattern.compile("\\s+=\\s+");

    try
    {
      for (String entry : entries)
      {
        if (StringUtil.isBlank(entry)) continue;

        String arg = entry.trim();
        Matcher m = equals.matcher(arg);
        if (m.find())
        {
          arg = m.replaceFirst("=");
        }

        String key = arg;
        String value = null;
        int pos = arg.indexOf('=');

        int whiteSpace = StringUtil.findFirstWhiteSpace(arg);
        if (pos > -1 && whiteSpace > 0 && whiteSpace < pos)
        {
          appendNonArg(arg.substring(whiteSpace));
          arg = arg.substring(0, whiteSpace);
          pos = arg.indexOf('=');
          key = arg;
        }

        boolean wasQuoted = false;

        if (pos > -1)
        {
          key = arg.substring(0, pos).trim();
          value = pos < arg.length() - 1 ? arg.substring(pos + 1).trim() : "";
          if (value.length() > 0)
          {
            char first = value.charAt(0);
            char last = value.charAt(value.length() - 1);
            if ( (first == '"' && last == '"') || (first == '\'' && last == '\''))
            {
              int otherPos = value.indexOf(first, 1);
              if (otherPos == -1 || otherPos == value.length() - 1)
              {
                value = StringUtil.trimQuotes(value);
                wasQuoted = true;
              }
            }
          }
        }

        if (value == null)
        {
          value = ARG_PRESENT;
        }
        else if (!wasQuoted)
        {
          value = value.trim();
        }

        if (arguments.containsKey(key))
        {
          ArgumentType type = argTypes.get(key);
          if (repeatableTypes.contains(type))
          {
            List<String> list = (List<String>)arguments.get(key);
            if (list == null)
            {
              list = new ArrayList<>();
              arguments.put(key, list);
            }
            if (wasQuoted)
            {
              list.add(value);
            }
            else if (type == ArgumentType.RepeatableValue)
            {
              list.add(StringUtil.trimQuotes(value));
            }
            else
            {
              List<String> result = StringUtil.stringToList(value, ",", true, true, false);
              list.addAll(result);
            }
          }
          else
          {
            arguments.put(key, value);
          }
          this.argCount ++;
        }
        else
        {
          appendNonArg(entry);
          this.unknownParameters.add(key);
        }
      }
    }
    catch (Exception e)
    {
      LogMgr.logError("ArgumentParser.parse()", "Error when parsing entries", e);
    }
  }

  public List<String> getArgumentsOnCommandLine()
  {
    ArrayList<String> result = new ArrayList<>(this.arguments.size());
    for (Map.Entry<String, Object> entry : arguments.entrySet())
    {
      if (entry.getValue() != null && getArgumentType(entry.getKey()) != ArgumentType.Repeatable)
      {
        result.add(entry.getKey());
      }
    }
    return result;
  }

  /**
   * Returns the list of known arguments for this ArgumentParser
   * @return the registered argument types
   */
  public List<String> getRegisteredArguments()
  {
    Iterator<Map.Entry<String, ArgumentType>> itr = this.argTypes.entrySet().iterator();

    List<String> result = new ArrayList<>(this.argTypes.size());
    while (itr.hasNext())
    {
      Map.Entry<String, ArgumentType> entry = itr.next();
      if (!deprecatedParameters.contains(entry.getKey()))
      {
        result.add(entry.getKey());
      }
    }
    return result;
  }

  /**
   * Returns the type of an argument
   */
  public ArgumentType getArgumentType(String arg)
  {
    return this.argTypes.get(arg);
  }

  /**
   * Checks if any arguments have been defined.
   *
   * @return true if at least one argument was provided
   */
  public boolean hasArguments()
  {
    return this.argCount > 0;
  }

  public int getArgumentCount()
  {
    return this.argCount;
  }

  public boolean hasUnknownArguments()
  {
    return this.unknownParameters.size() > 0;
  }

  /**
   * Return a list of unknown arguments.
   *
   * Each argument passed in the original command line
   * that has not been registered using addArgument()
   * will be listed in the result. For each argument
   * in this list, isRegistered() would return false.
   *
   * @return a comma separated string with unknown arguments
   */
  public String getUnknownArguments()
  {
    StringBuilder msg = new StringBuilder();

    if (unknownParameters.size() > 0)
    {
      for (int i=0; i < unknownParameters.size(); i++)
      {
        if (i > 0) msg.append(' ');
        msg.append('-');
        msg.append(unknownParameters.get(i));
      }
    }
    return msg.toString();
  }

  /**
   * Check if the given argument is a valid argument for the current commandline
   * @return true if arg was registered with addArgument()
   */
  public boolean isRegistered(String arg)
  {
    return this.arguments.containsKey(arg);
  }

  public boolean isArgNotPresent(String arg)
  {
    return !isArgPresent(arg);
  }

  /**
   * Check if the given argument was passed on the commandline.
   * This does not check if a value has been supplied with the
   * argument. This can be used for argument-less parameters
   * e.g. -showEncodings
   *
   * @return true if the given argument was part of the commandline
   */
  public boolean isArgPresent(String arg)
  {
    if (arg == null) return false;
    // Even arguments without a value will have something
    // in the map (the ARG_PRESENT marker object), otherwise
    // they could not be distinguished from arguments that
    // are merely registered.
    Object value = this.arguments.get(arg);
    return (value != null);
  }

  public void removeArgument(String arg)
  {
    if (arguments.get(arg) != null)
    {
      argCount --;
    }
    this.arguments.remove(arg);
    this.argTypes.remove(arg);
    this.allowedValues.remove(arg);
  }

  private void reset()
  {
    Iterator<String> keys = this.arguments.keySet().iterator();
    while (keys.hasNext())
    {
      String key = keys.next();
      this.arguments.put(key, null);
    }
    this.argCount = 0;
    this.unknownParameters.clear();
  }

  /**
   * Return a parameter as a boolean.
   * @return the value as passed on the command line
   *         false if no value was specified
   *
   * @see #getBoolean(String, boolean)
   */
  public boolean getBoolean(String key)
  {
    return getBoolean(key, false);
  }

  /**
   * Return a parameter value as a boolean.
   * If no value was specified the given default value will be returned
   *
   * @param key the parameter key
   * @param defaultValue the default to be returned if the parameter is not present
   *                     (this is ignored for parameters of type BoolSwitch)
   *
   * @return the value as passed on the commandline
   *         the defaultValue if the parameter was not supplied by the user
   *
   * @see #getValue(String)
   * @see #getBoolean(String)
   * @see StringUtil#stringToBool(String)
   */
  public boolean getBoolean(String key, boolean defaultValue)
  {
    String value = this.getValue(key);
    if (StringUtil.isBlank(value))
    {
      if (getArgumentType(key) == ArgumentType.BoolSwitch && isArgPresent(key))
      {
        return true;
      }
      return defaultValue;
    }
    return StringUtil.stringToBool(value);
  }

  /**
   * Return the parameter value for the given argument.
   *
   * If no value was specified or the parameter was not
   * passed on the commandline null will be returned.
   * Any leading or trailing quotes will be removed from the argument
   * before it is returned. To check if the parameter was present
   * but without a value isArgPresent() should be used.
   *
   * @param key the parameter to retrieve
   * @return the value as provided by the user or null if no value specified
   *
   * @see #isArgPresent(java.lang.String)
   * @see StringUtil#trimQuotes(String)
   */
  public String getValue(String key)
  {
    if (getArgumentType(key) == ArgumentType.Repeatable)
    {
      List<String> list = getList(key);
      if (CollectionUtil.isEmpty(list)) return null;

      if (list.size() == 1)
      {
        return list.get(0);
      }
      throw new IllegalStateException("Cannot return a single string from a List");
    }
    Object value = this.arguments.get(key);
    if (value == ARG_PRESENT) return null;
    if (value == null) return null;
    return (String)value;
  }

  /**
   * Returns all values that were supplied for a repeatable argument.
   *
   * A repeatable argument can be supplied multiple times, e.g. -constant=a -constant=b
   * For that example the returned list will contain two values "a", "b".
   *
   * @param key the argument that is marked as ArgumentType.repeatable.
   * @return all values supplied, never null. An empty List is returned if no values where specified
   * @see ArgumentType#Repeatable
   */
  public List<String> getList(String key)
  {
    if (repeatableTypes.contains(getArgumentType(key)))
    {
      Object value = this.arguments.get(key);
      if (value == ARG_PRESENT || value == null) return Collections.emptyList();
      return (List<String>)value;
    }
    String value = getValue(key);
    if (value == null) return Collections.emptyList();
    return CollectionUtil.arrayList(value);
  }

  public String getValue(String key, String defaultValue)
  {
    String value = getValue(key);
    if (value == null) return defaultValue;
    return value;
  }

  /**
   * Returns the values of a parameter that allows comma delimited values.
   *
   * @param key the argument name
   * @return the values that were specified.
   */
  public List<String> getListValue(String key)
  {
    if (repeatableTypes.contains(getArgumentType(key)))
    {
      return getList(key);
    }
    String value = this.getValue(key);
    if (value == null) return Collections.emptyList();
    List<String> result = StringUtil.stringToList(value, ",", true, true, false);
    return result;
  }

  public Map<String, String> getMapValue(String key)
  {
    List<String> entries = getListValue(key);
    if (CollectionUtil.isEmpty(entries)) return new HashMap<>();

    Map<String, String> result = new HashMap<>(entries.size());
    for (String entry : entries)
    {
      String[] param = entry.split("=");
      if (param == null || param.length != 2)
      {
        param = entry.split(":");
      }
      if (param != null && param.length == 2)
      {
        result.put(param[0], StringUtil.trimQuotes(param[1]));
      }
    }
    return result;
  }

  public int getIntValue(String key, int def)
  {
    return StringUtil.getIntValue(this.getValue(key),def);
  }

  /**
   * Return the enum value for the given argument.
   *
   * @param <T> the enum type
   * @param argName the argument name (previously registered using addArgument())
   * @param defaultValue the default value if nothing was specified
   * @return the value supplied by the user or the default value if nothing was specified
   * @throws IllegalArgumentException if the user supplied an invalid enum value
   * @throws NullPointerException if the defaultValue is null
   *
   * @see #getEnumValue(java.lang.String, java.lang.Enum)
   * @see #getEnumValue(java.lang.String, java.lang.Enum, java.lang.Class)
   * @see #addArgument(java.lang.String, java.lang.Class)
   */
  public <T extends Enum<T>> T getEnumValue(String argName, T defaultValue)
  {
    return getEnumValue(argName, defaultValue, defaultValue.getDeclaringClass());
  }

  /**
   * Return the enum value for the given argument.
   *
   * This method can be used if no default value for the Enum should be used. To convert
   * the user supplied string into an enum instance, the enum class is necessary.
   *
   * @param <T> the enum type
   * @param argName the argument name (previously registered using addArgument())
   * @param enumClass the enum class
   * @return the value supplied by the user or the default value if nothing was specified
   * @throws IllegalArgumentException if the user supplied an invalid enum value
   *
   * @see #getEnumValue(java.lang.String, java.lang.Class)
   * @see #getEnumValue(java.lang.String, java.lang.Enum, java.lang.Class)
   * @see #addArgument(java.lang.String, java.lang.Class)
   */

  public <T extends Enum<T>> T getEnumValue(String argName, Class<T> enumClass)
  {
    return getEnumValue(argName, null, enumClass);
  }

  /**
   * Return the enum value for the given argument.
   *
   * @param <T> the enum type
   * @param argName the argument name (previously registered using addArgument())
   * @param defaultValue the default value if nothing was specified
   * @param enumClass the enum class
   * @return the value supplied by the user or the default value if nothing was specified
   * @throws IllegalArgumentException if the user supplied an invalid enum value
   *
   * @see #getEnumValue(java.lang.String, java.lang.Class)
   * @see #getEnumValue(java.lang.String, java.lang.Enum)
   * @see #addArgument(java.lang.String, java.lang.Class)
   */
  public <T extends Enum<T>> T getEnumValue(String argName, T defaultValue, Class<T> enumClass)
    throws IllegalArgumentException
  {
    String value = getValue(argName, null);
    if (value == null)
    {
      return defaultValue;
    }
    return Enum.valueOf(enumClass, value);
  }
}
