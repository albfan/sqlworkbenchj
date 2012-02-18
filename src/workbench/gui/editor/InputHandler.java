package workbench.gui.editor;

/*
 * InputHandler.java - Manages key bindings and executes actions
 * Copyright (C) 1999 Slava Pestov
 *
 * You may use and modify this package for any purpose. Redistribution is
 * permitted, in both source and binary form, provided that this notice
 * remains intact in all source distributions of this package.
 */

import java.awt.Component;
import java.awt.EventQueue;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.EventObject;
import java.util.HashMap;

import java.util.List;
import java.util.Map;
import javax.swing.InputMap;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.text.BadLocationException;
import workbench.gui.actions.WbAction;
import workbench.gui.fontzoom.DecreaseFontSize;
import workbench.gui.editor.actions.DelPrevWord;
import workbench.gui.editor.actions.DeleteChar;
import workbench.gui.editor.actions.DeleteWord;
import workbench.gui.editor.actions.DocumentEnd;
import workbench.gui.editor.actions.DocumentHome;
import workbench.gui.editor.actions.EditorAction;
import workbench.gui.fontzoom.IncreaseFontSize;
import workbench.gui.editor.actions.LineEnd;
import workbench.gui.editor.actions.LineStart;
import workbench.gui.editor.actions.NextChar;
import workbench.gui.editor.actions.NextLine;
import workbench.gui.editor.actions.NextPage;
import workbench.gui.editor.actions.NextWord;
import workbench.gui.editor.actions.PrevWord;
import workbench.gui.editor.actions.PreviousChar;
import workbench.gui.editor.actions.PreviousLine;
import workbench.gui.editor.actions.PreviousPage;
import workbench.gui.fontzoom.ResetFontSize;
import workbench.gui.editor.actions.SelectDocumentEnd;
import workbench.gui.editor.actions.SelectDocumentHome;
import workbench.gui.editor.actions.SelectLineEnd;
import workbench.gui.editor.actions.SelectLineStart;
import workbench.gui.editor.actions.SelectNextChar;
import workbench.gui.editor.actions.SelectNextLine;
import workbench.gui.editor.actions.SelectNextPage;
import workbench.gui.editor.actions.SelectNextWord;
import workbench.gui.editor.actions.SelectPrevWord;
import workbench.gui.editor.actions.SelectPreviousChar;
import workbench.gui.editor.actions.SelectPreviousLine;
import workbench.gui.editor.actions.SelectPreviousPage;
import workbench.resource.Settings;
import workbench.resource.ShortcutManager;

/**
 * An input handler converts the user's key strokes into concrete actions.
 * It also takes care of macro recording and action repetition.<p>
 *
 * This class provides all the necessary support code for an input
 * handler, but doesn't actually do any key binding logic. It is up
 * to the implementations of this class to do so.
 *
 * @author Slava Pestov (initial developer)
 * @author Thomas Kellerer (enhancements and bugfixes)
 */
