/*
 * FileUtil.java
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
import java.io.ByteArrayOutputStream;
import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

import workbench.WbManager;
import workbench.log.LogMgr;

import org.mozilla.universalchardet.UniversalDetector;

/**
 * @author  Thomas Kellerer
 */
public class FileUtil
{
  /**
   * The size of the buffer used by copy()
   */
  private static final int BUFF_SIZE = 2048*1024;

  /*
   * Closes all streams in the list.
   * @param a list of streams to close
   * @see #closeQuitely(Closeable)
   */
  public static void closeStreams(Collection<Closeable> streams)
  {
    if (streams == null) return;

    for (Closeable str : streams)
    {
      try
      {
        closeQuietely(str);
      }
      catch (Exception ex)
      {
        LogMgr.logWarning("FileUtil.closeStreams()", "Error when closing stream", ex);
      }
    }
  }

  /**
   * Read the lines of the given Reader into a Collection.
   *
   * The Reader will be closed after all lines have been read.
   * Empty lines are ignored and not add to the collection.
   *
   * @param in the "file" to read
   * @return a Collection with all the lines in the file
   */
  public static List<String> getLines(BufferedReader in)
  {
    return getLines(in, false);
  }

  public static List<String> getLines(BufferedReader input, boolean trim)
  {
    return getLines(input, trim, false);
  }

  /**
   * Read the lines of the given Reader into a Collection.
   * <br/>
   * The Reader will be closed after all lines have been read.
   * Empty lines are ignored and not added to the collection.
   *
   * @param input the "file" to read
   * @param trim if true, each line will be trimmed after reading
   *
   * @return a Collection with all the lines in the file
   */
  public static List<String> getLines(BufferedReader input, boolean trim, boolean checkComments)
  {
    return getLines(input, trim, checkComments, Integer.MAX_VALUE);
  }

  public static List<String> getLines(BufferedReader input, boolean trim, boolean checkComments, int numLines)
  {
    List<String> result = new ArrayList<>();

    try
    {
      String line;
      while ( (line = input.readLine()) != null)
      {
        if (StringUtil.isNonEmpty(line))
        {
          if (checkComments && line.trim().startsWith("#"))
          {
            continue;
          }
          result.add(trim ? line.trim() : line);
        }
        if (result.size() > numLines) break;
      }
    }
    catch (Exception e)
    {
      LogMgr.logError("FileUtil.getLines", "Error reading lines", e);
    }
    finally
    {
      closeQuietely(input);
    }
    return result;
  }

  /**
   * Read the contents of the Reader into the provided StringBuilder.
   *
   * Up to numLines lines are read. The Reader will not be closed.
   *
   * @param in the Reader to be used
   * @param buffer the StringBuilder to received the lines
   * @param numLines the max. number of lines to be read
   * @param lineEnd the lineEnding to be used
   * @return the number of lines read
   */
  public static int readLines(BufferedReader in, StringBuilder buffer, int numLines, String lineEnd)
    throws IOException
  {
    int lines = 0;
    String line = in.readLine();
    while (line != null)
    {
      buffer.append(line);
      buffer.append(lineEnd);
      lines ++;
      if (lines >= numLines) break;
      line = in.readLine();
    }
    return lines;
  }

  public static String getLineEnding(File input, String encoding)
  {
    String lineEnd = null;
    BufferedReader reader = null;
    try
    {
      reader = EncodingUtil.createBufferedReader(input, encoding);
      lineEnd = getLineEnding(reader);
      if (lineEnd == null)
      {
        lineEnd = StringUtil.LINE_TERMINATOR;
      }
    }
    catch (IOException io)
    {
      lineEnd = StringUtil.LINE_TERMINATOR;
    }
    finally
    {
      FileUtil.closeQuietely(reader);
    }
    return lineEnd;
  }

  /**
   * Try to detect the type of line ending used by the passed Reader.
   *
   * This will advance the reader until a line ending is found.
   * The reader will not be closed
   *
   * @param in the "file" to test
   * @return the sequence of characters used as the line ending (e.g. \n or \r\n)
   * @throws java.io.IOException
   */
  public static String getLineEnding(Reader in)
    throws IOException
  {
    String ending = null;
    int c = in.read();
    while (c != -1)
    {
      if (c == '\r')
      {
        char n = (char)in.read();
        if (n == '\n')
        {
          ending = "\r\n";
          break;
        }
      }
      else if (c == '\n')
      {
        ending = "\n";
        break;
      }
      c = in.read();
    }
    return ending;
  }


