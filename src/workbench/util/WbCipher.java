/*
 * WbCipher.java
 *
 * Created on September 4, 2002, 1:38 PM
 */

package workbench.util;

/**
 * @author  workbench@kellerer.org
 */
public interface WbCipher
{
	String decryptString(String aValue);
	String encryptString(String aValue);
}
