package workbench.interfaces;

import java.util.EventListener;

/**
 *
 * @author  workbench@kellerer.org
 */
public interface TextChangeListener extends EventListener
{
	void textStatusChanged(boolean modified);
}
