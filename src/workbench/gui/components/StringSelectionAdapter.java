/*
 * ClipBoardCopier.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2016, Thomas Kellerer
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at.
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
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
package workbench.gui.components;

import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.ClipboardOwner;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.io.*;

import workbench.log.LogMgr;
import workbench.resource.Settings;


/**
 * A class to put plain text and HTML content into the clipboard.
 *
 * @author Francesco Trentini
 */
public class StringSelectionAdapter
  implements Transferable, ClipboardOwner
{
  private final DataFlavor[] flavors;

  private final String data; //plaintext
  private final String dataAsHTML; //html filled if requested

  public StringSelectionAdapter(String text, boolean includeHtml)
  {
    this.data = text;
    if (includeHtml)
    {
      flavors = new DataFlavor[] { DataFlavor.fragmentHtmlFlavor, DataFlavor.stringFlavor };
      dataAsHTML = createHtmlFragment(text);
    }
    else
    {
      flavors = new DataFlavor[] { DataFlavor.stringFlavor };
      dataAsHTML = null;
    }
  }

  private String createHtmlFragment(String text)
  {
    try
    {
      String defaultCss =
        "table, th, td { border: 1px solid black; border-collapse: collapse; } " +
        "th, td { padding: 5px; text-align: left; } " +
        "table tr:nth-child(even) { background-color: #eee; } table tr:nth-child(odd) { background-color:#fff; } " +
        "table th { background-color: black; color: white; }";

      String css = Settings.getInstance().getCssForClipboardHtml(defaultCss);
      String preHtml = "<html><head><style>" + css + "</style></head><body><table>";
      String postHtml = "</table></body></html>";
      String trOpen = "<tr>";
      String trClose = "</tr>";
      String tdOpen = "<td>";
      String tdClose = "</td>";
      StringReader srctext = new StringReader(text);
      BufferedReader src = new BufferedReader(srctext);
      StringBuilder dst = new StringBuilder(text.length());


      dst.append(preHtml);
      for (String line = src.readLine(); line != null; line = src.readLine())
      {
        String[] fields = line.split("\t");
        for (int i = 0; i < fields.length; i++)
        {
          fields[i] = tdOpen + fields[i] + tdClose;
        }
        dst.append(trOpen);
        for (String tk : fields)
        {
          dst.append(tk);
        }
        dst.append(trClose);
      }
      dst.append(postHtml);

      return dst.toString();
    }
    catch (Exception ex)
    {
      LogMgr.logError("StringSelectionAdapter.createHtmlFragment()", "Could not create HTML fragment", ex);
      return null;
    }
  }

  @Override
  public DataFlavor[] getTransferDataFlavors()
  {
    return (DataFlavor[])flavors.clone();
  }

  @Override
  public boolean isDataFlavorSupported(DataFlavor flavor)
  {
    for (DataFlavor flv : flavors)
    {
      if (flv.equals(flavor)) return true;
    }
    return false;
  }

  @Override
  public Object getTransferData(DataFlavor flavor)
    throws UnsupportedFlavorException, IOException
  {
    if (!isDataFlavorSupported(flavor))
    {
      throw new UnsupportedFlavorException(flavor);
    }

    if (flavor.equals(DataFlavor.fragmentHtmlFlavor) && dataAsHTML != null)
    {
      return dataAsHTML;
    }

    return data;
  }

  @Override
  public void lostOwnership(Clipboard clipboard, Transferable contents)
  {
  }
}
