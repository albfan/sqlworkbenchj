/*
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2016 Thomas Kellerer.
 *
 * Licensed under a modified Apache License, Version 2.0 (the "License")
 * that restricts the use for certain governments.
 * You may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.sql-workbench.net/manual/license.html
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * To contact the author please send an email to: support@sql-workbench.net
 */
package workbench.gui.renderer;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.sql.Types;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EventObject;
import java.util.List;
import java.util.Map;

import javax.swing.CellEditor;
import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.border.EmptyBorder;
import javax.swing.event.CellEditorListener;
import javax.swing.event.ChangeEvent;
import javax.swing.table.TableCellEditor;
import javax.swing.text.JTextComponent;

import workbench.interfaces.NullableEditor;
import workbench.resource.Settings;

import workbench.db.postgres.HstoreMap;
import workbench.db.postgres.HstoreSupport;

import workbench.gui.WbSwingUtilities;
import workbench.gui.actions.SetNullAction;
import workbench.gui.components.FlatButton;
import workbench.gui.components.MapEditor;
import workbench.gui.components.TextComponentMouseListener;
import workbench.gui.components.ValidatingDialog;
import workbench.gui.components.WbTable;

import workbench.storage.DataStore;



/**
 *
 * @author Thomas Kellerer
 */
public class HstoreEditor
  implements CellEditor, TableCellEditor, ActionListener, MouseListener, NullableEditor
{
  private final String CONFIG_PROP = "workbench.gui.hstoreeditor";
  private JPanel component;
  private JTextField textField;
  private FlatButton openButton;
  private final List<CellEditorListener> listeners = Collections.synchronizedList(new ArrayList<>());
  private SetNullAction setNull;
  private TextComponentMouseListener contextMenu;
  private boolean isNull;
  private Color defaultBackground;

  public HstoreEditor()
  {
    component = new JPanel(new BorderLayout(0,0));
    textField = new JTextField()
    {
      @Override
      public Insets getInsets(Insets insets)
      {
        return new Insets(0, 0, 0, 0);
      }

      @Override
      public Insets getInsets()
      {
        return new Insets(0, 0, 0, 0);
      }

    };
    defaultBackground = textField.getBackground();
    textField.setBorder(new EmptyBorder(0,0,0,0));
    textField.setFont(Settings.getInstance().getDataFont(true));
    textField.addMouseListener(this);
    contextMenu = new TextComponentMouseListener();
    setNull = new SetNullAction(this);
    contextMenu.addAction(setNull);
    setNull.addToInputMap(textField);
    textField.addMouseListener(contextMenu);

    component.add(textField, BorderLayout.CENTER);

  	openButton = new FlatButton("...");
		openButton.setBasicUI();
		openButton.setFlatLook();
		openButton.setBorder(WbSwingUtilities.FLAT_BUTTON_BORDER);
		openButton.setEnabled(true);
		openButton.setFocusable(false);
    openButton.addActionListener(this);
    component.add(openButton, BorderLayout.LINE_END);
  }

  @Override
  public void setNull(boolean setToNull)
  {
    if (setToNull)
    {
      textField.setText("");
    }
    isNull = setToNull;
  }

  @Override
  public JTextComponent getEditor()
  {
    return textField;
  }

  @Override
  public void restoreOriginal()
  {
  }

  @Override
  public void actionPerformed(ActionEvent e)
  {
    showEditDialog();
  }

  protected void showEditDialog()
  {
    Map<String, String> hstore = getHstore();
    MapEditor mapEditor = new MapEditor(hstore)
    {
      @Override
      protected DataStore createDataStore()
      {
        String[] cols = new String[]
        {
          "Key", "Value"
        };
        int[] types = new int[]
        {
          Types.VARCHAR, Types.VARCHAR
        };
        int[] sizes = new int[]
        {
          15, 25
        };
        return new DataStore(cols, types, sizes);
      }
    };

    ValidatingDialog dialog = ValidatingDialog.createDialog(WbSwingUtilities.getWindowAncestor(this.component), mapEditor, "Edit", null, 0, false);

    if (!Settings.getInstance().restoreWindowSize(dialog, CONFIG_PROP))
    {
      dialog.setSize(400, 300);
    }

    mapEditor.optimizeColumnWidths();
    mapEditor.setEditable(textField.isEditable());
    dialog.setVisible(true);

    Settings.getInstance().storeWindowSize(dialog, CONFIG_PROP);

    if (!dialog.isCancelled() && mapEditor.isModified())
    {
      Map<String, String> map = mapEditor.getMap();
      textField.setText(HstoreSupport.getDisplay(map));
      textField.selectAll();
    }
  }

  private Map getHstore()
  {
    String value = textField.getText();
    Map<String, String> hstore = HstoreSupport.parseLiteral(value);
    return hstore == null ? Collections.emptyMap() : hstore;
  }

  public void setEditable(boolean flag)
  {
    textField.setEditable(flag);
    if (!flag)
    {
      textField.setBackground(defaultBackground);
    }
    textField.getCaret().setVisible(true);
  }

  @Override
  public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int row, int column)
  {
    String display = "";
    if (value instanceof Map)
    {
      display = HstoreSupport.getDisplay((Map)value);
    }

		isNull = false;

		WbTable tbl = (WbTable)table;
		setEditable(!tbl.isReadOnly());

    textField.setText(display);
    textField.selectAll();
    return component;
  }

  @Override
  public Object getCellEditorValue()
  {
    if (isNull) return null;
    return new HstoreMap(getHstore());
  }

  @Override
  public boolean isCellEditable(EventObject anEvent)
  {
    if (anEvent instanceof MouseEvent)
    {
      return ((MouseEvent)anEvent).getClickCount() == 2;
    }
    return false;
  }

  @Override
  public boolean shouldSelectCell(EventObject anEvent)
  {
    return true;
  }

  @Override
  public boolean stopCellEditing()
  {
    fireEditingStopped();
    return true;
  }

  @Override
  public void cancelCellEditing()
  {
    fireEditingCancelled();
  }

  @Override
  public void addCellEditorListener(CellEditorListener l)
  {
    listeners.add(l);
  }

  @Override
  public void removeCellEditorListener(CellEditorListener l)
  {
    listeners.remove(l);
  }

  protected void fireEditingStopped()
  {
    ChangeEvent changeEvent = new ChangeEvent(this);
    List<CellEditorListener> list = new ArrayList<>(listeners);
    for (CellEditorListener l : list)
    {
      l.editingStopped(changeEvent);
    }
  }

  protected void fireEditingCancelled()
  {
    ChangeEvent changeEvent = new ChangeEvent(this);
    List<CellEditorListener> list = new ArrayList<>(listeners);
    for (CellEditorListener l : list)
    {
      l.editingCanceled(changeEvent);
    }
  }

  @Override
  public void mouseClicked(MouseEvent e)
  {
    if (e.getClickCount() == 2)
    {
      showEditDialog();
    }
  }

  @Override
  public void mousePressed(MouseEvent e)
  {
  }

  @Override
  public void mouseReleased(MouseEvent e)
  {
  }

  @Override
  public void mouseEntered(MouseEvent e)
  {
  }

  @Override
  public void mouseExited(MouseEvent e)
  {
  }


}
