/*
 * DbObjectSourcePanel.java
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
package workbench.gui.dbobjects;

import java.awt.BorderLayout;
import java.awt.Cursor;
import java.awt.EventQueue;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.concurrent.CancellationException;

import javax.swing.Box;
import javax.swing.JPanel;
import javax.swing.JSplitPane;

import workbench.interfaces.Reloadable;
import workbench.interfaces.Replaceable;
import workbench.interfaces.Resettable;
import workbench.interfaces.ResultLogger;
import workbench.log.LogMgr;
import workbench.resource.DbExplorerSettings;
import workbench.resource.IconMgr;
import workbench.resource.ResourceMgr;

import workbench.db.SourceStatementsHelp;
import workbench.db.WbConnection;

import workbench.gui.MainWindow;
import workbench.gui.WbSwingUtilities;
import workbench.gui.actions.ReloadAction;
import workbench.gui.actions.WbAction;
import workbench.gui.components.DropDownButton;
import workbench.gui.components.FocusIndicator;
import workbench.gui.components.PlainEditor;
import workbench.gui.components.WbSplitPane;
import workbench.gui.components.WbToolbar;
import workbench.gui.sql.EditorPanel;
import workbench.gui.sql.Highlighter;
import workbench.gui.sql.PanelContentSender;
import workbench.gui.sql.PasteType;

import workbench.sql.DelimiterDefinition;
import workbench.sql.ErrorDescriptor;
import workbench.sql.ExecutionStatus;
import workbench.sql.parser.ScriptParser;

import workbench.util.StringUtil;

/**
 * @author Thomas Kellerer
 */
