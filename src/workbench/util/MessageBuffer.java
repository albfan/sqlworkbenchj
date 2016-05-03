/*
 * MessageBuffer.java
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

import java.util.ArrayDeque;
import java.util.Deque;

import workbench.interfaces.ResultLogger;
import workbench.resource.ResourceMgr;
import workbench.resource.Settings;

/**
 * A class to store messages efficiently.
 * The messages are internally stored in a LinkedList, but only up to
 * a specified maximum number of entries (not total size in bytes)
 *
 * If the maximum is reached {@link #getBuffer()} will add "(...)" at the beginning
 * of the generated result to indicate that messages have been cut off.
 *
 * This ensures that collecting warnings or errors during long running
 * jobs, does not cause an OutOfMemory error.
 *
 * @author Thomas Kellerer
 */
public class MessageBuffer
{
  private Deque<CharSequence> messages = new ArrayDeque<>();
  private int length = 0;
  private final String newLine = "\n";
  private final int maxSize;
  private boolean trimmed = false;

  /**
   * Create a new MessageBuffer, retrieving the max. number of entries
   * from the Settings object. If nothing has been specified in the .settings
   * file, a maximum number of 1000 entries is used.
   */
  public MessageBuffer()
  {
    this(Settings.getInstance().getIntProperty("workbench.messagebuffer.maxentries", 2500));
  }

  /**
   * Create a new MessageBuffer holding a maximum number of <tt>maxEntries</tt>
   * Entries in its internal list
   * @param maxEntries the max. number of entries to hold in the internal list
   */
  public MessageBuffer(int maxEntries)
  {
    maxSize = maxEntries;
  }

  public synchronized void clear()
  {
    this.messages.clear();
    this.length = 0;
  }

  /**
   * Write the messages of this MessageBuffer directly to a ResultLogger
   * The internal buffer is cleared during the writing
   *
   * @return the total number of characters written
   */
  public synchronized int appendTo(ResultLogger log)
  {
    int size = 0;
    while (messages.size() > 0)
    {
      CharSequence s = messages.removeFirst();
      size += s.length();
      log.appendToLog(s.toString());
    }
    length = 0;
    return size;
  }

  /**
   * Create a StringBuilder that contains the collected messages.
   * Once the result is returned, the internal list is emptied.
   * This means the second call to this method returns an empty
   * buffer if no messages have been added between the calls.
   */
  public synchronized CharSequence getBuffer()
  {
    StringBuilder result = new StringBuilder(this.length + 50);
    if (trimmed) result.append("(...)\n");

    while (messages.size() > 0)
    {
      CharSequence s = messages.removeFirst();
      result.append(s);
    }
    length = 0;
    return result;
  }

  private synchronized void trimSize()
  {
    if (maxSize > 0 && messages.size() >= maxSize)
    {
      trimmed = true;
      while (messages.size() >= maxSize)
      {
        CharSequence s = messages.removeFirst();
        if (s != null) this.length -= s.length();
      }
    }
  }

  /**
   * Returns the total length in characters of all messages
   * that are currently kept in this MessageBuffer
   */
  public synchronized int getLength()
  {
    return length;
  }

  public synchronized void append(MessageBuffer buff)
  {
    if (buff == null) return;
 		int count = buff.messages.size();
    if (count == 0) return;
    this.length += buff.length;
    while (this.messages.size() + count > maxSize)
    {
      CharSequence s = messages.removeFirst();
      if (s != null) this.length -= s.length();
    }
    for (CharSequence s : buff.messages)
    {
      this.messages.add(s);
    }
  }

  public synchronized void append(CharSequence s)
  {
    if (StringUtil.isEmptyString(s)) return;
    trimSize();
    this.messages.add(s);
    length += s.length();
  }

  public synchronized void appendMessageKey(String key)
  {
    append(ResourceMgr.getString(key));
  }

  public synchronized void appendNewLine()
  {
    append(newLine);
  }

  @Override
  public String toString()
  {
    return "[" + getLength() + " messages]";
  }

}
