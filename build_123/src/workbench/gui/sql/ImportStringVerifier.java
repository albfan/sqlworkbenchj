/*
 * ImportStringVerifier.java
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
package workbench.gui.sql;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Frame;
import java.io.BufferedReader;
import java.io.StringReader;
import java.util.List;

import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.UIManager;
import javax.swing.border.Border;
import javax.swing.border.EmptyBorder;

import workbench.WbManager;
import workbench.db.ColumnIdentifier;
import workbench.db.importer.TextFileParser;
import workbench.gui.WbSwingUtilities;
import workbench.gui.components.ValidatingDialog;
import workbench.gui.dialogs.dataimport.GeneralImportOptionsPanel;
import workbench.db.importer.ImportOptions;
import workbench.db.importer.TextImportOptions;
import workbench.gui.dialogs.dataimport.TextOptionsPanel;
import workbench.resource.ResourceMgr;
import workbench.resource.Settings;
import workbench.storage.ResultInfo;
import workbench.util.ClipboardFile;

/**
 * This class checks the content of an input string and tries to match
 * it against a ResultInfo.
 *
 * @author Thomas Kellerer
 */
public class ImportStringVerifier
{
	private ResultInfo target;
	private String content;
	private JPanel optionsPanel;
	private TextOptionsPanel textOptions;
	private GeneralImportOptionsPanel generalOptions;
	private boolean columnNamesMatched;
	private boolean columnCountMatched;

	public ImportStringVerifier(String data, ResultInfo result)
	{
		this.target = result;
		this.content = data;
	}

	/**
	 * Check the contents of the data string if it matches
	 * the structure of our target ResultInfo.
	 * The user will be prompted to correct the problems
	 * if possible.
	 * The following things are checked:
	 * <ul>
	 * <li>the input data has to have a header line which defines the columns</li>
	 * <li>If there is no header line (i.e. no matching columns found) then
	 *     the import is OK, if the column count is the same</li>
	 * <li>at least one column from the input data must occur in the ResultInfo</li>
	 * </ul>
	 *
	 * @return false if the data cannot be imported
	 */
	public boolean checkData()
	{
		ClipboardFile f = new ClipboardFile(this.content);
		TextFileParser parser = new TextFileParser(f);
		parser.setContainsHeader(true);
		if (this.textOptions != null)
		{
			// textOptions != null then we have displayed the options
			// dialog to the user. If the ClipBoard does not contain
			// a header line, we simply assume that it matches the c
			// columns from the result set.
			if (!textOptions.getContainsHeader()) return true;

			parser.setTextDelimiter(textOptions.getTextDelimiter());
		}
		List<ColumnIdentifier> cols = parser.getColumnsFromFile();

		int matchingColumns = 0;
		for (ColumnIdentifier col : cols)
		{
			int index = target.findColumn(col.getColumnName());
			if (index > -1)
			{
				matchingColumns ++;
			}
		}

		this.columnCountMatched = (cols.size() == target.getColumnCount());
		this.columnNamesMatched = (matchingColumns > 0);
		return (columnCountMatched || columnNamesMatched);
	}


	public boolean columnNamesMatched()
	{
		return this.columnNamesMatched;
	}

	private void createOptionsPanel()
	{
		if (this.optionsPanel != null)	return;

		this.textOptions = new TextOptionsPanel();
		this.textOptions.restoreSettings("clipboard");
		if (this.columnCountMatched && !this.columnNamesMatched)
		{
			textOptions.setContainsHeader(false);
		}

		this.generalOptions = new GeneralImportOptionsPanel();
		this.generalOptions.setModeSelectorEnabled(false);
		this.generalOptions.setEncodingVisible(false);
		this.generalOptions.restoreSettings("clipboard");

		this.optionsPanel = new JPanel(new BorderLayout());
		this.optionsPanel.add(generalOptions, BorderLayout.NORTH);
		this.optionsPanel.add(textOptions, BorderLayout.SOUTH);
	}

	/**
	 * If no columns are found, then most probably the (default) column
	 * delimiter is not correct, so let the user supply the import options
	 */
	public boolean showOptionsDialog()
	{
		createOptionsPanel();
		JPanel p = new JPanel(new BorderLayout(0,5));

		JTextArea preview = new JTextArea();
		StringBuilder s = getLines(content, 15);
		int l = s.length();
		for (int i = 0; i < l; i++)
		{
			if (s.charAt(i) == '\t') s.setCharAt(i, '\u00bb');
		}
		preview.setText(s.toString());
		preview.setFont(Settings.getInstance().getEditorFont());
		preview.setEditable(false);
		preview.setDisabledTextColor(preview.getForeground());
		preview.setCaretPosition(0);

		JScrollPane scroll = new JScrollPane(preview);
		Dimension d = new Dimension(350, 250);
		preview.setMaximumSize(d);
		scroll.setMaximumSize(d);
		scroll.setPreferredSize(d);

		JTextField msg = new JTextField();
		msg.setEnabled(false);
		msg.setText(ResourceMgr.getString("MsgClipFormat"));
		msg.setBackground(UIManager.getColor("TextArea.background"));
		msg.setDisabledTextColor(msg.getForeground());
		Border b = new EmptyBorder(4,2,4,2);
		msg.setBorder(b);

		p.add(this.optionsPanel, BorderLayout.EAST);
		p.add(msg, BorderLayout.NORTH);
		p.add(scroll, BorderLayout.CENTER);

		Frame f = WbManager.getInstance().getCurrentWindow();
		ValidatingDialog dialog = new ValidatingDialog(f, "Import", p);
		WbSwingUtilities.center(dialog, f);
		dialog.setVisible(true);
		boolean ok = !dialog.isCancelled();
		textOptions.saveSettings("clipboard");
		generalOptions.saveSettings("clipboard");
		return ok;
	}

	public ImportOptions getImportOptions()
	{
		return generalOptions;
	}

	public TextImportOptions getTextImportOptions()
	{
		return textOptions;
	}

	public final StringBuilder getLines(String s, int lineCount)
	{
		StringBuilder result = new StringBuilder(lineCount * 100);
		try
		{
			BufferedReader r = new BufferedReader(new StringReader(s));
			int lines = 0;
			String line = r.readLine();
			while (line != null && lines < lineCount)
			{
				result.append(line);
				result.append('\n');
				lines ++;
				line = r.readLine();
			}
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
		return result;
	}

}
