package workbench.interfaces;


/**
 *
 * @author  workbench@kellerer.org
 */
public interface TextFileContainer
{
	boolean saveFile();
	boolean saveCurrentFile();
	boolean openFile();
	boolean closeFile(boolean clearText);
}
