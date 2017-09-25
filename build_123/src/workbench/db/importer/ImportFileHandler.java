/*
 * ImportFileHandler.java
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
package workbench.db.importer;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.io.StringReader;
import java.util.Enumeration;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import workbench.log.LogMgr;
import workbench.resource.Settings;

import workbench.db.exporter.RowDataConverter;

import workbench.util.ClipboardFile;
import workbench.util.EncodingUtil;
import workbench.util.FileUtil;
import workbench.util.WbFile;
import workbench.util.ZipUtil;

/**
 * This class manages access to an import file and possible attachments that
 * were created by {@link workbench.db.exporter.DataExporter}.
 *
 * The import file can either be a regular file, or stored in a ZIP archive.
 *
 * @author Thomas Kellerer
 */
public class ImportFileHandler
{
  private File baseFile;
  private File baseDir;
  private String encoding;
  private boolean isZip;
  private ZipFile mainArchive;
  private ZipFile attachments;
  private BufferedReader mainReader;
  private String usedZipEntry;

  /**
   * Define the main input file used by this handler.
   * <br/>
   * If the file is a ZIP Archive getMainFileReader() will
   * return a Reader for the first file in the archive.
   * (DataExporter creates an archive with a single
   * file in it).
   *
   * @param mainFile the basefile
   * @param enc the encoding for the basefile
   */
  public void setMainFile(File mainFile, String enc)
    throws IOException
  {
    this.done();
    this.encoding = enc;

    this.baseFile = mainFile;
    this.baseDir = baseFile.getParentFile();
    if (this.baseDir == null) baseDir = new File(".");
    isZip = ZipUtil.isZipFile(baseFile);
    this.initAttachements();
  }

  /**
   * Used for unit tests
   *
   * @return true if the import file is a ZIP file
   */
  boolean isZip()
  {
    return isZip;
  }

  /**
   * Return a Reader that is suitable for reading the contents
   * of the main file. The reader will be created with the
   * encoding that was specified in {@link #setMainFile(File, String)}
   *
   * @return a BufferedReader for the main file
   * @see #setMainFile(File, String)
   */
  public BufferedReader getMainFileReader()
    throws IOException
  {
    FileUtil.closeQuietely(mainReader);

    Reader r = null;
    if (baseFile instanceof ClipboardFile)
    {
      ClipboardFile cb = (ClipboardFile)baseFile;
      r = new StringReader(cb.getContents());
    }
    else if (isZip)
    {
      if (mainArchive == null)
      {
        mainArchive = new ZipFile(baseFile);
      }
      Enumeration entries = mainArchive.entries();
      if (entries.hasMoreElements())
      {
        ZipEntry entry = (ZipEntry)entries.nextElement();
        LogMgr.logInfo("ImportFileHandler.getMainFileReader()", "Using ZIP entry " + entry.getName() + " from input file " + baseFile.getAbsolutePath());
        usedZipEntry = entry.getName();
        InputStream in = mainArchive.getInputStream(entry);
        r = EncodingUtil.createReader(in, encoding);
      }
      else
      {
        throw new FileNotFoundException("Zipfile " + this.baseFile.getAbsolutePath() + " does not contain any entries!");
      }
    }
    else
    {
      r = EncodingUtil.createReader(baseFile, encoding);
    }
    mainReader = new BufferedReader(r, getFileBufferSize());
    return mainReader;
  }

  public String getInputFilename()
  {
    if (this.baseFile == null) return "";
    String result = baseFile.getAbsolutePath();
    if (this.isZip && usedZipEntry != null)
    {
      result += ":" + usedZipEntry;
    }
    return result;
  }

  private int getFileBufferSize()
  {
    return Settings.getInstance().getIntProperty("workbench.import.file.buffsize", 64*1024);
  }

  private void initAttachements()
    throws IOException
  {
    if (baseFile instanceof ClipboardFile) return;

    WbFile f = new WbFile(baseFile);
    String basename = f.getFileName();
    String attFileName = basename + RowDataConverter.BLOB_ARCHIVE_SUFFIX + ".zip";
    File attFile = new File(baseDir, attFileName);
    if (attFile.exists())
    {
      this.attachments = new ZipFile(attFile);
    }
  }

  protected ZipEntry findEntry(File f)
    throws IOException
  {
    ZipEntry entry = this.attachments.getEntry(f.getName());
    if (entry != null) return entry;

    throw new FileNotFoundException("Attachment file " + f.getName() + " not found in archive " + this.attachments.getName());
  }

  /**
   * Get an input stream for a possible attachment (LOB file).
   * <br/>
   * When exporting LOB data {@link workbench.db.exporter.DataExporter} will write
   * the LOB data for each row/column into separate files. These files might
   * reside in a second ZIP archive.
   *
   * @param attachmentFile the attachment to read
   * @return an InputStream to read the attachment
   */
  public InputStream getAttachedFileStream(File attachmentFile)
    throws IOException
  {
    if (baseFile instanceof ClipboardFile) throw new IOException("Attachments not supported for Clipboard");

    if (this.isZip)
    {
      ZipEntry entry = findEntry(attachmentFile);
      return attachments.getInputStream(entry);
    }
    else
    {
      File inputFile = getRealFile(attachmentFile);
      return new BufferedInputStream(new FileInputStream(inputFile), getFileBufferSize());
    }
  }

  private File getRealFile(File attachmentFile)
  {
    if (attachmentFile.isAbsolute())
    {
      return attachmentFile;
    }
    else
    {
      File subdir = attachmentFile.getParentFile();
      File blobdir = null;
      if (subdir != null)
      {
        blobdir = new File(baseDir, subdir.getName());
      }
      else
      {
        blobdir = baseDir;
      }
      File realFile = new File(blobdir,attachmentFile.getName());
      return realFile;
    }
  }

  /**
   * Retrieve the length of the file in characters, rather than bytes.
   * <br/>
   * For single-byte encodings this  is the same, but for a multi-byte
   * encoding the file length is not the same as the number of
   * characters in the file.
   *
   * @param f the file to check
   * @return the number of characters (not bytes) in that file.
   * @throws java.io.IOException
   *
   * @see FileUtil#getCharacterLength(java.io.File, java.lang.String)
   */
  public long getCharacterLength(File f)
    throws IOException
  {
    if (this.isZip)
    {
      return getLength(f);
    }

    File inputFile = getRealFile(f);
    return FileUtil.getCharacterLength(inputFile, this.encoding);
  }

  /**
   * Gets the length of the file.
   *
   * @param toTest
   * @return the number of bytes in the given file
   * @throws java.io.IOException
   */
  public long getLength(File toTest)
    throws IOException
  {
    if (this.isZip)
    {
      ZipEntry entry = findEntry(toTest);
      return entry.getSize();
    }

    File realFile = getRealFile(toTest);
    return realFile.length();
  }

  public String getEncoding()
  {
    return this.encoding;
  }

  public void done()
  {
    FileUtil.closeQuietely(mainReader);
    ZipUtil.closeQuitely(mainArchive);
    ZipUtil.closeQuitely(attachments);
    mainReader = null;
    mainArchive = null;
    attachments = null;
    usedZipEntry = null;
  }
}
