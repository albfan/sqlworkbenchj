/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package workbench.db.importer.detector;

import java.io.File;
import java.util.List;

import org.junit.Test;
import static org.junit.Assert.*;

import workbench.TestUtil;
import workbench.WbTestCase;
import workbench.db.ColumnIdentifier;
import workbench.db.importer.ExcelReaderTest;

/**
 *
 * @author Thomas Kellerer
 */
public class SpreadSheetTableDetectorTest
  extends WbTestCase
{

  public SpreadSheetTableDetectorTest()
  {
    super("SpreadSheetTableDetectorTest");
  }

  @Test
  public void testAnalyzeFile()
    throws Exception
  {
    TestUtil util = getTestUtil();
		File input = util.copyResourceFile(ExcelReaderTest.class, "data.xls");

    SpreadSheetTableDetector detector = new SpreadSheetTableDetector(input, true, 0);
    detector.setSampleSize(100);
    detector.analyzeFile();
    List<ColumnIdentifier> columns = detector.getDBColumns();
    assertNotNull(columns);
    assertEquals(6,columns.size());
    String sql = detector.getCreateTable(null, "person");
    System.out.println(sql);
    String expected =
      "CREATE TABLE person\n" +
      "(\n" +
      "  id           DECIMAL(3),\n" +
      "  firstname    VARCHAR(32767),\n" +
      "  lastname     VARCHAR(32767),\n" +
      "  hiredate     TIMESTAMP,\n" +
      "  salary       DECIMAL(7),\n" +
      "  last_login   TIMESTAMP\n" +
      ")";
    assertEquals(expected.toLowerCase(), sql.trim().toLowerCase());
    assertTrue(input.delete());
  }

}