  /**
   * Tries to estimate the number of records in the given file using the first 5 lines.
   *
   * @param f the file to check
   * @see #estimateRecords(java.io.File, long)
   */
  public static long estimateRecords(File f)
    throws IOException
  {
    return estimateRecords(f, 5);
  }

  /**
   * Tries to estimate the number of records in the given file.
   *
   * This is done by reading the first <tt>sampleLines</tt> records
   * of the file and assuming the average size of an row in the first
   * lines is close to the average row in the complete file.
   *
   * The first line is always ignored assuming this is a header line.
   *
   * @param f the file to check
   * @param sampleLines the number of lines to read
   */
  public static long estimateRecords(File f, long sampleLines)
    throws IOException
  {
    if (sampleLines <= 0) throw new IllegalArgumentException("Sample size must be greater then zero");
    if (!f.exists()) return -1;
    if (!f.isFile()) return -1;
    long size = f.length();
    if (size == 0) return 0;

    long lineSize = 0;

    BufferedReader in = null;
    try
    {
      in = new BufferedReader(new FileReader(f), 256 * 1024);
      in.readLine(); // skip the first line
      int lfSize = StringUtil.LINE_TERMINATOR.length();
      for (int i=0; i < sampleLines; i++)
      {
        String line = in.readLine();
        if (line == null) return i + 1;
        lineSize += (line.length() + lfSize);
      }
    }
    finally
    {
      closeQuietely(in);
    }
    return (size / (lineSize / sampleLines));

  }

  /**
   * Copies the source file to the destination file.
   *
   * @param source
   * @param destination
   * 
   * @throws java.io.IOException
   *
   * @see Files#copy(java.nio.file.Path, java.nio.file.Path, java.nio.file.CopyOption...)
   */
  public static void copy(File source, File destination)
    throws IOException
  {
    if (source == null || destination == null) return;
    Files.copy(source.toPath(), destination.toPath(), StandardCopyOption.REPLACE_EXISTING);
  }

  /**
   * Copies a file without throwing an exception.
   *
   * @param fromFile
   * @param toFile
   *
   * @see #copy(java.io.File, java.io.File)
   */
  public static void copySilently(File fromFile, File toFile)
  {
    if (fromFile == null || toFile == null) return;
    if (!fromFile.exists()) return;

    try
    {
      copy(fromFile, toFile);
    }
    catch (Exception ex)
    {
      LogMgr.logError("FileUtil.copySilently()", "Error when copying file: " + fromFile + " to " + toFile, ex);
    }
  }


  /**
   * Copies the content of the InputStream to the OutputStream.
   * Both streams are closed automatically.
   *
   * @return the number of bytes copied
   */
  public static long copy(InputStream in, OutputStream out)
    throws IOException
  {
    long filesize = 0;
    try
    {
      byte[] buffer = new byte[BUFF_SIZE];
      int bytesRead = in.read(buffer);
      while (bytesRead != -1)
      {
        filesize += bytesRead;
        out.write(buffer, 0, bytesRead);
        bytesRead = in.read(buffer);
      }
    }
    finally
    {
      closeQuietely(out);
      closeQuietely(in);
    }
    return filesize;
  }

  /**
   * Reads the contents of the file into a String.
   *
   * @param f the file to read
   * @param encoding the file's encoding
   * @return the file content as a single String
   *
   * @throws IOException
   * @see #readCharacters(java.io.Reader)
   */
  public static String readFile(File f, String encoding)
    throws IOException
  {
    Reader r = EncodingUtil.createReader(f, encoding);
    return readCharacters(r);
  }

