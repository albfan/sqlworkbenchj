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
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.InputEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.util.EventObject;
import java.util.HashMap;
import java.util.Iterator;

import java.util.Map;
import java.util.StringTokenizer;
import javax.swing.JPopupMenu;
import javax.swing.KeyStroke;
import javax.swing.text.BadLocationException;
import workbench.resource.PlatformShortcuts;

/**
 * An input handler converts the user's key strokes into concrete actions.
 * It also takes care of macro recording and action repetition.<p>
 *
 * This class provides all the necessary support code for an input
 * handler, but doesn't actually do any key binding logic. It is up
 * to the implementations of this class to do so.
 *
 * @author Slava Pestov
 * @version $Id: InputHandler.java,v 1.28 2008/10/04 13:03:01 thomas Exp $
 * @see DefaultInputHandler
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
	public static final ActionListener REPEAT = new repeat();
	public static final ActionListener TOGGLE_RECT = new toggle_rect();
	public static final ActionListener UNDO = new undo();
	public static final ActionListener REDO = new redo();

	// Default action
	public static final ActionListener INSERT_CHAR = new insert_char();

	private static Map<String, ActionListener> actions;

	protected boolean repeat;
	protected int repeatCount;

	static
	{
		actions = new HashMap<String, ActionListener>();
		actions.put("backspace",BACKSPACE);
		actions.put("backspace-word",BACKSPACE_WORD);
		actions.put("delete",DELETE);
		actions.put("delete-word",DELETE_WORD);
		actions.put("end",END);
		actions.put("select-end",SELECT_END);
		actions.put("document-end",DOCUMENT_END);
		actions.put("select-doc-end",SELECT_DOC_END);
		actions.put("insert-break",INSERT_BREAK);
		actions.put("insert-tab",INSERT_TAB);
		actions.put("home",HOME);
		actions.put("select-home",SELECT_HOME);
		actions.put("document-home",DOCUMENT_HOME);
		actions.put("select-doc-home",SELECT_DOC_HOME);
		actions.put("next-char",NEXT_CHAR);
		actions.put("next-line",NEXT_LINE);
		actions.put("next-page",NEXT_PAGE);
		actions.put("next-word",NEXT_WORD);
		actions.put("select-next-char",SELECT_NEXT_CHAR);
		actions.put("select-next-line",SELECT_NEXT_LINE);
		actions.put("select-next-page",SELECT_NEXT_PAGE);
		actions.put("select-next-word",SELECT_NEXT_WORD);
		actions.put("overwrite",OVERWRITE);
		actions.put("prev-char",PREV_CHAR);
		actions.put("prev-line",PREV_LINE);
		actions.put("prev-page",PREV_PAGE);
		actions.put("prev-word",PREV_WORD);
		actions.put("select-prev-char",SELECT_PREV_CHAR);
		actions.put("select-prev-line",SELECT_PREV_LINE);
		actions.put("select-prev-page",SELECT_PREV_PAGE);
		actions.put("select-prev-word",SELECT_PREV_WORD);
		actions.put("repeat",REPEAT);
		actions.put("toggle-rect",TOGGLE_RECT);
		actions.put("insert-char",INSERT_CHAR);
	}

	// private members
	private Map bindings;
	private Map currentBindings;

	public InputHandler()
	{
		bindings = currentBindings = new HashMap();
	}
	/**
	 * Returns a named text area action.
	 * @param name The action name
	 */
	public static ActionListener getAction(String name)
	{
		return actions.get(name);
	}

	/**
	 * Returns the name of the specified text area action.
	 * @param listener The action
	 */
	public static String getActionName(ActionListener listener)
	{
		Iterator itr = getActions();
		while(itr.hasNext())
		{
			String name = (String)itr.next();
			ActionListener _listener = getAction(name);
			if(_listener == listener)
				return name;
		}
		return null;
	}

	/**
	 * Returns an enumeration of all available actions.
	 */
	public static Iterator getActions()
	{
		return actions.keySet().iterator();
	}

	/**
	 * Adds the default key bindings to this input handler.
	 * This should not be called in the constructor of this
	 * input handler, because applications might load the
	 * key bindings from a file, etc.
	 */
	public void addDefaultKeyBindings()
	{
		addKeyBinding("BACK_SPACE",BACKSPACE);
		addKeyBinding("C+BACK_SPACE",BACKSPACE_WORD);
		addKeyBinding("DELETE",DELETE);
		addKeyBinding("C+DELETE",DELETE_WORD);

		addKeyBinding("ENTER",INSERT_BREAK);
		addKeyBinding("TAB",INSERT_TAB);

		addKeyBinding("INSERT",OVERWRITE);

		addKeyBinding("HOME",HOME);
		addKeyBinding("END",END);
		addKeyBinding("S+HOME",SELECT_HOME);
		addKeyBinding("S+END",SELECT_END);
		addKeyBinding("C+HOME",DOCUMENT_HOME);
		addKeyBinding("C+END",DOCUMENT_END);
		addKeyBinding("CS+HOME",SELECT_DOC_HOME);
		addKeyBinding("CS+END",SELECT_DOC_END);

		addKeyBinding("PAGE_UP",PREV_PAGE);
		addKeyBinding("PAGE_DOWN",NEXT_PAGE);
		addKeyBinding("S+PAGE_UP",SELECT_PREV_PAGE);
		addKeyBinding("S+PAGE_DOWN",SELECT_NEXT_PAGE);

		addKeyBinding("LEFT",PREV_CHAR);
		addKeyBinding("S+LEFT",SELECT_PREV_CHAR);
		addKeyBinding("C+LEFT",PREV_WORD);
		addKeyBinding("CS+LEFT",SELECT_PREV_WORD);
		addKeyBinding("RIGHT",NEXT_CHAR);
		addKeyBinding("S+RIGHT",SELECT_NEXT_CHAR);
		addKeyBinding("C+RIGHT",NEXT_WORD);
		addKeyBinding("CS+RIGHT",SELECT_NEXT_WORD);
		addKeyBinding("UP",PREV_LINE);
		addKeyBinding("S+UP",SELECT_PREV_LINE);
		addKeyBinding("DOWN",NEXT_LINE);
		addKeyBinding("S+DOWN",SELECT_NEXT_LINE);

//		addKeyBinding("C+U", MAKE_UPPER_CASE);
//		addKeyBinding("C+L", MAKE_LOWER_CASE);
		addKeyBinding("C+Z", UNDO);
		addKeyBinding("C+Y", REDO);
	}

	/**
	 * Adds a key binding to this input handler.
	 * @param keyBinding The key binding (the format of this is
	 * input-handler specific)
	 * @param action The action
	 */
	@SuppressWarnings("unchecked")
	public void addKeyBinding(String keyBinding, ActionListener action)
	{
		Map current = bindings;

		StringTokenizer st = new StringTokenizer(keyBinding);
		while(st.hasMoreTokens())
		{
			KeyStroke keyStroke = parseKeyStroke(st.nextToken());
			if(keyStroke == null)
				return;

			if(st.hasMoreTokens())
			{
				Object o = current.get(keyStroke);
				if (!(o instanceof HashMap))
				{
					o = new HashMap();
					current.put(keyStroke, o);
				}
				current = (HashMap)o;
			}
			else
			{
				current.put(keyStroke,action);
			}
		}
		this.currentBindings = bindings;
	}

	@SuppressWarnings("unchecked")
	public void addKeyBinding(KeyStroke key, ActionListener action)
	{
		this.bindings.put(key, action);
		this.currentBindings = this.bindings;
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

	public void keyPressed(KeyEvent evt)
	{
		int keyCode = evt.getKeyCode();
		int modifiers = evt.getModifiers();

		if (keyCode == KeyEvent.VK_CONTROL ||
			keyCode == KeyEvent.VK_SHIFT ||
			keyCode == KeyEvent.VK_ALT ||
			keyCode == KeyEvent.VK_META ||
			keyCode == KeyEvent.ALT_GRAPH_MASK)
			return;

		if ((modifiers & ~KeyEvent.SHIFT_MASK) != 0
			|| evt.isActionKey()
			|| keyCode == KeyEvent.VK_BACK_SPACE
			|| keyCode == KeyEvent.VK_DELETE
			|| keyCode == KeyEvent.VK_ENTER
			|| keyCode == KeyEvent.VK_TAB
			|| keyCode == KeyEvent.VK_ESCAPE)
		{

			if (keyCode == KeyEvent.VK_TAB)
			{
				JEditTextArea area = getTextArea(evt);
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

			KeyStroke keyStroke = KeyStroke.getKeyStroke(keyCode,modifiers);
			Object o = currentBindings.get(keyStroke);

			if (o == null)
			{
				currentBindings = bindings;
				return;
			}
			else if (o instanceof ActionListener)
			{
				currentBindings = bindings;

				executeAction(((ActionListener)o),evt.getSource(),null);

				evt.consume();
				return;
			}
			else if (o instanceof HashMap)
			{
				currentBindings = (HashMap)o;
				evt.consume();
				return;
			}
		}
	}

	public void keyTyped(KeyEvent evt)
	{
		if (evt.isControlDown() || evt.isAltDown() || evt.isMetaDown() || evt.isActionKey()) return;

		char c = evt.getKeyChar();

		if(c != KeyEvent.CHAR_UNDEFINED)
		{
			if(c >= 0x20 && c != 0x7f)
			{
				KeyStroke keyStroke = KeyStroke.getKeyStroke(Character.toUpperCase(c));
				Object o = currentBindings.get(keyStroke);

				if(o instanceof HashMap)
				{
					currentBindings = (HashMap)o;
					return;
				}
				else if(o instanceof ActionListener)
				{
					currentBindings = bindings;
					executeAction((ActionListener)o,evt.getSource(),String.valueOf(c));
					return;
				}

				currentBindings = bindings;

				// 0-9 adds another 'digit' to the repeat number
				if (repeat && Character.isDigit(c))
				{
					repeatCount *= 10;
					repeatCount += (c - '0');
					return;
				}

				executeAction(INSERT_CHAR,evt.getSource(),String.valueOf(evt.getKeyChar()));

				repeatCount = 0;
				repeat = false;
			}
		}
	}

	/**
	 * Converts a string to a keystroke. The string should be of the
	 * form <i>modifiers</i>+<i>shortcut</i> where <i>modifiers</i>
	 * is any combination of A for Alt, C for Control, S for Shift
	 * or M for Meta, and <i>shortcut</i> is either a single character,
	 * or a keycode name from the <code>KeyEvent</code> class, without
	 * the <code>VK_</code> prefix.
	 * @param keyStroke A string description of the key stroke
	 */
	public static KeyStroke parseKeyStroke(String keyStroke)
	{
		if(keyStroke == null)	return null;
		
		int modifiers = 0;
		int index = keyStroke.indexOf('+');
		if(index != -1)
		{
			for(int i = 0; i < index; i++)
			{
				switch(Character.toUpperCase(keyStroke.charAt(i)))
				{
				case 'A':
					modifiers |= InputEvent.ALT_MASK;
					break;
				case 'C':
				case 'M':
					modifiers |= PlatformShortcuts.getDefaultModifier();
					break;
				case 'S':
					modifiers |= InputEvent.SHIFT_MASK;
					break;
				}
			}
		}
		String key = keyStroke.substring(index + 1);
		if(key.length() == 1)
		{
			char ch = Character.toUpperCase(key.charAt(0));
			if(modifiers == 0)
				return KeyStroke.getKeyStroke(ch);
			else
				return KeyStroke.getKeyStroke(ch,modifiers);
		}
		else if(key.length() == 0)
		{
			return null;
		}
		else
		{
			int ch;

			try
			{
				ch = KeyEvent.class.getField("VK_".concat(key)).getInt(null);
			}
			catch(Exception e)
			{
				System.err.println("Invalid key stroke: "	+ keyStroke);
				return null;
			}

			return KeyStroke.getKeyStroke(ch,modifiers);
		}
	}

	/**
	 * Grabs the next key typed event and invokes the specified
	 * action with the key as a the action command.
	 * @param listener The Listener
	 */
//	public void grabNextKeyStroke(ActionListener listener)
//	{
//		grabAction = listener;
//	}

	/**
	 * Returns if repeating is enabled. When repeating is enabled,
	 * actions will be executed multiple times. This is usually
	 * invoked with a special key stroke in the input handler.
	 */
	public boolean isRepeatEnabled()
	{
		return repeat;
	}

	/**
	 * Enables repeating. When repeating is enabled, actions will be
	 * executed multiple times. Once repeating is enabled, the input
	 * handler should read a number from the keyboard.
	 */
	public void setRepeatEnabled(boolean repeat)
	{
		this.repeat = repeat;
	}

	/**
	 * Returns the number of times the next action will be repeated.
	 */
	public int getRepeatCount()
	{
		return (repeat ? Math.max(1,repeatCount) : 1);
	}

	/**
	 * Sets the number of times the next action will be repeated.
	 * @param repeatCount The repeat count
	 */
	public void setRepeatCount(int repeatCount)
	{
		this.repeatCount = repeatCount;
	}

	/**
	 * Executes the specified action, repeating and recording it as
	 * necessary.
	 * @param listener The action listener
	 * @param source The event source
	 * @param actionCommand The action command
	 */
	public void executeAction(ActionListener listener, Object source, String actionCommand)
	{
		// create event
		ActionEvent evt = new ActionEvent(source,ActionEvent.ACTION_PERFORMED,actionCommand);

		// don't do anything if the action is a wrapper
		// (like EditAction.Wrapper)
		if(listener instanceof Wrapper)
		{
			listener.actionPerformed(evt);
			return;
		}

		// remember old values, in case action changes them
		boolean _repeat = repeat;
		int _repeatCount = getRepeatCount();

		// execute the action
		if(listener instanceof InputHandler.NonRepeatable)
		{
			listener.actionPerformed(evt);
		}
		else
		{
			for(int i = 0; i < Math.max(1,repeatCount); i++)
			{
				listener.actionPerformed(evt);
			}
		}

		// If repeat was true originally, clear it
		// Otherwise it might have been set by the action, etc
		if (_repeat)
		{
			repeat = false;
			repeatCount = 0;
		}
	}

	/**
	 * Returns the text area that fired the specified event.
	 * @param evt The event
	 */
	public static JEditTextArea getTextArea(EventObject evt)
	{
		if(evt != null)
		{
			Object o = evt.getSource();
			if(o instanceof Component)
			{
				// find the parent text area
				Component c = (Component)o;
				for(;;)
				{
					if(c instanceof JEditTextArea)
						return (JEditTextArea)c;
					else if(c == null)
						break;
					if(c instanceof JPopupMenu)
						c = ((JPopupMenu)c).getInvoker();
					else
						c = c.getParent();
				}
			}
		}

		// this shouldn't happen
		System.err.println("BUG: getTextArea() returning null");
		System.err.println("Report this to Slava Pestov <sp@gjt.org>");
		return null;
	}

	/**
	 * If an action implements this interface, it should not be repeated.
	 * Instead, it will handle the repetition itself.
	 */
	public interface NonRepeatable {}

	/**
	 * If an action implements this interface, it should not be recorded
	 * by the macro recorder. Instead, it will do its own recording.
	 */
	public interface NonRecordable {}

	/**
	 * For use by EditAction.Wrapper only.
	 * @since jEdit 2.2final
	 */
	public interface Wrapper {}

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

			if(!textArea.isEditable())
			{
				textArea.getToolkit().beep();
				return;
			}

			if (textArea.getSelectionStart() != textArea.getSelectionEnd())
			{
				textArea.setSelectedText("");
			}
			else
			{
				int caret = textArea.getCaretPosition();
				if(caret == 0)
				{
					textArea.getToolkit().beep();
					return;
				}
				SyntaxDocument doc = textArea.getDocument();
				try
				{
					doc.remove(caret - 1,1);
				}
				catch(BadLocationException bl)
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

			textArea.overwriteSetSelectedText("\t");
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

	public static class repeat implements ActionListener,
		InputHandler.NonRecordable
	{
		public void actionPerformed(ActionEvent evt)
		{
			JEditTextArea textArea = getTextArea(evt);
			textArea.getInputHandler().setRepeatEnabled(true);
			String actionCommand = evt.getActionCommand();
			if(actionCommand != null)
			{
				textArea.getInputHandler().setRepeatCount(Integer.parseInt(actionCommand));
			}
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

	public static class insert_char implements ActionListener,
		InputHandler.NonRepeatable
	{
		public void actionPerformed(ActionEvent evt)
		{
			JEditTextArea textArea = getTextArea(evt);
			String str = evt.getActionCommand();
			int repeatCount = textArea.getInputHandler().getRepeatCount();

			if(textArea.isEditable())
			{
				StringBuilder buf = new StringBuilder();
				for(int i = 0; i < repeatCount; i++) buf.append(str);
				textArea.overwriteSetSelectedText(buf.toString());
			}
			else
			{
				textArea.getToolkit().beep();
			}
		}
	}
}