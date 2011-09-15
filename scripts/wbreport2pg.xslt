<?xml version="1.0" encoding="ISO-8859-1"?>
<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform">
<!--
  Convert a SQL Workbench/J schema report (http://www.sql-workbench.net)
  into a SQL script for PostgreSQL (http://www.postgresql.org)
  Author: Thomas Kellerer
-->

<xsl:output
  encoding="iso-8859-15"
  method="text"
  indent="no"
  standalone="yes"
  omit-xml-declaration="yes"
/>

  <xsl:param name="useJdbcTypes">true</xsl:param>

  <xsl:strip-space elements="*"/>
  <xsl:variable name="quote">
    <xsl:text>"</xsl:text>
  </xsl:variable>
  <xsl:variable name="newline">
    <xsl:text>&#10;</xsl:text>
  </xsl:variable>
  <xsl:variable name="backtick">
    <xsl:text>&#96;</xsl:text>
  </xsl:variable>


  <xsl:template match="/">
    <xsl:apply-templates select="/schema-report/table-def"/>
    <xsl:apply-templates select="/schema-report/view-def"/>
    <xsl:call-template name="process-fk"/>
  </xsl:template>

  <xsl:template match="table-def">

    <xsl:variable name="tablename">
      <xsl:call-template name="write-object-name">
        <xsl:with-param name="objectname" select="table-name"/>
      </xsl:call-template>
    </xsl:variable>

    <xsl:text>DROP TABLE IF EXISTS </xsl:text>
    <xsl:value-of select="$tablename"/>
    <xsl:text> CASCADE;</xsl:text>
    <xsl:value-of select="$newline"/>

    <xsl:text>CREATE TABLE </xsl:text>
    <xsl:value-of select="$tablename"/>
    <xsl:value-of select="$newline"/>
    <xsl:text>(</xsl:text>
    <xsl:value-of select="$newline"/>

    <xsl:for-each select="column-def">
      <xsl:sort select="dbms-position"/>
      <xsl:variable name="colname">
        <xsl:call-template name="write-object-name">
          <xsl:with-param name="objectname" select="column-name"/>
        </xsl:call-template>
      </xsl:variable>
      <xsl:variable name="nullable">
        <xsl:if test="nullable = 'false'">
          <xsl:text> NOT NULL</xsl:text>
        </xsl:if>
      </xsl:variable>

      <xsl:variable name="defaultvalue">
        <xsl:if test="string-length(default-value) &gt; 0">
          <xsl:text> DEFAULT </xsl:text>
          <xsl:value-of select="default-value"/>
        </xsl:if>
      </xsl:variable>

      <xsl:variable name="datatype">
        <xsl:if test="$useJdbcTypes = 'true'">
          <xsl:call-template name="write-data-type">
            <xsl:with-param name="type-id" select="java-sql-type"/>
            <xsl:with-param name="precision" select="dbms-data-size"/>
            <xsl:with-param name="scale" select="dbms-data-digits"/>
            <xsl:with-param name="dbms-type" select="dbms-data-type"/>
          </xsl:call-template>
        </xsl:if>

        <xsl:if test="$useJdbcTypes = 'false'">
          <xsl:choose>
            <xsl:when test="dbms-data-type = 'CLOB'">
              <xsl:value-of select="'text'"/>
            </xsl:when>
            <xsl:when test="dbms-data-type = 'BLOB'">
              <xsl:value-of select="'bytea'"/>
            </xsl:when>
            <xsl:when test="java-sql-type-name = 'VARCHAR'">
              <xsl:value-of select="'varchar('"/>
              <xsl:value-of select="dbms-data-size"/>
              <xsl:value-of select="')'"/>
            </xsl:when>
            <xsl:otherwise>
              <xsl:value-of select="dbms-data-type"/>
            </xsl:otherwise>
          </xsl:choose>
        </xsl:if>

      </xsl:variable>

      <xsl:text>  </xsl:text>
      <xsl:copy-of select="$colname"/>
      <xsl:text> </xsl:text>
      <xsl:value-of select="$datatype"/>
      <xsl:value-of select="$defaultvalue"/>
      <xsl:value-of select="$nullable"/>
      <xsl:if test="position() &lt; last()">
        <xsl:text>,</xsl:text>
        <xsl:value-of select="$newline"/>
      </xsl:if>
    </xsl:for-each>

    <xsl:for-each select="table-constraints/constraint-definition">
      <xsl:text>,</xsl:text>
      <xsl:value-of select="$newline"/>
      <xsl:text>  </xsl:text>
      <xsl:if test="@generated-name = 'false'">
        <xsl:text>CONSTRAINT </xsl:text>
        <xsl:call-template name="write-object-name">
          <xsl:with-param name="objectname" select="@name"/>
        </xsl:call-template>
        <xsl:text> </xsl:text>
      </xsl:if>
      <xsl:text>CHECK </xsl:text>
      <xsl:copy-of select="normalize-space(.)"/>
    </xsl:for-each>

    <xsl:value-of select="$newline"/>
    <xsl:text>);</xsl:text>
    <xsl:value-of select="$newline"/>

    <xsl:variable name="pkcount">
      <xsl:value-of select="count(column-def[primary-key='true'])"/>
    </xsl:variable>

    <xsl:if test="$pkcount &gt; 0">
      <xsl:text>ALTER TABLE </xsl:text>
      <xsl:value-of select="$tablename"/>
      <xsl:text> ADD PRIMARY KEY </xsl:text>
      <xsl:text>(</xsl:text>
      <xsl:for-each select="column-def[primary-key='true']">
        <xsl:text>  </xsl:text>
        <xsl:value-of select="column-name"/>
        <xsl:if test="position() &lt; last()">
          <xsl:text>,</xsl:text>
        </xsl:if>
      </xsl:for-each>
      <xsl:text>);</xsl:text>
      <xsl:value-of select="$newline"/>
    </xsl:if>

    <xsl:if test="string-length(table-comment) &gt; 0">
      <xsl:text>COMMENT ON TABLE </xsl:text>
      <xsl:value-of select="$tablename"/>
      <xsl:text> IS '</xsl:text>
      <xsl:value-of select="table-comment"/>
      <xsl:text>';</xsl:text>
      <xsl:value-of select="$newline"/>
    </xsl:if>

    <xsl:value-of select="$newline"/>
    <xsl:for-each select="column-def">
      <xsl:sort select="column-name"/>
      <xsl:if test="string-length(comment) &gt; 0">
        <xsl:text>COMMENT ON COLUMN </xsl:text>
        <xsl:value-of select="$tablename"/>
        <xsl:text>.</xsl:text>
        <xsl:value-of select="column-name"/>
        <xsl:text> IS '</xsl:text>
        <xsl:value-of select="comment"/>
        <xsl:text>';</xsl:text>
        <xsl:value-of select="$newline"/>
      </xsl:if>
    </xsl:for-each>

    <xsl:for-each select="index-def">
      <xsl:call-template name="create-index">
        <xsl:with-param name="tablename" select="$tablename"/>
      </xsl:call-template>
    </xsl:for-each>
    <xsl:value-of select="$newline"/>

  </xsl:template>

  <xsl:template name="create-index">
    <xsl:param name="tablename"/>
    <xsl:variable name="pk" select="primary-key"/>
    <xsl:if test="$pk = 'false'">
      <xsl:variable name="unique">
        <xsl:if test="unique='true'">UNIQUE </xsl:if>
      </xsl:variable>
      <xsl:variable name="prefix">
        <xsl:choose>
          <xsl:when test="contains(name, 'IDX')">
            <xsl:value-of select="''"/>
          </xsl:when>
          <xsl:otherwise>
            <xsl:value-of select="'IDX_'"/>
          </xsl:otherwise>
        </xsl:choose>
      </xsl:variable>
      <xsl:text>CREATE </xsl:text>
      <xsl:value-of select="$unique"/>
      <xsl:text>INDEX </xsl:text>
      <xsl:value-of select="$prefix"/>
      <xsl:value-of select="name"/>
      <xsl:text> ON </xsl:text>
      <xsl:value-of select="$tablename"/>
      <xsl:value-of select="$newline"/>
      <xsl:text>(</xsl:text>
      <xsl:value-of select="$newline"/>
      <xsl:for-each select="column-list/column">
        <xsl:text>  </xsl:text>
        <xsl:value-of select="@name"/>
        <xsl:if test="position() &lt; last()">
          <xsl:text>,</xsl:text>
        </xsl:if>
        <xsl:value-of select="$newline"/>
      </xsl:for-each>
      <xsl:text>);</xsl:text>
      <xsl:value-of select="$newline"/>
      <xsl:value-of select="$newline"/>
    </xsl:if>
  </xsl:template>

  <xsl:template name="process-fk">
    <xsl:for-each select="/schema-report/table-def">
      <xsl:variable name="table">
        <xsl:call-template name="write-object-name">
          <xsl:with-param name="objectname" select="table-name"/>
        </xsl:call-template>
      </xsl:variable>
      <xsl:if test="count(foreign-keys) &gt; 0">
        <xsl:for-each select="foreign-keys/foreign-key">
          <xsl:variable name="targetTable" select="references/table-name"/>
          <xsl:value-of select="$newline"/>
          <xsl:text>ALTER TABLE </xsl:text>
          <xsl:value-of select="$table"/>
          <xsl:value-of select="$newline"/>
          <xsl:text> ADD CONSTRAINT </xsl:text>
          <xsl:call-template name="write-object-name">
            <xsl:with-param name="objectname" select="constraint-name"/>
          </xsl:call-template>
          <xsl:value-of select="$newline"/>
          <xsl:text> FOREIGN KEY (</xsl:text>
          <xsl:for-each select="source-columns/column">
            <xsl:call-template name="write-object-name">
              <xsl:with-param name="objectname" select="."/>
            </xsl:call-template>
            <xsl:if test="position() &lt; last()">
              <xsl:text>,</xsl:text>
            </xsl:if>
          </xsl:for-each>
          <xsl:text>)</xsl:text>
          <xsl:value-of select="$newline"/>
          <xsl:text> REFERENCES </xsl:text>
          <xsl:call-template name="write-object-name">
            <xsl:with-param name="objectname" select="$targetTable"/>
          </xsl:call-template>
          <xsl:text> (</xsl:text>
          <xsl:for-each select="referenced-columns/column">
            <xsl:call-template name="write-object-name">
              <xsl:with-param name="objectname" select="."/>
            </xsl:call-template>
            <xsl:if test="position() &lt; last()">
              <xsl:text>,</xsl:text>
            </xsl:if>
          </xsl:for-each>
          <xsl:text>);</xsl:text>
          <xsl:value-of select="$newline"/>
        </xsl:for-each>
        <xsl:value-of select="$newline"/>
      </xsl:if>
    </xsl:for-each>
  </xsl:template>

  <xsl:template match="view-def">
    <xsl:value-of select="$newline"/>
    <xsl:text>CREATE OR REPLACE VIEW </xsl:text>
    <xsl:value-of select="view-name"/>
    <xsl:value-of select="$newline"/>
    <xsl:text>(</xsl:text>
    <xsl:value-of select="$newline"/>

    <xsl:for-each select="column-def">
      <xsl:sort select="dbms-position"/>

      <xsl:variable name="colname">
        <xsl:call-template name="write-object-name">
          <xsl:with-param name="objectname" select="column-name"/>
        </xsl:call-template>
      </xsl:variable>

      <xsl:text>  </xsl:text>
      <xsl:copy-of select="$colname"/>
      <xsl:if test="position() &lt; last()">
        <xsl:text>,</xsl:text>
      </xsl:if>
      <xsl:value-of select="$newline"/>
    </xsl:for-each>
    <xsl:text>)</xsl:text>
    <xsl:value-of select="$newline"/>
    <xsl:text>AS </xsl:text>
    <xsl:value-of select="$newline"/>

    <xsl:call-template name="_replace_text">
      <xsl:with-param name="text" select="view-source"/>
      <xsl:with-param name="replace" select="$backtick"/>
      <xsl:with-param name="by" select="''"/>
    </xsl:call-template>
  </xsl:template>

  <xsl:template name="_replace_text">
    <xsl:param name="text"/>
    <xsl:param name="replace"/>
    <xsl:param name="by"/>
    <xsl:choose>
      <xsl:when test="contains($text, $replace)">
        <xsl:value-of select="substring-before($text, $replace)"/>
        <xsl:copy-of select="$by"/>
        <xsl:call-template name="_replace_text">
          <xsl:with-param name="text" select="substring-after($text, $replace)"/>
          <xsl:with-param name="replace" select="$replace"/>
          <xsl:with-param name="by" select="$by"/>
        </xsl:call-template>
      </xsl:when>
      <xsl:otherwise>
        <xsl:value-of select="$text"/>
      </xsl:otherwise>
    </xsl:choose>
  </xsl:template>


  <!--
    Map jdbc data types (from java.sql.Types) to a proper data type
    using scale and precision where approriate.
  -->
  <xsl:template name="write-data-type">
    <xsl:param name="type-id"/>
    <xsl:param name="precision"/>
    <xsl:param name="scale"/>
    <xsl:param name="dbms-type"/>
    <xsl:choose>
      <xsl:when test="$type-id = 2005"> <!-- CLOB -->
        <xsl:text>text</xsl:text>
      </xsl:when>
      <xsl:when test="$type-id = 2011"> <!-- NCLOB -->
        <xsl:text>text</xsl:text>
      </xsl:when>
      <xsl:when test="$type-id = 2004"> <!-- BLOB -->
        <xsl:text>bytea</xsl:text>
      </xsl:when>
      <xsl:when test="$type-id = -3"> <!-- VARBINARY -->
        <xsl:text>bytea</xsl:text>
      </xsl:when>
      <xsl:when test="$type-id = -4"> <!-- LONGVARBINARY -->
        <xsl:text>bytea</xsl:text>
      </xsl:when>
      <xsl:when test="$type-id = -1"> <!-- LONGVARCHAR -->
        <xsl:text>text</xsl:text>
      </xsl:when>
      <xsl:when test="$type-id = 93">
        <xsl:text>timestamp</xsl:text>
      </xsl:when>
      <xsl:when test="$type-id = 92">
        <xsl:text>time</xsl:text>
      </xsl:when>
      <xsl:when test="$type-id = 91">
        <xsl:text>date</xsl:text>
      </xsl:when>
      <xsl:when test="$type-id = 1">
        <xsl:text>char(</xsl:text><xsl:value-of select="$precision"/><xsl:text>)</xsl:text>
      </xsl:when>
      <xsl:when test="$type-id = -15"> <!-- NCHAR -->
        <xsl:text>char(</xsl:text><xsl:value-of select="$precision"/><xsl:text>)</xsl:text>
      </xsl:when>
      <xsl:when test="$type-id = 4">
        <xsl:text>integer</xsl:text>
      </xsl:when>
      <xsl:when test="$type-id = -5">
        <xsl:text>bigint</xsl:text>
      </xsl:when>
      <xsl:when test="$type-id = 5">
        <xsl:text>smallint</xsl:text>
      </xsl:when>
      <xsl:when test="$type-id = -6">
        <xsl:text>smallint</xsl:text>
      </xsl:when>
      <xsl:when test="$type-id = 8">
        <xsl:text>double</xsl:text>
      </xsl:when>
      <xsl:when test="$type-id = 7">
        <xsl:text>real</xsl:text>
      </xsl:when>
      <xsl:when test="$type-id = 6">
        <xsl:text>float</xsl:text>
      </xsl:when>
      <xsl:when test="$type-id = 16">
        <xsl:text>boolean</xsl:text>
      </xsl:when>
      <xsl:when test="$type-id = -7"> <!-- BIT -->
        <xsl:text>boolean</xsl:text>
      </xsl:when>
      <xsl:when test="$type-id = 2 or $type-id = 3">
        <xsl:if test="$scale &gt; 0">
          <xsl:text>numeric(</xsl:text><xsl:value-of select="$precision"/><xsl:text>,</xsl:text><xsl:value-of select="$scale"/><xsl:text>)</xsl:text>
        </xsl:if>
        <xsl:if test="$scale = 0 or $scale = ''">
          <xsl:if test="$precision &lt; 11">
            <xsl:text>integer</xsl:text>
          </xsl:if>
          <xsl:if test="$precision &gt; 10">
            <xsl:text>bigint</xsl:text>
          </xsl:if>
        </xsl:if>
      </xsl:when>
      <xsl:when test="$type-id = 12">
        <xsl:text>varchar(</xsl:text><xsl:value-of select="$precision"/><xsl:text>)</xsl:text>
      </xsl:when>
      <xsl:when test="$type-id = -9"> <!-- NVARCHAR -->
        <xsl:text>varchar(</xsl:text><xsl:value-of select="$precision"/><xsl:text>)</xsl:text>
      </xsl:when>
      <xsl:otherwise>
          <xsl:value-of select="$dbms-type"/>
      </xsl:otherwise>
    </xsl:choose>
  </xsl:template>

  <xsl:template name="write-object-name">
    <xsl:param name="objectname"/>
    <xsl:variable name="lcletters">abcdefghijklmnopqrstuvwxyz</xsl:variable>
    <xsl:variable name="ucletters">ABCDEFGHIJKLMNOPQRSTUVWXYZ</xsl:variable>

    <xsl:variable name="lower-name">
      <xsl:value-of select="translate($objectname,$ucletters,$lcletters)"/>
    </xsl:variable>

    <xsl:variable name="clean-name">
      <xsl:call-template name="_replace_text">
        <xsl:with-param name="text" select="objectname"/>
        <xsl:with-param name="replace" select="$backtick"/>
        <xsl:with-param name="by" select="''"/>
      </xsl:call-template>
    </xsl:variable>

    <xsl:choose>
      <xsl:when test="substring($clean-name,1,1) = $quote and substring($clean-name,string-length($clean-name),1) = $quote">
        <xsl:value-of select="$clean-name"/>
      </xsl:when>
      <xsl:when test="contains($clean-name,' ')">
        <xsl:value-of select="concat($quote, $clean-name, $quote)"/>
      </xsl:when>
      <xsl:when test="$objectname != $lower-name">
        <xsl:text>"</xsl:text><xsl:value-of select="$objectname"/><xsl:text>"</xsl:text>
      </xsl:when>
      <xsl:otherwise>
        <xsl:value-of select="$clean-name"/>
      </xsl:otherwise>
    </xsl:choose>
  </xsl:template>

</xsl:stylesheet>

