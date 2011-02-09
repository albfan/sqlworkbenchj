/*
 * TextAreaPainter.java - Paints the text area
 * Copyright (C) 1999 Slava Pestov
 *
 * You may use and modify this package for any purpose. Redistribution is
 * permitted, in both source and binary form, provided that this notice
 * remains intact in all source distributions of this package.
 */
package workbench.gui.editor;

import java.awt.Color;
import java.awt.Cursor;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.Toolkit;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import java.util.Map;
import java.util.Set;
import javax.swing.JComponent;
import javax.swing.text.PlainDocument;
import javax.swing.text.Segment;
import javax.swing.text.TabExpander;
import javax.swing.text.Utilities;
import workbench.gui.WbSwingUtilities;
import workbench.log.LogMgr;
import workbench.resource.Settings;
import workbench.util.CollectionUtil;
import workbench.util.NumberStringCache;
import workbench.util.StringUtil;

/**
 * The text area repaint manager. It performs double buffering and paints
 * lines of text.
 *
 * @author Slava Pestov (Initial development)
 * @author Thomas Kellerer (bugfixes and enhancements)
 */
public class TextAreaPainter
	extends JComponent
	implements TabExpander, PropertyChangeListener
{
	Token currentLineTokens;
	private Segment currentLine;

	// protected members
	protected JEditTextArea textArea;
	protected SyntaxStyle[] styles;
	protected Color caretColor;
	protected Color selectionColor;
	protected Color currentLineColor;

	protected boolean bracketHighlight;

	protected int tabSize = -1;
	protected FontMetrics fm;

	protected boolean showLineNumbers = false;
	protected int gutterWidth = 0;
	protected int gutterCharWidth = 0;

	protected static final int GUTTER_MARGIN = 2;
	public static final Color GUTTER_BACKGROUND = new Color(238,240,238);
	public static final Color GUTTER_COLOR = Color.DARK_GRAY;

	private final Object stylesLockMonitor = new Object();
	
	private static final Set<String> COLOR_PROPS = CollectionUtil.treeSet(
		Settings.PROPERTY_EDITOR_FG_COLOR,
		Settings.PROPERTY_EDITOR_BG_COLOR,
		Settings.PROPERTY_EDITOR_CURSOR_COLOR,
		Settings.PROPERTY_EDITOR_CURRENT_LINE_COLOR);

	private Map renderingHints;

	public TextAreaPainter(JEditTextArea textArea)
	{
		super();
		this.textArea = textArea;

		setDoubleBuffered(true);
		setOpaque(true);

		currentLine = new Segment();

		setCursor(Cursor.getPredefinedCursor(Cursor.TEXT_CURSOR));
		super.setFont(Settings.getInstance().getEditorFont());

		setForeground(Settings.getInstance().getEditorTextColor());
		setBackground(Settings.getInstance().getEditorBackgroundColor());

		caretColor = Settings.getInstance().getEditorCursorColor();
		selectionColor = Settings.getInstance().getEditorSelectionColor();
		currentLineColor = Settings.getInstance().getEditorCurrentLineColor();

		bracketHighlight = true;
		showLineNumbers = Settings.getInstance().getShowLineNumbers();

		Settings.getInstance().addPropertyChangeListener(this,
			Settings.PROPERTY_EDITOR_TAB_WIDTH,
			Settings.PROPERTY_EDITOR_FG_COLOR,
			Settings.PROPERTY_EDITOR_BG_COLOR,
			Settings.PROPERTY_EDITOR_CURSOR_COLOR,
			Settings.PROPERTY_EDITOR_DATATYPE_COLOR,
			Settings.PROPERTY_EDITOR_CURRENT_LINE_COLOR,
			"workbench.editor.color.comment1",
			"workbench.editor.color.comment2",
			"workbench.editor.color.keyword1",
			"workbench.editor.color.keyword2",
			"workbench.editor.color.keyword3",
			"workbench.editor.color.literal1",
			"workbench.editor.color.literal2",
			"workbench.editor.color.operator",
			"workbench.editor.color.invalid",
			Settings.PROPERTY_SHOW_LINE_NUMBERS);

		Toolkit tk = Toolkit.getDefaultToolkit();
		renderingHints = (Map) tk.getDesktopProperty("awt.font.desktophints");
	}

	public void dispose()
	{
		Settings.getInstance().removePropertyChangeListener(this);
	}

	public void propertyChange(PropertyChangeEvent evt)
	{
		if (Settings.PROPERTY_EDITOR_TAB_WIDTH.equals(evt.getPropertyName()))
		{
			this.calculateTabSize();
		}
		else if (Settings.PROPERTY_SHOW_LINE_NUMBERS.equals(evt.getPropertyName()))
		{
			this.showLineNumbers = Settings.getInstance().getShowLineNumbers();
			invalidate();
		}
		else if (COLOR_PROPS.contains(evt.getPropertyName()))
		{
			setColors();
			invalidate();
			WbSwingUtilities.repaintLater(this);
		}
	}

	private void setColors()
	{
		WbSwingUtilities.invoke(new Runnable()
		{
			public void run()
			{
				setForeground(Settings.getInstance().getEditorTextColor());
				setBackground(Settings.getInstance().getEditorBackgroundColor());
				setCaretColor(Settings.getInstance().getEditorCursorColor());
				setStyles(SyntaxUtilities.getDefaultSyntaxStyles());
				currentLineColor = Settings.getInstance().getEditorCurrentLineColor();
			}
		});
	}

	/**
	 * Returns if this component can be traversed by pressing the
	 * Tab key. This returns false.
	 */
	@SuppressWarnings("deprecation")
	public boolean isManagingFocus()
	{
		return false;
	}

	public FontMetrics getStyleFontMetrics(byte tokenId)
	{
		if (tokenId == Token.NULL || styles == null || tokenId < 0 || tokenId >= styles.length)
		{
			return getFontMetrics();
		}
		else
		{
			return styles[tokenId].getFontMetrics(getFont(), this);
		}
	}

	/**
	 * Returns the syntax styles used to paint colorized text. Entry <i>n</i>
	 * will be used to paint tokens with id = <i>n</i>.
	 * @see Token
	 */
	public SyntaxStyle[] getStyles()
	{
		synchronized (stylesLockMonitor)
		{
			return styles;
		}
	}

	/**
	 * Sets the syntax styles used to paint colorized text. Entry <i>n</i>
	 * will be used to paint tokens with id = <i>n</i>.
	 * @param styles The syntax styles
	 * @see Token
	 */
	public void setStyles(SyntaxStyle[] styles)
	{
		synchronized (stylesLockMonitor)
		{
			this.styles = styles;
		}
		repaint();
	}

	@Override
	public void invalidate()
	{
		super.invalidate();
		invalidateLineRange(0, textArea.getLineCount());
	}

	@Override
	public void validate()
	{
		super.validate();
		invalidateLineRange(0, textArea.getLineCount());
	}

	/**
	 * Returns the caret color.
	 */
	public final Color getCaretColor()
	{
		return caretColor;
	}

	/**
	 * Sets the caret color.
	 * @param caretColor The caret color
	 */
	public final void setCaretColor(Color caretColor)
	{
		this.caretColor = caretColor;
		invalidateSelectedLines();
	}

	/**
	 * Returns the selection color.
	 */
	public final Color getSelectionColor()
	{
		return selectionColor;
	}

	/**
	 * Sets the selection color.
	 * @param selectionColor The selection color
	 */
	public final void setSelectionColor(Color selectionColor)
	{
		this.selectionColor = selectionColor;
		invalidateSelectedLines();
	}

	/**
	 * Returns true if bracket highlighting is enabled, false otherwise.
	 * When bracket highlighting is enabled, the bracket matching the
	 * one before the caret (if any) is highlighted using the caret color
	 */
	public final boolean isBracketHighlightEnabled()
	{
		return bracketHighlight;
	}

	/**
	 * Enables or disables bracket highlighting.
	 * When bracket highlighting is enabled, the bracket matching the
	 * one before the caret (if any) is highlighted.
	 * @param bracketHighlight True if bracket highlighting should be
	 * enabled, false otherwise
	 */
	public final void setBracketHighlightEnabled(boolean bracketHighlight)
	{
		this.bracketHighlight = bracketHighlight;
		invalidateLine(textArea.getBracketLine());
	}

	/**
	 * Returns the font metrics used by this component.
	 */
	public FontMetrics getFontMetrics()
	{
		if (fm == null)
		{
			this.fm = getFontMetrics(getFont());
		}
		return fm;
	}

	/**
	 * Sets the font for this component. This is overridden to update the
	 * cached font metrics and to recalculate which lines are visible.
	 * @param font The font
	 */
	public void setFont(Font font)
	{
		super.setFont(font);
		currentLineTokens = null;
		this.fm = getFontMetrics(font);
		synchronized (stylesLockMonitor)
		{
			if (styles != null)
			{
				for (SyntaxStyle style : styles)
				{
					if (style != null)
					{
						style.clearFontCache();
					}
				}
			}
		}
		calculateTabSize();
		calculateGutterWidth();
	}

	private void calculateGutterWidth()
	{
		FontMetrics cfm = getFontMetrics();
		if (cfm == null)
		{
			this.gutterCharWidth = 18;
		}
		else
		{
			this.gutterCharWidth = cfm.charWidth('9');
		}
		if (this.showLineNumbers)
		{
			int lastLine = textArea.getLineCount();
			int chars = StringUtil.numDigits(lastLine);
			this.gutterWidth = (chars * gutterCharWidth) + (GUTTER_MARGIN * 2);
		}
		else
		{
			this.gutterWidth = 0;
		}
	}

	public void calculateTabSize()
	{
		this.tabSize = -1;
		if (this.textArea == null) return;
		if (this.textArea.getDocument() == null) return;
		FontMetrics cfm = getFontMetrics();
		if (cfm == null) return;

		Object tab = textArea.getDocument().getProperty(PlainDocument.tabSizeAttribute);
		int t = -1;
		if (tab == null)
		{
			t = Settings.getInstance().getEditorTabWidth();
		}
		else
		{
			Integer tsize = (Integer)tab;
			t = tsize.intValue();
		}
		this.tabSize = cfm.charWidth(' ') * t;
	}

	public void paint(Graphics gfx)
	{
		calculateGutterWidth();

		Rectangle clipRect = gfx.getClipBounds();

		int editorWidth = getWidth() - gutterWidth;
		int editorHeight = getHeight();

		if (clipRect != null)
		{
			gfx.setColor(this.getBackground());
			gfx.fillRect(clipRect.x, clipRect.y, clipRect.width, clipRect.height);

			if (this.showLineNumbers)
			{
				gfx.setColor(GUTTER_BACKGROUND);
				gfx.fillRect(clipRect.x, clipRect.y, gutterWidth - clipRect.x, clipRect.height);
			}
		}

		Graphics2D g2d = (Graphics2D) gfx;
		if (renderingHints != null)
		{
			g2d.addRenderingHints(renderingHints);
		}

		final int lastLine = textArea.getLineCount();
		final int visibleCount = textArea.getVisibleLines();
		final int firstVisible = textArea.getFirstLine();

		int fheight = fm.getHeight();
		final int fmHeight = fm.getLeading() + fm.getMaxDescent();
		int firstInvalid = firstVisible + (clipRect.y / fheight);
		if (firstInvalid > 1) firstInvalid --;

		int lastInvalid = firstVisible + ((clipRect.y + clipRect.height) / fheight);
		if (lastInvalid > lastLine) lastInvalid = lastLine;
		else lastInvalid++;

		try
		{
			TokenMarker tokenMarker = textArea.getDocument().getTokenMarker();
			int x = textArea.getHorizontalOffset();

			int endLine = firstVisible + visibleCount + 1;
			if (endLine > lastLine) endLine = lastLine;

			int gutterX = this.gutterWidth - GUTTER_MARGIN;

			final int caretLine = textArea.getCaretLine();

			for (int line = firstVisible; line <= endLine; line++)
			{
				final int y = textArea.lineToY(line);

				if (this.showLineNumbers)
				{
					// It seems that the Objects created by Integer.toString()
					// that are passed to drawString() are not garbage collected
					// correctly (as seen in the profiler). So each time
					// the editor gets redrawn a small amount of memory is lost
					// To workaround this, I'm caching (some of) the values
					// that are needed here.
					final String s = NumberStringCache.getNumberString(line);

					// As we are only allowing fixed-width fonts, this should be ok
					// otherwise fm.stringWidth(str) needs to be used
					final int w = s.length() * this.gutterCharWidth;

					// make sure the line numbers do not show up outside the gutter
					gfx.setClip(0, 0, gutterWidth, editorHeight);

					gfx.setColor(GUTTER_COLOR);
					gfx.drawString(s, gutterX - w, y);
				}

				if (line >= firstInvalid && line < lastInvalid)
				{
					if (this.showLineNumbers)
					{
						gfx.setClip(this.gutterWidth, 0, editorWidth, editorHeight);
						gfx.translate(this.gutterWidth,0);
					}

					if (line == caretLine && this.currentLineColor != null)
					{
						gfx.setColor(currentLineColor);
						gfx.fillRect(0, y + fmHeight, editorWidth, fheight);
						gfx.setColor(getBackground());
					}

					paintLine(gfx, tokenMarker, line, y, x);

					if (this.showLineNumbers)
					{
						gfx.translate(-this.gutterWidth,0);
						gfx.setClip(null);
					}
				}
			}
		}
		catch (Exception e)
		{
			LogMgr.logError("TextAreaPainter.paint()", "Error repainting line range {" + firstInvalid + "," + lastInvalid + "}", e);
		}
	}

	/**
	 * Marks a line as needing a repaint.
	 * @param line The line to invalidate
	 */
	public final void invalidateLine(int line)
	{
		repaint(0,textArea.lineToY(line) + fm.getMaxDescent() + fm.getLeading(),getWidth(),fm.getHeight());
	}

	public int getGutterWidth()
	{
		return this.gutterWidth;
	}

	/**
	 * Marks a range of lines as needing a repaint.
	 * @param firstLine The first line to invalidate
	 * @param lastLine The last line to invalidate
	 */
	public final void invalidateLineRange(int firstLine, int lastLine)
	{
		repaint(0, textArea.lineToY(firstLine) + fm.getMaxDescent() + fm.getLeading(), getWidth(), (lastLine - firstLine + 1) * fm.getHeight());
	}

	/**
	 * Repaints the lines containing the selection.
	 */
	public final void invalidateSelectedLines()
	{
		invalidateLineRange(textArea.getSelectionStartLine(),textArea.getSelectionEndLine());
	}

	/**
	 * Implementation of TabExpander interface. Returns next tab stop after
	 * a specified point.
	 * @param x The x co-ordinate
	 * @param tabOffset Ignored
	 * @return The next tab stop after <i>x</i>
	 */
	public float nextTabStop(float x, int tabOffset)
	{
		if (tabSize == -1)
		{
			this.calculateTabSize();
		}
		int offset = textArea.getHorizontalOffset();
		int ntabs = ((int)x - offset) / tabSize;
		return (ntabs + 1) * tabSize + offset;
	}

	protected void paintLine(Graphics gfx, TokenMarker tokenMarker,	int line, int y, int x)
	{
		Font defaultFont = getFont();
		Color defaultColor = getForeground();

		if (tokenMarker == null)
		{
			paintPlainLine(gfx,line,defaultFont,defaultColor,x,y);
		}
		else
		{
			paintSyntaxLine(gfx,tokenMarker,line,defaultFont,defaultColor,x,y);
		}
	}

	protected void paintPlainLine(Graphics gfx, int line, Font defaultFont, Color defaultColor, int x, int y)
	{
		textArea.getLineText(line,currentLine);

		paintHighlight(gfx,line,y);

		gfx.setFont(defaultFont);
		gfx.setColor(defaultColor);

		y += fm.getHeight();
		Utilities.drawTabbedText(currentLine,x,y,gfx,this,0);
	}

	protected void paintSyntaxLine(Graphics gfx, TokenMarker tokenMarker,
		int line, Font defaultFont, Color defaultColor, int x, int y)
	{
		textArea.getLineText(line,currentLine);

		currentLineTokens = tokenMarker.markTokens(currentLine,	line);

		paintHighlight(gfx,line,y);

		gfx.setFont(defaultFont);
		gfx.setColor(defaultColor);
		y += fm.getHeight();
		SyntaxUtilities.paintSyntaxLine(currentLine,currentLineTokens,styles,this,gfx,x,y,0);
	}

	protected void paintHighlight(Graphics gfx, int line, int y)
	{
		if (line >= textArea.getSelectionStartLine()	&& line <= textArea.getSelectionEndLine())
		{
			paintLineHighlight(gfx,line,y);
		}

		if (bracketHighlight && line == textArea.getBracketLine())
		{
			paintBracketHighlight(gfx,line,y);
		}

		if (line == textArea.getCaretLine())
		{
			paintCaret(gfx,line,y);
		}
	}

	protected void paintLineHighlight(Graphics gfx, int line, int y)
	{
		int selectionStart = textArea.getSelectionStart();
		int selectionEnd = textArea.getSelectionEnd();

		if (selectionStart == selectionEnd) return;

		int height = fm.getHeight();
		y += fm.getLeading() + fm.getMaxDescent();

		Color c = this.textArea.getAlternateSelectionColor();
		if (c != null)
		{
			gfx.setColor(c);
		}
		else
		{
			gfx.setColor(selectionColor);
		}

		int selectionStartLine = textArea.getSelectionStartLine();
		int selectionEndLine = textArea.getSelectionEndLine();
		int lineStart = textArea.getLineStartOffset(line);

		int x1, x2;
		if (textArea.isSelectionRectangular())
		{
			int lineLen = textArea.getLineLength(line);
			x1 = textArea._offsetToX(line,Math.min(lineLen,selectionStart - textArea.getLineStartOffset(selectionStartLine)));// + this.gutterWidth;
			x2 = textArea._offsetToX(line,Math.min(lineLen,selectionEnd - textArea.getLineStartOffset(selectionEndLine)));// + this.gutterWidth;
			if(x1 == x2) x2++;
		}
		else if (selectionStartLine == selectionEndLine)
		{
			x1 = textArea._offsetToX(line,selectionStart - lineStart);
			x2 = textArea._offsetToX(line,selectionEnd - lineStart);
		}
		else if (line == selectionStartLine)
		{
			x1 = textArea._offsetToX(line,selectionStart - lineStart);
			x2 = getWidth();
		}
		else if (line == selectionEndLine)
		{
			x1 = 0;
			x2 = textArea._offsetToX(line,selectionEnd - lineStart);
		}
		else
		{
			x1 = 0; //gutterWidth;
			x2 = getWidth();
		}


		// "inlined" min/max()
		gfx.fillRect(x1 > x2 ? x2 : x1,y,x1 > x2 ? (x1 - x2) : (x2 - x1),height);

	}

	protected void paintBracketHighlight(Graphics gfx, int line, int y)
	{
		int position = textArea.getBracketPosition();
		if(position == -1) return;

		y += fm.getLeading() + fm.getMaxDescent();
		int x = textArea._offsetToX(line,position);// + this.gutterWidth;
		gfx.setColor(caretColor);
		// Hack!!! Since there is no fast way to get the character
		// from the bracket matching routine, we use "(" since all
		// brackets probably have the same width anyway
		gfx.drawRect(x,y,fm.charWidth('(') - 1,	fm.getHeight() - 1);
	}

	protected void paintCaret(Graphics gfx, int line, int y)
	{
		if (textArea.isCaretVisible())
		{
			int offset = textArea.getCaretPosition() - textArea.getLineStartOffset(line);
			int caretX = textArea._offsetToX(line, offset);
			int caretWidth = (textArea.isOverwriteEnabled() ? fm.charWidth('w') : 2);
			y += fm.getLeading() + fm.getMaxDescent();
			int height = fm.getHeight();

			gfx.setColor(caretColor);

			if(textArea.isOverwriteEnabled())
			{
				gfx.fillRect(caretX,y + height - 1,	caretWidth,1);
			}
			else
			{
				gfx.drawRect(caretX,y,caretWidth - 1,height - 1);
			}
		}
	}
}