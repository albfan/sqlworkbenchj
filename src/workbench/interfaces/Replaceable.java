/*
 * Searchable.java
 *
 * Created on August 8, 2002, 9:08 AM
 */

package workbench.interfaces;

/**
 *
 * @author  workbench@kellerer.org
 */
public interface Replaceable
	extends Searchable
{
	void replace();
	void replaceNext();
}
