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
import javax.swing.undo.UndoableEdit;
import workbench.log.LogMgr;
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
	private int compoundLevelCounter = 0;
	private WbCompoundEdit compoundEditItem = null;
	private boolean undoSuspended = false;
	private int maxLineLength = 0;
	
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
	
	public static final String DEFAULT_NO_WORD_SEP = "";
	
	protected void initDefaultProperties()
	{
		this.putProperty("noWordSep", DEFAULT_NO_WORD_SEP);
		this.putProperty("filterNewlines", Boolean.FALSE);
		this.putProperty(PlainDocument.tabSizeAttribute,new Integer(Settings.getInstance().getEditorTabWidth()));
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
		tokenMarker = tm;
		if (tm == null) return;
		tokenMarker.insertLines(0,getDefaultRootElement().getElementCount());
		tokenizeLines();
	}

	public void dispose()
	{
		this.clearUndoBuffer();
		if (this.compoundEditItem != null)
		{
			this.compoundEditItem.clear();
			this.compoundEditItem = null;
		}
		if (tokenMarker != null) tokenMarker.dispose();
		try { this.remove(0, this.getLength()); } catch (Throwable th) {}
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
		this.undoManager.discardAllEdits();
	}

	public void redo()
	{
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
		tokenizeLines(0,getDefaultRootElement().getElementCount());
	}

	/**
	 * Reparses the document, by passing the specified lines to the
	 * token marker. This should be called after a large quantity of
	 * text is first inserted.
	 * @param start The first line to parse
	 * @param len The number of lines, after the first one to parse
	 */
	public void tokenizeLines(int start, int len)
	{
		if(tokenMarker == null || !tokenMarker.supportsMultilineTokens())
			return;

		Segment lineSegment = new Segment();
		Element map = getDefaultRootElement();

		len += start;

		try
		{
			for (int i = start; i < len; i++)
			{
				Element lineElement = map.getElement(i);
				int lineStart = lineElement.getStartOffset();
				getText(lineStart,lineElement.getEndOffset() - lineStart - 1,lineSegment);
				tokenMarker.markTokens(lineSegment,i);
			}
		}
		catch(BadLocationException bl)
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
				getText(lineStart,lineElement.getEndOffset() - lineStart - 1,lineSegment);
				if (lineSegment.count > this.maxLineLength) this.maxLineLength = lineSegment.count;
			}
		}
		catch(BadLocationException bl)
		{
			// Ignore
		}
	}
	
	public int getMaxLineLength()
	{
		return this.maxLineLength;
	}
	
	public synchronized void undoableEditHappened(UndoableEditEvent e)
	{
		if (undoSuspended) return;
		
		if (this.compoundEditItem != null)
		{
			this.compoundEditItem.addEdit(e.getEdit());
		}
		else
		{
			undoManager.addEdit(e.getEdit());
		}
	}
	
	/**
	 * Starts a compound edit that can be undone in one operation.
	 */
	public synchronized void beginCompoundEdit() 
	{
		if (undoSuspended) return;
		if (compoundLevelCounter == 0) 
		{
			this.compoundEditItem = new WbCompoundEdit();
		}
		this.compoundLevelCounter ++;
	}

	/**
	 * Ends a compound edit that can be undone in one operation.
	 */
	public synchronized void endCompoundEdit() 
	{
		this.calcMaxLineLength();
		if (undoSuspended) return;
		if (compoundLevelCounter == 1)
		{
			// it's important to finish the current compound undo
			// because the UndoManager asks the last UndoableEdit whether
			// it accepts more elements. If the compound wasn't finished
			// the single item added would be added to the compound rather
			// than to the UndoManager's item list
			compoundEditItem.finished();
			
			if (compoundEditItem.getSize() == 1)
			{
				UndoableEdit single = compoundEditItem.getLast();
				undoManager.addEdit(single);
				compoundEditItem.clear();
			}
			else if (compoundEditItem.getSize() > 1)
			{
				undoManager.addEdit(compoundEditItem);
			}
			compoundEditItem = null;		
			compoundLevelCounter = 0;
		}
		else if (compoundLevelCounter == 0)
		{
			Exception e = new IllegalStateException();
			LogMgr.logError("SyntaxDocument.endCompoundEdit", "Unbalanced endCompoundEdit()", e);
		}
		else 
		{
			this.compoundLevelCounter --;
		}
	}

	public void addUndoableEdit(UndoableEdit edit)
	{
	}

	public int getPositionOfLastChange() { return lastChangePosition; }
	
	private int lastChangePosition = -1;
	
	/**
	 * We overwrite this method to update the token marker
	 * state immediately so that any event listeners get a
	 * consistent token marker.
	 */
	protected void fireInsertUpdate(DocumentEvent evt)
	{
		if (tokenMarker != null)
		{
			DocumentEvent.ElementChange ch = evt.getChange(getDefaultRootElement());
			if(ch != null)
			{
				tokenMarker.insertLines(ch.getIndex() + 1,ch.getChildrenAdded().length - ch.getChildrenRemoved().length);
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
	protected void fireRemoveUpdate(DocumentEvent evt)
	{
		if (tokenMarker != null)
		{
			DocumentEvent.ElementChange ch = evt.getChange(getDefaultRootElement());
			if(ch != null)
			{
				tokenMarker.deleteLines(ch.getIndex() + 1,ch.getChildrenRemoved().length - ch.getChildrenAdded().length);
			}
		}

		lastChangePosition = evt.getOffset();
		super.fireRemoveUpdate(evt);
	}
}
