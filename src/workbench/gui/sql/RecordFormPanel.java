/*
 * 
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 * Copyright 2002-2008, Thomas Kellerer
 * 
 * No part of this code maybe reused without the permission of the author
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
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.ScrollPaneConstants;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.EtchedBorder;
import javax.swing.text.JTextComponent;
import workbench.db.ColumnIdentifier;
import workbench.gui.WbSwingUtilities;
import workbench.gui.actions.WbAction;
import workbench.gui.components.BlobHandler;
import workbench.gui.components.DataStoreTableModel;
import workbench.gui.components.WbDocument;
import workbench.gui.components.WbTable;
import workbench.gui.components.WbTraversalPolicy;
import workbench.gui.renderer.BlobColumnPanel;
import workbench.gui.renderer.RendererFactory;
import workbench.gui.renderer.WbRenderer;
import workbench.interfaces.ValidatingComponent;
import workbench.log.LogMgr;
import workbench.resource.GuiSettings;
import workbench.resource.Settings;
import workbench.storage.ResultInfo;
import workbench.util.SqlUtil;

/**
 * A Panel that displays a single record from a WbTable
 * but lets the user step through the records.
 * 
 * The fields are defined through a ResultInfo.
 *
 * @author support@sql-workbench.net
 */
public class RecordFormPanel
	extends JPanel
	implements ValidatingComponent, ActionListener
{
	private ResultInfo fieldDef;
	private JComponent[] inputFields;
	private WbRenderer[] renderer;
	private BlobHandler[] blobHandlers;
	
	private int currentRow;
	private WbTable data;
	private int toFocus = 0;
	
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

	protected void buildEntryForm()
	{
		WbSwingUtilities.invoke(new Runnable()
		{
			public void run()
			{
				_buildEntryForm();
			}
		});
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

		inputFields = new JComponent[fieldDef.getColumnCount()];
		blobHandlers = new BlobHandler[inputFields.length];
		Color requiredColor = GuiSettings.getRequiredFieldColor();
		Insets labelInsets = new Insets(2, 0,10, 0);
		Insets fieldInsets = new Insets(0,10,10,10);

		for (int i=0; i < fieldDef.getColumnCount(); i++)
		{
			c.gridx = 0;
			c.fill = GridBagConstraints.NONE;
			c.weightx = 0.0;
			c.insets = labelInsets;
			
			ColumnIdentifier col = fieldDef.getColumn(i);
			JLabel label = new JLabel(col.getColumnName());
			label.setToolTipText(col.getDbmsType());
			formPanel.add(label, c);
			c.gridx = 1;
			c.weightx = 1.0;
			c.insets = fieldInsets;

			Component toAdd = null;
			Font displayFont = Settings.getInstance().getDataFont(true);
			FontMetrics fm = getFontMetrics(displayFont);
			int numChars = GuiSettings.getDefaultFormFieldWidth();
			int charWidth = fm.getMaxAdvance();
			int charHeight = fm.getHeight() + 5;
			int fieldWidth = charWidth * numChars;
			int areaHeight = charHeight * GuiSettings.getDefaultFormFieldLines();
			
			if (SqlUtil.isMultiLineColumn(col))
			{
				JTextArea area = new JTextArea(new WbDocument());
				area.setLineWrap(false);
				
				inputFields[i] = area;
				Dimension min = new Dimension(fieldWidth, areaHeight);

				JScrollPane scroll = new JScrollPane(inputFields[i], ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED, ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
				inputFields[i].setMinimumSize(min);
				inputFields[i].setPreferredSize(min);
				inputFields[i].setFont(displayFont);
				c.fill = GridBagConstraints.BOTH;
				c.weighty = 1.0;
				toAdd = scroll;
			}
			else if (SqlUtil.isBlobType(col.getDataType()))
			{
				JButton b = new JButton(" (BLOB) ");
				b.addActionListener(this);
				inputFields[i] = b;
				c.fill = GridBagConstraints.NONE;
				c.weighty = 0.0;
				toAdd = inputFields[i];
			}
			else
			{
				inputFields[i] = new JTextField(new WbDocument(), null, numChars);
				inputFields[i].setFont(displayFont);
				c.fill = GridBagConstraints.HORIZONTAL;
				c.weighty = 0.0;
				toAdd = inputFields[i];
			}
			

			if (i == fieldDef.getColumnCount() - 1)
			{
				c.weighty = 1.0;
			}
			formPanel.add(toAdd, c);
			if (GuiSettings.getHighlightRequiredFields() && requiredColor != null)
			{
				if (!col.isNullable())
				{
					inputFields[i].setBackground(requiredColor);
				}
			}
			
			c.gridy ++;
		}

		WbTraversalPolicy policy = new WbTraversalPolicy();
		for (int i=0; i < inputFields.length; i++)
		{
			policy.addComponent(inputFields[i]);
		}
		JScrollPane formScroll = new JScrollPane(formPanel, ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED, ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
		formScroll.setBorder(new CompoundBorder(new EtchedBorder(EtchedBorder.LOWERED), new EmptyBorder(10,5,10,5)));
		this.add(formScroll, BorderLayout.CENTER);
		policy.setDefaultComponent(inputFields[0]);
		initRenderer();

		this.add(new FormNavigation(this), BorderLayout.SOUTH);
	}

	private void initRenderer()
	{
		if (fieldDef == null) return;
		if (inputFields == null) return;
		renderer = new WbRenderer[inputFields.length];

		Settings sett = Settings.getInstance();
		String dateFormat = sett.getDefaultDateFormat();
		WbRenderer dateRenderer = (WbRenderer)RendererFactory.getDateRenderer(dateFormat);
		String tsFormat = sett.getDefaultTimestampFormat();
		WbRenderer tsRenderer = (WbRenderer)RendererFactory.getDateRenderer(tsFormat);

		int maxDigits = sett.getMaxFractionDigits();
		char sep = sett.getDecimalSymbol().charAt(0);

		WbRenderer numberRenderer = (WbRenderer)RendererFactory.createNumberRenderer(maxDigits, sep);
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
	 * @param row
	 */
	public void showRecord(int toDisplay)
	{
		currentRow = toDisplay;
		WbSwingUtilities.invoke(new Runnable()
		{
			public void run()
			{
				_showRecord();
			}
		});
	}
	
	protected void _showRecord()
	{
		for (int i=0; i < fieldDef.getColumnCount(); i++)
		{
			Object value = data.getValueAt(currentRow, getTableColumn(i));
			if (value == null)
			{
				if (inputFields[i] instanceof JTextComponent)
				{
					((JTextComponent)inputFields[i]).setText("");
				}
				if (inputFields[i] instanceof BlobColumnPanel)
				{
					((BlobColumnPanel)inputFields[i]).setValue(null);
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
				
					if (inputFields[i] instanceof JTextComponent)
					{
						JTextComponent text = (JTextComponent)inputFields[i];
						text.setText(display);
						text.setCaretPosition(0);
					}
				}
			}
		}
		resetDocuments();
	}

	protected void resetDocuments()
	{
		for (int i=0; i < inputFields.length; i++)
		{
			if (inputFields[i] instanceof JTextComponent)
			{
				JTextComponent text = (JTextComponent)inputFields[i];
				WbDocument doc = (WbDocument)text.getDocument();
				doc.resetModified();
			}
		}
	}
	
	public boolean isChanged()
	{
		boolean isChanged = false;
		for (int i=0; i < inputFields.length; i++)
		{
			if (inputFields[i] instanceof JTextComponent)
			{
				JTextComponent text = (JTextComponent)inputFields[i];
				WbDocument doc = (WbDocument)text.getDocument();
				isChanged = isChanged || doc.isModified();
			}
			else if (inputFields[i] instanceof JButton)
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
		for (int i=0; i < inputFields.length; i++)
		{
			if (inputFields[i] instanceof JTextComponent)
			{
				JTextComponent text = (JTextComponent)inputFields[i];
				WbDocument doc = (WbDocument)text.getDocument();
				if (doc.isModified())
				{
					String newValue = text.getText();
					data.setValueAt(newValue, currentRow, getTableColumn(i));
				}
			}
			else if (inputFields[i] instanceof JButton)
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
		int offset = data.getShowStatusColumn() ? 1 : 0;
		return dataColumn + offset;
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

	public void componentDisplayed()
	{
		if (inputFields == null) return;
		
		EventQueue.invokeLater(new Runnable()
		{
			public void run()
			{
				inputFields[toFocus].requestFocusInWindow();
			}
		});
	}

	private int getColumnIndexFor(Component component)
	{
		for (int i=0; i < inputFields.length; i++)
		{
			if (inputFields[i] == component) return i;
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
			blobHandlers[column].showBlobInfoDialog(null, currentValue);
		}

		//Object newValue = blobHandlers[column].getValueToUse();
	}

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
