package workbench.interfaces;

import java.util.EventListener;

/**
 *
 * @author  workbench@kellerer.org
 */
public interface TextSelectionListener
	extends EventListener
{
	void selectionChanged(int newStart, int newEnd);
}
