/*
 * JEditTextArea.java - jEdit's text component
 * Copyright (C) 1999 Slava Pestov
 *
 * You may use and modify this package for any purpose. Redistribution is
 * permitted, in both source and binary form, provided that this notice
 * remains intact in all source distributions of this package.
 */
package workbench.gui.editor;

import java.awt.AWTEvent;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.EventQueue;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Point;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.Transferable;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.AdjustmentEvent;
import java.awt.event.AdjustmentListener;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionListener;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import javax.swing.JComponent;
import javax.swing.JPopupMenu;
import javax.swing.JScrollBar;
import javax.swing.KeyStroke;
import javax.swing.Timer;
import javax.swing.event.CaretEvent;
import javax.swing.event.CaretListener;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.event.EventListenerList;
import javax.swing.text.BadLocationException;
import javax.swing.text.Element;
import javax.swing.text.PlainDocument;
import javax.swing.text.Segment;
import javax.swing.text.Utilities;

import workbench.WbManager;
import workbench.interfaces.ClipboardSupport;
import workbench.interfaces.EditorStatusbar;
import workbench.interfaces.TextChangeListener;
import workbench.interfaces.TextSelectionListener;
import workbench.interfaces.Undoable;
import workbench.log.LogMgr;
import workbench.resource.GuiSettings;
import workbench.resource.Settings;

import workbench.db.QuoteHandler;

import workbench.gui.WbSwingUtilities;
import workbench.gui.actions.CopyAction;
import workbench.gui.actions.CutAction;
import workbench.gui.actions.PasteAction;
import workbench.gui.actions.ScrollDownAction;
import workbench.gui.actions.ScrollUpAction;
import workbench.gui.actions.SelectAllAction;
import workbench.gui.actions.WbAction;
import workbench.gui.fontzoom.FontZoomProvider;
import workbench.gui.fontzoom.FontZoomer;
import workbench.gui.menu.TextPopup;

import workbench.util.MemoryWatcher;
import workbench.util.NumberStringCache;
import workbench.util.StringUtil;

/**
 * jEdit's text area component. It is more suited for editing program
 * source code than JEditorPane, because it drops the unnecessary features
 * (images, variable-width lines, and so on) and adds a whole bunch of
 * useful goodies such as:
 * <ul>
 * <li>More flexible key binding scheme
 * <li>Supports macro recorders
 * <li>Rectangular selection
 * <li>Bracket highlighting
 * <li>Syntax highlighting
 * <li>Command repetition
 * <li>Block caret can be enabled
 * </ul>
 * It is also faster and doesn't have as many problems. It can be used
 * in other applications; the only other part of jEdit it depends on is
 * the syntax package.<p>
 *
 * To use it in your app, treat it like any other component, for example:
 * <pre>JEditTextArea ta = new JEditTextArea();
 * ta.setTokenMarker(new JavaTokenMarker());
 * ta.setText("public class Test {\n"
 *     + "    public static void main(String[] args) {\n"
 *     + "        System.out.println(\"Hello World\");\n"
 *     + "    }\n"
 *     + "}");</pre>
 *
 * @author Slava Pestov (Initial development)
 * @author Thomas Kellerer (bugfixes and enhancements)
 */
