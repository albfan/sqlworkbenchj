/*
 * WbNullCipher.java
 *
 * Created on September 4, 2002, 1:38 PM
 */

package workbench.util;

/**
 * @author  workbench@kellerer.org
 */
public class WbNullCipher
	implements WbCipher
{
	public WbNullCipher()
	{
	}
	
	public String decryptString(String aValue) { return aValue; }
	public String encryptString(String aValue) { return aValue; }
}
