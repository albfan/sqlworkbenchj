package workbench.print;

import java.awt.print.PageFormat;

/*
 * PrintUtil.java
 *
 * Created on May 17, 2003, 9:06 PM
 */

/**
 *
 * @author  thomas
 */
public class PrintUtil
{

	/** Creates a new instance of PrintUtil */
	public PrintUtil()
	{
	}

	public static boolean pageFormatEquals(PageFormat first, PageFormat second)
	{
		if (first.getOrientation() != second.getOrientation()) return false;
		if (first.getHeight() != second.getHeight()) return false;
		if (first.getWidth() != second.getWidth()) return false;
		if (first.getImageableX() != second.getImageableX()) return false;
		if (first.getImageableY() != second.getImageableY()) return false;
		if (first.getImageableWidth() != second.getImageableWidth()) return false;
		if (first.getImageableHeight() != second.getImageableHeight()) return false;
		return true;
	}
	
	public static double millimeterToPoints(double mm)
	{
		double inch = millimeterToInch(mm);
		return (inch * 72);
	}
	public static double pointsToMillimeter(double points)
	{
		// convert mm to inch
		double inch = points / 72;
		return inchToMillimeter(inch);
	}

	public static double millimeterToInch(double mm)
	{
		return (mm / 25.4);
	}

	public static double inchToMillimeter(double inch)
	{
		return (inch * 25.4);
	}
	
	public static void printPageFormat(String aName, PageFormat aFormat)
	{
		System.out.println(aName + ": width=" + aFormat.getWidth() + ",height=" + aFormat.getHeight() + 
		                    ",imageableX=" + aFormat.getImageableX() + ",imageableY=" + aFormat.getImageableY() + 
												",imageableWidth=" + aFormat.getImageableWidth() + ",imageableHeight=" + aFormat.getImageableHeight());
		
	}

	public static void main(String[] args)
	{
		/*
		System.out.println("72 point = " + pointsToMillimeter(72) + "mm");
		System.out.println("1 inch = " + inchToMillimeter(1) + "mm");
		System.out.println("1 mm = " + millimeterToInch(1) + "inch");
		System.out.println("25 mm = " + millimeterToPoints(25.4) + "points");
		System.out.println("20 mm = " + millimeterToPoints(20) + "points");
		*/
		System.out.println(pointsToMillimeter(612.0));
	}
}
