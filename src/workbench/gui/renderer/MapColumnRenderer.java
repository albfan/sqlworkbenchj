/*
 * BlobColumnRenderer.java
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
package workbench.gui.renderer;

import java.sql.Types;
import java.util.Map;

import javax.swing.JPanel;

import workbench.resource.Settings;

import workbench.gui.WbSwingUtilities;
import workbench.gui.components.MapEditor;
import workbench.gui.components.ValidatingDialog;

import workbench.storage.DataStore;

/**
 * A class to render and edit BLOB columns in a result set.
 * <br/>
 * It uses a BlobColumnPanel to display the information to the user. The renderer
 * is registered with the BlobColumnPanel's button as an actionlistener and will then
 * display a dialog with details about the blob.
 * <br/>
 *
 * @see workbench.gui.components.BlobHandler#showBlobInfoDialog(java.awt.Frame, Object, boolean)
 *
 * @author  Thomas Kellerer
 */
public class MapColumnRenderer
	extends AbstractDialogRenderer
{
  private final String CONFIG_PROP = "workbench.gui.mapeditor";
	private ButtonDisplayPanel display;

	public MapColumnRenderer()
	{
		super();
	}

  @Override
  protected JPanel createDisplayPanel()
  {
    display = new ButtonDisplayPanel();
    display.addActionListener(this);
    return display;
  }

  @Override
  protected void setCurrentValue(Object value)
  {
    display.setDisplayValue(value == null ? "" : value.toString());
  }

  @Override
  protected void showEditDialog(boolean allowEditing, boolean ctrlPressed, boolean shiftPressed)
  {
    Map<String, String> data = null;
    if (currentValue instanceof Map)
    {
      data = (Map<String, String>)currentValue;
    }
    MapEditor editor = new MapEditor(data)
    {
      @Override
      protected DataStore createDataStore()
      {
        String[] cols = new String[] { "Key", "Value" };
        int[] types = new int[] { Types.VARCHAR, Types.VARCHAR };
        int[] sizes = new int[] { 15, 25 };
        return new DataStore(cols, types, sizes);
      }
    };

    ValidatingDialog dialog = ValidatingDialog.createDialog(WbSwingUtilities.getWindowAncestor(currentTable), editor, "Edit", null, 0, false);

    if (!Settings.getInstance().restoreWindowSize(dialog, CONFIG_PROP))
    {
      dialog.setSize(400, 300);
    }
    editor.optimizeColumnWidths();
    editor.setEditable(allowEditing);
    dialog.setVisible(true);

    Settings.getInstance().storeWindowSize(dialog, CONFIG_PROP);

    if (!dialog.isCancelled() && editor.isModified())
    {
      Map<String, String> map = editor.getMap();
      currentTable.setValueAt(map, currentRow, currentColumn);
      currentValue = map;
    }

	}

}
