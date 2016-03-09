/*
 * EncodingUtil.java
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

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.UnsupportedEncodingException;
import java.io.Writer;
import java.nio.charset.Charset;
import java.util.List;
import java.util.SortedMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.JComponent;

import workbench.log.LogMgr;
import workbench.resource.Settings;

/**
 * Utility class to handle encoding related stuff
 *
 * @author  Thomas Kellerer
 */
public class EncodingUtil
{
  private static String[] charsets;

  /**
   *	Create a BufferedReader for the given file and encoding
   *  The buffer size is set to 64K
   */
  public static Reader createReader(File f, String encoding)
    throws IOException, UnsupportedEncodingException
  {
    InputStream inStream = new FileInputStream(f);
    return createReader(inStream, encoding);
  }

  public static Reader createReader(InputStream in, String encoding)
    throws IOException, UnsupportedEncodingException
  {
    Reader r = null;
    if (encoding != null)
    {
      try
      {
        String enc = cleanupEncoding(encoding);

        if (enc.toLowerCase().startsWith("utf"))
        {
          r = new UnicodeReader(in, enc);
        }
        else
        {
          r = new InputStreamReader(in, enc);
        }
      }
      catch (UnsupportedEncodingException e)
      {
        throw e;
      }
    }
    else
    {
      r = new InputStreamReader(in);
    }
    return r;
  }

  /**
   * Create a BufferedReader for the given file and encoding.
   * If no encoding is given, then a regular FileReader without
   * a specific encoding is used.
   * The default buffer size is 16kb
   */
  public static BufferedReader createBufferedReader(File f, String encoding)
    throws IOException
  {
    return createBufferedReader(f, encoding, 1024*1024);
  }

  /**
   * Create a BufferedReader for the given file, encoding and buffer size.
   * If no encoding is given, then a regular FileReader without
   * a specific encoding is used.
   */
  public static BufferedReader createBufferedReader(File f, String encoding, int buffSize)
    throws IOException
  {
    Reader r = createReader(f, encoding);
    return new BufferedReader(r, buffSize);
  }

  /**
   * Allow some common other names for encodings (e.g. UTF for UTF-8)
   */
  public static String cleanupEncoding(String input)
  {
    if (input == null) return null;
    if ("utf".equalsIgnoreCase(input)) return "UTF-8";

    String upcase = input.toUpperCase();

    Pattern utf = Pattern.compile("UTF[0-9]+.*");
    Matcher um = utf.matcher(upcase);
    if (um.matches())
    {
      return upcase.replace("UTF", "UTF-");
    }

    Pattern iso = Pattern.compile("ISO8859[0-9]+");
    Matcher m = iso.matcher(upcase);

    if (m.matches())
    {
      return upcase.replace("8859", "-8859-");
    }
    return upcase;
  }

  /**
   * Return all available encodings.
   */
  public synchronized static String[] getEncodings()
  {
    if (charsets == null)
    {
      List<String> toUse = Settings.getInstance().getEncodingsToUse();
      if (CollectionUtil.isEmpty(toUse))
      {
        charsets = getSystemCharsets();
      }
      else
      {
        charsets = new String[toUse.size()];
        int i=0;
        for (String encoding : toUse)
        {
          charsets[i] = encoding;
          i++;
        }
      }
    }
    return charsets;
  }

  public static boolean isMultibyte(String encoding)
  {
    try
    {
      Charset c = Charset.forName(encoding);
      return c.newEncoder().maxBytesPerChar() > 1.0f;
    }
    catch (Throwable th)
    {
      return false;
    }
  }

  private static String[] getSystemCharsets()
  {
    long start = System.currentTimeMillis();
    SortedMap<String, Charset> sets = java.nio.charset.Charset.availableCharsets();
    String[] result = new String[sets.size()];
    int i = 0;
    for (String name : sets.keySet())
    {
      result[i] = name;
      i++;
    }
    long duration = System.currentTimeMillis() - start;
    LogMgr.logDebug("EncodingUtil.getEncodings()", "Retrieving encodings took: " + duration + "ms");
    return result;
  }

  /**
   * Test if the given encoding is supported.
   *
   * Before this is tested, cleanupEncoding() is called to allow for some common "abbreviations"
   *
   * @see #cleanupEncoding(String)
   */
  public static boolean isEncodingSupported(String encoding)
  {
    if (StringUtil.isBlank(encoding)) return false;

    String enc = cleanupEncoding(encoding);
    try
    {
      return Charset.isSupported(enc);
    }
    catch (Throwable e)
    {
      return false;
    }
  }

  public static Writer createWriter(File outfile, String encoding, boolean append)
    throws IOException
  {
    return createWriter(new FileOutputStream(outfile, append), encoding);
  }

  public static Writer createWriter(OutputStream stream, String encoding)
    throws IOException
  {
    Writer pw = null;
    final int buffSize = 4*1024*1024;
    if (encoding != null)
    {
      try
      {
        OutputStreamWriter out = new OutputStreamWriter(stream, cleanupEncoding(encoding));
        pw = new BufferedWriter(out, buffSize);
      }
      catch (UnsupportedEncodingException e)
      {
        // Fall back to default encoding
        pw = new BufferedWriter(new OutputStreamWriter(stream), buffSize);
        LogMgr.logError("EncodingUtil.createWriter()", "Invalid encoding: " + encoding, e);
      }
    }
    return pw;
  }

  public static String getDefaultEncoding()
  {
    return System.getProperty("file.encoding");
  }

  public static JComponent createEncodingPanel()
  {
    try
    {
      return (JComponent)Class.forName("workbench.gui.components.EncodingPanel").newInstance();
    }
    catch (Exception e)
    {
      return null;
    }
  }

  public static void fetchEncodings()
  {
    WbThread encodings = new WbThread("Fetch Encodings")
    {
      @Override
      public void run()
      {
        getEncodings();
      }
    };
    encodings.start();
  }

}