public class DbObjectSourcePanel
	extends JPanel
  implements ActionListener, Resettable, ResultLogger
{
	protected EditorPanel sourceEditor;
  protected PlainEditor errorLog;
  private JSplitPane splitPane;
	protected ReloadAction reloadSource;
	protected DropDownButton editButton;
	private EditorTabSelectMenu selectTabMenu;
	private MainWindow parentWindow;
	private boolean initialized;
	private boolean allowReformat;
	private boolean allowEditAndExec;
	private String objectName;
  private WbToolbar toolbar;
  private WbConnection connection;
  private WbAction runScript;
  private final String runScriptCommand = "run-script";
  private String objectType;

	public DbObjectSourcePanel(MainWindow window, Reloadable reloader)
	{
		super(new BorderLayout());
		parentWindow = window;
		if (reloader != null)
		{
			reloadSource = new ReloadAction(reloader);
			reloadSource.setEnabled(false);
		}
	}

	public Replaceable getEditor()
	{
		return sourceEditor.getReplacer();
	}

  private void checkReadonly()
  {
    boolean readOnly = (connection == null || connection.getProfile().isReadOnly()) || runScript == null;
    if (runScript != null) runScript.setEnabled(!readOnly);
    sourceEditor.setEditable(!readOnly);
  }

  public void allowEditing(boolean flag)
  {
    boolean old = allowEditAndExec;
    allowEditAndExec = flag;

    if (initialized && allowEditAndExec != old)
    {
      toolbar.invalidate();

      if (allowEditAndExec)
      {
        enableSourceEditing();
      }
      else
      {
        disableSourceEditing();
      }

      checkReadonly();

      WbSwingUtilities.invokeLater(toolbar::validate);
    }
  }

  @Override
  public void setEnabled(boolean enabled)
  {
    super.setEnabled(enabled);
    sourceEditor.setEnabled(enabled);

    if (editButton != null)
    {
      editButton.setEnabled(enabled);
    }

    if (enabled)
    {
      checkReadonly();
    }
    else if (runScript != null)
    {
      runScript.setEnabled(false);
    }

    if (reloadSource != null)
    {
      reloadSource.setEnabled(enabled);
    }
  }

	public void allowReformat()
	{
		this.allowReformat = true;
	}

	private void initGui()
	{
		if (initialized) return;

		WbSwingUtilities.invoke(this::_initGui);
	}

	private void _initGui()
	{
		if (initialized) return;

		this.sourceEditor = EditorPanel.createSqlEditor();
		this.sourceEditor.showFindOnPopupMenu();
		this.sourceEditor.setEditable(false);
    this.sourceEditor.setAllowFileLoading(false);
		this.sourceEditor.setAllowReformatOnReadonly(true);
		if (allowReformat)
		{
			this.sourceEditor.showFormatSql();
		}

		toolbar = new WbToolbar();

		if (reloadSource != null)
		{
			toolbar.add(reloadSource);
			reloadSource.addToInputMap(sourceEditor);
		}

		this.add(this.sourceEditor, BorderLayout.CENTER);

		if (parentWindow != null)
		{
			editButton = new DropDownButton(ResourceMgr.getString("LblEditScriptSource"));
			selectTabMenu = new EditorTabSelectMenu(ResourceMgr.getString("LblEditScriptSource"), "LblEditInNewTab", "LblEditInTab", parentWindow);
      selectTabMenu.setActionListener(this);
			editButton.setDropDownMenu(selectTabMenu.getPopupMenu());
			toolbar.add(editButton);
		}

    if (allowEditAndExec)
    {
      enableSourceEditing();
    }

    this.add(toolbar, BorderLayout.PAGE_START);


		if (DbExplorerSettings.showFocusInDbExplorer())
		{
			new FocusIndicator(sourceEditor, sourceEditor);
		}

		initialized = true;
	}

  private void disableSourceEditing()
  {
    sourceEditor.setEditable(false);
    if (runScript != null)
    {
      runScript.setEnabled(false);
    }
  }

  private void enableSourceEditing()
  {
    if (runScript == null)
    {
      runScript = new WbAction(this, runScriptCommand);
      runScript.setIcon("execute_sel");
      toolbar.add(Box.createHorizontalStrut(IconMgr.getInstance().getSizeForLabel()));
      toolbar.addSeparator();
      toolbar.add(runScript);
    }
    runScript.setEnabled(sourceEditor.getDocumentLength() > 0);
    sourceEditor.setEditable(true);
  }

	@Override
	public void setCursor(Cursor newCursor)
	{
		super.setCursor(newCursor);
		sourceEditor.setCursor(newCursor);
	}

	@Override
	public void actionPerformed(ActionEvent e)
	{
		String command = e.getActionCommand();

    if (runScriptCommand.equals(command))
    {
      runScript();
    }
		if (command.startsWith(EditorTabSelectMenu.PANEL_CMD_PREFIX) && this.parentWindow != null)
		{
			try
			{
				final int panelIndex = Integer.parseInt(command.substring(EditorTabSelectMenu.PANEL_CMD_PREFIX.length()));

				// Allow the selection change to finish so that
				// we have the correct table name in the instance variables
				EventQueue.invokeLater(() ->
        {
          editText(panelIndex);
        });
			}
			catch (Exception ex)
			{
				LogMgr.logError("TableListPanel().actionPerformed()", "Error when accessing editor tab", ex);
			}
		}
	}

	private void editText(final int panelIndex)
	{
		if (this.parentWindow != null)
		{
			PanelContentSender sender = new PanelContentSender(this.parentWindow, this.objectName);
			sender.sendContent(getText(), panelIndex, PasteType.overwrite);
		}
	}

	public void setPlainText(final String message)
	{
		initGui();
		setEditorText(message, false);
	}

	/**
	 * Set the SQL source. If the text contains an error message
	 * indicating that the source is not available (as returned by DbMetadata
	 * if the SQL Queries have not been configured for e.g. stored procedures)
	 * syntax highlighting will be disabled.
	 */
	public void setText(final String sql, String name, String type)
	{
		initGui();
		boolean hasText = StringUtil.isNonEmpty(sql);
		this.objectName = name;
    this.objectType = type;

    if (reloadSource != null)
    {
      reloadSource.setEnabled(hasText);
    }

    if (runScript != null)
    {
      runScript.setEnabled(allowEditAndExec && hasText);
    }

		if (editButton != null)
    {
      editButton.setEnabled(hasText);
    }

		if (hasText &&
					(sql.startsWith(SourceStatementsHelp.VIEW_ERROR_START) ||
					 sql.startsWith(SourceStatementsHelp.PROC_ERROR_START) ||
					 sql.startsWith(ResourceMgr.getString("MsgSynonymSourceNotImplemented"))
					)
			 )
		{
			setEditorText(sql, false);
		}
		else
		{
			setEditorText(sql, true);
		}
	}

	private void setEditorText(final String text, final boolean enableHighlight)
	{
		if (enableHighlight)
		{
			sourceEditor.enableSqlHighlight();
		}
		else
		{
			sourceEditor.disableSqlHighlight();
		}

		WbSwingUtilities.invoke(() ->
    {
      hideErrorLog();
      sourceEditor.setText(text == null ? "" : text);
      sourceEditor.clearUndoBuffer();
      sourceEditor.invalidate();
      sourceEditor.doLayout();
    });

	}

  public void appendText(final String text)
  {
    if (StringUtil.isEmptyString(text)) return;
    WbSwingUtilities.invoke(() ->
    {
      sourceEditor.appendLine(text);
      sourceEditor.invalidate();
      sourceEditor.doLayout();
    });
  }

  public void appendDelimiter(WbConnection dbConnection)
  {
    if (dbConnection == null) return;

    if (dbConnection.getDbSettings().ddlNeedsCommit() && !dbConnection.getAutoCommit())
    {
      DelimiterDefinition delim = dbConnection.getAlternateDelimiter();
      appendText("\nCOMMIT");
      if (delim == null)
      {
        appendText(";\n");
      }
      else
      {
        appendText("\n" + delim.getDelimiter() + "\n");
      }
    }
  }

	public String getText()
	{
		if (sourceEditor == null) return null;
		return sourceEditor.getText();
	}

	@Override
	public void requestFocus()
	{
		if (sourceEditor != null)
		{
			this.sourceEditor.requestFocus();
		}
	}

	@Override
	public boolean requestFocusInWindow()
	{
		if (sourceEditor != null)
		{
			return this.sourceEditor.requestFocusInWindow();
		}
		return false;
	}

	public void setCaretPosition(int pos, boolean selectLine)
	{
		initGui();
		sourceEditor.setCaretPosition(pos);
		if (selectLine)
		{
			int line = sourceEditor.getCaretLine();
			int length = sourceEditor.getLineLength(line);
			int lineStart = sourceEditor.getLineStartOffset(line);
			if (lineStart > 0 && length > 0)
			{
				sourceEditor.select(lineStart, lineStart + length);
			}
		}
	}

	public void scrollToCaret()
	{
		if (sourceEditor != null)
		{
			sourceEditor.scrollToCaret();
		}
	}

	public void setDatabaseConnection(WbConnection con)
	{
		initGui();
		sourceEditor.setDatabaseConnection(con);
    connection = con;
    checkReadonly();
	}

	public void setEditable(boolean flag)
	{
		if (sourceEditor != null)
		{
			sourceEditor.setEditable(flag);
		}
	}

	@Override
	public void reset()
	{
		if (sourceEditor != null)
		{
			setText("", null, null);
      WbSwingUtilities.invoke(this::hideErrorLog);
		}
	}

	public void dispose()
	{
		reset();
		WbSwingUtilities.removeAllListeners(this);
		if (selectTabMenu != null)
		{
			selectTabMenu.dispose();
			selectTabMenu = null;
		}
		if (sourceEditor != null)
		{
			sourceEditor.dispose();
			sourceEditor = null;
		}
		if (editButton != null)
		{
			editButton.dispose();
			editButton = null;
		}
		if (reloadSource != null)
		{
			reloadSource.dispose();
			reloadSource = null;
		}
    if (runScript != null)
    {
      runScript.dispose();
      runScript = null;
    }
    if (errorLog != null)
    {
      errorLog.setText("");
      errorLog = null;
    }
	}



  private void showErrorLog()
  {
    if (errorLog != null) return;
    remove(sourceEditor);
    errorLog = new PlainEditor(WbSwingUtilities.PROP_ERROR_MSG_WRAP, false, false);
    splitPane = new WbSplitPane(JSplitPane.VERTICAL_SPLIT, true, sourceEditor, errorLog);
    add(splitPane, BorderLayout.CENTER);
    invalidate();
    doLayout();
    splitPane.setDividerLocation(0.8);
  }

  private void hideErrorLog()
  {
    if (errorLog == null) return;
    splitPane.remove(sourceEditor);
    splitPane.remove(errorLog);
    remove(splitPane);
    add(sourceEditor, BorderLayout.CENTER);
    splitPane = null;
    errorLog.setText("");
    errorLog = null;
    invalidate();
    doLayout();
  }

  @Override
  public void clearLog()
  {
    if (errorLog != null)
    {
      WbSwingUtilities.invoke(errorLog::clear);
    }
  }

  @Override
  public void appendToLog(final String msg)
  {
    if (errorLog != null)
    {
      if (StringUtil.isBlank(errorLog.getText()) && StringUtil.isBlank(msg)) return;

      WbSwingUtilities.invoke(() ->
      {
        errorLog.append(msg);
      });
    }
  }

  @Override
  public void showLogMessage(String msg)
  {
    if (errorLog != null)
    {
      WbSwingUtilities.invoke(() ->
      {
        errorLog.setText(msg);
      });
    }
  }

  private void runScript()
  {
    if (connection == null) return;
    if (connection.getProfile().isReadOnly()) return;

    if (WbSwingUtilities.isConnectionIdle(this, connection) == false) return;

    String script = getText();
    if (script.isEmpty()) return;

    String type = objectType == null ? "object" : objectType.toLowerCase();

    boolean confirmExec = (false == DbExplorerSettings.objectTypesToRunWithoutConfirmation().contains(type));

    String msg = ResourceMgr.getFormattedString("MsgConfirmRecreate", type, objectName);
    if (confirmExec && false == WbSwingUtilities.getYesNo(this, msg))
    {
      return;
    }

    showErrorLog();
    try
    {
      runScript.setEnabled(false);

      ScriptExecutionFeedback feedback = new ScriptExecutionFeedback(connection, script, this);
      feedback.runScript(objectName, parentWindow);

      ExecutionStatus status = feedback.get();

      if (status == ExecutionStatus.Error)
      {
        ErrorDescriptor error = feedback.getLastError();
        if (error != null)
        {
          Highlighter highlighter = new Highlighter(sourceEditor);
          ScriptParser parser = ScriptParser.createScriptParser(connection);
          parser.setScript(script);
          highlighter.markError(false, parser, feedback.getLastErrorIndex(), 0, error);
        }
      }
    }
    catch (CancellationException ex)
    {
      // ignore
    }
    catch (Exception ex)
    {
      LogMgr.logWarning("DbObjectSourcePanel.runScript()", "Error when running script", ex);
    }
    finally
    {
      runScript.setEnabled(true);
      WbSwingUtilities.requestFocus(sourceEditor);
    }
  }
}
