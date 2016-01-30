/*
 * RecordFormPanel.java
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
package workbench.gui.sql;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.EventQueue;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.sql.Types;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollBar;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.ScrollPaneConstants;
import javax.swing.UIManager;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.table.TableModel;
import javax.swing.text.JTextComponent;

import workbench.interfaces.ValidatingComponent;
import workbench.log.LogMgr;
import workbench.resource.GuiSettings;
import workbench.resource.Settings;

import workbench.db.ColumnIdentifier;

import workbench.gui.WbSwingUtilities;
import workbench.gui.actions.MultilineWrapAction;
import workbench.gui.actions.WbAction;
import workbench.gui.components.BlobHandler;
import workbench.gui.components.DataStoreTableModel;
import workbench.gui.components.TextComponentMouseListener;
import workbench.gui.components.WbDocument;
import workbench.gui.components.WbTable;
import workbench.gui.components.WbTraversalPolicy;
import workbench.gui.renderer.BlobColumnPanel;
import workbench.gui.renderer.NumberColumnRenderer;
import workbench.gui.renderer.RendererFactory;
import workbench.gui.renderer.WbRenderer;
import workbench.gui.renderer.WrapEnabledEditor;

import workbench.storage.DataStore;
import workbench.storage.ResultInfo;

import workbench.util.SqlUtil;

/**
 * A Panel that displays a single record from a WbTable
 * but lets the user step through the records.
 *
 * The fields are defined through a ResultInfo.
 *
 * @author Thomas Kellerer
 */
