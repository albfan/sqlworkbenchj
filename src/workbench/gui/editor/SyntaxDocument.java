package workbench.gui.editor;

import javax.swing.event.DocumentEvent;
import javax.swing.event.UndoableEditEvent;
import javax.swing.event.UndoableEditListener;
import javax.swing.text.AbstractDocument;
import javax.swing.text.AbstractDocument.DefaultDocumentEvent;
import javax.swing.text.BadLocationException;
import javax.swing.text.Element;
import javax.swing.text.PlainDocument;
import javax.swing.text.Segment;
import javax.swing.undo.CannotRedoException;
import javax.swing.undo.CannotUndoException;
import javax.swing.undo.UndoManager;

import workbench.resource.Settings;

/**
 * A document implementation that can be tokenized by the syntax highlighting
 * system.
 *
 * @author Slava Pestov
 */
public class SyntaxDocument
	extends PlainDocument
	implements UndoableEditListener
{
	private UndoManager undoManager = new UndoManager();
	protected TokenMarker tokenMarker;
	private WbCompoundEdit undoItem = new WbCompoundEdit();
	private boolean undoSuspended;
	private int maxLineLength;
	private int maxCompoundEditDelay = Settings.getInstance().getIntProperty("workbench.gui.editor.compoundedit.delay", 150);

	public SyntaxDocument()
	{
		super();
		this.addUndoableEditListener(this);
		this.initDefaultProperties();
	}

	public SyntaxDocument(AbstractDocument.Content aContent)
	{
		super(aContent);
		this.addUndoableEditListener(this);
		this.initDefaultProperties();
	}

	public DocumentEvent createChangedEvent()
	{
		DefaultDocumentEvent evt = new DefaultDocumentEvent(0, this.getLength(), DocumentEvent.EventType.CHANGE);
		return evt;
	}

	protected final void initDefaultProperties()
	{
		this.putProperty("filterNewlines", Boolean.FALSE);
		this.putProperty(PlainDocument.tabSizeAttribute,Integer.valueOf(Settings.getInstance().getEditorTabWidth()));
	}
	/**
	 * Returns the token marker that is to be used to split lines
	 * of this document up into tokens. May return null if this
	 * document is not to be colorized.
	 */
	public TokenMarker getTokenMarker()
	{
		return tokenMarker;
	}

	/**
	 * Sets the token marker that is to be used to split lines of
	 * this document up into tokens. May throw an exception if
	 * this is not supported for this type of document.
	 * @param tm The new token marker
	 */
	public void setTokenMarker(TokenMarker tm)
	{
		if (tokenMarker != null) tokenMarker.reset();
		tokenMarker = tm;
		if (tm == null) return;
		tokenMarker.insertLines(0,getDefaultRootElement().getElementCount());
		tokenizeLines();
	}

	public void reset()
	{
		clearUndoBuffer();

		lastChangePosition = 0;

		try
		{
			suspendUndo();
			this.remove(0, this.getLength());
		}
		catch (Throwable th)
		{
		}
		finally
		{
			resumeUndo();
		}

		if (tokenMarker != null)
		{
			tokenMarker.reset();
		}
	}

	public void suspendUndo()
	{
		this.undoSuspended = true;
	}

	public void resumeUndo()
	{
		this.undoSuspended = false;
	}

	public void clearUndoBuffer()
	{
		undoManager.discardAllEdits();
		undoItem.reset();
	}

	public void redo()
	{
		// make sure any pending edits are added to the UndoManager
		endCompoundEdit();

		if (!undoManager.canRedo()) return;

		try
		{
			undoManager.redo();
		}
		catch (CannotRedoException cre)
		{
			cre.printStackTrace();
		}
	}

	public void undo()
	{
		// make sure any pending edits are added to the UndoManager
		endCompoundEdit();

		if (!undoManager.canUndo()) return;
		try
		{
			undoManager.undo();
		}
		catch (CannotUndoException cre)
		{
			cre.printStackTrace();
		}
	}
	/**
	 * Reparses the document, by passing all lines to the token
	 * marker. This should be called after the document is first
	 * loaded.
	 */
	public void tokenizeLines()
	{
		tokenizeLines(0, getDefaultRootElement().getElementCount());
	}

	/**
	 * Reparses the document, by passing the specified lines to the
	 * token marker. This should be called after a large quantity of
	 * text is first inserted.
	 *
	 * @param start The first line to parse
	 * @param len The number of lines, after the first one to parse
	 */
	public void tokenizeLines(int start, int len)
	{
		if (tokenMarker == null) return;

		Segment lineSegment = new Segment();
		Element map = getDefaultRootElement();

		len += start;

    maxLineLength = 0;

		try
		{
			for (int i = start; i < len; i++)
			{
				Element lineElement = map.getElement(i);
				if (lineElement == null) break;
				int lineStart = lineElement.getStartOffset();
				getText(lineStart, lineElement.getEndOffset() - lineStart - 1, lineSegment);
				if (lineSegment.count > this.maxLineLength)
        {
          maxLineLength = lineSegment.count;
        }
				tokenMarker.markTokens(lineSegment, i);
			}
		}
		catch (BadLocationException bl)
		{
			bl.printStackTrace();
		}
	}

	private void calcMaxLineLength()
	{
		Segment lineSegment = new Segment();
		Element map = getDefaultRootElement();

		int len = getDefaultRootElement().getElementCount();

		this.maxLineLength = 0;

		try
		{
			for (int i = 0; i < len; i++)
			{
				Element lineElement = map.getElement(i);
				int lineStart = lineElement.getStartOffset();
				getText(lineStart, lineElement.getEndOffset() - lineStart - 1, lineSegment);
				if (lineSegment.count > this.maxLineLength)
        {
          this.maxLineLength = lineSegment.count;
        }
			}
		}
		catch (BadLocationException bl)
		{
			// Ignore
		}
	}

	public int getMaxLineLength()
	{
    if (maxLineLength <= 0 && getLength() > 0)
    {
      calcMaxLineLength();
    }
		return this.maxLineLength;
	}

	@Override
	public synchronized void undoableEditHappened(UndoableEditEvent e)
	{
		if (undoSuspended) return;
		undoItem.addEdit(e.getEdit());
	}

	/**
	 * Starts a compound edit that can be undone in one operation.
	 */
	public synchronized void beginCompoundEdit()
	{
		if (undoSuspended) return;

		long duration = undoItem.getDurationSinceLastEdit();
		if (duration > maxCompoundEditDelay && undoItem.getSize() > 0)
		{
			// store the collected edits
			endCompoundEdit();
		}
	}

	/**
	 * Ends a compound edit that can be undone in one operation.
	 */
	public synchronized void endCompoundEdit()
	{
		if (undoSuspended) return;

		long duration = undoItem.getDurationSinceLastEdit();

		if (duration > maxCompoundEditDelay && undoItem.getSize() > 0)
		{
			undoItem.finished();
			undoManager.addEdit(undoItem);
			undoItem = new WbCompoundEdit();
		}
	}

	public int getPositionOfLastChange()
	{
		return lastChangePosition;
	}

	private int lastChangePosition = -1;

	/**
	 * We overwrite this method to update the token marker
	 * state immediately so that any event listeners get a
	 * consistent token marker.
	 */
	@Override
	protected void fireInsertUpdate(DocumentEvent evt)
	{
		if (tokenMarker != null)
		{
			DocumentEvent.ElementChange ch = evt.getChange(getDefaultRootElement());
			if(ch != null)
			{
				int index = ch.getIndex() + 1;
				int lines = ch.getChildrenAdded().length - ch.getChildrenRemoved().length;
				tokenMarker.insertLines(index, lines);
			}
		}
		lastChangePosition = evt.getOffset();
		super.fireInsertUpdate(evt);
	}

	/**
	 * We overwrite this method to update the token marker
	 * state immediately so that any event listeners get a
	 * consistent token marker.
	 */
	@Override
	protected void fireRemoveUpdate(DocumentEvent evt)
	{
		if (tokenMarker != null)
		{
			DocumentEvent.ElementChange ch = evt.getChange(getDefaultRootElement());
			if(ch != null)
			{
				tokenMarker.deleteLines(ch.getIndex() + 1, ch.getChildrenRemoved().length - ch.getChildrenAdded().length);
			}
		}
		lastChangePosition = evt.getOffset();
		super.fireRemoveUpdate(evt);
	}
}
