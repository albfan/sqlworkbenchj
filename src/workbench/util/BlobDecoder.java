/*
 * BlobDecoder.java
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

import java.io.File;
import java.io.IOException;

import javax.xml.bind.DatatypeConverter;

import workbench.db.exporter.BlobMode;

import workbench.storage.BlobLiteralType;

import static workbench.db.exporter.BlobMode.*;

/**
 *
 * @author Thomas Kellerer
 */
public class BlobDecoder
{
  public File baseDir;
  public BlobMode mode;

  public BlobDecoder()
  {
    mode = BlobMode.SaveToFile;
  }

  public void setBlobMode(BlobMode bmode)
  {
    mode = bmode;
  }

  public void setBaseDir(File dir)
  {
    baseDir = dir;
  }

  public Object decodeBlob(String value)
    throws IOException
  {
    if (StringUtil.isEmptyString(value)) return null;

    switch (mode)
    {
      case SaveToFile:
        File bfile = new File(value.trim());
        if (!bfile.isAbsolute() && baseDir != null)
        {
          bfile = new File(value.trim());
        }
        return bfile;

      case Base64:
        return DatatypeConverter.parseBase64Binary(value);

      case AnsiLiteral:
        return decodeHex(value);
    }
    return value;
  }

  public byte[] decodeString(String value, BlobLiteralType type)
    throws IOException
  {
    if (StringUtil.isEmptyString(value)) return null;
    if (type == BlobLiteralType.base64)
    {
      return DatatypeConverter.parseBase64Binary(value);
    }
    else if (type == BlobLiteralType.octal)
    {
      return decodeOctal(value);
    }
    else if (type == BlobLiteralType.hex)
    {
      return decodeHex(value);
    }
    throw new IllegalArgumentException("BlobLiteralType " + type + " not supported");
  }

  private byte[] decodeOctal(String value)
    throws IOException
  {
    byte[] result = new byte[value.length() / 4];
    for (int i = 0; i < result.length; i++)
    {
      String digit = value.substring((i*4)+1, (i*4)+ 4);
      byte b = (byte)Integer.parseInt(digit, 8);
      result[i] = b;
    }
    return result;
  }

  private byte[] decodeHex(String value)
  {
    int offset = 0;
    int len = value.length();
    if (value.toLowerCase().startsWith("0x"))
    {
      offset = 2;
    }
    else if (value.toLowerCase().startsWith("x'"))
    {
      // ANSI BLOB literal X'ff...00'
      offset = 2;
      len --;
    }

    byte[] result = new byte[(len - offset) / 2];

    for (int i = 0; i < result.length; i++)
    {
      String digit = value.substring((i*2)+offset, (i*2) + 2 + offset);
      byte b = (byte)Integer.parseInt(digit, 16);
      result[i] = b;
    }
    return result;
  }
}