public class JEditTextArea
	extends JComponent
	implements MouseWheelListener, Undoable, ClipboardSupport, FocusListener, LineScroller, FontZoomProvider, PropertyChangeListener
{
	protected boolean rightClickMovesCursor = false;

	private Color alternateSelectionColor;
	private static final Color ERROR_COLOR = Color.RED.brighter();
	private static final Color TEMP_COLOR = Color.GREEN.brighter();
	private boolean currentSelectionIsTemporary;
	protected String commentChar;
	private TokenMarker currentTokenMarker;

	private KeyListener keyEventInterceptor;
	private KeyListener keyNotificationListener;
	protected EditorStatusbar statusBar;

	protected static final String CENTER = "center";
	protected static final String RIGHT = "right";
	protected static final String BOTTOM = "bottom";

	protected Timer caretTimer;

	protected TextAreaPainter painter;

	protected TextPopup popup;

	protected EventListenerList listeners;
	private final MutableCaretEvent caretEvent;

	protected boolean caretBlinks;
	protected boolean caretVisible;
	protected boolean blink;

	protected boolean editable = true;
	protected boolean autoIndent = true;

	protected int firstLine;
	protected int visibleLines;
	protected int electricScroll;

	protected int horizontalOffset;

	protected JScrollBar vertical;
	protected JScrollBar horizontal;
	protected boolean scrollBarsInitialized;

	protected InputHandler inputHandler;
	protected SyntaxDocument document;
	private final DocumentHandler documentHandler;

	protected Segment lineSegment;

	protected int selectionStart;
	protected int selectionStartLine;
	protected int selectionEnd;
	protected int selectionEndLine;
	protected boolean biasLeft;
	protected Color currentColor = null;

	protected int bracketPosition;
	protected int bracketLine;

	protected int magicCaret;
	protected boolean overwrite;
	protected boolean rectSelect;
	protected Boolean highlightSelection;

	private long lastModified;
	private int invalidationInterval = 10;

	private final FontZoomer fontZoomer;

	private BracketCompleter bracketCompleter;
	private boolean smartClosing = true;

	private MacroExpander expander;

	/**
	 * Creates a new JEditTextArea with the default settings.
	 */
	public JEditTextArea()
	{
		super();
		enableEvents(AWTEvent.KEY_EVENT_MASK);

		painter = new TextAreaPainter(this);
		setBackground(Color.WHITE);
    setDoubleBuffered(true);

		documentHandler = new DocumentHandler();
		listeners = new EventListenerList();
		caretEvent = new MutableCaretEvent();
		lineSegment = new Segment();
		bracketLine = -1;
		bracketPosition = -1;
		lastModified = 0;
		blink = true;

    setLayout(new BorderLayout());
    add(painter, BorderLayout.CENTER);

		vertical = new JScrollBar(JScrollBar.VERTICAL);
    add(vertical, BorderLayout.EAST);
    vertical.setVisible(false);


		horizontal = new JScrollBar(JScrollBar.HORIZONTAL);
    add(horizontal, BorderLayout.SOUTH);
    horizontal.setVisible(false);

		// Add some event listeners
		vertical.addAdjustmentListener(new AdjustHandler());
		horizontal.addAdjustmentListener(new AdjustHandler());
		painter.addComponentListener(new ComponentHandler());
		painter.addMouseListener(new MouseHandler());
		painter.addMouseMotionListener(new DragHandler());
		this.addMouseWheelListener(this);
		addFocusListener(this);

		// Load the defaults
		this.inputHandler = new InputHandler();
		setDocument(new SyntaxDocument());

		// Let the focusGained() event display the caret
		caretVisible = false;
		caretBlinks = true;

		electricScroll = Settings.getInstance().getElectricScroll();
		this.setTabSize(Settings.getInstance().getEditorTabWidth());
		this.popup = new TextPopup(this);

		boolean extendedCutCopyPaste = Settings.getInstance().getBoolProperty("workbench.editor.extended.cutcopypaste", true);

		CopyAction copy = new CopyAction(this);
		PasteAction paste = new PasteAction(this);
		CutAction cut = new CutAction(this);

		this.addKeyBinding(copy);
		this.addKeyBinding(paste);
		this.addKeyBinding(cut);
		this.addKeyBinding(new SelectAllAction(this));

		if (extendedCutCopyPaste)
		{
			this.inputHandler.addKeyBinding(KeyStroke.getKeyStroke(KeyEvent.VK_INSERT, KeyEvent.CTRL_MASK), copy);
			this.inputHandler.addKeyBinding(KeyStroke.getKeyStroke(KeyEvent.VK_INSERT, KeyEvent.SHIFT_MASK), paste);
			this.inputHandler.addKeyBinding(KeyStroke.getKeyStroke(KeyEvent.VK_DELETE, KeyEvent.SHIFT_MASK), cut);
		}

		this.addKeyBinding(new ScrollDownAction(this));
		this.addKeyBinding(new ScrollUpAction(this));

		this.invalidationInterval = Settings.getInstance().getIntProperty("workbench.editor.update.lineinterval", 10);
		this.fontZoomer = new FontZoomer(painter);
		initWheelZoom();
		smartClosing = Settings.getInstance().getBoolProperty(GuiSettings.PROPERTY_SMART_COMPLETE, true);
		Settings.getInstance().addPropertyChangeListener(this,
			GuiSettings.PROPERTY_SMART_COMPLETE,
			Settings.PROPERTY_EDITOR_OCCURANCE_HIGHLIGHT,
			Settings.PROPERTY_EDITOR_OCCURANCE_HIGHLIGHT_MINLEN,
			Settings.PROPERTY_EDITOR_OCCURANCE_HIGHLIGHT_NO_WHITESPACE,
			GuiSettings.PROP_FONT_ZOOM_WHEEL);
	}

	private void initWheelZoom()
	{
		if (GuiSettings.getZoomFontWithMouseWheel())
		{
			this.addMouseWheelListener(fontZoomer);
		}
		else
		{
			this.removeMouseWheelListener(fontZoomer);
		}
	}

	@Override
	public void propertyChange(PropertyChangeEvent evt)
	{
		if (evt.getPropertyName().equals(GuiSettings.PROPERTY_SMART_COMPLETE))
		{
			smartClosing = Settings.getInstance().getBoolProperty(GuiSettings.PROPERTY_SMART_COMPLETE, true);
		}
		else if (evt.getPropertyName().startsWith(Settings.PROPERTY_EDITOR_OCCURANCE_HIGHLIGHT_BASE))
		{
			updateOccuranceHilite();
		}
		else if (evt.getPropertyName().startsWith(GuiSettings.PROP_FONT_ZOOM_WHEEL))
		{
			initWheelZoom();
		}
	}

	@Override
	public void setCursor(Cursor c)
	{
		super.setCursor(c);
		this.painter.setCursor(c);
	}

	public int getHScrollBarHeight()
	{
		if (horizontal != null && horizontal.isVisible())
		{
			return (int) horizontal.getPreferredSize().getHeight();
		}
		else
		{
			return 0;
		}
	}

	@Override
	public FontZoomer getFontZoomer()
	{
		return fontZoomer;
	}

	public Point getCursorLocation()
	{
		int line = getCaretLine();
		int pos = getCaretPosition() - getLineStartOffset(line);
		FontMetrics fm = painter.getFontMetrics();
		int y = (line - firstLine + 1) * fm.getHeight();
		if (y <= 0) y = 0;
		y += 4;
		int x = offsetToX(line, pos);
		if (x < 0) x = 0;
		x += this.getPainter().getGutterWidth();
		return new Point(x,y);
	}

	private String fixLinefeed(String input)
	{
		return StringUtil.makePlainLinefeed(input);
	}

	private void changeCase(boolean toLower)
	{
		String sel = this.getSelectedText();
		if (sel == null || sel.length() == 0) return;
		int start = this.getSelectionStart();
		int end = this.getSelectionEnd();
		if (toLower)
		{
			sel = sel.toLowerCase();
		}
		else
		{
			sel = sel.toUpperCase();
		}
		this.setSelectedText(sel);
		this.select(start, end);
	}

	public String getCommentChar()
	{
		return this.commentChar;
	}

	public void toLowerCase()
	{
		this.changeCase(true);
	}

	public void toUpperCase()
	{
		this.changeCase(false);
	}

	public void matchBracket()
	{
		try
		{
			int bracket = getBracketPosition() + 1;
			int line = getBracketLine();
			int caret = getLineStartOffset(line) + bracket;
			if (caret > -1)
			{
				scrollTo(line, caret);
				setCaretPosition(caret);
			}
		}
		catch (Exception e)
		{
			// ignore
		}
	}

	public JScrollBar getVerticalScrollBar()
	{
		return vertical;
	}

	public JScrollBar getHorizontalBar()
	{
		return horizontal;
	}

	public void notifiyKeyEvents(KeyListener l)
	{
		this.keyNotificationListener = l;
	}

	public void stopKeyNotification()
	{
		this.keyNotificationListener = null;
	}

	public final void addKeyBinding(WbAction anAction)
	{
		this.inputHandler.addKeyBinding(anAction);
	}

	public void removeKeyBinding(KeyStroke key)
	{
		this.inputHandler.removeKeyBinding(key);
	}

	public void removeKeyBinding(WbAction action)
	{
		this.inputHandler.removeKeyBinding(action);
	}

	/**
	 * isManagingFocus() returns true to make sure the tab key is handled
	 * by the editor, and does not move the focus to the next component
	 *
	 * @return true
	 */
	@SuppressWarnings("deprecation")
	@Override
	public final boolean isManagingFocus()
	{
		return true;
	}

	/**
	 * Returns the object responsible for painting this text area.
	 */
	public final TextAreaPainter getPainter()
	{
		return painter;
	}

	/**
	 * Returns true if the caret is blinking, false otherwise.
	 */
	public final boolean isCaretBlinkEnabled()
	{
		return caretBlinks;
	}

	protected void stopBlinkTimer()
	{
		if (this.caretTimer != null)
		{
			caretTimer.stop();
		}
		caretTimer = null;
	}

	private void startBlinkTimer()
	{
		if (caretTimer != null) return;

		final int blinkInterval = 750;
		caretTimer = new Timer(blinkInterval,
		 new ActionListener()
			{
				@Override
				public void actionPerformed(ActionEvent evt)
				{
					blinkCaret();
				}
			});
		caretTimer.setInitialDelay(blinkInterval);
		caretTimer.start();
	}

	/**
	 * Toggles caret blinking.
	 * @param caretBlinks True if the caret should blink, false otherwise
	 */
	public void setCaretBlinkEnabled(boolean caretBlinks)
	{
		this.caretBlinks = caretBlinks;
		if(!caretBlinks)
		{
			blink = false;
		}
		if (caretBlinks)
		{
			startBlinkTimer();
		}
		else
		{
			stopBlinkTimer();
		}
		painter.invalidateSelectedLines();
	}

	public void dispose()
	{
		if (bracketCompleter != null) bracketCompleter.dispose();
		if (expander != null) expander.dispose();
		Settings.getInstance().removePropertyChangeListener(this);
	}

	public MacroExpander getMacroExpander()
	{
		return expander;
	}

	public void setMacroExpansionEnabled(boolean flag, int clientId)
	{
		if (flag && expander == null)
		{
			expander = new MacroExpander(clientId, this);
		}
		else if (!flag)
		{
			if (expander != null)
			{
				expander.dispose();
			}
			expander = null;
		}
	}

	public boolean expandWordAtCursor()
	{
		if (expander == null) return false;
		return expander.expandWordAtCursor();
	}

	public boolean removeClosingBracket(int position)
	{
		if (bracketCompleter == null || currentTokenMarker == null) return false;
		String toBeRemoved = getText(position - 1, 1);
		if (toBeRemoved == null) return false;

		String toComplete = bracketCompleter.getCompletionChar(toBeRemoved.charAt(0));
		if (toComplete == null) return false;

		String next = getText(position, 1);
		return StringUtil.equalString(toComplete, next);
	}

	public void completeBracket(char currentChar)
	{
		if (bracketCompleter == null || currentTokenMarker == null) return;

		String toComplete = bracketCompleter.getCompletionChar(currentChar);
		if (toComplete == null) return;

		int line = getCaretLine();
		int caret = getCaretPositionInLine(line);

		getLineText(getCaretLine(), lineSegment);

		if (caret > 0 && caret < lineSegment.length())
		{
			char nextChar = lineSegment.charAt(caret);
			if (!Character.isWhitespace(nextChar)) return;
		}

		// do not complete brackets inside string literals
		if (!currentTokenMarker.isStringLiteralAt(line, caret))
		{
			// insertText() will move the caret, so we need to remember the last position
			// in order to put the caret back where it was
			int caretPosition = getCaretPosition();
			insertText(caretPosition, toComplete);
			setCaretPosition(caretPosition);
		}
	}

	/**
	 * Check if the currently typed character should be inserted.
	 *
	 * This is only valid if auto-closing of brackets is enabled.
	 *
	 * In that case typing a closing bracket right in front of a closing bracket
	 * will only be "honored" if the brackets in the current line are un-balanced.
	 *
	 * @param currentChar the typed char.
	 * @return true, the character should be insert,
	 *         false ignore this character - just move the caret
	 */
	public boolean shouldInsert(char currentChar)
	{
		if (!smartClosing) return true;
		if (bracketCompleter == null || currentTokenMarker == null) return true;
		char opening = bracketCompleter.getOpeningChar(currentChar);
		if (opening == 0) return true;

		getLineText(getCaretLine(), lineSegment);
		int caret = getCaretPositionInLine(getCaretLine());

		if (caret <= 0 || caret >= lineSegment.length()) return true;

		char nextChar = lineSegment.charAt(caret);

		if (nextChar != currentChar) return true;

		int count = 0;
		for (int i=0; i < lineSegment.length(); i++)
		{
			char c = lineSegment.charAt(i);
			if (c == opening)
			{
				count ++;
			}
			else if (c == currentChar)
			{
				count --;
			}
		}
		return count != 0;
	}

	public void setBracketCompletionEnabled(boolean flag)
	{
		if (bracketCompleter == null && flag)
		{
			this.bracketCompleter = new BracketCompleter();
		}
		else if (!flag)
		{
			if (bracketCompleter != null) bracketCompleter.dispose();
			bracketCompleter = null;
		}
	}

	/**
	 * Returns true if the caret is visible, false otherwise.
	 */
	public final boolean isCaretVisible()
	{
		return (!caretBlinks || blink) && caretVisible;
	}

	public boolean isTextSelected()
	{
		int start = this.getSelectionStart();
		int end = this.getSelectionEnd();
		return (start < end);
	}

	/**
	 * Sets if the caret should be visible.
	 * @param caretVisible True if the caret should be visible, false
	 * otherwise
	 */
	public void setCaretVisible(boolean caretVisible)
	{
		this.caretVisible = caretVisible;
		blink = true;
		painter.invalidateSelectedLines();
	}

	@Override
	public void focusGained(FocusEvent e)
	{
		inputHandler.resetStatus();
		setCaretVisible(true);
	}

	@Override
	public void focusLost(FocusEvent e)
	{
		inputHandler.resetStatus();
		setCaretVisible(false);
	}

	/**
	 * Blinks the caret.
	 */
	public final void blinkCaret()
	{
		if (!caretVisible) return;

		if (caretBlinks)
		{
			blink = !blink;
			painter.invalidateSelectedLines();
		}
		else
		{
			blink = true;
		}
	}

	/**
	 * Returns the number of lines from the top and button of the
	 * text area that are always visible.
	 */
	public final int getElectricScroll()
	{
		return electricScroll;
	}

	/**
	 * Sets the number of lines from the top and bottom of the text
	 * area that are always visible
	 * @param electricScroll The number of lines always visible from
	 * the top or bottom
	 */
	public final void setElectricScroll(int electricScroll)
	{
		this.electricScroll = electricScroll;
	}

	/**
	 * Updates the state of the scroll bars.
	 *
	 * This should be called when the number of lines in the document changes, or when the size of the text area changes.
	 */
	public void updateScrollBars()
	{
		if (!EventQueue.isDispatchThread())
		{
			LogMgr.logDebug("JEditTextArea.updateScrollbars()", "updateScrollbars() not called from within the EDT!", new Exception());
		}

		if (visibleLines > 0)
		{
      int lineCount = getLineCount();
      vertical.setValues(firstLine, visibleLines, 0, lineCount);
      vertical.setUnitIncrement(1);
      vertical.setBlockIncrement(visibleLines);

			if (visibleLines > lineCount)
			{
				setFirstLine(0);
        vertical.setVisible(false);
			}
			else
			{
        vertical.setVisible(true);
			}
		}

    int width = painter.getWidth();

		if (width > 0)
		{
      int charWidth = painter.getFontMetrics().charWidth('M');
      int maxLineLength = getDocument().getMaxLineLength();
      int maxLineWidth = (charWidth * maxLineLength) + this.painter.getGutterWidth() + charWidth;

      horizontal.setValues(-horizontalOffset, width, 0, maxLineWidth);
      horizontal.setUnitIncrement(charWidth);
      horizontal.setBlockIncrement(width / 3);

			if (maxLineWidth < width)
			{
        horizontal.setVisible(false);
			}
			else
			{
        horizontal.setVisible(true);
			}
		}
	}

  public void centerLine(final int line)
  {
    if (visibleLines <= 0)
    {
      recalculateVisibleLines();
    }

    int newFirst = line - (visibleLines / 2);
    int numLines = getLineCount();

    if (newFirst + visibleLines > numLines)
    {
      newFirst = numLines - visibleLines;
    }

    if (newFirst < 0)
    {
      newFirst = 0;
    }

    firstLine = newFirst;

    updateScrollBars();
    validate();
		painter.repaint();
  }

	/**
	 * Returns the line displayed at the text area's origin.
	 */
	public final int getFirstLine()
	{
		return (firstLine < 0 ? 0 : firstLine);
	}

	/**
	 * Sets the line displayed at the text area's origin.
	 */
	public void setFirstLine(int firstLine)
	{
		setFirstLine(firstLine, true);
	}

	protected void setFirstLine(int newFirst, boolean updateScrollbar)
	{
		if (newFirst == this.firstLine) return;
		firstLine = newFirst;

		if (updateScrollbar && firstLine != vertical.getValue())
		{
			updateScrollBars();
		}
		painter.repaint();
	}

	/**
	 * Returns the number of lines visible in this text area.
	 */
	public final int getVisibleLines()
	{
		return visibleLines;
	}

	/**
	 * Recalculates the number of visible lines. This should not
	 * be called directly.
	 */
	final void recalculateVisibleLines()
	{
		if (painter == null) return;

		int height = painter != null ? painter.getHeight() : this.getHeight();
    if (height <= 0) height = this.getHeight();
		int lineHeight = painter.getFontMetrics().getHeight();
		if (lineHeight == 0) return;

		visibleLines = height / lineHeight;
	}

	/**
	 * Returns the horizontal offset of drawn lines.
	 */
	public final int getHorizontalOffset()
	{
		return horizontalOffset;
	}

	/**
	 * Sets the horizontal offset of drawn lines. This can be used to
	 * implement horizontal scrolling.
	 * @param horizontalOffset offset The new horizontal offset
	 */
	public void setHorizontalOffset(int horizontalOffset)
	{
		setHorizontalOffset(horizontalOffset, true);
	}

	protected void setHorizontalOffset(int offset, boolean updateScrollbar)
	{
		if (offset == this.horizontalOffset) return;
		this.horizontalOffset = offset;
		if (updateScrollbar && offset != horizontal.getValue())
		{
			updateScrollBars();
		}
		painter.repaint();
	}

	@Override
	public boolean canScrollDown()
	{
		int startLine = getFirstLine();
		int lastLine = startLine + getVisibleLines();
		return (lastLine < getLineCount());
	}

	@Override
	public void scrollDown()
	{
		if (canScrollDown())
		{
			setOrigin(getFirstLine() + 1, getHorizontalOffset());
		}
	}

	@Override
	public boolean canScrollUp()
	{
		return (getFirstLine() > 0);
	}

	@Override
	public void scrollUp()
	{
		if (canScrollUp())
		{
			setOrigin(getFirstLine() - 1, getHorizontalOffset());
		}
	}

	/**
	 * A fast way of changing both the first line and horizontal
	 * offset.
	 * @param newFirst The new first line
	 * @param newLeft The new horizontal offset
	 * @return True if any of the values were changed, false otherwise
	 */
	public boolean setOrigin(int newFirst, int newLeft)
	{
		boolean changed = false;

		if (newLeft != this.horizontalOffset)
		{
			this.horizontalOffset = newLeft;
			changed = true;
		}

		if (newFirst != this.firstLine)
		{
			this.firstLine = newFirst;
			changed = true;
		}

		if (changed)
		{
			updateScrollBars();
			painter.repaint();
		}

		return changed;
	}

	/**
	 * Ensures that the caret is visible by scrolling the text area if
	 * necessary.
	 *
	 * @return True if scrolling was actually performed, false if the
	 * caret was already visible
	 */
	public boolean scrollToCaret()
	{
		int line = getCaretLine();
		int lineStart = getLineStartOffset(line);
		int offset = Math.max(0, Math.min(getLineLength(line) - 1, getCaretPosition() - lineStart));

		return scrollTo(line, offset);
	}

	/**
	 * Checks if the given line is visible and inside the "electric sroll" margin of the editor.
	 */
	public boolean isLineVisible(int line)
	{
		if (visibleLines == 0) return false;
		return firstLine + electricScroll <= line && line <= firstLine + visibleLines - electricScroll;
	}

	/**
	 * Ensures that the specified line and offset is visible by scrolling
	 * the text area if necessary.
	 *
	 * @param line The line to scroll to
	 * @param offset The offset in the line to scroll to
	 * @return True if scrolling was actually performed, false if the
	 * line and offset was already visible
	 */
	public boolean scrollTo(final int line, final int offset)
	{
		if (visibleLines == 0) return false;

		int newFirstLine = firstLine;
		int newHorizontalOffset = horizontalOffset;
		int lineCount = getLineCount();

		if (line < firstLine + electricScroll)
		{
			newFirstLine = Math.max(0, line - electricScroll);
		}
		else if (line + electricScroll >= firstLine + visibleLines)
		{
			newFirstLine = (line - visibleLines) + electricScroll + 1;

			if (newFirstLine + visibleLines >= lineCount)
			{
				newFirstLine = lineCount - visibleLines;
			}
			if (newFirstLine < 0)
			{
				newFirstLine = 0;
			}
		}

		int x = _offsetToX(line, offset);
		int width = painter.getFontMetrics().charWidth('w');
		int pwidth = painter.getWidth();

		if (x < 0)
		{
			newHorizontalOffset = Math.min(0, horizontalOffset - x + width + 5);
		}
		else if (x + width >= pwidth)
		{
			newHorizontalOffset = horizontalOffset + (pwidth - x) - width - 5;
			newHorizontalOffset -= painter.getGutterWidth();
		}

		return setOrigin(newFirstLine, newHorizontalOffset);
	}

	/**
	 * Converts a line index to a y co-ordinate.
	 * @param line The line
	 */
	public int lineToY(int line)
	{
		FontMetrics fm = painter.getFontMetrics();
		return ((line - firstLine) * fm.getHeight()) - (fm.getLeading() + fm.getDescent());
	}

	/**
	 * Converts a y co-ordinate to a line index.
	 * @param y The y co-ordinate
	 */
	public int yToLine(int y)
	{
		FontMetrics fm = painter.getFontMetrics();
		int height = fm.getHeight();
		return Math.max(0,Math.min(getLineCount() - 1, y / height + firstLine));
	}

	/**
	 * Converts an offset in a line into an x co-ordinate. This is a
	 * slow version that can be used any time.
	 * @param line The line
	 * @param offset The offset, from the start of the line
	 */
	public final int offsetToX(int line, int offset)
	{
		// don't use cached tokens
		painter.currentLineTokens = null;
		return _offsetToX(line,offset);
	}

	/**
	 * Converts an offset in a line into an x co-ordinate. This is a
	 * fast version that should only be used if no changes were made
	 * to the text since the last repaint.
	 *
	 * @param line The line
	 * @param offset The offset, from the start of the line
	 */
	public int _offsetToX(int line, int offset)
	{
		TokenMarker tokenMarker = getTokenMarker();

		getLineText(line, lineSegment);

		int segmentOffset = lineSegment.offset;
		int x = horizontalOffset;

		/* If syntax coloring is disabled, do simple translation */
		if (tokenMarker == null)
		{
			lineSegment.count = offset;
			FontMetrics fm = painter.getFontMetrics();
			return x + Utilities.getTabbedTextWidth(lineSegment, fm, x, painter, 0);
		}
		else
		{
			// If syntax coloring is enabled, we have to do this because
			// tokens can vary in width
			Token tokens = tokenMarker.markTokens(lineSegment, line);

			while (tokens != null)
			{
				FontMetrics styledMetrics = painter.getStyleFontMetrics(tokens.id);
				int length = tokens.length;

				if (offset + segmentOffset < lineSegment.offset + length)
				{
					lineSegment.count = offset - (lineSegment.offset - segmentOffset);
					return x + Utilities.getTabbedTextWidth(lineSegment, styledMetrics, x, painter, 0);
				}
				else
				{
					lineSegment.count = length;
					x += Utilities.getTabbedTextWidth(lineSegment, styledMetrics, x, painter, 0);
					lineSegment.offset += length;
				}
				tokens = tokens.next;
			}
		}
		return x;
	}

	/**
	 * Converts an x co-ordinate to an offset within a line.
	 * @param line The line
	 * @param x The x co-ordinate
	 */
	public int xToOffset(int line, int x)
	{
		TokenMarker tokenMarker = getTokenMarker();

		getLineText(line,lineSegment);

		char[] segmentArray = lineSegment.array;
		int segmentOffset = lineSegment.offset;
		int segmentCount = lineSegment.count;

		int width = horizontalOffset;

		if (tokenMarker == null)
		{
			FontMetrics fm = painter.getFontMetrics();

			for (int i = 0; i < segmentCount; i++)
			{
				char c = segmentArray[i + segmentOffset];
				int charWidth;

				if (c == '\t')
				{
					charWidth = (int) painter.nextTabStop(width,i) - width;
				}
				else
				{
					charWidth = fm.charWidth(c);
				}
				if (x - charWidth / 2 <= width)
				{
					return i;
				}
				width += charWidth;
			}

			return segmentCount;
		}
		else
		{
			Token tokens = tokenMarker.markTokens(lineSegment, line);

			int offset = 0;

			while (tokens != null)
			{
				FontMetrics styledMetrics = painter.getStyleFontMetrics(tokens.id);

				int length = tokens.length;

				for (int i = 0; i < length; i++)
				{
					char c = segmentArray[segmentOffset + offset + i];
					int charWidth = styledMetrics.charWidth(c);

					if (c == '\t')
					{
						charWidth = (int)painter.nextTabStop(width, offset + i) - width;
					}

					if (x - charWidth / 2 <= width) return offset + i;

					width += charWidth;
				}

				offset += length;
				tokens = tokens.next;
			}
			return offset;
		}

	}

	/**
	 * Converts a point to an offset, from the start of the text.
	 * @param x The x co-ordinate of the point
	 * @param y The y co-ordinate of the point
	 */
	public int xyToOffset(int x, int y)
	{
		int line = yToLine(y);
		int start = getLineStartOffset(line);
		return start + xToOffset(line,x);
	}

	/**
	 * Returns the document this text area is editing.
	 */
	public final SyntaxDocument getDocument()
	{
		return document;
	}

	/**
	 * Calls SyntaxDocument.reset() and removes the documentListener from the current document.
	 */
	protected void clearCurrentDocument()
	{
		if (this.document != null)
		{
			this.document.removeDocumentListener(documentHandler);
			this.document.reset();
		}
	}

	/**
	 * Sets the document this text area is editing.
	 * @param newDocument The document
	 */
	public final void setDocument(SyntaxDocument newDocument)
	{
		if (this.document == newDocument) return;

		clearCurrentDocument();

		document = newDocument;
    document.tokenizeLines();

		select(0,0,null);

		if (this.document != null)
		{
			painter.calculateTabSize();

			if (this.currentTokenMarker != null)
			{
				this.document.setTokenMarker(this.currentTokenMarker);
			}

			this.document.addDocumentListener(documentHandler);

			EventQueue.invokeLater(new Runnable()
			{
				@Override
				public void run()
				{
					updateScrollBars();
					validate();
					painter.repaint();
				}
			});

		}
	}

	@Override
	public void validate()
	{
		painter.invalidateLineRange(0, getLineCount());
		super.validate();
	}

	@Override
	public void setFont(Font aNewFont)
	{
		super.setFont(aNewFont);
		this.painter.setFont(aNewFont);
	}

	/**
	 * Returns the document's token marker. Equivalent to calling
	 * <code>getDocument().getTokenMarker()</code>.
	 */
	public final TokenMarker getTokenMarker()
	{
		return document.getTokenMarker();
	}

	/**
	 * Sets the document's token marker. Equivalent to caling
	 * <code>getDocument().setTokenMarker()</code>.
	 * @param tokenMarker The token marker
	 */
	public final void setTokenMarker(TokenMarker tokenMarker)
	{
		this.currentTokenMarker = tokenMarker;
		document.setTokenMarker(tokenMarker);
	}

	/**
	 * Returns the length of the document. Equivalent to calling
	 * <code>getDocument().getLength()</code>.
	 */
	public final int getDocumentLength()
	{
		if (document == null) return 0;
		return document.getLength();
	}

	/**
	 * Returns the number of lines in the document.
	 */
	public final int getLineCount()
	{
		if (document == null) return 0;
		if (document.getDefaultRootElement() == null) return 0;
		return document.getDefaultRootElement().getElementCount();
	}

	/**
	 * Returns the line containing the specified offset.
   *
	 * @param offset The offset
   * @see #getColumnOfOffset(int)
	 */
	public final int getLineOfOffset(int offset)
	{
		return document.getDefaultRootElement().getElementIndex(offset);
	}

	/**
	 * Returns the column in the line of the specified offset.
   *
	 * @param offset The offset
   * @see #getLineOfOffset(int)
	 */
	public final int getColumnOfOffset(int offset)
	{
    int line = getLineOfOffset(offset);
    int lineStart = getLineStartOffset(line);
    return offset - lineStart;
	}

	public int getCaretPositionInLine(int line)
	{
		int pos = getCaretPosition();
		int start = getLineStartOffset(line);
		return (pos - start);

	}

	/**
	 * Returns the start offset of the specified line.
	 * @param line The line
	 * @return The start offset of the specified line, or -1 if the line is
	 * invalid
	 */
	public int getLineStartOffset(int line)
	{
		Element lineElement = document.getDefaultRootElement().getElement(line);
		if (lineElement == null)
		{
			return -1;
		}
		else
		{
			return lineElement.getStartOffset();
		}
	}

	/**
	 * Returns the end offset of the specified line.
	 * @param line The line
	 * @return The end offset of the specified line, or -1 if the line is
	 * invalid.
	 */
	public int getLineEndOffset(int line)
	{
		Element lineElement = document.getDefaultRootElement().getElement(line);
		if (lineElement == null)
		{
			return -1;
		}
		else
		{
			return lineElement.getEndOffset();
		}
	}

	/**
	 * Returns the length of the specified line, without the line end terminator
	 * @param line The line
	 */
	public int getLineLength(int line)
	{
		Element lineElement = document.getDefaultRootElement().getElement(line);
		if (lineElement == null)
		{
			return -1;
		}
		else
		{
			return lineElement.getEndOffset() - lineElement.getStartOffset() - 1;
		}
	}

	/**
	 * Returns the entire text of this text area.
	 */
	public String getText()
	{
		int len = document.getLength();
		if (len < 0) return null;
		try
		{
			return document.getText(0,document.getLength());
		}
		catch (BadLocationException bl)
		{
			LogMgr.logError("JEditTextArea.getText()", "Error setting text", bl);
			return null;
		}
	}

	/**
	 *	Set the tab size to be used in characters.
	 */
	public final void setTabSize(int aSize)
	{
		document.putProperty(PlainDocument.tabSizeAttribute, Integer.valueOf(aSize));
	}


	/**
	 *	Return the current Tab size in characters
	 */
	public int getTabSize()
	{
		Integer tab = (Integer)document.getProperty(PlainDocument.tabSizeAttribute);

		if (tab != null)
		{
			return tab.intValue();
		}
		else
		{
			return 4;
		}

	}

	public void appendLine(String aLine)
	{
		if (MemoryWatcher.isMemoryLow(true))
		{
			WbManager.getInstance().showLowMemoryError();
			return;
		}

		try
		{
			document.beginCompoundEdit();
			document.insertString(document.getLength(),fixLinefeed(aLine),null);
		}
		catch (BadLocationException bl)
		{
			LogMgr.logError("JEditTextArea.appendLine()", "Error setting text", bl);
		}
		finally
		{
			document.endCompoundEdit();
		}
	}

	public void reset()
	{
		setDocument(new SyntaxDocument());
		resetModified();
	}

	/**
	 * Sets the entire text of this text area.
	 */
	public void setText(String text)
	{
		try
		{
      select(0,0,null);
			document.beginCompoundEdit();

			if (document.getLength() > 0)
			{
				document.remove(0,document.getLength());
			}
			if (text != null && text.length() > 0)
			{
				String realtext = fixLinefeed(text);
				document.insertString(0,realtext,null);
			}
		}
		catch (BadLocationException bl)
		{
			LogMgr.logError("JEditTextArea.setText()", "Error setting text", bl);
		}
		finally
		{
			document.endCompoundEdit();
			document.tokenizeLines();
		}

		if (isVisible())
		{
			EventQueue.invokeLater(new Runnable()
			{
				@Override
				public void run()
				{
					updateScrollBars();
					validate();
				}
			});
		}
	}

	public boolean isReallyVisible()
	{
		boolean result = isVisible();
		Component p = getParent();
		result = result && (p != null);
		while (p != null)
		{
			result = result && p.isVisible();
			p = p.getParent();
		}
		return result;
	}

	/**
	 * Returns the specified substring of the document.
	 * @param start The start offset
	 * @param len The length of the substring
	 * @return The substring, or null if the offsets are invalid
	 */
	public final String getText(int start, int len)
	{
		try
		{
			return document.getText(start,len);
		}
		catch (BadLocationException bl)
		{
			LogMgr.logError("JEditTextArea.getText()", "Error setting text", bl);
			return null;
		}
	}

	/**
	 * Copies the specified substring of the document into a segment.
	 * If the offsets are invalid, the segment will contain a null string.
	 * @param start The start offset
	 * @param len The length of the substring
	 * @param segment The segment
	 */
	public final void getText(int start, int len, Segment segment)
	{
		if (len < 0) return;
		try
		{
			document.getText(start, len ,segment);
		}
		catch(BadLocationException bl)
		{
			LogMgr.logError("JEditTextArea.getText()", "Error setting text", bl);
			segment.offset = 0;
			segment.count = 0;
		}
	}

	public String getWordAtCursor()
	{
		return getWordAtCursor(Settings.getInstance().getEditorNoWordSep());
	}

	public String getWordAtCursor(String wordCharacters)
	{
		int currentLine = getCaretLine();
		String line = this.getLineText(currentLine);
		int pos = this.getCaretPositionInLine(currentLine);
		int start = TextUtilities.findWordStart(line, pos, wordCharacters);
		int end = TextUtilities.findWordEnd(line, pos, wordCharacters);
		if (start < end && start >= 0)
		{
			return line.substring(start, end);
		}
		return null;
	}
	/**
	 * Returns the word that is left of the cursor.
	 * If the character left of the cursor is a whitespace
	 * this method returns null.
	 * @param wordBoundaries additional word boundary characters (whitespace is always a word boundary)
	 */
	public String getWordLeftOfCursor(String wordBoundaries)
	{
		int currentLine = getCaretLine();
		String line = this.getLineText(currentLine);
		int pos = this.getCaretPositionInLine(currentLine);
		return StringUtil.getWordLeftOfCursor(line, pos, wordBoundaries);
	}

	public void selectWordAtCursor(String wordBoundaries)
	{
		int currentLine = getCaretLine();
		String line = this.getLineText(currentLine);
		int caret = this.getCaretPosition();
		int lineStart = this.getLineStartOffset(currentLine);
		int pos = (caret - lineStart);

		if (pos <= 0) return;
		if (Character.isWhitespace(line.charAt(pos - 1))) return;
		int start = StringUtil.findWordBoundary(line, pos - 1, wordBoundaries);
		if (start == 0)
		{
			this.select(lineStart + start, caret);
		}
		else if (start > 0)
		{
			this.select(lineStart + start + 1, caret);
		}
	}
	/**
	 * Returns the text on the specified line without the line terminator
	 * @param lineIndex The line
	 * @return The text, or null if the line is invalid
	 */
	public final String getLineText(int lineIndex)
	{
		Element lineElement = document.getDefaultRootElement().getElement(lineIndex);
		int start = (lineElement != null ? lineElement.getStartOffset() : -1);
		int end = (lineElement != null ? lineElement.getEndOffset() : -1);
		return getText(start, end - start - 1);
	}

	/**
	 * Copies the text on the specified line into a segment. If the line
	 * is invalid, the segment will contain a null string.
	 * @param lineIndex The line
	 */
	public final void getLineText(int lineIndex, Segment segment)
	{
		Element lineElement = document.getDefaultRootElement().getElement(lineIndex);
		int start = (lineElement != null ? lineElement.getStartOffset() : -1);
		int end = (lineElement != null ? lineElement.getEndOffset() : -1);
		getText(start, end - start - 1,segment);
	}

	/**
	 * Returns the selection start offset.
	 */
	public final int getSelectionStart()
	{
		return selectionStart;
	}

	/**
	 * Returns the offset where the selection starts on the specified
	 * line.
	 */
	public int getSelectionStart(int line)
	{
		if (line == selectionStartLine)
		{
			return selectionStart;
		}
		else if (rectSelect)
		{
			Element map = document.getDefaultRootElement();
			int start = selectionStart - map.getElement(selectionStartLine).getStartOffset();

			Element lineElement = map.getElement(line);
			int lineStart = lineElement.getStartOffset();
			int lineEnd = lineElement.getEndOffset() - 1;
			return Math.min(lineEnd,lineStart + start);
		}
		else
		{
			return getLineStartOffset(line);
		}
	}

	/**
	 * Returns the selection start line.
	 */
	public final int getSelectionStartLine()
	{
		return selectionStartLine;
	}

	/**
	 * Sets the selection start. The new selection will be the new
	 * selection start and the old selection end.
	 * @param selectionStart The selection start
	 * @see #select(int,int)
	 */
	public final void setSelectionStart(int selectionStart)
	{
		select(selectionStart, selectionEnd);
	}

	/**
	 * Returns the selection end offset.
	 */
	public final int getSelectionEnd()
	{
		return selectionEnd;
	}

	/**
	 * Returns the offset where the selection ends on the specified
	 * line.
	 */
	public int getSelectionEnd(int line)
	{
		if (line == selectionEndLine)
		{
			return selectionEnd;
		}
		else if (rectSelect)
		{
			Element map = document.getDefaultRootElement();
			int end = selectionEnd - map.getElement(selectionEndLine).getStartOffset();

			Element lineElement = map.getElement(line);
			int lineStart = lineElement.getStartOffset();
			int lineEnd = lineElement.getEndOffset() - 1;
			return Math.min(lineEnd,lineStart + end);
		}
		else
		{
			return getLineEndOffset(line) - 1;
		}
	}

	/**
	 * Returns the selection end line.
	 */
	public final int getSelectionEndLine()
	{
		return selectionEndLine;
	}

	/**
	 * Sets the selection end. The new selection will be the old
	 * selection start and the bew selection end.
	 * @param selectionEnd The selection end
	 * @see #select(int,int)
	 */
	public final void setSelectionEnd(int selectionEnd)
	{
		select(selectionStart,selectionEnd);
	}

	/**
	 * Returns the caret position. This will either be the selection
	 * start or the selection end, depending on which direction the
	 * selection was made in.
	 */
	public final int getCaretPosition()
	{
		return (biasLeft ? selectionStart : selectionEnd);
	}

	/**
	 * Returns the caret line.
	 */
	public final int getCaretLine()
	{
		return (biasLeft ? selectionStartLine : selectionEndLine);
	}

	/**
	 * Returns the mark position. This will be the opposite selection
	 * bound to the caret position.
	 * @see #getCaretPosition()
	 */
	public final int getMarkPosition()
	{
		return (biasLeft ? selectionEnd : selectionStart);
	}

	/**
	 * Returns the mark line.
	 */
	public final int getMarkLine()
	{
		return (biasLeft ? selectionEndLine : selectionStartLine);
	}

	/**
	 * Sets the caret position. The new selection will consist of the
	 * caret position only (hence no text will be selected)
	 * @param caret The caret position
	 * @see #select(int,int)
	 */
	public final void setCaretPosition(int caret)
	{
		select(caret,caret);
	}

	/**
	 * Selects all text in the document.
	 */
	@Override
	public final void selectAll()
	{
		select(0,getDocumentLength());
	}

	/**
	 * Moves the mark to the caret position.
	 */
	public final void selectNone()
	{
		select(getCaretPosition(), getCaretPosition());
	}

	public void clearUndoBuffer()
	{
		this.document.clearUndoBuffer();
	}

	@Override
	public void undo()
	{
		this.document.undo();
		int pos = this.document.getPositionOfLastChange();
		if (pos > -1)
		{
			this.setCaretPosition(pos);
			this.scrollToCaret();
		}
	}

	@Override
	public void redo()
	{
		this.document.redo();
		int pos = this.document.getPositionOfLastChange();
		if (pos > -1)
		{
			this.setCaretPosition(pos);
			this.scrollToCaret();
		}
	}

	public boolean currentSelectionIsTemporary()
	{
		return currentSelectionIsTemporary;
	}

	public void selectError(int start, int end)
	{
		this.selectCommand(start, end, ERROR_COLOR);
	}

	public void selectStatementTemporary(int start, int end)
	{
		this.selectCommand(start, end, TEMP_COLOR);
	}

	private void selectCommand(int start, int end, Color alternateColor)
	{
		if (start >= end) return;

		int len = (end - start);
		String text = this.getText(start, len);
		if (text == null || text.length() == 0) return;

		len = text.length();
		int pos = 0;
		char c = text.charAt(pos);
		while (Character.isWhitespace(c) && pos < len)
		{
			pos ++;
			c = text.charAt(pos);
		}

		int newStart = start + pos;
		int maxIndex = len - 1;

		pos = 0;
		c = text.charAt(maxIndex - pos);
		while (Character.isWhitespace(c) && pos < maxIndex)
		{
			pos ++;
			c = text.charAt(maxIndex - pos);
		}
		int newEnd = end - pos;
		this.select(newStart, newEnd, alternateColor);
	}

	public void select(int start, int end)
	{
		select(start, end, null);
	}

	/**
	 * Selects from the start offset to the end offset. This is the
	 * general selection method used by all other selecting methods.
	 * The caret position will be start if start &lt; end, and end
	 * if end &gt; start.
	 * @param start The start offset
	 * @param end The end offset
	 */
	private void select(int start, int end, Color alternateColor)
	{
		if (document == null) return;
		if (painter == null) return;

		int newStart, newEnd;
		boolean newBias;

		if (start <= end)
		{
			newStart = start;
			newEnd = end;
			newBias = false;
		}
		else
		{
			newStart = end;
			newEnd = start;
			newBias = true;
		}

		if (newStart < 0 || newEnd > getDocumentLength())
		{
			throw new IllegalArgumentException("Bounds out of"+ " range: " + newStart + "," +	newEnd);
		}

		if (newStart != selectionStart || newEnd != selectionEnd || newBias != biasLeft)
		{
			this.alternateSelectionColor = alternateColor;
			this.currentSelectionIsTemporary = (alternateColor != null);

			int newStartLine = getLineOfOffset(newStart);
			int newEndLine = getLineOfOffset(newEnd);

			if (painter.isBracketHighlightEnabled())
			{
				if (bracketLine != -1)	painter.invalidateLine(bracketLine);
				updateBracketHighlight(end);
				if (bracketLine != -1) painter.invalidateLine(bracketLine);
			}

			painter.invalidateLineRange(selectionStartLine, selectionEndLine);
			painter.invalidateLineRange(newStartLine, newEndLine);

			selectionStart = newStart;
			selectionEnd = newEnd;
			selectionStartLine = newStartLine;
			selectionEndLine = newEndLine;
			biasLeft = newBias;

			fireCaretEvent();
		}

		updateOccuranceHilite();

		// When the user is typing, etc, we don't want the caret to blink
		blink = true;
		if (caretTimer != null) caretTimer.restart();

		// Disable rectangle select if selection start = selection end
		if (selectionStart == selectionEnd) rectSelect = false;

		// Clear the "magic" caret position used by up/down
		magicCaret = -1;

		scrollToCaret();
		fireSelectionEvent();
	}

	public boolean isGlobalSelectionHighlight()
	{
		return this.highlightSelection == null;
	}

	/**
	 * Enables/disables the highlighting of the current selection for this editor.
	 *
	 * This will overwrite the global setting
	 *
	 * @param flag
	 *
	 * @see Settings#getHighlightCurrentSelection()
	 */
	public void setHighlightSelection(boolean flag)
	{
		this.highlightSelection = Boolean.valueOf(flag);
		updateOccuranceHilite();
	}

	public boolean isSelectionHighlightEnabled()
	{
		if (this.highlightSelection == null)
		{
			return Settings.getInstance().getHighlightCurrentSelection();
		}
		return highlightSelection.booleanValue();
	}

	private void updateOccuranceHilite()
	{
		String text = null;
		boolean enableHilite = isSelectionHighlightEnabled();

		int minLength = Settings.getInstance().getMinLengthForSelectionHighlight();

		if (enableHilite && (selectionStartLine == selectionEndLine) && (selectionEnd - selectionStart) >= minLength)
		{
			text = getSelectedText();
			if (Settings.getInstance().getSelectionHighlightNoWhitespace() && !getQuoteHandler().isQuoted(text))
			{
				int pos = StringUtil.findFirstWhiteSpace(text, (char)0);
				if (pos > -1)
				{
					text = null;
				}
			}
		}
		painter.setHighlightValue(text);
	}

	protected QuoteHandler getQuoteHandler()
	{
		return QuoteHandler.STANDARD_HANDLER;
	}

	public Color getAlternateSelectionColor()
	{
		return this.alternateSelectionColor;
	}

	/**
	 * Returns the number of selected characters.
	 *
	 * This is not accurate if a rectangular selection is active!
	 *
	 */
	public int getSelectionLength()
	{
		return selectionStart - selectionEnd;
	}

	/**
	 * Returns the selected text, or null if no selection is active.
	 */
	public final String getSelectedText()
	{
		if (selectionStart == selectionEnd) return null;

		if (rectSelect)
		{
			// Return each row of the selection on a new line
			Element map = document.getDefaultRootElement();

			int start = selectionStart - map.getElement(selectionStartLine).getStartOffset();
			int end = selectionEnd - map.getElement(selectionEndLine).getStartOffset();

			// Certain rectangles satisfy this condition...
			if(end < start)
			{
				int tmp = end;
				end = start;
				start = tmp;
			}

			StringBuilder buf = new StringBuilder();
			Segment seg = new Segment();

			for(int i = selectionStartLine; i <= selectionEndLine; i++)
			{
				Element lineElement = map.getElement(i);
				int lineStart = lineElement.getStartOffset();
				int lineEnd = lineElement.getEndOffset() - 1;

				lineStart = Math.min(lineStart + start, lineEnd);
				int lineLen = Math.min(end - start, lineEnd - lineStart);

				getText(lineStart,lineLen,seg);
				buf.append(seg.array,seg.offset,seg.count);

				if (i != selectionEndLine) buf.append('\n');
			}
			return buf.toString();
		}
		else
		{
			return getText(selectionStart,selectionEnd - selectionStart);
		}

	}

	public boolean isEmptyRectangleSelection()
	{
		if (!rectSelect) return false;

		Element map = document.getDefaultRootElement();
		int start = selectionStart - map.getElement(selectionStartLine).getStartOffset();
		int end = selectionEnd - map.getElement(selectionEndLine).getStartOffset();

		// Certain rectangles satisfy this condition...
		if (end < start)
		{
			int tmp = end;
			end = start;
			start = tmp;
		}

		Element lineElement = map.getElement(selectionStartLine);
		int lineStart = lineElement.getStartOffset();
		int lineEnd = lineElement.getEndOffset() - 1;
		int rectStart = Math.min(lineEnd, lineStart + start);
		int caret = getCaretPositionInLine(getCaretLine());
		return rectStart == caret;
	}

	public void doRectangleDeleteChar()
	{
		selectionStart ++;
		setSelectedText("");
	}

	public void doRectangleBackspace()
	{
		selectionStart --;
		setSelectedText("");
	}

	public void replaceText(int line, int start, int end, String replacement)
	{
		if (!editable) return;

		try
		{
			document.beginCompoundEdit();
			int lineStart = getLineStartOffset(line);
			document.remove(lineStart + start, (end - start));
			document.insertString(lineStart + start, replacement, null);
			int newCaret = lineStart + start + replacement.length();
			int newEndline = getLineOfOffset(newCaret);
			painter.invalidateLineRange(line, newEndline);
			setCaretPosition(newCaret);
		}
		catch (BadLocationException bl)
		{
			LogMgr.logError("JEditTextArea.setSelectedText()", "Error setting text", bl);
			throw new InternalError("Cannot replace selection");
		}
		finally
		{
			document.endCompoundEdit();
		}
	}

  public void replaceText(int startPos, int endPos, String newText)
  {
		try
		{
			document.beginCompoundEdit();
      document.remove(startPos, (endPos - startPos));
      document.insertString(startPos, fixLinefeed(newText), null);
    }
		catch (BadLocationException bl)
		{
			LogMgr.logError("JEditTextArea.setSelectedText()", "Error setting text", bl);
			throw new InternalError("Cannot replace selection");
		}
		finally
		{
			document.endCompoundEdit();
		}
		updateScrollBars();
    setCaretPosition(startPos);
		int startLine = getLineOfOffset(selectionStart);
		int endLine = getLineOfOffset(selectionStart + newText.length());
		painter.invalidateLineRange(startLine, endLine);
  }

	/**
	 * Replaces the selection with the specified text.
	 * @param selectedText The replacement text for the selection
	 */
	public void setSelectedText(String selectedText)
	{
		if (!editable) return;

		try
		{
			selectedText = fixLinefeed(selectedText);
			document.beginCompoundEdit();

			if (rectSelect)
			{
				Element map = document.getDefaultRootElement();

				int start = selectionStart - map.getElement(selectionStartLine).getStartOffset();
				int end = selectionEnd - map.getElement(selectionEndLine).getStartOffset();

				// Certain rectangles satisfy this condition...
				if (end < start)
				{
					int tmp = end;
					end = start;
					start = tmp;
				}

				// document.remove might change these lines (due to events)
				int startLine = selectionStartLine;
				int endLine = selectionEndLine;

				String[] selectedLines = selectedText.split("\n");
				int lineNum = 0;

				for (int i = startLine; i <= endLine; i++)
				{
					Element lineElement = map.getElement(i);
					int lineStart = lineElement.getStartOffset();
					int lineEnd = lineElement.getEndOffset() - 1;
					int rectStart = Math.min(lineEnd, lineStart + start);

					document.remove(rectStart, Math.min(lineEnd - rectStart, end - start));

					// if the selected text is not empty, it needs to overwrite the currently selected text
					if (StringUtil.isNonEmpty(selectedText))
					{
						// if the new text is only a single line (the default when e.g. typing a character)
						// then use the full text
						if (selectedLines.length == 1)
						{
							document.insertString(rectStart, selectedText, null);
						}
						else if (lineNum < selectedLines.length)
						{
							// if mutiple lines are passed, only use those that have a match
							document.insertString(rectStart, selectedLines[lineNum], null);
						}
					}
					lineNum ++;
				}
			}
			else
			{
				document.remove(selectionStart, selectionEnd - selectionStart);

				if(selectedText != null)
				{
					document.insertString(selectionStart, selectedText, null);
				}
				if (this.autoIndent)
				{
					int c = this.getCaretLine();
					if (c > 0 && selectedText.equals("\n"))
					{
						String s = this.getLineText(c - 1);
						String p = StringUtil.getStartingWhiteSpace(s);
						if (p!= null && p.length() > 0)
						{
							document.insertString(selectionEnd, p, null);
						}
					}
				}
			}
		}
		catch (BadLocationException bl)
		{
			LogMgr.logError("JEditTextArea.setSelectedText()", "Error setting text", bl);
			throw new InternalError("Cannot replace selection");
		}
		finally
		{
			document.endCompoundEdit();
		}

		updateScrollBars();
		if (rectSelect)
		{
			if (StringUtil.isNonEmpty(selectedText))
			{
				selectionStart ++;
				if (overwrite)
				{
					selectionEnd ++;
				}
				else
				{
					if (selectionStart > selectionEnd) selectionEnd ++;
				}
			}
		}
		else
		{
			setCaretPosition(selectionEnd);
		}
		int startLine = getLineOfOffset(selectionStart);
		int endLine = getLineOfOffset(selectionStart + selectedText.length());
		painter.invalidateLineRange(startLine, endLine);
	}

	public void insertText(String text)
	{
		insertText(getCaretPosition(), text);
	}

	public void insertText(int position, String text)
	{
		if (MemoryWatcher.isMemoryLow(true))
		{
			WbManager.getInstance().showLowMemoryError();
			return;
		}

		try
		{
			document.beginCompoundEdit();
			document.insertString(position, fixLinefeed(text), null);
		}
		catch (Exception e)
		{
			LogMgr.logError("JEditTextArea.insertText()", "Error setting text", e);
		}
		finally
		{
			document.endCompoundEdit();
		}
	}

	public void setAutoIndent(boolean aFlag)
	{
		this.autoIndent = aFlag;
	}

	public boolean getAutoIndent()
	{
		return this.autoIndent;
	}

	/**
	 * Returns true if this text area is editable, false otherwise.
	 */
	public final boolean isEditable()
	{
		return editable;
	}

	@Override
	public void setEnabled(boolean flag)
	{
		super.setEnabled(flag);
		inputHandler.setEnabled(flag);
	}

	/**
	 * Sets if this component is editable.
	 * @param flag True if this text area should be editable,
	 * false otherwise
	 */
	public void setEditable(boolean flag)
	{
		if (editable == flag) return; // no change, do nothing

		this.editable = flag;

		if (this.popup != null)
		{
			this.popup.getCutAction().setEnabled(flag);
			this.popup.getClearAction().setEnabled(flag);
			this.popup.getPasteAction().setEnabled(flag);
		}
	}

	/**
	 * Returns the right click popup menu.
	 */
	public final JPopupMenu getRightClickPopup()
	{
		return popup;
	}

	/**
	 * Sets the right click popup menu.
	 * @param popup The popup
	 */
	public final void setRightClickPopup(TextPopup popup)
	{
		this.popup = popup;
	}

	/**
	 * Returns the `magic' caret position. This can be used to preserve
	 * the column position when moving up and down lines.
	 */
	public final int getMagicCaretPosition()
	{
		return magicCaret;
	}

	/**
	 * Sets the 'magic' caret position. This can be used to preserve
	 * the column position when moving up and down lines.
	 * @param magicCaret The magic caret position
	 */
	public final void setMagicCaretPosition(int magicCaret)
	{
		this.magicCaret = magicCaret;
	}

	/**
	 * Similar to <code>setSelectedText()</code>, but overstrikes the
	 * appropriate number of characters if overwrite mode is enabled.
	 * @param str The string
	 * @see #setSelectedText(String)
	 * @see #isOverwriteEnabled()
	 */
	public void overwriteSetSelectedText(String str)
	{
		// Don't overstrike if there is a selection
		if (!overwrite || selectionStart != selectionEnd)
		{
			setSelectedText(str);
			return;
		}

		// Don't overstrike if we're on the end of the line
		int caret = getCaretPosition();
		int caretLineEnd = getLineEndOffset(getCaretLine());
		if (caretLineEnd - caret <= str.length())
		{
			setSelectedText(str);
			return;
		}

		document.beginCompoundEdit();

		try
		{
			str = fixLinefeed(str);
			document.remove(caret,str.length());
			document.insertString(caret,str,null);
		}
		catch (BadLocationException bl)
		{
			LogMgr.logError("JEditTextArea.overwriteSelectedText()", "Error setting text", bl);
		}
		finally
		{
			document.endCompoundEdit();
		}
		updateScrollBars();
	}

	/**
	 * Returns true if overwrite mode is enabled, false otherwise.
	 */
	public final boolean isOverwriteEnabled()
	{
		return overwrite;
	}

	/**
	 * Sets if overwrite mode should be enabled.
	 * @param overwrite True if overwrite mode should be enabled,
	 * false otherwise.
	 */
	public final void setOverwriteEnabled(boolean overwrite)
	{
		this.overwrite = overwrite;
		painter.invalidateSelectedLines();
	}

	/**
	 * Returns true if the selection is rectangular, false otherwise.
	 */
	public final boolean isSelectionRectangular()
	{
		return rectSelect;
	}

	/**
	 * Sets if the selection should be rectangular.
	 * @param rectSelect True if the selection should be rectangular,
	 * false otherwise.
	 */
	public final void setSelectionRectangular(boolean rectSelect)
	{
		this.rectSelect = rectSelect;
		painter.invalidateSelectedLines();
	}

	/**
	 * Returns the position of the highlighted bracket (the bracket
	 * matching the one before the caret)
	 */
	public final int getBracketPosition()
	{
		return bracketPosition;
	}

	/**
	 * Returns the line of the highlighted bracket (the bracket
	 * matching the one before the caret)
	 */
	public final int getBracketLine()
	{
		return bracketLine;
	}

	public final void addSelectionListener(TextSelectionListener l)
	{
		listeners.add(TextSelectionListener.class, l);
	}

	public final void removeSelectionListener(TextSelectionListener l)
	{
		listeners.remove(TextSelectionListener.class, l);
	}

	public final void addTextChangeListener(TextChangeListener l)
	{
		listeners.add(TextChangeListener.class, l);
	}

	public final void removeTextChangeListener(TextChangeListener l)
	{
		listeners.remove(TextChangeListener.class, l);
	}

	/**
	 * Deletes the selected text from the text area and places it
	 * into the clipboard.
	 */
	@Override
	public void cut()
	{
		if (editable)
		{
			copy();
			setSelectedText("");
		}
	}

	/**
	 *	Deletes the selected text from the text area
	 **/
	@Override
	public void clear()
	{
		if (editable)
		{
			setSelectedText("");
		}
	}

	/**
	 * Places the selected text into the clipboard.
	 */
	@Override
	public void copy()
	{
		if (selectionStart != selectionEnd)
		{
			Clipboard clipboard = getToolkit().getSystemClipboard();

			String selection = getSelectedText();

			clipboard.setContents(new StringSelection(selection), null);
		}
	}

	/**
	 * Inserts the clipboard contents into the text.
	 */
	@Override
	public void paste()
	{
		if (editable)
		{
			if (MemoryWatcher.isMemoryLow(true))
			{
				WbManager.getInstance().showLowMemoryError();
				return;
			}

			try
			{
        Clipboard clipboard = getToolkit().getSystemClipboard();
				Transferable content = clipboard.getContents(this);
        LogMgr.logTrace("JEditTextArea.paste()", "Received flavors: " + WbSwingUtilities.getFlavors(content));
        Object data = content.getTransferData(DataFlavor.stringFlavor);
        if (data != null)
        {
          setSelectedText(data.toString());
        }
			}
			catch(Throwable th)
			{
				LogMgr.logError("JEditTextArea.paste()", "Could not get string data from clipboard", th);
			}
		}
	}

	public void setKeyEventInterceptor(KeyListener c)
	{
		this.keyEventInterceptor = c;
	}

	public void removeKeyEventInterceptor()
	{
		this.keyEventInterceptor = null;
	}

	private void forwardKeyEvent(KeyListener l, KeyEvent evt)
	{
		if (l == null) return;

		switch (evt.getID())
		{
			case KeyEvent.KEY_TYPED:
				l.keyTyped(evt);
				break;
			case KeyEvent.KEY_PRESSED:
				l.keyPressed(evt);
				break;
			case KeyEvent.KEY_RELEASED:
				l.keyReleased(evt);
			break;
		}
	}

	/**
	 * Forwards key events directly to the input handler.
	 * This is slightly faster than using a KeyListener
	 * because some Swing overhead is avoided.
	 */
	@Override
	public void processKeyEvent(KeyEvent evt)
	{
		if (evt.isConsumed()) return;

		if (inputHandler == null) return;

		if (keyEventInterceptor != null)
		{
			forwardKeyEvent(keyEventInterceptor, evt);
			return;
		}

		int oldcount = NumberStringCache.getNumberString(this.getLineCount()).length();
		switch (evt.getID())
		{
			case KeyEvent.KEY_TYPED:
				inputHandler.keyTyped(evt);
				break;
			case KeyEvent.KEY_PRESSED:
				inputHandler.keyPressed(evt);
				break;
			case KeyEvent.KEY_RELEASED:
				inputHandler.keyReleased(evt);
				break;
		}

		if (keyNotificationListener != null)
		{
			forwardKeyEvent(keyNotificationListener, evt);
		}

		if (!evt.isConsumed())
		{
			super.processKeyEvent(evt);
		}

		int newcount = NumberStringCache.getNumberString(this.getLineCount()).length();
		boolean changed = false;

		if (this.getFirstLine() < 0)
		{
			updateScrollBars();
			changed = true;
		}

		changed = changed || (oldcount != newcount);

		if (changed)
		{
			this.invalidate();
			this.repaint();
		}
	}

	protected void fireTextStatusChanged(boolean isModified)
	{
		Object[] list = listeners.getListenerList();
		for (int i = list.length - 2; i >= 0; i--)
		{
			if(list[i] == TextChangeListener.class)
			{
				((TextChangeListener)list[i+1]).textStatusChanged(isModified);
			}
		}
	}

	protected void fireSelectionEvent()
	{
		Object[] list = listeners.getListenerList();
		for (int i = list.length - 2; i >= 0; i--)
		{
			if(list[i] == TextSelectionListener.class)
			{
				((TextSelectionListener)list[i+1]).selectionChanged(this.getSelectionStart(), this.getSelectionEnd());
			}
		}
	}

	public void setStatusBar(EditorStatusbar bar)
	{
		this.statusBar = bar;
		updateStatusBar();
	}

	private void updateStatusBar()
	{
		if (this.statusBar != null)
		{
			int line = this.getCaretLine();
			this.statusBar.setEditorLocation(line + 1, this.getCaretPositionInLine(line) + 1);
		}
	}

	protected void fireCaretEvent()
	{
		Object[] list = listeners.getListenerList();
		for(int i = list.length - 2; i >= 0; i--)
		{
			if (list[i] == CaretListener.class)
			{
				((CaretListener)list[i+1]).caretUpdate(caretEvent);
			}
		}
		updateStatusBar();
	}

	public void invalidateBracketLine()
	{
		updateBracketHighlight(getCaretPosition());
	}

	protected void updateBracketHighlight(int newCaretPosition)
	{
		if (newCaretPosition == 0 || !Settings.getInstance().isBracketHighlightEnabled())
		{
			bracketPosition = -1;
			bracketLine = -1;
			return;
		}

		try
		{
			boolean matchBefore = Settings.getInstance().getBracketHighlightLeft();
			int charOffset = matchBefore ? -1 : 0;
			int offset = TextUtilities.findMatchingBracket(document, newCaretPosition + charOffset);
			if (offset != -1)
			{
				bracketLine = getLineOfOffset(offset);
				bracketPosition = offset - getLineStartOffset(bracketLine);
				return;
			}
		}
		catch (BadLocationException bl)
		{
			LogMgr.logError("JEditTextArea.updateBracketHighlight()", "Error setting text", bl);
		}

		bracketLine = -1;
		bracketPosition = -1;
	}

	protected void documentChanged(DocumentEvent evt)
	{
		DocumentEvent.ElementChange ch = evt.getChange(document.getDefaultRootElement());

		int count;
		if (ch == null)
		{
			count = 0;
		}
		else
		{
			count = ch.getChildrenAdded().length - ch.getChildrenRemoved().length;
		}

		int line = getLineOfOffset(evt.getOffset());
		invalidateLines(line);

		if (count == 0)
		{
			painter.invalidateLine(line);
		}
		else
		{
			painter.invalidateLineRange(line,(firstLine < 0 ? 0 : firstLine) + visibleLines);
		}

		boolean wasModified = isModified();
		setModified();

		// only fire event if modified status is changed
		if (!wasModified)
		{
			this.fireTextStatusChanged(true);
		}
		updateScrollBars();
	}

	private void invalidateLines(int changedLine)
	{
		TokenMarker marker = getTokenMarker();
		if (marker == null) return;

		// In order to display multi-line literals correctly
		// I simply invalidate some line above and below the
		// currently changed line. This still can leave
		// incorrect tokens with regards to multiline literals
		// but my assumptioin is, that literals spanning more than
		// 'invalidationInterval * 2' number of lines are used very rarely in SQL scripts.

		// Testing for possible literals in those lines and then only
		// invalidating the lines that need it, is probably
		// not much faster then simply invalidating a constant range of lines
		int startInvalid = changedLine - this.invalidationInterval;
		int endInvalid = changedLine + this.invalidationInterval;

		if (startInvalid < 0) startInvalid = 0;
		if (endInvalid > marker.getLineCount()) endInvalid = marker.getLineCount() - 1;

		// re-tokenize all lines
		document.tokenizeLines(startInvalid, endInvalid);

		// re-paint the lines that need it
		int repaintStart = (startInvalid < getFirstLine() ? getFirstLine() : startInvalid);
		int repaintEnd = (endInvalid > (repaintStart + getVisibleLines()) ? repaintStart + getVisibleLines() : endInvalid);
		painter.invalidateLineRange(repaintStart, repaintEnd);
	}

	public void showContextMenu()
	{
		Point p = getCursorLocation();
		popup.show(painter, p.x, p.y);
	}

	public long getLastModifiedTime()
	{
		return lastModified;
	}

	public boolean isModifiedAfter(long timeInMillis)
	{
		return (lastModified > 0) && (lastModified > timeInMillis);
	}

	public void setModified()
	{
		lastModified = System.currentTimeMillis();
	}

	public boolean isModified()
	{
		return lastModified > 0;
	}

	public void resetModified()
	{
		boolean wasModified = isModified();
		lastModified = 0;
		if (wasModified)
		{
			this.fireTextStatusChanged(false);
		}
	}

	/**
	 * Invoked when the mouse wheel is rotated.
	 * @see MouseWheelEvent
	 */
	@Override
	public void mouseWheelMoved(MouseWheelEvent e)
	{
		if (e.getScrollType() != MouseWheelEvent.WHEEL_UNIT_SCROLL) return;

		// Do not scroll if the Ctrl-Key is pressed
		// because that combination is handled by the font zoomer
		if (WbAction.isCtrlPressed(e.getModifiers())) return;

		int units = GuiSettings.getWheelScrollLines();

		// The user configured a value that is different from the OS settings
		if (units > 0)
		{
			if (e.getUnitsToScroll() < 0)
			{
				units = -units; // need to scroll up
			}
		}
		else
		{
			units = e.getUnitsToScroll();
		}
		vertical.setValue(vertical.getValue() + units);
	}

	class MutableCaretEvent extends CaretEvent
	{
		MutableCaretEvent()
		{
			super(JEditTextArea.this);
		}

		@Override
		public int getDot()
		{
			return getCaretPosition();
		}

		@Override
		public int getMark()
		{
			return getMarkPosition();
		}
	}

	class AdjustHandler implements AdjustmentListener
	{
		@Override
		public void adjustmentValueChanged(final AdjustmentEvent evt)
		{
			if (!scrollBarsInitialized)
			{
				return;
			}

			if (evt.getAdjustable() == vertical)
			{
				setFirstLine(vertical.getValue(), false);
			}
			else if (evt.getAdjustable() == horizontal)
			{
				setHorizontalOffset(-horizontal.getValue(), false);
			}
		}
	}

	class ComponentHandler extends ComponentAdapter
	{
		@Override
		public void componentResized(ComponentEvent evt)
		{
      if (evt.getID() == ComponentEvent.COMPONENT_RESIZED)
      {
        recalculateVisibleLines();
        updateScrollBars();
        scrollBarsInitialized = true;
      }
		}
	}

	class DocumentHandler implements DocumentListener
	{
		@Override
		public void insertUpdate(DocumentEvent evt)
		{
			documentChanged(evt);

			int offset = evt.getOffset();
			int length = evt.getLength();

			int newStart;
			int newEnd;

			if (selectionStart > offset || (selectionStart == selectionEnd && selectionStart == offset))
			{
				newStart = selectionStart + length;
			}
			else
			{
				newStart = selectionStart;
			}

			if (selectionEnd >= offset)
			{
				newEnd = selectionEnd + length;
			}
			else
			{
				newEnd = selectionEnd;
			}

			select(newStart,newEnd);
		}

		@Override
		public void removeUpdate(DocumentEvent evt)
		{
			documentChanged(evt);

			int offset = evt.getOffset();
			int length = evt.getLength();

			int newStart;
			int newEnd;

			if (selectionStart > offset)
			{
				if (selectionStart > offset + length)
				{
					newStart = selectionStart - length;
				}
				else
				{
					newStart = offset;
				}
			}
			else
			{
				newStart = selectionStart;
			}

			if (selectionEnd > offset)
			{
				if (selectionEnd > offset + length)
				{
					newEnd = selectionEnd - length;
				}
				else
				{
					newEnd = offset;
				}
			}
			else
			{
				newEnd = selectionEnd;
			}
			select(newStart,newEnd);
		}

		@Override
		public void changedUpdate(DocumentEvent evt)
		{
		}
	}

	class DragHandler implements MouseMotionListener
	{
		@Override
		public void mouseDragged(MouseEvent evt)
		{
			if (popup != null && popup.isVisible()) return;

			setSelectionRectangular((evt.getModifiers()	& Settings.getInstance().getRectSelectionModifier()) != 0);

			int x = evt.getX() - painter.getGutterWidth();
			int y = evt.getY();
			select(getMarkPosition(),xyToOffset(x,y));
		}

		@Override
		public void mouseMoved(MouseEvent evt) {}
	}

	class MouseHandler extends MouseAdapter
	{
		@Override
		public void mousePressed(MouseEvent evt)
		{
			requestFocus();

			// Focus events not fired sometimes?
			setCaretVisible(true);

			int x = evt.getX() - painter.getGutterWidth();
			int line = yToLine(evt.getY());
			int offset = xToOffset(line,x);
			int dot = getLineStartOffset(line) + offset;


			if ((evt.getModifiers() & InputEvent.BUTTON3_MASK) != 0 && popup != null)
			{
				if (rightClickMovesCursor && !isTextSelected())
				{
					setCaretPosition(dot);
				}
				popup.show(painter,x,evt.getY());
				return;
			}

			switch(evt.getClickCount())
			{
				case 1:
					doSingleClick(evt,line,offset,dot);
					break;
				case 2:
					// It uses the bracket matching stuff, so
					// it can throw a BLE
					try
					{
						doDoubleClick(evt,line,offset,dot);
					}
					catch(BadLocationException bl)
					{
						LogMgr.logError("MouseHandler.mousePressed()", "Error setting text", bl);
					}
					break;
				case 3:
					doTripleClick(evt,line,offset,dot);
					break;
			}
		}

		protected void doSingleClick(MouseEvent evt, int line,int offset, int dot)
		{
			if ((evt.getModifiers() & InputEvent.SHIFT_MASK) != 0)
			{
				rectSelect = (evt.getModifiers() & Settings.getInstance().getRectSelectionModifier()) != 0;
				select(getMarkPosition(),dot);
			}
			else
			{
				setCaretPosition(dot);
			}
		}

		protected void doDoubleClick(MouseEvent evt, int line, int offset, int dot)
			throws BadLocationException
		{
			// Ignore empty lines
			if (getLineLength(line) == 0) return;

			try
			{
				int bracket = TextUtilities.findMatchingBracket(document,Math.max(0,dot - 1));
				if(bracket != -1)
				{
					int mark = getMarkPosition();
					// Hack
					if(bracket > mark)
					{
						bracket++;
						mark--;
					}
					select(mark,bracket);
					return;
				}
			}
			catch(BadLocationException bl)
			{
				LogMgr.logError("JEditTextArea.doDoubleClick()", "Error", bl);
			}

			// Ok, it's not a bracket... select the word
			String lineText = getLineText(line);
			char ch = lineText.charAt(Math.max(0,offset - 1));

			String noWordSep = Settings.getInstance().getEditorNoWordSep();
			if (noWordSep == null)	noWordSep = "";

			// If the user clicked on a non-letter char,
			// we select the surrounding non-letters
			boolean selectNoLetter = (!Character.isLetterOrDigit(ch) && noWordSep.indexOf(ch) == -1);

			int wordStart = 0;

			for(int i = offset - 1; i >= 0; i--)
			{
				ch = lineText.charAt(i);
				if(selectNoLetter ^ (!Character.isLetterOrDigit(ch) && noWordSep.indexOf(ch) == -1))
				{
					wordStart = i + 1;
					break;
				}
			}

			int wordEnd = lineText.length();
			for(int i = offset; i < lineText.length(); i++)
			{
				ch = lineText.charAt(i);
				if(selectNoLetter ^ (!Character.isLetterOrDigit(ch) && noWordSep.indexOf(ch) == -1))
				{
					wordEnd = i;
					break;
				}
			}

			int lineStart = getLineStartOffset(line);
			select(lineStart + wordStart,lineStart + wordEnd);
		}

		protected void doTripleClick(MouseEvent evt, int line,int offset, int dot)
		{
			select(getLineStartOffset(line),getLineEndOffset(line)-1);
		}
	}

	public boolean isRightClickMovesCursor()
	{
		return rightClickMovesCursor;
	}

	public void setRightClickMovesCursor(boolean rightClickMovesCursor)
	{
		this.rightClickMovesCursor = rightClickMovesCursor;
	}
}