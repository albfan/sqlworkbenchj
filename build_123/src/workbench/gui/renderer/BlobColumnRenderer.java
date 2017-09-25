/*
 * BlobColumnRenderer.java
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
package workbench.gui.renderer;

import java.io.File;

import javax.swing.JPanel;

import workbench.WbManager;
import workbench.resource.ResourceMgr;

import workbench.gui.components.*;

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
public class BlobColumnRenderer
	extends AbstractDialogRenderer
{
	private ButtonDisplayPanel blobPanel;

	public BlobColumnRenderer()
	{
		super();
	}

  @Override
  protected JPanel createDisplayPanel()
  {
    blobPanel = new ButtonDisplayPanel();
    blobPanel.addActionListener(this);
    blobPanel.setToolTipText(ResourceMgr.getDescription("LblShowBlobInfo", true));

    return blobPanel;
  }

  @Override
  protected void setCurrentValue(Object value)
  {
		if (value == null)
		{
			blobPanel.setDisplayValue("");
		}
		else
		{
			blobPanel.setDisplayValue("(BLOB)");
		}
  }

  @Override
  protected void showEditDialog(boolean allowEditing, boolean ctrlPressed, boolean shiftPressed)
  {
		BlobHandler handler = new BlobHandler();

		if (ctrlPressed)
		{
			handler.showBlobAsText(currentValue);
		}
		else if (shiftPressed)
		{
			handler.showBlobAsImage(currentValue);
		}
		else
		{
			handler.showBlobInfoDialog(WbManager.getInstance().getCurrentWindow(), currentValue, !allowEditing);
		}

		if (allowEditing)
		{
			File f = handler.getUploadFile();
			if (f != null)
			{
				currentTable.setValueAt(f, currentRow, currentColumn);
			}
			else if (handler.isChanged())
			{
				currentTable.setValueAt(handler.getNewValue(), currentRow, currentColumn);
			}
			else if (handler.setToNull())
			{
				currentTable.setValueAt(null, currentRow, currentColumn);
			}
		}
	}
}
