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
import javax.swing.JPopupMenu;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import javax.swing.text.BadLocationException;
import workbench.log.LogMgr;
import workbench.resource.Settings;

/**
 * An input handler converts the user's key strokes into concrete actions.
 * It also takes care of macro recording and action repetition.<p>
 *
 * This class provides all the necessary support code for an input
 * handler, but doesn't actually do any key binding logic. It is up
 * to the implementations of this class to do so.
 *
 * @author Slava Pestov
 */
public class InputHandler
	extends KeyAdapter
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
	public static final ActionListener BACKSPACE_WORD = new backspace_word();
	public static final ActionListener DELETE = new delete();
	public static final ActionListener DELETE_WORD = new delete_word();
	public static final ActionListener END = new end(false);
	public static final ActionListener DOCUMENT_END = new document_end(false);
	public static final ActionListener SELECT_END = new end(true);
	public static final ActionListener SELECT_DOC_END = new document_end(true);
	public static final ActionListener INSERT_BREAK = new insert_break();
	public static final ActionListener INSERT_TAB = new insert_tab();
	public static final ActionListener HOME = new home(false);
	public static final ActionListener DOCUMENT_HOME = new document_home(false);
	public static final ActionListener SELECT_HOME = new home(true);
	public static final ActionListener SELECT_DOC_HOME = new document_home(true);
	public static final ActionListener NEXT_CHAR = new next_char(false);
	public static final ActionListener NEXT_LINE = new next_line(false);
	public static final ActionListener NEXT_PAGE = new next_page(false);
	public static final ActionListener NEXT_WORD = new next_word(false);
	public static final ActionListener SELECT_NEXT_CHAR = new next_char(true);
	public static final ActionListener SELECT_NEXT_LINE = new next_line(true);
	public static final ActionListener SELECT_NEXT_PAGE = new next_page(true);
	public static final ActionListener SELECT_NEXT_WORD = new next_word(true);
	public static final ActionListener OVERWRITE = new overwrite();
	public static final ActionListener PREV_CHAR = new prev_char(false);
	public static final ActionListener PREV_LINE = new prev_line(false);
	public static final ActionListener PREV_PAGE = new prev_page(false);
	public static final ActionListener PREV_WORD = new prev_word(false);
	public static final ActionListener SELECT_PREV_CHAR = new prev_char(true);
	public static final ActionListener SELECT_PREV_LINE = new prev_line(true);
	public static final ActionListener SELECT_PREV_PAGE = new prev_page(true);
	public static final ActionListener SELECT_PREV_WORD = new prev_word(true);
	public static final ActionListener TOGGLE_RECT = new toggle_rect();
	public static final ActionListener UNDO = new undo();
	public static final ActionListener REDO = new redo();

	// Default action
	public static final ActionListener INSERT_CHAR = new insert_char();

	private Map<KeyStroke, ActionListener> bindings;

	private boolean keySequence = false;
	private boolean sequenceIsMapped = false;

	public InputHandler()
	{
		bindings = new HashMap();
	}

	/**
	 * Adds the default key bindings to this input handler.
	 * This should not be called in the constructor of this
	 * input handler, because applications might load the
	 * key bindings from a file, etc.
	 */
	public void addDefaultKeyBindings()
	{
		addKeyBinding(KeyStroke.getKeyStroke(KeyEvent.VK_BACK_SPACE, 0), BACKSPACE);
		addKeyBinding(KeyStroke.getKeyStroke(KeyEvent.VK_BACK_SPACE, KeyEvent.CTRL_MASK), BACKSPACE_WORD);

		addKeyBinding(KeyStroke.getKeyStroke(KeyEvent.VK_DELETE, 0), DELETE);
		addKeyBinding(KeyStroke.getKeyStroke(KeyEvent.VK_DELETE, KeyEvent.CTRL_MASK), DELETE_WORD);

		addKeyBinding(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0), INSERT_BREAK);
		addKeyBinding(KeyStroke.getKeyStroke(KeyEvent.VK_TAB, 0), INSERT_TAB);

		addKeyBinding(KeyStroke.getKeyStroke(KeyEvent.VK_INSERT, 0), OVERWRITE);

		addKeyBinding(KeyStroke.getKeyStroke(KeyEvent.VK_HOME, 0), HOME);
		addKeyBinding(KeyStroke.getKeyStroke(KeyEvent.VK_HOME, KeyEvent.SHIFT_MASK), SELECT_HOME);
		addKeyBinding(KeyStroke.getKeyStroke(KeyEvent.VK_HOME, KeyEvent.CTRL_MASK),DOCUMENT_HOME);
		addKeyBinding(KeyStroke.getKeyStroke(KeyEvent.VK_HOME, KeyEvent.SHIFT_MASK | KeyEvent.CTRL_MASK), SELECT_DOC_HOME);

		addKeyBinding(KeyStroke.getKeyStroke(KeyEvent.VK_END, 0), END);
		addKeyBinding(KeyStroke.getKeyStroke(KeyEvent.VK_END, KeyEvent.SHIFT_MASK), SELECT_END);
		addKeyBinding(KeyStroke.getKeyStroke(KeyEvent.VK_END, KeyEvent.CTRL_MASK), DOCUMENT_END);
		addKeyBinding(KeyStroke.getKeyStroke(KeyEvent.VK_END, KeyEvent.SHIFT_MASK | KeyEvent.CTRL_MASK), SELECT_DOC_END);

		addKeyBinding(KeyStroke.getKeyStroke(KeyEvent.VK_PAGE_UP, 0), PREV_PAGE);
		addKeyBinding(KeyStroke.getKeyStroke(KeyEvent.VK_PAGE_UP, KeyEvent.SHIFT_MASK), SELECT_PREV_PAGE);

		addKeyBinding(KeyStroke.getKeyStroke(KeyEvent.VK_PAGE_DOWN, 0), NEXT_PAGE);
		addKeyBinding(KeyStroke.getKeyStroke(KeyEvent.VK_PAGE_DOWN, KeyEvent.SHIFT_MASK), SELECT_NEXT_PAGE);

		addKeyBinding(KeyStroke.getKeyStroke(KeyEvent.VK_LEFT, 0), PREV_CHAR);
		addKeyBinding(KeyStroke.getKeyStroke(KeyEvent.VK_LEFT, KeyEvent.SHIFT_MASK), SELECT_PREV_CHAR);
		addKeyBinding(KeyStroke.getKeyStroke(KeyEvent.VK_LEFT, KeyEvent.CTRL_MASK), PREV_WORD);
		addKeyBinding(KeyStroke.getKeyStroke(KeyEvent.VK_LEFT, KeyEvent.CTRL_MASK | KeyEvent.SHIFT_MASK), SELECT_PREV_WORD);

		addKeyBinding(KeyStroke.getKeyStroke(KeyEvent.VK_RIGHT, 0), NEXT_CHAR);
		addKeyBinding(KeyStroke.getKeyStroke(KeyEvent.VK_RIGHT, KeyEvent.SHIFT_MASK), SELECT_NEXT_CHAR);
		addKeyBinding(KeyStroke.getKeyStroke(KeyEvent.VK_RIGHT, KeyEvent.CTRL_MASK), NEXT_WORD);
		addKeyBinding(KeyStroke.getKeyStroke(KeyEvent.VK_RIGHT, KeyEvent.CTRL_MASK | KeyEvent.SHIFT_MASK), SELECT_NEXT_WORD);

		addKeyBinding(KeyStroke.getKeyStroke(KeyEvent.VK_UP, 0), PREV_LINE);
		addKeyBinding(KeyStroke.getKeyStroke(KeyEvent.VK_UP, KeyEvent.SHIFT_MASK), SELECT_PREV_LINE);

		addKeyBinding(KeyStroke.getKeyStroke(KeyEvent.VK_DOWN, 0), NEXT_LINE);
		addKeyBinding(KeyStroke.getKeyStroke(KeyEvent.VK_DOWN, KeyEvent.SHIFT_MASK), SELECT_NEXT_LINE);

		addKeyBinding(KeyStroke.getKeyStroke(KeyEvent.VK_Z, KeyEvent.CTRL_MASK ), UNDO);
		addKeyBinding(KeyStroke.getKeyStroke(KeyEvent.VK_Y, KeyEvent.CTRL_MASK ), REDO);
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
	public void keyPressed(final KeyEvent evt)
	{
		int keyCode = evt.getKeyCode();
		int modifiers = evt.getModifiers();

		KeyStroke keyStroke = KeyStroke.getKeyStrokeForEvent(evt);
		
		if (!keySequence)
		{
			keySequence = true;
		}

		if (!evt.isActionKey() && !sequenceIsMapped)
		{
			sequenceIsMapped = isMapped(evt);
		}

		if (keyCode == KeyEvent.VK_TAB)
		{
			JEditTextArea area = getTextArea(evt);
			if (area.isEditable())
			{
				int start = area.getSelectionStart();
				int end = area.getSelectionEnd();
				if (start < end)
				{
					TextIndenter indenter = new TextIndenter(area);
					if ((modifiers & KeyEvent.SHIFT_MASK) == KeyEvent.SHIFT_MASK)
					{
						indenter.unIndentSelection();
					}
					else
					{
						indenter.indentSelection();
					}
					return;
				}
			}
		}
		else if (keyCode == KeyEvent.VK_CONTEXT_MENU)
		{
			EventQueue.invokeLater(new Runnable()
			{
				public void run()
				{
					JEditTextArea area = getTextArea(evt);
					area.showContextMenu();
				}
			});
		}
		ActionListener l = bindings.get(keyStroke);

		if (l != null)
		{
			executeAction(l, evt.getSource(), null);
			evt.consume();
		}
	}

	@Override
	public void keyTyped(KeyEvent evt)
	{
//		System.out.println("keyTyped: " + evt.toString());

		boolean isMapped = sequenceIsMapped;
		sequenceIsMapped = false;
		keySequence = false;

		if (isMapped)
		{
			return;
		}

		char c = evt.getKeyChar();

		if (c >= 0x20 && c != 0x7f)
		{
			KeyStroke key = KeyStroke.getKeyStrokeForEvent(evt);
			ActionListener l = bindings.get(key);

			if (l != null)
			{
				executeAction(l, evt.getSource(), String.valueOf(c));
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
			// where A+TAB window switching activates
			// the menu bar on Windows.
			evt.consume();
		}
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
				for (KeyStroke key : keys)
				{
					allKeys.add(key);
				}
			}
		}
		return allKeys;
	}

	public boolean isMapped(KeyEvent evt)
	{
		if (evt == null) return false;

		KeyStroke toTest = KeyStroke.getKeyStrokeForEvent(evt);
		JEditTextArea area = getTextArea(evt);

		List<KeyStroke> allKeys = new ArrayList<KeyStroke>();
		allKeys.addAll(getKeys(area));

		Window w = SwingUtilities.getWindowAncestor(area);
		if (w instanceof JFrame)
		{
			JFrame f = (JFrame)w;
			JMenuBar bar = f.getJMenuBar();
			for (Component c : bar.getComponents())
			{
				allKeys.addAll( getKeys((JComponent)c) );
				if (c instanceof JMenu)
				{
					JMenu m = (JMenu)c;
					for (int i=0; i < m.getItemCount(); i++)
					{
						allKeys.addAll( getKeys(m.getItem(i)));
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

		// this shouldn't happen
		LogMgr.logError("InputHandler.getTextArea()", "Could not find text area!", null);
		return null;
	}

	/**
	 * Macro recorder.
	 */
	public interface MacroRecorder
	{
		void actionPerformed(ActionListener listener,
			String actionCommand);
	}

	public static class redo implements ActionListener
	{
		public void actionPerformed(ActionEvent evt)
		{
			JEditTextArea textArea = getTextArea(evt);
			textArea.redo();
		}
	}

	public static class undo implements ActionListener
	{
		public void actionPerformed(ActionEvent evt)
		{
			JEditTextArea textArea = getTextArea(evt);
			textArea.undo();
		}
	}

	public static class backspace implements ActionListener
	{
		public void actionPerformed(ActionEvent evt)
		{
			JEditTextArea textArea = getTextArea(evt);

			if (!textArea.isEditable())
			{
				return;
			}

			if (textArea.getSelectionStart() != textArea.getSelectionEnd())
			{
				textArea.setSelectedText("");
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

	public static class backspace_word implements ActionListener
	{
		public void actionPerformed(ActionEvent evt)
		{
			JEditTextArea textArea = getTextArea(evt);
			int start = textArea.getSelectionStart();
			if(start != textArea.getSelectionEnd())
			{
				textArea.setSelectedText("");
			}

			int line = textArea.getCaretLine();
			int lineStart = textArea.getLineStartOffset(line);
			int caret = start - lineStart;

			String lineText = textArea.getLineText(textArea.getCaretLine());

			if(caret == 0)
			{
				if(lineStart == 0)
				{
					textArea.getToolkit().beep();
					return;
				}
				caret--;
			}
			else
			{
				caret = TextUtilities.findWordStart(lineText, caret);
			}

			try
			{
				textArea.getDocument().remove(caret + lineStart,start - (caret + lineStart));
			}
			catch(BadLocationException bl)
			{
				bl.printStackTrace();
			}
		}
	}

	public static class delete implements ActionListener
	{
		public void actionPerformed(ActionEvent evt)
		{
			JEditTextArea textArea = getTextArea(evt);

			if(!textArea.isEditable())
			{
				textArea.getToolkit().beep();
				return;
			}

			if(textArea.getSelectionStart() != textArea.getSelectionEnd())
			{
				textArea.setSelectedText("");
			}
			else
			{
				int caret = textArea.getCaretPosition();
				if(caret == textArea.getDocumentLength())
				{
					textArea.getToolkit().beep();
					return;
				}
				try
				{
					textArea.getDocument().remove(caret,1);
				}
				catch(BadLocationException bl)
				{
					bl.printStackTrace();
				}
			}
		}
	}

	public static class delete_word implements ActionListener
	{
		public void actionPerformed(ActionEvent evt)
		{
			JEditTextArea textArea = getTextArea(evt);
			int start = textArea.getSelectionStart();
			if(start != textArea.getSelectionEnd())
			{
				textArea.setSelectedText("");
			}

			int line = textArea.getCaretLine();
			int lineStart = textArea.getLineStartOffset(line);
			int caret = start - lineStart;

			String lineText = textArea.getLineText(textArea.getCaretLine());

			if(caret == lineText.length())
			{
				if(lineStart + caret == textArea.getDocumentLength())
				{
					textArea.getToolkit().beep();
					return;
				}
				caret++;
			}
			else
			{
				caret = TextUtilities.findWordEnd(lineText, caret);
			}

			try
			{
				textArea.getDocument().remove(start,
					(caret + lineStart) - start);
			}
			catch(BadLocationException bl)
			{
				bl.printStackTrace();
			}
		}
	}

	public static class end implements ActionListener
	{
		private final boolean select;

		public end(boolean select)
		{
			this.select = select;
		}

		public void actionPerformed(ActionEvent evt)
		{
			JEditTextArea textArea = getTextArea(evt);

			int line = textArea.getCaretLine();
			int caret = textArea.getCaretPosition();

			int lastOfLine = textArea.getLineEndOffset(line) - 1;
			int lastVisibleLine = textArea.getFirstLine()	+ textArea.getVisibleLines();
			if(lastVisibleLine >= textArea.getLineCount())
			{
				lastVisibleLine = Math.min(textArea.getLineCount() - 1,lastVisibleLine);
			}
			else
			{
				lastVisibleLine -= (textArea.getElectricScroll() + 1);
			}

			int lastVisible = textArea.getLineEndOffset(lastVisibleLine) - 1;
			int lastDocument = textArea.getDocumentLength();

			if(caret == lastDocument)
			{
				textArea.getToolkit().beep();
				if (!select)
				{
					textArea.selectNone();
				}
				return;
			}
			else if(!Boolean.TRUE.equals(textArea.getClientProperty(SMART_HOME_END_PROPERTY)))
				caret = lastOfLine;
			else if(caret == lastVisible)
				caret = lastDocument;
			else if(caret == lastOfLine)
				caret = lastVisible;
			else
				caret = lastOfLine;

			if(select)
				textArea.select(textArea.getMarkPosition(),caret);
			else
				textArea.setCaretPosition(caret);
		}
	}

	public static class document_end implements ActionListener
	{
		private boolean select;

		public document_end(boolean select)
		{
			this.select = select;
		}

		public void actionPerformed(ActionEvent evt)
		{
			JEditTextArea textArea = getTextArea(evt);
			if(select)
			{
				textArea.select(textArea.getMarkPosition(),textArea.getDocumentLength());
			}
			else
			{
				textArea.selectNone();
				textArea.setCaretPosition(textArea.getDocumentLength());
			}
		}
	}

	public static class home implements ActionListener
	{
		private boolean select;

		public home(boolean select)
		{
			this.select = select;
		}

		public void actionPerformed(ActionEvent evt)
		{
			JEditTextArea textArea = getTextArea(evt);

			int caret = textArea.getCaretPosition();

			int firstLine = textArea.getFirstLine();

			int firstOfLine = textArea.getLineStartOffset(textArea.getCaretLine());
			int firstVisibleLine = (firstLine == 0 ? 0 : firstLine + textArea.getElectricScroll());
			int firstVisible = textArea.getLineStartOffset(firstVisibleLine);

			if(caret == 0)
			{
				textArea.getToolkit().beep();
				if (!select) textArea.selectNone();
				return;
			}
			else if(!Boolean.TRUE.equals(textArea.getClientProperty(SMART_HOME_END_PROPERTY)))
				caret = firstOfLine;
			else if(caret == firstVisible)
				caret = 0;
			else if(caret == firstOfLine)
				caret = firstVisible;
			else
				caret = firstOfLine;

			if(select)
			{
				textArea.select(textArea.getMarkPosition(),caret);
			}
			else
			{
				textArea.selectNone();
				textArea.setCaretPosition(caret);
			}
		}
	}

	public static class document_home implements ActionListener
	{
		private boolean select;

		public document_home(boolean select)
		{
			this.select = select;
		}

		public void actionPerformed(ActionEvent evt)
		{
			JEditTextArea textArea = getTextArea(evt);
			if(select)
				textArea.select(textArea.getMarkPosition(),0);
			else
				textArea.setCaretPosition(0);
		}
	}

	public static class insert_break implements ActionListener
	{
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

	public static class insert_tab implements ActionListener
	{
		public void actionPerformed(ActionEvent evt)
		{
			JEditTextArea textArea = getTextArea(evt);

			if(!textArea.isEditable())
			{
				textArea.getToolkit().beep();
				return;
			}
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

	public static class next_char implements ActionListener
	{
		private boolean select;

		public next_char(boolean select)
		{
			this.select = select;
		}

		public void actionPerformed(ActionEvent evt)
		{
			JEditTextArea textArea = getTextArea(evt);
			int caret = textArea.getCaretPosition();
			if(caret == textArea.getDocumentLength())
			{
				textArea.getToolkit().beep();
				if (!select) textArea.selectNone();
				return;
			}

			if(select)
				textArea.select(textArea.getMarkPosition(),caret + 1);
			else
				textArea.setCaretPosition(caret + 1);
		}
	}

	public static class next_line implements ActionListener
	{
		private boolean select;

		public next_line(boolean select)
		{
			this.select = select;
		}

		public void actionPerformed(ActionEvent evt)
		{
			JEditTextArea textArea = getTextArea(evt);
			int caret = textArea.getCaretPosition();
			int line = textArea.getCaretLine();

			if(line == textArea.getLineCount() - 1)
			{
				textArea.getToolkit().beep();
				if (!select) textArea.selectNone();
				return;
			}

			int magic = textArea.getMagicCaretPosition();
			if(magic == -1)
			{
				magic = textArea.offsetToX(line,
					caret - textArea.getLineStartOffset(line));
			}

			caret = textArea.getLineStartOffset(line + 1) + textArea.xToOffset(line + 1,magic);

			if(select)
				textArea.select(textArea.getMarkPosition(),caret);
			else
				textArea.setCaretPosition(caret);

			textArea.setMagicCaretPosition(magic);
		}
	}

	public static class next_page implements ActionListener
	{
		private boolean select;

		public next_page(boolean select)
		{
			this.select = select;
		}

		public void actionPerformed(ActionEvent evt)
		{
			JEditTextArea textArea = getTextArea(evt);
			int lineCount = textArea.getLineCount();
			int firstLine = textArea.getFirstLine();
			int visibleLines = textArea.getVisibleLines();
			int line = textArea.getCaretLine();

			firstLine += visibleLines;

			if(firstLine + visibleLines >= lineCount - 1)
				firstLine = lineCount - visibleLines;

			textArea.setFirstLine(firstLine);

			int caret = textArea.getLineStartOffset(Math.min(textArea.getLineCount() - 1,line + visibleLines));

			if(select)
				textArea.select(textArea.getMarkPosition(),caret);
			else
				textArea.setCaretPosition(caret);
		}
	}

	public static class next_word implements ActionListener
	{
		private boolean select;

		public next_word(boolean select)
		{
			this.select = select;
		}

		public void actionPerformed(ActionEvent evt)
		{
			JEditTextArea textArea = getTextArea(evt);
			int caret = textArea.getCaretPosition();
			int line = textArea.getCaretLine();
			int lineStart = textArea.getLineStartOffset(line);
			caret -= lineStart;

			String lineText = textArea.getLineText(textArea.getCaretLine());

			if(caret == lineText.length())
			{
				if(lineStart + caret == textArea.getDocumentLength())
				{
					textArea.getToolkit().beep();
					return;
				}
				caret++;
			}
			else
			{
				caret = TextUtilities.findWordEnd(lineText,caret);
			}

			if(select)
				textArea.select(textArea.getMarkPosition(),lineStart + caret);
			else
				textArea.setCaretPosition(lineStart + caret);
		}
	}

	public static class overwrite implements ActionListener
	{
		public void actionPerformed(ActionEvent evt)
		{
			JEditTextArea textArea = getTextArea(evt);
			textArea.setOverwriteEnabled(!textArea.isOverwriteEnabled());
		}
	}

	public static class prev_char implements ActionListener
	{
		private boolean select;

		public prev_char(boolean select)
		{
			this.select = select;
		}

		public void actionPerformed(ActionEvent evt)
		{
			JEditTextArea textArea = getTextArea(evt);
			int caret = textArea.getCaretPosition();
			if(caret == 0)
			{
				textArea.getToolkit().beep();
				if (!select) textArea.selectNone();
				return;
			}

			if(select)
				textArea.select(textArea.getMarkPosition(),caret - 1);
			else
				textArea.setCaretPosition(caret - 1);
		}
	}

	public static class prev_line implements ActionListener
	{
		private boolean select;

		public prev_line(boolean select)
		{
			this.select = select;
		}

		public void actionPerformed(ActionEvent evt)
		{
			JEditTextArea textArea = getTextArea(evt);
			int caret = textArea.getCaretPosition();
			int line = textArea.getCaretLine();

			if(line == 0)
			{
				textArea.getToolkit().beep();
				if (!select) textArea.selectNone();
				return;
			}

			int magic = textArea.getMagicCaretPosition();
			if(magic == -1)
			{
				magic = textArea.offsetToX(line,caret - textArea.getLineStartOffset(line));
			}

			caret = textArea.getLineStartOffset(line - 1) + textArea.xToOffset(line - 1,magic);

			if(select)
				textArea.select(textArea.getMarkPosition(),caret);
			else
				textArea.setCaretPosition(caret);

			textArea.setMagicCaretPosition(magic);
		}
	}

	public static class prev_page implements ActionListener
	{
		private boolean select;

		public prev_page(boolean select)
		{
			this.select = select;
		}

		public void actionPerformed(ActionEvent evt)
		{
			JEditTextArea textArea = getTextArea(evt);
			int firstLine = textArea.getFirstLine();
			int visibleLines = textArea.getVisibleLines();
			int line = textArea.getCaretLine();

			if(firstLine < visibleLines) firstLine = visibleLines;

			textArea.setFirstLine(firstLine - visibleLines);

			int caret = textArea.getLineStartOffset(Math.max(0,line - visibleLines));

			if(select)
				textArea.select(textArea.getMarkPosition(),caret);
			else
				textArea.setCaretPosition(caret);
		}
	}

	public static class prev_word implements ActionListener
	{
		private boolean select;

		public prev_word(boolean select)
		{
			this.select = select;
		}

		public void actionPerformed(ActionEvent evt)
		{
			JEditTextArea textArea = getTextArea(evt);
			int caret = textArea.getCaretPosition();
			int line = textArea.getCaretLine();
			int lineStart = textArea.getLineStartOffset(line);
			caret -= lineStart;

			String lineText = textArea.getLineText(textArea.getCaretLine());

			if(caret == 0)
			{
				if(lineStart == 0)
				{
					textArea.getToolkit().beep();
					return;
				}
				caret--;
			}
			else
			{
				caret = TextUtilities.findWordStart(lineText, caret);
			}

			if(select)
				textArea.select(textArea.getMarkPosition(),lineStart + caret);
			else
				textArea.setCaretPosition(lineStart + caret);
		}
	}

	public static class toggle_rect implements ActionListener
	{
		public void actionPerformed(ActionEvent evt)
		{
			JEditTextArea textArea = getTextArea(evt);
			textArea.setSelectionRectangular(!textArea.isSelectionRectangular());
		}
	}

	public static class insert_char
		implements ActionListener
	{
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