  /**
   * Read the content of the Reader into a String.
   *
   * The Reader is closed automatically.
   */
  public static String readCharacters(Reader in)
    throws IOException
  {
    if (in == null) return null;
    StringBuilder result = new StringBuilder(1024);
    char[] buff = new char[BUFF_SIZE];
    int bytesRead = in.read(buff);
    try
    {
      while (bytesRead > -1)
      {
        result.append(buff, 0, bytesRead);
        bytesRead = in.read(buff);
      }
    }
    finally
    {
      closeQuietely(in);
    }
    return result.toString();
  }

  /**
   * Read the content of the InputStream into a ByteArray.
   * The InputStream is closed automatically.
   */
  public static byte[] readBytes(InputStream in)
    throws IOException
  {
    if (in == null) return null;
    ByteBuffer result = new ByteBuffer();
    byte[] buff = new byte[BUFF_SIZE];

    try
    {
      int bytesRead = in.read(buff);
      while (bytesRead > -1)
      {
        result.append(buff, 0, bytesRead);
        bytesRead = in.read(buff);
      }
    }
    finally
    {
      closeQuietely(in);
    }
    return result.getBuffer();
  }

  /**
   * Returns the number of characters according to the
   * encoding in the specified file. For single-byte
   * encodings this should be identical to source.length()
   * <br/>
   * For large files this might take some time!
   *
   * @param source the (text) file to check
   * @param encoding the encoding of the text file
   * @return the number of characters (not bytes) in the file
   */
  public static long getCharacterLength(File source, String encoding)
    throws IOException
  {
    long result = 0;
    if (EncodingUtil.isMultibyte(encoding))
    {
      BufferedReader r = null;
      try
      {
        r = EncodingUtil.createBufferedReader(source, encoding, 512 * 1024);
      // Not very efficient, but I can't think of a different solution
        // to retrieve the number of characters
        result = r.skip(Long.MAX_VALUE);
      }
      finally
      {
        closeQuietely(r);
      }
    }
    else
    {
      // if this is a single-byte encoding we can use the length of the file
      result = source.length();
    }

    return result;
  }

  /**
   * Closes a Closeable without throwing an IOException.
   *
   * @param c the Closeable to close
   */
  public static void closeQuietely(Closeable c)
  {
    if (c == null) return;

    try
    {
      c.close();
    }
    catch (IOException e)
    {
      // ignore
    }
  }

  public static boolean hasWildcard(String fname)
  {
    return fname != null && (fname.indexOf('?') > -1 || fname.indexOf('*') > -1);
  }

  /**
   * List all files denoted by the file pattern.
   *
   * Only the directory specfied in the argument is searched, no recursive search is done.
   *
   * The returned list is sorted by name (case insensitive).
   *
   * @param toSearch  the files to search, e.g. /temp/*.sql
   * @return a list of files found in the directory denoted by the search pattern
   */
  public static List<WbFile> listFiles(String toSearch, String baseDir)
  {
    if (StringUtil.isEmptyString(toSearch)) return Collections.emptyList();

    File f = new File(toSearch);
    File parentDir = f.getParentFile();
    if (parentDir == null || !f.isAbsolute())
    {
      if (baseDir == null)
      {
        parentDir = f.getAbsoluteFile().getParentFile();
      }
      else
      {
        if (f.getParent() != null)
        {
          parentDir = new File(baseDir, f.getParent());
        }
        else
        {
          parentDir = new File(baseDir);
        }
      }
    }

    List<WbFile> result = new ArrayList<>();

    try
    {
      LogMgr.logDebug("FileUtil.listFiles()", "Looking for files matching " + f.getName() + " in " + parentDir.toPath());
      DirectoryStream<Path> stream = Files.newDirectoryStream(parentDir.toPath(), f.getName());
      for (Path file : stream)
      {
        result.add(new WbFile(file.toFile()));
      }
    }
    catch (Exception ex)
    {
      LogMgr.logWarning("FileUtil.listFiles()", "Could not get file list", ex);
    }

    Comparator<File> fnameSorter = (File o1, File o2) -> o1.getName().compareToIgnoreCase(o2.getName());
    result.sort(fnameSorter);

    return result;
  }

  public static void writeString(File file, String content)
    throws IOException
  {
    writeString(file, content, EncodingUtil.getDefaultEncoding(), false);
  }

  public static void writeString(File file, String content, boolean append)
    throws IOException
  {
    writeString(file, content, EncodingUtil.getDefaultEncoding(), append);
  }