public class RecordFormPanel
	extends JPanel
	implements ValidatingComponent, ActionListener
{
	private ResultInfo fieldDef;
	private JComponent[] inputControls;
	private WbRenderer[] renderer;
	private BlobHandler[] blobHandlers;

	private int currentRow;
	private WbTable data;
	private int toFocus;

	public RecordFormPanel(WbTable table, int displayRow)
	{
		this(table, displayRow, 0);
	}

	public RecordFormPanel(WbTable table, int displayRow, int columnToFocus)
	{
		super(new BorderLayout());
		fieldDef = table.getDataStore().getResultInfo();
		data = table;
		toFocus = (columnToFocus >= 0 ? columnToFocus : 0);
		currentRow = displayRow;
		buildEntryForm();
		showRecord(displayRow);
	}

	public DataStore getDataStore()
	{
		if (this.data == null) return null;
		return data.getDataStore();
	}

	protected void buildEntryForm()
	{
		WbSwingUtilities.invoke(this::_buildEntryForm);
	}

	public boolean isEditingAllow()
	{
		boolean allowEditing = true;
		TableModel model = data.getModel();
		if (model instanceof DataStoreTableModel)
		{
			allowEditing = ((DataStoreTableModel)model).getAllowEditing();
		}
		return allowEditing;
	}

	public int getCurrentRow()
	{
		return currentRow;
	}

	public int getRowCount()
	{
		return data.getRowCount();
	}

	protected void _buildEntryForm()
	{
		if (fieldDef == null) return;

		this.removeAll();
		JPanel formPanel = new JPanel(new GridBagLayout());

		GridBagConstraints c = new GridBagConstraints();
		c.gridx = 0;
		c.gridy = 0;
		c.anchor = GridBagConstraints.NORTHWEST;
		c.weighty = 0.0;

		inputControls = new JComponent[fieldDef.getColumnCount()];
		blobHandlers = new BlobHandler[inputControls.length];
		Color requiredColor = GuiSettings.getRequiredFieldColor();
		Insets labelInsets = new Insets(5, 0,5,0);
		Insets fieldInsets = new Insets(0,10,5,5);

		Font displayFont = Settings.getInstance().getDataFont(true);
		if (displayFont == null)
		{
			displayFont = UIManager.getFont("TextField.font");
		}
		FontMetrics fm = getFontMetrics(displayFont);
		int numChars = GuiSettings.getDefaultFormFieldWidth();
		int charWidth = (fm != null ? fm.getMaxAdvance() : 12);
		int charHeight = (fm != null ? fm.getHeight() + 5 : 16);
		int fieldWidth = charWidth * numChars;
		int areaHeight = charHeight * GuiSettings.getDefaultFormFieldLines();

		Dimension areaSize = new Dimension(fieldWidth, areaHeight);

		boolean editable = isEditingAllow();
		boolean showRequired = GuiSettings.getHighlightRequiredFields() && requiredColor != null;

		for (int i=0; i < fieldDef.getColumnCount(); i++)
		{
			c.gridx = 0;
			c.fill = GridBagConstraints.NONE;
			c.weightx = 0.0;
			c.insets = labelInsets;
			c.anchor = GridBagConstraints.NORTHEAST;

			ColumnIdentifier col = fieldDef.getColumn(i);
			JLabel label = new JLabel(col.getColumnName());
			label.setToolTipText(col.getDbmsType());
			formPanel.add(label, c);

			c.gridx = 1;
			c.weightx = 1.0;
			c.insets = fieldInsets;
			c.anchor = GridBagConstraints.NORTHWEST;

			Component toAdd = null;
			if (SqlUtil.isMultiLineColumn(col))
			{
				final JTextArea area = new JTextArea(new WbDocument());
				WrapEnabledEditor wrap = new WrapEnabledEditor()
				{
					@Override
					public void setWordwrap(boolean flag)
					{
						area.setLineWrap(flag);
						area.setWrapStyleWord(flag);
					}
				};
				area.setEditable(editable);
				TextComponentMouseListener contextMenu = new TextComponentMouseListener();
				contextMenu.addAction(new MultilineWrapAction(wrap, area, null));

				area.addMouseListener(contextMenu);
				area.setLineWrap(false);

				inputControls[i] = area;

				JScrollPane scroll = new JScrollPane(area, ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED, ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
				scroll.setMinimumSize(areaSize);
				scroll.setPreferredSize(areaSize);
				inputControls[i].setFont(displayFont);
				c.fill = GridBagConstraints.BOTH;
				c.weighty = 1.0;
				toAdd = scroll;
			}
			else if (SqlUtil.isBlobType(col.getDataType()))
			{
				JButton b = new JButton(" (BLOB) ");
				b.addActionListener(this);
				inputControls[i] = b;
				c.fill = GridBagConstraints.NONE;
				c.weighty = 0.0;
				toAdd = inputControls[i];
			}
			else
			{
				JTextField f = new JTextField(new WbDocument(), null, numChars);
				f.setEditable(editable);
				TextComponentMouseListener.addListener(f);
				inputControls[i] = f;
				inputControls[i].setFont(displayFont);
				c.fill = GridBagConstraints.HORIZONTAL;
				c.weighty = 0.0;
				toAdd = inputControls[i];
			}

			if (i == fieldDef.getColumnCount() - 1)
			{
				c.weighty = 1.0;
			}
			formPanel.add(toAdd, c);
			if (showRequired && !col.isNullable())
			{
				inputControls[i].setBackground(requiredColor);
			}

			c.gridy ++;
		}

		// Initialize the correct tab focus order
		WbTraversalPolicy policy = new WbTraversalPolicy();
    for (JComponent inputControl : inputControls)
    {
      policy.addComponent(inputControl);
    }

		// Put the input form into a scrollpane in case there are a lot of columns in the row
		JScrollPane formScroll = new JScrollPane(formPanel, ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED, ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
		formScroll.setWheelScrollingEnabled(true);
		JScrollBar vscroll = formScroll.getVerticalScrollBar();
		if (vscroll != null)
		{
			vscroll.setUnitIncrement(charHeight * 2); // clicking on the scrollbar arrows, scrolls by two "lines"
			vscroll.setBlockIncrement(charHeight * 10); // clicking on the scrollbar "free area", scrolls by ten "lines"
		}
		formScroll.setBorder(new CompoundBorder(BorderFactory.createEtchedBorder(), new EmptyBorder(10,5,10,5)));
		this.add(formScroll, BorderLayout.CENTER);

		// make the first input field the default component
		// this might be changed later when the componentDisplayed()
		// method is invoked and an initial column has been defined
		policy.setDefaultComponent(inputControls[0]);
		initRenderer();

		this.add(new FormNavigation(this), BorderLayout.SOUTH);
	}

	private void initRenderer()
	{
		if (fieldDef == null) return;
		if (inputControls == null) return;
		renderer = new WbRenderer[inputControls.length];

		Settings sett = Settings.getInstance();
		String dateFormat = sett.getDefaultDateFormat();
		WbRenderer dateRenderer = (WbRenderer)RendererFactory.getDateRenderer(dateFormat);
		String tsFormat = sett.getDefaultTimestampFormat();
		WbRenderer tsRenderer = (WbRenderer)RendererFactory.getDateRenderer(tsFormat);

		int maxDigits = sett.getMaxFractionDigits();
		char sep = sett.getDecimalSymbol().charAt(0);

		WbRenderer numberRenderer = new NumberColumnRenderer(maxDigits, sep);
		for (int i=0; i < fieldDef.getColumnCount(); i++)
		{
			int type = fieldDef.getColumnType(i);
			if (SqlUtil.isNumberType(type))
			{
				renderer[i] = numberRenderer;
			}
			else if (type == Types.DATE)
			{
				renderer[i] = dateRenderer;
			}
			else if (type == Types.TIMESTAMP)
			{
				renderer[i] = tsRenderer;
			}
		}
	}


	/**
	 * Displays the data from the passed RowData.
	 *
	 * @param rowNumber the row number to display
	 */
	public void showRecord(int rowNumber)
	{
		currentRow = rowNumber;
		WbSwingUtilities.invoke(this::_showRecord);
	}

	protected void _showRecord()
	{
		for (int i=0; i < fieldDef.getColumnCount(); i++)
		{
			Object value = data.getValueAt(currentRow, getTableColumn(i));
			if (value == null)
			{
				if (inputControls[i] instanceof JTextComponent)
				{
					((JTextComponent)inputControls[i]).setText("");
				}
				if (inputControls[i] instanceof BlobColumnPanel)
				{
					((BlobColumnPanel)inputControls[i]).setValue(null);
				}
			}
			else
			{
				if (SqlUtil.isBlobType(fieldDef.getColumnType(i)))
				{
					blobHandlers[i] = null;
				}
				else
				{
					String display = null;
					if (renderer[i] != null)
					{
						renderer[i].prepareDisplay(value);
						display = renderer[i].getDisplayValue();
					}
					else
					{
						display = value.toString();
					}

					if (inputControls[i] instanceof JTextComponent)
					{
						JTextComponent text = (JTextComponent)inputControls[i];
						text.setText(display);
						text.setCaretPosition(0);
					}
				}
			}
		}
		resetDocuments();
	}

	/**
	 * Reset the modified flag of the input fields
	 */
  protected void resetDocuments()
  {
    for (JComponent inputControl : inputControls)
    {
      if (inputControl instanceof JTextComponent)
      {
        JTextComponent text = (JTextComponent)inputControl;
        WbDocument doc = (WbDocument)text.getDocument();
        doc.resetModified();
      }
    }
  }

	/**
	 * Check if anything has changed on the input fields
	 *
	 * @return true if at least one column value has been modified by the user
	 */
	public boolean isChanged()
	{
		boolean isChanged = false;
		for (int i=0; i < inputControls.length; i++)
		{
			if (inputControls[i] instanceof JTextComponent)
			{
				JTextComponent text = (JTextComponent)inputControls[i];
				WbDocument doc = (WbDocument)text.getDocument();
				isChanged = isChanged || doc.isModified();
			}
			else if (inputControls[i] instanceof JButton)
			{
				BlobHandler handler = blobHandlers[i];
				if (handler != null)
				{
					File f = handler.getUploadFile();
					if (f != null)
					{
						isChanged = true;
					}
					else if (handler.isChanged())
					{
						isChanged = true;
					}
					else if (handler.setToNull())
					{
						isChanged = true;
					}
				}
			}
		}
		return isChanged;
	}


	public void applyChanges()
	{
		for (int i=0; i < inputControls.length; i++)
		{
			if (inputControls[i] instanceof JTextComponent)
			{
				JTextComponent text = (JTextComponent)inputControls[i];
				WbDocument doc = (WbDocument)text.getDocument();
				if (doc.isModified())
				{
					String newValue = text.getText();
					data.setValueAt(newValue, currentRow, getTableColumn(i));
				}
			}
			else if (inputControls[i] instanceof JButton)
			{
				BlobHandler handler = blobHandlers[i];
				if (handler != null)
				{
					File f = handler.getUploadFile();
					if (f != null)
					{
						data.setValueAt(f, currentRow, getTableColumn(i));
					}
					else if (handler.isChanged())
					{
						data.setValueAt(handler.getNewValue(), currentRow, getTableColumn(i));
					}
					else if (handler.setToNull())
					{
						data.setValueAt(null, currentRow, getTableColumn(i));
					}
				}
			}
		}
	}

	private int getTableColumn(int dataColumn)
	{
		int offset = data.isStatusColumnVisible() ? 1 : 0;
		int colIndex = dataColumn + offset;
		int viewIndex = data.convertColumnIndexToView(colIndex);
		return viewIndex;
	}

	private boolean startEdit()
	{
		Container tableParent = data.getParent();
		DwPanel panel = null;
		while (tableParent != null)
		{
			if (tableParent instanceof DwPanel)
			{
				panel = ((DwPanel)tableParent);
				break;
			}
			tableParent = tableParent.getParent();
		}
		if (panel != null)
		{
			return panel.startEdit();
		}
		return false;
	}

	@Override
	public boolean validateInput()
	{
		if (!isChanged()) return true;
		DataStoreTableModel model = data.getDataStoreTableModel();
		try
		{
			if (!startEdit()) return false;
			model.setShowConverterError(false);
			applyChanges();
			return true;
		}
		catch (Exception e)
		{
			LogMgr.logError("RecordFormPanel.validateInput()", "Error during validate", e);
			WbSwingUtilities.showErrorMessage(e.getMessage());
			return false;
		}
		finally
		{
			model.setShowConverterError(true);
		}
	}

  @Override
  public void componentWillBeClosed()
  {
		// nothing to do
  }

	@Override
	public void componentDisplayed()
	{
		if (inputControls == null) return;
		EventQueue.invokeLater(inputControls[toFocus]::requestFocusInWindow);
	}

	private int getColumnIndexFor(Component component)
	{
		for (int i=0; i < inputControls.length; i++)
		{
			if (inputControls[i] == component) return i;
		}
		return -1;
	}

	private void showBlob(int column, boolean ctrlPressed, boolean shiftPressed)
	{
		Object currentValue = null;
		if (blobHandlers[column] == null)
		{
			blobHandlers[column] = new BlobHandler();
			currentValue = data.getValueAt(currentRow, column);
		}
		else
		{
			File f = blobHandlers[column].getUploadFile();
			if (f != null)
			{
				currentValue = f;
			}
			else if (blobHandlers[column].isChanged())
			{
				currentValue = blobHandlers[column].getNewValue();
			}
			else if (blobHandlers[column].setToNull())
			{
				currentValue = null;
			}
		}

		if (ctrlPressed)
		{
			blobHandlers[column].showBlobAsText(currentValue);
		}
		else if (shiftPressed)
		{
			blobHandlers[column].showBlobAsImage(currentValue);
		}
		else
		{
			blobHandlers[column].showBlobInfoDialog(null, currentValue, !isEditingAllow());
		}

		//Object newValue = blobHandlers[column].getValueToUse();
	}

	@Override
	public void actionPerformed(ActionEvent e)
	{
		boolean ctrlPressed = WbAction.isCtrlPressed(e);
		boolean shiftPressed = WbAction.isShiftPressed(e);
		Object source = e.getSource();

		if (source instanceof JComponent)
		{
			JComponent button = (JComponent)source;
			int index = getColumnIndexFor(button);
			if (index > -1)
			{
				showBlob(index, ctrlPressed, shiftPressed);
			}
		}
	}

}
