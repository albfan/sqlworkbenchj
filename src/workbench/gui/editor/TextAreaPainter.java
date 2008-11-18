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
import java.awt.Rectangle;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import javax.swing.JComponent;
import javax.swing.text.PlainDocument;
import javax.swing.text.Segment;
import javax.swing.text.TabExpander;
import javax.swing.text.Utilities;
import workbench.resource.Settings;
import workbench.util.NumberStringCache;
import workbench.util.StringUtil;

/**
 * The text area repaint manager. It performs double buffering and paints
 * lines of text.
 * @author Slava Pestov
 */
public class TextAreaPainter
	extends JComponent
	implements TabExpander, PropertyChangeListener
{
	int currentLineIndex;
	Token currentLineTokens;
	Segment currentLine;

	// protected members
	protected JEditTextArea textArea;
	protected SyntaxStyle[] styles;
	protected Color caretColor;
	protected Color selectionColor;
	protected Color bracketHighlightColor;
	protected Color errorColor;
	protected Color currentLineColor;

	protected boolean bracketHighlight;

	protected int tabSize = -1;
	protected FontMetrics fm;

	protected boolean showLineNumbers = false;
	protected int gutterWidth = 0;
	protected int gutterCharWidth = 0;

	protected static final int GUTTER_MARGIN = 2;
	private static final Color GUTTER_BACKGROUND = new Color(238,240,238);
	private static final Color GUTTER_COLOR = Color.DARK_GRAY;

	public TextAreaPainter(JEditTextArea textArea)
	{
		super();
		this.textArea = textArea;

		setDoubleBuffered(true);
		setOpaque(true);

		currentLine = new Segment();
		currentLineIndex = -1;

		setCursor(Cursor.getPredefinedCursor(Cursor.TEXT_CURSOR));
		setFont(Settings.getInstance().getEditorFont());
		setForeground(Color.BLACK);
		setBackground(Color.WHITE);

		caretColor = Color.BLACK;
		errorColor = Settings.getInstance().getEditorErrorColor();
		selectionColor = Settings.getInstance().getEditorSelectionColor();
		currentLineColor = Settings.getInstance().getEditorCurrentLineColor();
		bracketHighlightColor = Color.BLACK;
		bracketHighlight = true;
		showLineNumbers = Settings.getInstance().getShowLineNumbers();
		Settings.getInstance().addPropertyChangeListener(this,
			Settings.PROPERTY_EDITOR_TAB_WIDTH,
			Settings.PROPERTY_EDITOR_CURRENT_LINE_COLOR,
			Settings.PROPERTY_SHOW_LINE_NUMBERS);
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
		else if (Settings.PROPERTY_EDITOR_CURRENT_LINE_COLOR.equals(evt.getPropertyName()))
		{
			this.currentLineColor = Settings.getInstance().getEditorCurrentLineColor();
			invalidate();
		}
		else if (Settings.PROPERTY_SHOW_LINE_NUMBERS.equals(evt.getPropertyName()))
		{
			this.showLineNumbers = Settings.getInstance().getShowLineNumbers();
			invalidate();
		}
	}

//	public final boolean getShowLineNumbers()
//	{
//		return this.showLineNumbers;
//	}

	/**
	 * Returns if this component can be traversed by pressing the
	 * Tab key. This returns false.
	 */
	@SuppressWarnings("deprecation")
	public final boolean isManagingFocus()
	{
		return false;
	}

	/**
	 * Returns the syntax styles used to paint colorized text. Entry <i>n</i>
	 * will be used to paint tokens with id = <i>n</i>.
	 * @see Token
	 */
	public final SyntaxStyle[] getStyles()
	{
		return styles;
	}

	/**
	 * Sets the syntax styles used to paint colorized text. Entry <i>n</i>
	 * will be used to paint tokens with id = <i>n</i>.
	 * @param styles The syntax styles
	 * @see Token
	 */
	public final void setStyles(SyntaxStyle[] styles)
	{
		this.styles = styles;
		repaint();
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
	 * Returns the bracket highlight color.
	 */
	public final Color getBracketHighlightColor()
	{
		return bracketHighlightColor;
	}

	/**
	 * Sets the bracket highlight color.
	 * @param bracketHighlightColor The bracket highlight color
	 */
	public final void setBracketHighlightColor(Color bracketHighlightColor)
	{
		this.bracketHighlightColor = bracketHighlightColor;
		invalidateLine(textArea.getBracketLine());
	}

	/**
	 * Returns true if bracket highlighting is enabled, false otherwise.
	 * When bracket highlighting is enabled, the bracket matching the
	 * one before the caret (if any) is highlighted.
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
		this.fm = getFontMetrics(font);
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
		FontMetrics cfm = this.getFontMetrics();
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

		int cw = getWidth() - gutterWidth;
		int ch = getHeight();

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


		final int lastLine = textArea.getLineCount();
		final int visibleCount = textArea.getVisibleLines();
		final int firstVisible = textArea.getFirstLine();

		int fheight = fm.getHeight();
		int firstInvalid = firstVisible + clipRect.y / fheight;
		int lastInvalid = firstVisible + ((clipRect.y + clipRect.height) / fheight);
		if (lastInvalid > lastLine) lastInvalid = lastLine;

		try
		{
			TokenMarker tokenMarker = textArea.getDocument().getTokenMarker();
			int x = textArea.getHorizontalOffset();

			int endLine = firstVisible + visibleCount + 1;
			if (endLine > lastLine) endLine = lastLine;

			int gutterX = this.gutterWidth - GUTTER_MARGIN;

			final int caretLine = textArea.getCaretLine();
			final int fmHeight = fm.getLeading() + fm.getMaxDescent();

			for (int line = firstVisible; line <= endLine; line++)
			{
				int y = textArea.lineToY(line);

				if (this.showLineNumbers)
				{
					// It seems that the Objects created by Integer.toString()
					// that are passed to drawString() are not garbage collected
					// correctly (as seen in the profiler). So each time
					// the editor gets redrawn a small amount of memory is lost
					// To workaround this, I'm caching (some of) the values
					// that are needed here.
					String s = NumberStringCache.getNumberString(line);

					// As we are only allowing fixed-width fonts, this should be ok
					// otherwise fm.stringWidth(str) needs to be used
					int w = s.length() * this.gutterCharWidth;

					// make sure the line numbers do not show up outside the gutter
					gfx.setClip(0, 0, gutterWidth, ch);

					gfx.setColor(GUTTER_COLOR);
					gfx.drawString(s, gutterX - w, y);
				}

				if (line >= firstInvalid && line <= lastInvalid)
				{
					if (this.showLineNumbers)
					{
						gfx.setClip(this.gutterWidth, 0, cw, ch);
						gfx.translate(this.gutterWidth,0);
					}

					if (line == caretLine && this.currentLineColor != null)
					{
						gfx.setColor(currentLineColor);
						gfx.fillRect(0, y + fmHeight, cw, fheight);
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
			System.err.println("Error repainting line range {" + firstInvalid + "," + lastInvalid + "}:" + e.getMessage());
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
		repaint(0,textArea.lineToY(firstLine) + fm.getMaxDescent() + fm.getLeading(),getWidth(),(lastLine - firstLine + 1) * fm.getHeight());
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

		currentLineIndex = line;

		if(tokenMarker == null)
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
		gfx.setColor(bracketHighlightColor);
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
			int caretX = textArea._offsetToX(line,offset);// + this.gutterWidth;
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