  public static void writeString(File file, String content, String encoding, boolean append)
    throws IOException
  {
    Writer writer = null;
    try
    {
      writer = EncodingUtil.createWriter(file, encoding, append);
      writer.write(content);
    }
    finally
    {
      closeQuietely(writer);
    }
  }

  public static String detectFileEncoding(File file)
  {
    if (file == null) return null;
    if (!file.exists()) return null;

    byte[] buf = new byte[4096];

    UniversalDetector detector = new UniversalDetector(null);
    String encoding = null;
    FileInputStream fis = null;
    try
    {
      fis = new FileInputStream(file);
      int nread;
      while ((nread = fis.read(buf)) > 0 && !detector.isDone())
      {
        detector.handleData(buf, 0, nread);
      }
      detector.dataEnd();
      encoding = detector.getDetectedCharset();

      if (encoding != null && encoding.toUpperCase().startsWith("UTF"))
      {
        Reader r = null;
        try
        {
          r = EncodingUtil.createReader(file, encoding);
          if (r instanceof UnicodeReader)
          {
            UnicodeReader ur = (UnicodeReader)r;
            if (ur.hasBOM())
            {
              encoding = ur.getEncoding();
            }
          }
        }
        finally
        {
          closeQuietely(r);
        }
      }
      LogMgr.logInfo("FileUtil.detectFileEncoding()", "Detected encoding: " + encoding + " for file " + file.getAbsolutePath());
    }
    catch (Throwable th)
    {
      LogMgr.logError("FileUtil.detectFileEncoding()", "Could not detect file encoding", th);
    }
    finally
    {
      closeQuietely(fis);
      detector.reset();
    }
    return encoding;
  }

  /**
   * Sorts the file such that directories come first, and regular files afterwards.
   *
   * @param files
   */
  public static void sortFiles(List<? extends File> files)
  {
    Comparator<File> comp = (File o1, File o2) ->
    {
      if (Objects.equals(o1, o2)) return 0;
      if (o1 == null && o2 != null) return -1;
      if (o1 != null && o2 == null) return 1;

      if (o1.isDirectory() && o2.isFile()) return -1;
      if (o1.isFile() && o2.isDirectory()) return 1;

      // both objects are either files or directories;
      return o1.compareTo(o2);
    };
    Collections.sort(files, comp);
  }

  public static boolean deleteSilently(File toDelete)
  {
    if (toDelete == null) return false;
    try
    {
      return toDelete.delete();
    }
    catch (Throwable th)
    {
      LogMgr.logWarning("FileUtil.deleteSilently()", "Could not delete file: " + toDelete);
      return false;
    }
  }

  /**
   * Reads input from System.in if data is avaiable.
   *
   * @return  the current (pending) input from System.in or null if nothing is there to read.
   */
  public static String getSystemIn()
  {
    try
    {
      ByteArrayOutputStream out = new ByteArrayOutputStream(50);
      if (System.in.available() != 0)
      {
        int ch = System.in.read();
        while (ch > -1)
        {
          out.write(ch);
          ch = System.in.read();
        }
        String data = out.toString();
        return data;
      }
    }
    catch (Throwable th)
    {
    }
    return null;
  }

  public static boolean isDLLAvailable(String dllName)
  {
    String jarPath = WbManager.getInstance().getJarPath();
    WbFile dll = new WbFile(jarPath, dllName);
    if (dll.exists())
    {
      return true;
    }
    String libPath = System.getProperty("java.library.path");
    String separator = StringUtil.quoteRegexMeta(File.pathSeparator);
    String[] pathElements = libPath.split(separator);
    for (String dir : pathElements)
    {
      File f = new File(dir, dllName);
      if (f.exists()) return true;
    }
    return false;
  }

  public static boolean isDirectoryOnLibraryPath(File toTest)
  {
    if (toTest == null) return false;

    String libPath = System.getProperty("java.library.path");
    String separator = StringUtil.quoteRegexMeta(File.pathSeparator);
    String[] pathElements = libPath.split(separator);
    for (String dir : pathElements)
    {
      File f = new File(dir);
      if (f.equals(toTest)) return true;
    }
    return false;
  }
}
