package workbench.interfaces;

import java.awt.event.ItemListener;
import java.beans.PropertyChangeListener;

/**
 *
 * @author  workbench@kellerer.org
 */
public interface SimplePropertyEditor
{
	void setSourceObject(Object aSource, String aProperty);
	void applyChanges();
	boolean isChanged();
	void addPropertyChangeListener(PropertyChangeListener aListener);
	void removePropertyChangeListener(PropertyChangeListener aListener);
}
