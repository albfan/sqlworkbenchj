package workbench.gui.editor;

/*
 * TextAreaPainter.java - Paints the text area
 * Copyright (C) 1999 Slava Pestov
 *
 * You may use and modify this package for any purpose. Redistribution is
 * permitted, in both source and binary form, provided that this notice
 * remains intact in all source distributions of this package.
 */

import java.awt.Color;
import java.awt.Cursor;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.event.MouseEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import javax.swing.JComponent;
import javax.swing.ToolTipManager;
import javax.swing.text.PlainDocument;
import javax.swing.text.Segment;
import javax.swing.text.TabExpander;
import javax.swing.text.Utilities;
import workbench.resource.Settings;

/**
 * The text area repaint manager. It performs double buffering and paints
 * lines of text.
 * @author Slava Pestov
 * @version $Id: TextAreaPainter.java,v 1.22 2006-08-26 14:04:12 thomas Exp $
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
		this.textArea = textArea;

		setDoubleBuffered(true);
		setOpaque(true);

		currentLine = new Segment();
		currentLineIndex = -1;

		setCursor(Cursor.getPredefinedCursor(Cursor.TEXT_CURSOR));

		setFont(new Font("Monospaced",Font.PLAIN,12));
		setForeground(Color.BLACK);
		setBackground(Color.WHITE);

		caretColor = Color.BLACK;
		errorColor = Settings.getInstance().getEditorErrorColor();
		selectionColor = Settings.getInstance().getEditorSelectionColor();
		bracketHighlightColor = Color.BLACK;
		bracketHighlight = true;
		Settings.getInstance().addPropertyChangeListener(this);
	}


	public void propertyChange(PropertyChangeEvent evt)
	{
		if (Settings.PROPERTY_EDITOR_TAB_WIDTH.equals(evt.getPropertyName()))
		{
			this.calculateTabSize();
		}
	}
	 
	/**
	 * Switches display of line numbers in the left gutter area.
	 *
	 * @param aFlag
	 */
	public final void setShowLineNumbers(boolean aFlag)
	{
		this.showLineNumbers = aFlag;
		this.invalidate();
	}

	public final boolean getShowLineNumbers() 
	{ 
		return this.showLineNumbers; 
	}

	/**
	 * Returns if this component can be traversed by pressing the
	 * Tab key. This returns false.
	 */
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
			Font f = this.getFont();
			Graphics g = this.getGraphics();
			if (g != null)
			{
				fm = g.getFontMetrics(f);
			}
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
		this.fm = this.getFontMetrics(font);
		this.gutterCharWidth = fm.charWidth('9');
		calculateTabSize();
	}

	public void calculateTabSize()
	{
		this.tabSize = -1;
		if (this.textArea == null) return;
		if (this.textArea.getDocument() == null) return;
		if (this.fm == null)
		{
			this.fm = this.getFontMetrics(getFont());
			if (this.fm == null) return;
		}
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
		this.tabSize = fm.charWidth(' ') * t;
	}
	
	public void paint(Graphics gfx)
	{
		Graphics2D gf2d = (Graphics2D)gfx;
//		gf2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_SPEED);
//		gf2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_OFF);

		int lastLine = textArea.getLineCount();
		
		int visibleCount = textArea.getVisibleLines();
		int firstVisible = textArea.getFirstLine();
		
		if (this.showLineNumbers)
		{
			String s = Integer.toString(lastLine > visibleCount ? lastLine : visibleCount);
			int	chars = s.length();
			this.gutterWidth = (chars * gutterCharWidth) + (GUTTER_MARGIN * 2);
		}
		else
		{
			this.gutterWidth = 0;
		}

		boolean paintLineNumbers = false;
		
		Rectangle clipRect = gfx.getClipBounds();

		gfx.setColor(this.getBackground());
		gfx.fillRect(clipRect.x,clipRect.y,clipRect.width,clipRect.height);
		
		if (this.showLineNumbers && clipRect.x < gutterWidth)
		{
			paintLineNumbers = true;
			gfx.setColor(GUTTER_BACKGROUND);
			gfx.fillRect(clipRect.x, clipRect.y, gutterWidth - clipRect.x, clipRect.height);
		}
		
//		System.out.println("repainting: x=" + clipRect.x + ", y=" + clipRect.y + ", w=" +  clipRect.width + ", h=" + clipRect.height);
		
		// We don't use yToLine() here because that method doesn't
		// return lines past the end of the document
		int height = fm.getHeight();
		
		int firstInvalid = firstVisible + clipRect.y / height;
		int lastInvalid = firstVisible + (clipRect.y + clipRect.height) / height;
		
		try
		{
			TokenMarker tokenMarker = textArea.getDocument().getTokenMarker();
			int x = textArea.getHorizontalOffset();

			Color f = this.getForeground();
			
			int endLine = firstVisible + visibleCount;
			if (endLine > lastLine) endLine = lastLine;
			
			int cw = clipRect.width - gutterWidth;
			int gutterX = this.gutterWidth - GUTTER_MARGIN;
			
			for (int line = firstVisible; line <= endLine; line++)
			{
				int y = textArea.lineToY(line);
				if (paintLineNumbers)
				{
					String s = Integer.toString(line);
					int w = s.length() * this.gutterCharWidth;
					gf2d.setColor(GUTTER_COLOR);
					gf2d.drawString(s, gutterX - w, y);
					
					if (line >= firstInvalid && line < lastLine) 
					{
						gfx.setClip(this.gutterWidth, clipRect.y, cw, clipRect.height);
						gfx.translate(this.gutterWidth,0);		
						gf2d.setColor(f);

						paintLine(gfx,tokenMarker,line,y,x);

						gfx.setClip(null);
						gfx.translate(-this.gutterWidth,0);				
					}
				}
				else
				{
					if (line >= firstInvalid && line < lastLine) 
					{
						paintLine(gfx,tokenMarker,line,y,x);
					}
				}				
			}

			if (tokenMarker != null && tokenMarker.isNextLineRequested())
			{
				int h = clipRect.y + clipRect.height;
				repaint(0,h,getWidth(),getHeight() - h);
			}
		}
		catch(Exception e)
		{
			System.err.println("Error repainting line range {" + firstInvalid + "," + lastInvalid + "}:" + e.getMessage());
			//e.printStackTrace();
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
		return this.gutterWidth; // + GUTTER_MARGIN;
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
		x = Utilities.drawTabbedText(currentLine,x,y,gfx,this,0);
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
		if(textArea.isSelectionRectangular())
		{
			int lineLen = textArea.getLineLength(line);
			x1 = textArea._offsetToX(line,Math.min(lineLen,selectionStart - textArea.getLineStartOffset(selectionStartLine)));
			x2 = textArea._offsetToX(line,Math.min(lineLen,selectionEnd - textArea.getLineStartOffset(selectionEndLine)));
			if(x1 == x2) x2++;
		}
		else if(selectionStartLine == selectionEndLine)
		{
			x1 = textArea._offsetToX(line,selectionStart - lineStart);
			x2 = textArea._offsetToX(line,selectionEnd - lineStart);
		}
		else if(line == selectionStartLine)
		{
			x1 = textArea._offsetToX(line,selectionStart - lineStart);
			x2 = getWidth();
		}
		else if(line == selectionEndLine)
		{
			x1 = 0;
			x2 = textArea._offsetToX(line,selectionEnd - lineStart);
		}
		else
		{
			x1 = 0;
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
		int x = textArea._offsetToX(line,position);
		gfx.setColor(bracketHighlightColor);
		// Hack!!! Since there is no fast way to get the character
		// from the bracket matching routine, we use ( since all
		// brackets probably have the same width anyway
		gfx.drawRect(x,y,fm.charWidth('(') - 1,	fm.getHeight() - 1);
	}

	protected void paintCaret(Graphics gfx, int line, int y)
	{
		if(textArea.isCaretVisible())
		{
			int offset = textArea.getCaretPosition() - textArea.getLineStartOffset(line);
			int caretX = textArea._offsetToX(line,offset);
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