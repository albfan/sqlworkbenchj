/*
 * WbCompoundEdit.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2011, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.gui.editor;

import java.util.ArrayList;
import java.util.List;
import javax.swing.undo.CannotRedoException;
import javax.swing.undo.CannotUndoException;
import javax.swing.undo.UndoableEdit;

/**
 *
 * @author Thomas Kellerer
 */
public class WbCompoundEdit
	implements UndoableEdit
{
	private List<UndoableEdit> edits = new ArrayList<UndoableEdit>();
	private boolean acceptNew = true;

	public int getSize()
	{
		return edits.size();
	}

	public void clear()
	{
		this.edits.clear();
	}

	public void finished()
	{
		acceptNew = false;
	}

	public UndoableEdit getLast()
	{
		if (edits.size() == 0) return null;
		return edits.get(edits.size() - 1);
	}

	public void undo()
		throws CannotUndoException
	{
		if (edits.size() == 0) return;
		for (int i=edits.size() - 1; i > -1; i--)
		{
			UndoableEdit edit = edits.get(i);
			if (edit.canUndo() && edit.isSignificant()) edit.undo();
		}
	}

	public boolean canUndo()
	{
		if (edits.size() == 0) return false;
		for (int i=0; i < edits.size(); i++)
		{
			UndoableEdit edit = edits.get(i);
			if (!edit.canUndo()) return false;
		}
		return true;
	}

	public void redo()
		throws CannotRedoException
	{
		if (edits.size() == 0) return;

		for (int i=0; i < edits.size(); i++)
		{
			UndoableEdit edit = edits.get(i);
			edit.redo();
		}
	}

	public boolean canRedo()
	{
		if (edits.size() == 0) return false;
		for (UndoableEdit edit : edits)
		{
			if (!edit.canRedo()) return false;
		}
		return true;
	}

	public void die()
	{
		for (UndoableEdit edit : edits)
		{
			edit.die();
		}
	}

	public boolean addEdit(UndoableEdit anEdit)
	{
		if (!acceptNew) return false;
		return edits.add(anEdit);
	}

	public boolean replaceEdit(UndoableEdit anEdit)
	{
		return false;
	}

	public boolean isSignificant()
	{
		for (UndoableEdit edit : edits)
		{
			if (edit.isSignificant()) return true;
		}
		return false;
	}

	public String getPresentationName()
	{
		UndoableEdit edit = getLast();
		if (edit == null) return "";
		return edit.getPresentationName();
	}

	public String getUndoPresentationName()
	{
		UndoableEdit edit = getLast();
		if (edit == null) return "";
		return edit.getUndoPresentationName();
	}

	public String getRedoPresentationName()
	{
		UndoableEdit edit = getLast();
		if (edit == null) return "";
		return edit.getRedoPresentationName();
	}

}
