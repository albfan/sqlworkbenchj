/*
 * WbProperties.java
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

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import workbench.interfaces.PropertyStorage;
import workbench.resource.Settings;

/**
 * An enhanced Properties class
 *
 * @author Thomas Kellerer
 */
public class WbProperties
  extends Properties
  implements PropertyStorage
{
  private static final long serialVersionUID = 1L;
  private int distinctSections;

  private final Map<String, List<PropertyChangeListener>> changeListeners = new HashMap<>();
  private final Map<String, String> comments = new HashMap<>();

  private Object changeNotificationSource = null;
  private boolean changed = false;

  private Comparator<String> sortComparator = null;

  protected WbProperties()
  {
    this(null, 2);
  }

  public WbProperties(int num)
  {
    this(null, num);
  }

  public WbProperties(Object notificationSource)
  {
    this(notificationSource, 2);
  }

  public WbProperties(Object notificationSource, int num)
  {
    super();
    this.changeNotificationSource = (notificationSource == null ? this : notificationSource);
    this.distinctSections = num;
  }

  public boolean isModified()
  {
    return changed;
  }

  public synchronized void saveToFile(File filename)
    throws IOException
  {
    saveToFile(filename, null);
  }

  public synchronized void saveToFile(File filename, WbProperties reference)
    throws IOException
  {
    try (FileOutputStream out = new FileOutputStream(filename))
    {
      this.save(out, reference);
    }
  }

  public synchronized void save(OutputStream out)
    throws IOException
  {
    save(out, (WbProperties)null);
  }

  public void setSortComparator(Comparator<String> comp)
  {
    this.sortComparator = comp;
  }

  public synchronized void save(OutputStream out, WbProperties reference)
    throws IOException
  {
    List<String> keys = new ArrayList<>(getKeys());
    if (sortComparator == null)
    {
      Collections.sort(keys);
    }
    else
    {
      Collections.sort(keys, sortComparator);
    }
    BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(out));
    String lastKey = null;
    String key = null;
    boolean hadComment = false;

    for (Object key1 : keys)
    {
      key = (String) key1;
      if (reference != null && reference.size() > 0)
      {
        String currentValue = getProperty(key);
        String referenceValue = reference.getProperty(key);
        if (StringUtil.equalStringOrEmpty(currentValue, referenceValue))
        {
          // LogMgr.logTrace("WbProperties.save()", "Property: [" + key1 + "] has its default value. Property will not be saved");
          continue;
        }
      }
      String comment = comments.get(key);
      if (StringUtil.isNonBlank(comment))
      {
        if (!hadComment) bw.newLine();
        bw.write(comment);
        bw.newLine();
      }
      if (lastKey != null && distinctSections > 0)
      {
        String k1 = null;
        String k2 = null;
        k1 = getSections(lastKey, this.distinctSections);
        k2 = getSections(key, this.distinctSections);
        if (!k1.equals(k2) && StringUtil.isBlank(comment))
        {
          bw.newLine();
        }
      }
      final String newlineEscape = "_$wb$nl$_";
      String value = this.getProperty(key);
      if (value != null)
      {

        value = value.replace("\n", newlineEscape);
        value = StringUtil.escapeText(value, CharacterRange.RANGE_7BIT);

        // Newlines will also be encoded, but we want them "visible" with
        // line continuation in the written file
        value = value.replace(newlineEscape, "\\\n");

        if (value.length() > 0)
        {
          bw.write(key + "=" + value);
          bw.newLine();
        }
        else
        {
          bw.write(key + "=");
          bw.newLine();
        }
      }
      else
      {
        bw.write(key + "=");
        bw.newLine();
      }
      if (StringUtil.isBlank(comment))
      {
        hadComment = false;
      }
      else
      {
        bw.newLine();
        hadComment = true;
      }
      lastKey = key;
    }
    bw.flush();
    changed = false;
  }

  @Override
  public int getIntProperty(String property, int defaultValue)
  {
    String value = this.getProperty(property, null);
    if (value == null) return defaultValue;
    return StringUtil.getIntValue(value, defaultValue);
  }

  @Override
  public boolean getBoolProperty(String property, boolean defaultValue)
  {
    String value = this.getProperty(property, null);
    if (value == null) return defaultValue;
    //return StringUtil.stringToBool(value);
    return Boolean.valueOf(value);
  }

  @Override
  public void setProperty(String property, int value)
  {
    this.setProperty(property, Integer.toString(value));
  }

  @Override
  public void setProperty(String property, boolean value)
  {
    this.setProperty(property, Boolean.toString(value));
  }

  private String getSections(String aString, int aNum)
  {
    int pos = aString.indexOf('.');
    if (pos < 0) return aString;

    String result = null;
    for (int i=1; i < aNum; i++)
    {
      int pos2 = aString.indexOf('.', pos + 1);
      if (pos2 > -1)
      {
        pos = pos2;
      }
      else
      {
        if (i == (aNum - 1))
        {
          pos = aString.length();
        }
      }
    }
    result = aString.substring(0, pos);
    return result;
  }

  public String removeProperty(String key)
  {
    String old = getProperty(key, null);
    super.remove(key);
    firePropertyChanged(key, old, null);
    return old;
  }

  public void addPropertyChangeListener(PropertyChangeListener aListener, String ... properties)
  {
    if (aListener == null) return;

    synchronized (this.changeListeners)
    {
      for (String prop : properties)
      {
        List<PropertyChangeListener> listeners = this.changeListeners.get(prop);
        if (listeners == null)
        {
          listeners = new ArrayList<>();
          this.changeListeners.put(prop, listeners);
        }
        listeners.add(aListener);
      }
    }
  }

  public void removePropertyChangeListener(PropertyChangeListener aListener)
  {
    synchronized (this.changeListeners)
    {
      for (List<PropertyChangeListener> listeners : changeListeners.values())
      {
        if (listeners != null)
        {
          listeners.remove(aListener);
        }
      }
    }
  }

  private void firePropertyChanged(String name, String oldValue, String newValue)
  {
    changed = true;
    List<PropertyChangeListener> listeners = this.changeListeners.get(name);
    if (listeners == null || listeners.isEmpty()) return;

    // Making a shallow copy of the list prevents a ConcurrentModificationException
    List<PropertyChangeListener> l2 = new ArrayList<>(listeners);
    PropertyChangeEvent evt = new PropertyChangeEvent(this.changeNotificationSource, name, oldValue, newValue);

    for (PropertyChangeListener l : l2)
    {
      if (l != null)
      {
        l.propertyChange(evt);
      }
    }
  }

  @Override
  public Set<String> getKeys()
  {
    return super.stringPropertyNames();
  }

  public List<String> getKeysWithPrefix(String prefix)
  {
    if (prefix == null)
    {
      return new ArrayList<>(getKeys());
    }

    Set<String> keys = getKeys();
    List<String> result = new ArrayList<>();

    for (String key : keys)
    {
      if (key.contains(prefix))
      {
        result.add(key);
      }
    }
    return result;
  }

  @Override
  public Object setProperty(String name, String value)
  {
    return setProperty(name, value, true);
  }

  public void setTemporaryProperty(String key, String value)
  {
    String oldValue = System.getProperty(key, getProperty(key));
    if (!StringUtil.equalStringOrEmpty(oldValue, value))
    {
      System.setProperty(key, value);
      firePropertyChanged(key, oldValue, value);
    }
  }

  public Object setProperty(String name, String value, boolean firePropChange)
  {
    if (name == null) return null;

    String oldValue = null;

    synchronized (this)
    {
      if (value == null)
      {
        oldValue = (String) super.remove(name);
      }
      else
      {
        oldValue = (String) super.setProperty(name, value);
      }
    }

    if (firePropChange && StringUtil.stringsAreNotEqual(oldValue, value))
    {
      this.firePropertyChanged(name, oldValue, value);
    }
    return oldValue;
  }

  public void clearComments()
  {
    this.comments.clear();
  }

  public String getComment(String key)
  {
    return comments.get(key);
  }

  public void setComment(String key, String comment)
  {
    comments.put(key, comment);
  }

  public void addPropertyDefinition(String line)
  {
    addPropertyDefinition(line, null);
  }

  /**
   *	Adds a property definition in the form key=value
   *	Lines starting with # are ignored
   *	Lines that do not contain a = character are ignored
   *  Any text after a # sign in the value is ignored
   */
  public void addPropertyDefinition(String line, String comment)
  {
    if (line == null) return;
    if (StringUtil.isBlank(line)) return;
    if (line.charAt(0) == '#') return;
    int pos = line.indexOf('=');
    if (pos == -1) return;
    String key = line.substring(0, pos);
    String value = line.substring(pos + 1);
    this.setProperty(key, value.trim());
    comments.put(key, comment);
  }

  public void loadTextFile(String filename)
    throws IOException
  {
    loadTextFile(filename, null);
  }

  /**
   * Read the content of the file into this properties object.
   * This method does not support line continuation, but supports
   * an encoding (as opposed to the original properties class)
   *
   */
  public void loadTextFile(String filename, String encoding)
    throws IOException
  {
    BufferedReader in = null;
    File f = new File(filename);
    if (encoding == null)
    {
      encoding = Settings.getInstance().getDefaultEncoding();
    }
    try
    {
      in = EncodingUtil.createBufferedReader(f, encoding);
      loadFromReader(in);
    }
    finally
    {
      try { in.close(); } catch (Throwable th) {}
    }
  }

  public void loadFromStream(InputStream in)
    throws IOException
  {
    BufferedReader reader = new BufferedReader(new InputStreamReader(in));
    loadFromReader(reader);
  }

  public void loadFromReader(BufferedReader in)
    throws IOException
  {
    String line = in.readLine();
    String lastComment = null;
    String lineFragment = null;

    while (line != null)
    {
      if (StringUtil.isBlank(line))
      {
        lastComment = null;
      }

      if (line.trim().startsWith("#") && !line.trim().startsWith("#!"))
      {
        if (lastComment == null)
        {
          lastComment = line;
        }
        else
        {
          lastComment += "\n" + line;
        }
      }
      else if (line.trim().endsWith("\\"))
      {
        line = line.substring(0, line.lastIndexOf('\\'));
        if (lineFragment == null)
        {
          lineFragment = line;
        }
        else
        {
          lineFragment += "\n" + line;
        }
      }
      else
      {
        if (lineFragment != null)
        {
          lineFragment += "\n" + line;
          this.addPropertyDefinition(StringUtil.decodeUnicode(lineFragment), lastComment);
          lineFragment = null;
        }
        else
        {
          this.addPropertyDefinition(StringUtil.decodeUnicode(line), lastComment);
        }
        lastComment = null;
      }
      line = in.readLine();
    }
    changed = false;
  }

  public static Properties createCopy(Properties source)
  {
    if (source == null) return null;
    Properties copy = new Properties();
    copy.putAll(source);
    return copy;
  }

}