public class InputHandler
	extends KeyAdapter
	implements ChangeListener
{
	/**
	 * If this client property is set to Boolean.TRUE on the text area,
	 * the home/end keys will support 'smart' BRIEF-like behaviour
	 * (one press = start/end of line, two presses = start/end of
	 * viewscreen, three presses = start/end of document). By default,
	 * this property is not set.
	 */
	public static final String SMART_HOME_END_PROPERTY = "InputHandler.homeEnd";

	public static final ActionListener BACKSPACE = new backspace();
	public static final ActionListener OVERWRITE = new overwrite();

	public static final EditorAction DELETE = new DeleteChar();

	public static final EditorAction DELETE_WORD = new DeleteWord();
	public static final EditorAction DEL_PREV_WORD = new DelPrevWord();

	public static final EditorAction DOCUMENT_END = new DocumentEnd();
	public static final EditorAction SELECT_DOC_END = new SelectDocumentEnd();

	public static final EditorAction LINE_END = new LineEnd();
	public static final EditorAction SELECT_LINE_END = new SelectLineEnd();

	public static final EditorAction LINE_START = new LineStart();
	public static final EditorAction SELECT_LINE_START = new SelectLineStart();

	public static final EditorAction DOCUMENT_HOME = new DocumentHome();
	public static final EditorAction SELECT_DOC_HOME = new SelectDocumentHome();

	public static final ActionListener INSERT_BREAK = new insert_break();
	public static final ActionListener INSERT_TAB = new insert_tab();
	public static final ActionListener SHIFT_TAB = new shift_tab();

	public static final EditorAction PREV_WORD = new PrevWord();
	public static final EditorAction SELECT_PREV_WORD = new SelectPrevWord();
	public static final EditorAction NEXT_WORD = new NextWord();
	public static final EditorAction SELECT_NEXT_WORD = new SelectNextWord();

	public static final EditorAction NEXT_CHAR = new NextChar();
	public static final EditorAction SELECT_NEXT_CHAR = new SelectNextChar();
	public static final EditorAction PREV_CHAR = new PreviousChar();
	public static final EditorAction SELECT_PREV_CHAR = new SelectPreviousChar();

	public static final EditorAction NEXT_PAGE = new NextPage();
	public static final EditorAction PREV_PAGE = new PreviousPage();
	public static final EditorAction SELECT_PREV_PAGE = new SelectPreviousPage();
	public static final EditorAction SELECT_NEXT_PAGE = new SelectNextPage();

	public static final EditorAction NEXT_LINE = new NextLine();
	public static final EditorAction SELECT_NEXT_LINE = new SelectNextLine();
	public static final EditorAction SELECT_PREV_LINE = new SelectPreviousLine();
	public static final EditorAction PREV_LINE = new PreviousLine();

	public static final WbAction INCREASE_FONT = new IncreaseFontSize();
	public static final WbAction DECREASE_FONT = new DecreaseFontSize();
	public static final WbAction RESET_FONT = new ResetFontSize();

	// Default action
	public static final ActionListener INSERT_CHAR = new insert_char();

	private Map<KeyStroke, ActionListener> bindings;

	private boolean sequenceIsMapped = false;

	public InputHandler()
	{
		initKeyBindings();
		ShortcutManager.getInstance().addChangeListener(this);
	}

	/**
	 * Adds the default key bindings to this input handler.
	 */
	public final void initKeyBindings()
	{
		bindings = new HashMap<KeyStroke, ActionListener>();
		addKeyBinding(KeyStroke.getKeyStroke(KeyEvent.VK_BACK_SPACE, 0), BACKSPACE);

		addKeyBinding(KeyStroke.getKeyStroke(KeyEvent.VK_DELETE, 0), DELETE);

		addKeyBinding(DEL_PREV_WORD);
		addKeyBinding(DELETE_WORD);

		addKeyBinding(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0), INSERT_BREAK);
		addKeyBinding(KeyStroke.getKeyStroke(KeyEvent.VK_TAB, 0), INSERT_TAB);
		addKeyBinding(KeyStroke.getKeyStroke(KeyEvent.VK_TAB, KeyEvent.SHIFT_MASK), SHIFT_TAB);

		addKeyBinding(KeyStroke.getKeyStroke(KeyEvent.VK_INSERT, 0), OVERWRITE);

		addKeyBinding(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), new cancel_rectangle_select());

		addKeyBinding(LINE_START);
		addKeyBinding(SELECT_LINE_START);

		addKeyBinding(LINE_END);
		addKeyBinding(SELECT_LINE_END);

		addKeyBinding(DOCUMENT_HOME);
		addKeyBinding(SELECT_DOC_HOME);

		addKeyBinding(DOCUMENT_END);
		addKeyBinding(SELECT_DOC_END);


		addKeyBinding(PREV_PAGE);
		addKeyBinding(SELECT_PREV_PAGE);

		addKeyBinding(NEXT_PAGE);
		addKeyBinding(SELECT_NEXT_PAGE);

		addKeyBinding(PREV_CHAR);
		addKeyBinding(SELECT_PREV_CHAR);

		addKeyBinding(PREV_WORD);
		addKeyBinding(SELECT_PREV_WORD);

		addKeyBinding(NEXT_CHAR);
		addKeyBinding(SELECT_NEXT_CHAR);

		addKeyBinding(NEXT_WORD);
		addKeyBinding(SELECT_NEXT_WORD);

		addKeyBinding(PREV_LINE);
		addKeyBinding(SELECT_PREV_LINE);

		addKeyBinding(NEXT_LINE);
		addKeyBinding(SELECT_NEXT_LINE);

		addKeyBinding(INCREASE_FONT);
		addKeyBinding(DECREASE_FONT);
		addKeyBinding(RESET_FONT);
	}

	@SuppressWarnings("unchecked")
	public void addKeyBinding(WbAction action)
	{
		KeyStroke key = action.getAccelerator();
		if (key != null)
		{
			this.bindings.put(action.getAccelerator(), action);
		}
	}

	@SuppressWarnings("unchecked")
	public void addKeyBinding(KeyStroke key, ActionListener action)
	{
		this.bindings.put(key, action);
	}

	/**
	 * Removes a key binding from this input handler. This is not yet
	 * implemented.
	 * @param key The key binding
	 */
	public void removeKeyBinding(KeyStroke key)
	{
		bindings.remove(key);
	}

	/**
	 * Removes all key bindings from this input handler.
	 */
	public void removeAllKeyBindings()
	{
		bindings.clear();
	}

	@Override
	public void stateChanged(ChangeEvent e)
	{
		initKeyBindings();
	}

	/**
	 * Clears all keybindings and un-registers the changelistener from the shortcutmanager
	 */
	public void dispose()
	{
		ShortcutManager.getInstance().removeChangeListener(this);
		removeAllKeyBindings();
	}

	@Override
	public void keyPressed(final KeyEvent evt)
	{
		int keyCode = evt.getKeyCode();

		KeyStroke keyStroke = KeyStroke.getKeyStrokeForEvent(evt);

		if (!evt.isActionKey() && !sequenceIsMapped)
		{
			sequenceIsMapped = isMapped(evt);
		}

		if (keyCode == KeyEvent.VK_CONTEXT_MENU)
		{
			EventQueue.invokeLater(new Runnable()
			{
				@Override
				public void run()
				{
					JEditTextArea area = getTextArea(evt);
					area.showContextMenu();
				}
			});
			return;
		}

		ActionListener l = bindings.get(keyStroke);

		if (l != null)
		{
			executeAction(l, evt.getSource(), null);
			evt.consume();
		}
	}

	void resetStatus()
	{
		sequenceIsMapped = false;
	}

	@Override
	public void keyTyped(KeyEvent evt)
	{
		if (evt.isConsumed()) return;

		boolean isMapped = sequenceIsMapped;
		sequenceIsMapped = false;

		if (isMapped)
		{
			return;
		}

		char c = evt.getKeyChar();

		// For some reason we still wind up here even if Ctrl-Space was
		// already handled by keyPressed
		if (c == 0x20 && evt.getModifiers() == KeyEvent.CTRL_MASK)
		{
			KeyStroke pressed = KeyStroke.getKeyStroke(KeyEvent.VK_SPACE, KeyEvent.CTRL_MASK);
			if (bindings.get(pressed) != null)
			{
				// already processed!
				evt.consume();
				return;
			}
		}

		if (c >= 0x20 && c != 0x7f)
		{
			KeyStroke key = KeyStroke.getKeyStrokeForEvent(evt);
			ActionListener l = bindings.get(key);

			if (l != null)
			{
				executeAction(l, evt.getSource(), String.valueOf(c));
				evt.consume();
				return;
			}
			executeAction(INSERT_CHAR, evt.getSource(), String.valueOf(c));
		}
	}

	@Override
	public void keyReleased(KeyEvent evt)
	{
		if (evt.getKeyCode() == KeyEvent.VK_ALT)
		{
			// we consume this to work around the bug
			// where Alt+TAB window switching activates
			// the menu bar on Windows.
			evt.consume();
		}
	}

	public List<KeyStroke> getKeys(JMenu menu)
	{
		if (menu == null) return Collections.emptyList();
		List<KeyStroke> allKeys = new ArrayList<KeyStroke>();

		for (int i=0; i < menu.getItemCount(); i++)
		{
			JMenuItem item = menu.getItem(i);
			allKeys.addAll( getKeys(item));
			if (item instanceof JMenu)
			{
				allKeys.addAll(getKeys((JMenu)item));
			}
		}
		return allKeys;
	}

	public List<KeyStroke> getKeys(JComponent c)
	{
		if (c == null) return Collections.emptyList();
		int types[] = new int[] {
			JComponent.WHEN_FOCUSED,
			JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT,
			JComponent.WHEN_IN_FOCUSED_WINDOW
		};

		List<KeyStroke> allKeys = new ArrayList<KeyStroke>();
		for (int when : types)
		{
			InputMap map = c.getInputMap(when);
			KeyStroke[] keys = (map != null ? map.allKeys() : null);

			if (keys != null)
			{
				allKeys.addAll(Arrays.asList(keys));
			}
		}
		return allKeys;
	}

	public boolean isMapped(KeyEvent evt)
	{
		if (evt == null) return false;

		KeyStroke toTest = KeyStroke.getKeyStrokeForEvent(evt);
		if (toTest.getModifiers() == 0) return false;

		int code = toTest.getKeyCode();

		// if the keycode indicates a modifier key, only that key was
		// pressed, the modifier alone cannot be mapped...
		if (code == KeyEvent.VK_ALT || code == KeyEvent.VK_CONTROL ||
				code == KeyEvent.VK_META || code == KeyEvent.VK_CONTEXT_MENU)
		{
			return false;
		}

		JEditTextArea area = getTextArea(evt);

		List<KeyStroke> allKeys = new ArrayList<KeyStroke>();
		allKeys.addAll(getKeys(area));

		Window w = SwingUtilities.getWindowAncestor(area);
		if (w instanceof JFrame)
		{
			JMenuBar bar = ((JFrame)w).getJMenuBar();
			if (bar != null && bar.getComponents() != null)
			{
				for (Component c : bar.getComponents())
				{
					allKeys.addAll( getKeys((JComponent)c) );
					if (c instanceof JMenu)
					{
						JMenu m = (JMenu)c;
						allKeys.addAll(getKeys(m));
					}
				}
			}
		}

		return allKeys.contains(toTest);
	}


	/**
	 * Executes the specified action
	 *
	 * @param listener The action listener
	 * @param source The event source
	 * @param actionCommand The action command
	 */
	public void executeAction(ActionListener listener, Object source, String actionCommand)
	{
		ActionEvent evt = new ActionEvent(source, ActionEvent.ACTION_PERFORMED, actionCommand);
		listener.actionPerformed(evt);
	}

	/**
	 * Returns the text area that fired the specified event.
	 * @param evt The event
	 */
	public static JEditTextArea getTextArea(EventObject evt)
	{
		if (evt != null)
		{
			Object o = evt.getSource();
			if (o instanceof Component)
			{
				// find the parent text area
				Component c = (Component) o;
				for (;;)
				{
					if (c instanceof JEditTextArea)
					{
						return (JEditTextArea) c;
					}
					else if (c == null)
					{
						break;
					}
					if (c instanceof JPopupMenu)
					{
						c = ((JPopupMenu) c).getInvoker();
					}
					else
					{
						c = c.getParent();
					}
				}
			}
		}
		return null;
	}

	public static class backspace implements ActionListener
	{
		@Override
		public void actionPerformed(ActionEvent evt)
		{
			JEditTextArea textArea = getTextArea(evt);

			if (!textArea.isEditable())
			{
				return;
			}

			if (textArea.getSelectionStart() != textArea.getSelectionEnd())
			{
				if (textArea.isEmptyRectangleSelection())
				{
					textArea.doRectangleBackspace();
				}
				else
				{
					textArea.setSelectedText("");
				}
			}
			else
			{
				int caret = textArea.getCaretPosition();
				if (caret == 0) return;

				SyntaxDocument doc = textArea.getDocument();
				try
				{
					doc.remove(caret - 1, 1);
				}
				catch (BadLocationException bl)
				{
					bl.printStackTrace();
				}
			}
		}
	}

	public static class insert_break implements ActionListener
	{
		@Override
		public void actionPerformed(ActionEvent evt)
		{
			JEditTextArea textArea = getTextArea(evt);

			if(!textArea.isEditable())
			{
				textArea.getToolkit().beep();
				return;
			}

			textArea.setSelectedText("\n");
		}
	}
	public static class shift_tab implements ActionListener
	{
		@Override
		public void actionPerformed(ActionEvent evt)
		{
			JEditTextArea textArea = getTextArea(evt);
			if (!textArea.isEditable())
			{
				textArea.getToolkit().beep();
				return;
			}

			int start = textArea.getSelectionStart();
			int end = textArea.getSelectionEnd();

			if (start < end)
			{
				TextIndenter indenter = new TextIndenter(textArea);
				indenter.unIndentSelection();
			}
		}
	}
	public static class insert_tab implements ActionListener
	{
		@Override
		public void actionPerformed(ActionEvent evt)
		{
			JEditTextArea textArea = getTextArea(evt);

			if (!textArea.isEditable())
			{
				textArea.getToolkit().beep();
				return;
			}

			int start = textArea.getSelectionStart();
			int end = textArea.getSelectionEnd();

			if (start < end)
			{
				TextIndenter indenter = new TextIndenter(textArea);
				indenter.indentSelection();
			}
			else
			{
				boolean useTab = Settings.getInstance().getEditorUseTabCharacter();
				if (useTab)
				{
					textArea.overwriteSetSelectedText("\t");
				}
				else
				{
					int tabSize = Settings.getInstance().getEditorTabWidth();
					int lineStart = textArea.getLineStartOffset(textArea.getCaretLine());
					int posInLine = textArea.getCaretPosition() - lineStart;
					int inc = (tabSize - (posInLine % tabSize));
					StringBuilder spaces = new StringBuilder(inc);
					for (int i=0; i < inc; i++)
					{
						spaces.append(' ');
					}
					textArea.overwriteSetSelectedText(spaces.toString());
				}
			}
		}
	}

	public static class overwrite implements ActionListener
	{
		@Override
		public void actionPerformed(ActionEvent evt)
		{
			JEditTextArea textArea = getTextArea(evt);
			textArea.setOverwriteEnabled(!textArea.isOverwriteEnabled());
		}
	}

	public static class cancel_rectangle_select
		implements ActionListener
	{
		@Override
		public void actionPerformed(ActionEvent evt)
		{
			JEditTextArea textArea = getTextArea(evt);
			if (textArea.isSelectionRectangular())
			{
				// re-setting the position will clear the selection
				textArea.setCaretPosition(textArea.getCaretPosition());
			}
		}
	}

	public static class insert_char
		implements ActionListener
	{
		@Override
		public void actionPerformed(ActionEvent evt)
		{
			JEditTextArea textArea = getTextArea(evt);
			String str = evt.getActionCommand();

			if (textArea.isEditable())
			{
				textArea.overwriteSetSelectedText(str);
			}
			else
			{
				textArea.getToolkit().beep();
			}
		}
	}
}