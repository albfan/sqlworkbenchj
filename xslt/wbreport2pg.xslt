<?xml version="1.0" encoding="ISO-8859-1"?>
<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform" xmlns:wb="workbench.sql.NameUtil">
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

<!--
  Parameters can be overriden by using -xsltParameters
  For example: -xsltParameters="useJdbcTypes=true" sets param useJdbcTypes to true.
-->
  <xsl:param name="useJdbcTypes">false</xsl:param>
  <xsl:param name="makeLowerCase">true</xsl:param>
  <!-- Should column names be quoted (surrounded by variable quote)? -->
  <xsl:param name="quoteColumnName">true</xsl:param>
  <xsl:param name="commitAfterEachTable">true</xsl:param>

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
    <xsl:message>
Supported parameters:

* useJdbcTypes (current value: <xsl:value-of select="$useJdbcTypes"/>)
* makeLowerCase (current value: <xsl:value-of select="$makeLowerCase"/>)
* quoteColumnName (current value: <xsl:value-of select="$quoteColumnName"/>)
* commitAfterEachTable (current value: <xsl:value-of select="$commitAfterEachTable"/>)
    </xsl:message>

    <xsl:apply-templates select="/schema-report/sequence-def">
      <xsl:with-param name="definition-part" select="'create'"/>
    </xsl:apply-templates>
    <xsl:apply-templates select="/schema-report/table-def"/>
    <xsl:apply-templates select="/schema-report/sequence-def">
      <xsl:with-param name="definition-part" select="'owner'"/>
    </xsl:apply-templates>
    <xsl:apply-templates select="/schema-report/view-def"/>
    <xsl:call-template name="process-fk"/>
    <xsl:value-of select="$newline"/>
    <xsl:text>COMMIT;</xsl:text>
  </xsl:template>

  <xsl:template match="table-def">

    <!-- plain XSLT solution to cleanup the identifier name -->
    <xsl:variable name="tablename">
      <xsl:call-template name="write-object-name">
        <xsl:with-param name="objectname" select="table-name"/>
      </xsl:call-template>
    </xsl:variable>

    <!-- alternatively: use the Workbench utility class:
         Using this, the XSLT can only be executed from within SQL Workbench
    <xsl:variable name="tablename" select="wb:cleanupIdentifier(table-name, 'true')"/>
    <xsl:variable name="tablename" select="wb:cleanupIdentifier(table-name, $makeLowerCase)"/>
    -->


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
      <xsl:sort select="dbms-position" data-type="number"/>
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
      <xsl:if test="$quoteColumnName = 'true'"><xsl:value-of select="$quote" /></xsl:if>
 	  <xsl:copy-of select="$colname"/>
 	  <xsl:if test="$quoteColumnName = 'true'"><xsl:value-of select="$quote" /></xsl:if>

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
        <xsl:call-template name="write-object-name">
          <xsl:with-param name="objectname" select="column-name"/>
        </xsl:call-template>
        <xsl:if test="position() &lt; last()">
          <xsl:text>, </xsl:text>
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
      <xsl:sort select="column-name" data-type="number"/>
      <xsl:variable name="colname">
        <xsl:call-template name="write-object-name">
          <xsl:with-param name="objectname" select="column-name"/>
        </xsl:call-template>
      </xsl:variable>
      <xsl:if test="string-length(comment) &gt; 0">
        <xsl:text>COMMENT ON COLUMN </xsl:text>
        <xsl:value-of select="$tablename"/>
        <xsl:text>.</xsl:text>
        <xsl:value-of select="$colname"/>
        <xsl:text> IS '</xsl:text>
        <xsl:value-of select="comment"/>
        <xsl:text>';</xsl:text>
        <xsl:value-of select="$newline"/>
      </xsl:if>
    </xsl:for-each>

    <xsl:for-each select="index-def">
      <xsl:value-of select="$newline"/>
      <xsl:call-template name="create-index">
        <xsl:with-param name="tablename" select="$tablename"/>
      </xsl:call-template>
    </xsl:for-each>
    <xsl:value-of select="$newline"/>

    <xsl:if test="$commitAfterEachTable = 'true'">
    	<xsl:text>COMMIT;</xsl:text>
    	<xsl:value-of select="$newline"/>
    </xsl:if>
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
        <xsl:variable name="colname">
          <xsl:call-template name="write-object-name">
            <xsl:with-param name="objectname" select="@name"/>
          </xsl:call-template>
        </xsl:variable>
        <xsl:text>  </xsl:text>
        <xsl:value-of select="$colname"/>
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

  <xsl:template match="sequence-def">
    <xsl:param name="definition-part" select="'create'"/>
    <xsl:variable name="max-value" select="sequence-properties/property[@name='MAX_VALUE']/@value"/>
    <xsl:variable name="owned-by" select="sequence-properties/property[@name='OWNED_BY']/@value"/>
    <xsl:variable name="owner-table" select="owned-by-table"/>
    <xsl:variable name="owner-column" select="owned-by-column"/>

    <xsl:variable name="col-type" select="/schema-report/table-def[@name=$owner-table]/column-def[@name=$owner-column]/dbms-data-type"/>

    <xsl:if test="$col-type != 'serial' and $col-type != 'bigserial' and $definition-part = 'create'">
      <xsl:text>CREATE SEQUENCE </xsl:text>
      <xsl:call-template name="write-object-name">
        <xsl:with-param name="objectname" select="sequence-name"/>
      </xsl:call-template>
      <xsl:value-of select="$newline"/>
      <xsl:for-each select="sequence-properties/property">
      </xsl:for-each>
      <xsl:text>   INCREMENT BY </xsl:text><xsl:value-of select="sequence-properties/property[@name='INCREMENT']/@value"/>
      <xsl:if test="sequence-properties/property[@name='CACHE']/@value != '1'">
        <xsl:value-of select="$newline"/>
        <xsl:text>   CACHE </xsl:text><xsl:value-of select="sequence-properties/property[@name='CACHE']/@value"/>
      </xsl:if>
      <xsl:if test="sequence-properties/property[@name='CYCLE']/@value = 'true'">
        <xsl:value-of select="$newline"/>
        <xsl:text>   CYCLE</xsl:text>
      </xsl:if>
      <xsl:if test="sequence-properties/property[@name='MINVALUE']/@value != '1'">
        <xsl:value-of select="$newline"/>
        <xsl:text>   MINVALUE </xsl:text><xsl:value-of select="sequence-properties/property[@name='MINVALUE']/@value"/>
      </xsl:if>
      <xsl:if test="string-length($max-value) &gt; 0 and $max-value != '9223372036854775807'">
        <xsl:value-of select="$newline"/>
        <xsl:text>   MAXVALUE </xsl:text><xsl:value-of select="$max-value"/>
      </xsl:if>
      <xsl:text>;</xsl:text>
      <xsl:value-of select="$newline"/>
      <xsl:value-of select="$newline"/>
    </xsl:if>
    <xsl:if test="$col-type != 'serial' and $col-type != 'bigserial' and string-length($owned-by) &gt; 0 and $definition-part = 'owner'">
      <xsl:text>ALTER SEQUENCE </xsl:text>
      <xsl:call-template name="write-object-name">
        <xsl:with-param name="objectname" select="sequence-name"/>
      </xsl:call-template>
      <xsl:value-of select="$newline"/>
      <xsl:text>   OWNED BY </xsl:text><xsl:value-of select="$owned-by"/>
      <xsl:text>;</xsl:text>
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
          <xsl:text>  ADD CONSTRAINT </xsl:text>
          <xsl:call-template name="write-object-name">
            <xsl:with-param name="objectname" select="constraint-name"/>
          </xsl:call-template>
          <xsl:value-of select="$newline"/>
          <xsl:text>  FOREIGN KEY (</xsl:text>
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
          <xsl:text>  REFERENCES </xsl:text>
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
          <xsl:text>)</xsl:text>
          <xsl:call-template name="define-fk-actions"/>
          <xsl:call-template name="add-defer-rule"/>
          <xsl:text>;</xsl:text>
          <xsl:value-of select="$newline"/>
        </xsl:for-each>
        <xsl:value-of select="$newline"/>
      </xsl:if>
    </xsl:for-each>
  </xsl:template>

  <xsl:template name="define-fk-actions">
    <xsl:call-template name="add-fk-action">
      <xsl:with-param name="event-name" select="'ON DELETE'"/>
      <xsl:with-param name="action" select="delete-rule"/>
    </xsl:call-template>
    <xsl:call-template name="add-fk-action">
      <xsl:with-param name="event-name" select="'ON UPDATE'"/>
      <xsl:with-param name="action" select="update-rule"/>
    </xsl:call-template>
  </xsl:template>

  <xsl:template name="add-fk-action">
    <xsl:param name="event-name"/>
    <xsl:param name="action"/>
    <xsl:if test="$action != 'NO ACTION'">
      <xsl:value-of select="$newline"/>
      <xsl:text>  </xsl:text>
      <xsl:value-of select="$event-name"/><xsl:text> </xsl:text><xsl:value-of select="$action"/>
    </xsl:if>
  </xsl:template>

  <xsl:template name="add-defer-rule">
    <xsl:variable name="defer" select="deferrable"/>
    <xsl:choose>
      <xsl:when test="$defer='INITIALLY DEFERRED'">
        <xsl:value-of select="$newline"/>
        <xsl:text>  DEFERRABLE INITIALLY DEFERRED</xsl:text>
      </xsl:when>
      <xsl:when test="$defer='INITIALLY IMMEDIATE'">
        <xsl:value-of select="$newline"/>
        <xsl:text>  DEFERRABLE INITIALLY IMMEDIATE</xsl:text>
      </xsl:when>
    </xsl:choose>
  </xsl:template>

  <xsl:template match="view-def">
    <xsl:value-of select="$newline"/>
    <xsl:text>CREATE OR REPLACE VIEW </xsl:text>
    <xsl:value-of select="view-name"/>
    <xsl:value-of select="$newline"/>
    <xsl:text>(</xsl:text>
    <xsl:value-of select="$newline"/>

    <xsl:for-each select="column-def">
      <xsl:sort select="dbms-position" data-type="number"/>

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
        <xsl:text>double precision</xsl:text>
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
        <xsl:with-param name="text">
          <xsl:choose>
            <xsl:when test="$makeLowerCase = 'true'"><xsl:value-of select="$lower-name"/></xsl:when>
            <xsl:otherwise>
              <xsl:value-of select="$objectname"/>
            </xsl:otherwise>
          </xsl:choose>
        </xsl:with-param>
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
      <xsl:when test="$objectname != $lower-name and $makeLowerCase = 'false'">
        <xsl:text>"</xsl:text><xsl:value-of select="$objectname"/><xsl:text>"</xsl:text>
      </xsl:when>
      <xsl:otherwise>
        <xsl:value-of select="$clean-name"/>
      </xsl:otherwise>
    </xsl:choose>
  </xsl:template>

</xsl:stylesheet>
