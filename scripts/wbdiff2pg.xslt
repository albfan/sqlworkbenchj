<?xml version="1.0" encoding="ISO-8859-1"?>
<xsl:stylesheet
     version="1.2"
     xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
>
<!--
Convert the output of SQL Workbench's WbSchemaDiff command to a PostgreSQL script
Author: Thomas Kellerer, Henri Tremblay, Rogelio León Anaya
-->

<xsl:output  encoding="iso-8859-15" method="text" indent="no" standalone="yes" omit-xml-declaration="yes"/>

  <xsl:strip-space elements="*"/>
  <xsl:variable name="quote">"</xsl:variable>
  <xsl:variable name="newline">
    <xsl:text>&#10;</xsl:text>
  </xsl:variable>


  <xsl:template match="/">

    <xsl:text>-- Added Tables without Foreign Keys</xsl:text>
    <xsl:value-of select="$newline"/>
    <!-- Added Tables without Foreign Keys-->
    <xsl:apply-templates select="/schema-diff/add-table"/>

    <xsl:text>-- Drop Foreign Keys of Modified Tables</xsl:text>
    <xsl:value-of select="$newline"/>
    <!-- Drop Foreign Keys of Modified Tables. This assures that droped tables doesn't generate an error -->
    <xsl:for-each select="/schema-diff/modify-table">

      <xsl:variable name="table">
        <xsl:call-template name="write-object-name">
          <xsl:with-param name="objectname" select="@name"/>
        </xsl:call-template>
      </xsl:variable>

      <xsl:for-each select="drop-foreign-keys/foreign-key">
        <xsl:call-template name="drop-fk">
          <xsl:with-param name="tablename" select="$table"/>
        </xsl:call-template>
        <xsl:value-of select="$newline"/>
      </xsl:for-each>

    </xsl:for-each>

    <xsl:text>-- Modified Tables Definition</xsl:text>
    <xsl:value-of select="$newline"/>
    <!--Modified Tables Definition -->
    <xsl:for-each select="/schema-diff/modify-table">

      <xsl:variable name="table2">
        <xsl:call-template name="write-object-name">
          <xsl:with-param name="objectname" select="@name"/>
        </xsl:call-template>
      </xsl:variable>

      <xsl:apply-templates select="remove-column">
        <xsl:with-param name="table" select="$table2"/>
      </xsl:apply-templates>

      <xsl:apply-templates select="add-column">
        <xsl:with-param name="table" select="$table2"/>
      </xsl:apply-templates>

      <xsl:apply-templates select="modify-column">
        <xsl:with-param name="table" select="$table2"/>
      </xsl:apply-templates>

      <xsl:apply-templates select="remove-primary-key">
        <xsl:with-param name="table" select="$table2"/>
      </xsl:apply-templates>

      <xsl:apply-templates select="add-primary-key">
        <xsl:with-param name="table" select="$table2"/>
      </xsl:apply-templates>

      <xsl:apply-templates select="drop-index">
        <xsl:with-param name="table" select="$table2"/>
      </xsl:apply-templates>

      <xsl:apply-templates select="add-index">
        <xsl:with-param name="table" select="$table2"/>
      </xsl:apply-templates>

      <xsl:apply-templates select="update-trigger">
        <xsl:with-param name="table" select="$table2"/>
      </xsl:apply-templates>

      <xsl:for-each select="table-constraints/drop-constraint/constraint-definition">
        <xsl:text>ALTER TABLE </xsl:text>
        <xsl:value-of select="$table2"/>
        <xsl:text> DROP CONSTRAINT </xsl:text>
        <xsl:call-template name="write-object-name">
          <xsl:with-param name="objectname" select="@name"/>
        </xsl:call-template>
        <xsl:text>;</xsl:text>
        <xsl:value-of select="$newline"/>
      </xsl:for-each>
      <xsl:value-of select="$newline"/>

      <xsl:for-each select="table-constraints/modify-constraint/constraint-definition">
        <xsl:text>ALTER TABLE </xsl:text>
        <xsl:value-of select="$table2"/>
        <xsl:text> DROP CONSTRAINT </xsl:text>
        <xsl:call-template name="write-object-name">
          <xsl:with-param name="objectname" select="@name"/>
        </xsl:call-template>
        <xsl:text>;</xsl:text>
        <xsl:value-of select="$newline"/>

        <xsl:text>ALTER TABLE </xsl:text>
        <xsl:value-of select="$table2"/>
        <xsl:text> ADD</xsl:text>
        <xsl:if test="@generated-name != 'true'">
          <xsl:text> CONSTRAINT </xsl:text>
          <xsl:call-template name="write-object-name">
            <xsl:with-param name="objectname" select="@name"/>
          </xsl:call-template>
        </xsl:if>
        <xsl:text> CHECK </xsl:text>
        <xsl:value-of select="."/>
        <xsl:text>;</xsl:text>
        <xsl:value-of select="$newline"/>
      </xsl:for-each>

      <xsl:for-each select="table-constraints/add-constraint/constraint-definition">
        <xsl:text>ALTER TABLE </xsl:text>
        <xsl:value-of select="$table2"/>
        <xsl:text> ADD</xsl:text>
        <xsl:if test="@generated-name != 'true'">
          <xsl:text> CONSTRAINT </xsl:text>
          <xsl:call-template name="write-object-name">
            <xsl:with-param name="objectname" select="@name"/>
          </xsl:call-template>
        </xsl:if>
        <xsl:text> CHECK </xsl:text>
        <xsl:value-of select="."/>
        <xsl:text>;</xsl:text>
        <xsl:value-of select="$newline"/>
      </xsl:for-each>

    </xsl:for-each>


    <xsl:text>-- Foreign Keys of Added Tables</xsl:text>
    <xsl:value-of select="$newline"/>
    <!-- Foreign Keys of Added Tables -->
    <xsl:for-each select="/schema-diff/add-table">
      <xsl:variable name="tableFk">
        <xsl:call-template name="write-object-name">
          <xsl:with-param name="objectname" select="@name"/>
        </xsl:call-template>
      </xsl:variable>

      <xsl:for-each select="table-def/foreign-keys">
        <xsl:for-each select="foreign-key">
          <xsl:call-template name="add-fk">
            <xsl:with-param name="tablename" select="$tableFk"/>
          </xsl:call-template>
          <xsl:value-of select="$newline"/>
        </xsl:for-each>
        <xsl:for-each select="index-def">
          <xsl:call-template name="create-index">
            <xsl:with-param name="tablename" select="$tableFk"/>
          </xsl:call-template>
        </xsl:for-each>
      </xsl:for-each>

    </xsl:for-each>


    <xsl:text>-- Foreign Keys of Modified Tables</xsl:text>
    <xsl:value-of select="$newline"/>
    <!-- Foreign Keys of Modified Tables. This assures al tables needed for foreign keys are created -->
    <xsl:for-each select="/schema-diff/modify-table">

      <xsl:variable name="table3">
        <xsl:call-template name="write-object-name">
          <xsl:with-param name="objectname" select="@name"/>
        </xsl:call-template>
      </xsl:variable>

      <xsl:for-each select="add-foreign-keys/foreign-key">
        <xsl:call-template name="add-fk">
          <xsl:with-param name="tablename" select="$table3"/>
        </xsl:call-template>
        <xsl:value-of select="$newline"/>
      </xsl:for-each>

    </xsl:for-each>

    <xsl:if test="count(/schema-diff/modify-table) &gt; 0">
       <xsl:value-of select="$newline"/>
    </xsl:if>

    <!-- Processes out of table modifications cycles -->
    <xsl:for-each select="/schema-diff/drop-view">
      <xsl:text>DROP VIEW </xsl:text>
      <xsl:call-template name="write-object-name">
        <xsl:with-param name="objectname" select="view-name"/>
      </xsl:call-template>
      <xsl:text>;</xsl:text>
        <xsl:value-of select="$newline"/>
    </xsl:for-each>

    <xsl:for-each select="/schema-diff/create-view">
      <xsl:apply-templates select="view-def"/>
    </xsl:for-each>

    <xsl:for-each select="/schema-diff/update-view">
      <xsl:apply-templates select="view-def"/>
    </xsl:for-each>

    <xsl:value-of select="$newline"/>
    <xsl:text>COMMIT;</xsl:text>
    <xsl:value-of select="$newline"/>

  </xsl:template>

  <xsl:template match="drop-index">
    <xsl:param name="table"/>
    <xsl:text>DROP INDEX </xsl:text>
    <xsl:call-template name="write-object-name">
      <xsl:with-param name="objectname" select="."/>
    </xsl:call-template>
    <xsl:text>;</xsl:text>
    <xsl:value-of select="$newline"/>
  </xsl:template>

  <xsl:template match="add-index">
    <xsl:param name="table"/>
    <xsl:for-each select="index-def">
      <xsl:call-template name="create-index">
        <xsl:with-param name="tablename" select="$table"/>
      </xsl:call-template>
    </xsl:for-each>
  </xsl:template>

  <xsl:template match="update-trigger">
    <xsl:param name="table"/>
    <xsl:for-each select="trigger-def">
      <xsl:call-template name="create-trigger">
        <xsl:with-param name="tablename" select="$table"/>
      </xsl:call-template>
    </xsl:for-each>
  </xsl:template>

  <xsl:template match="remove-column">
    <xsl:param name="table"/>
    <xsl:text>ALTER TABLE </xsl:text>
    <xsl:value-of select="$table"/>
    <xsl:text> DROP COLUMN </xsl:text>
    <xsl:call-template name="write-object-name">
      <xsl:with-param name="objectname" select="@name"/>
    </xsl:call-template>
    <xsl:text>;</xsl:text>
    <xsl:value-of select="$newline"/>
  </xsl:template>

  <xsl:template match="add-column">
    <xsl:param name="table"/>
    <xsl:for-each select="column-def">
      <xsl:variable name="column">
        <xsl:call-template name="write-object-name">
          <xsl:with-param name="objectname" select="column-name"/>
        </xsl:call-template>
      </xsl:variable>
      <xsl:variable name="nullable" select="nullable"/>
      <xsl:text>ALTER TABLE </xsl:text>
      <xsl:value-of select="$table"/>
      <xsl:text> ADD COLUMN </xsl:text>
      <xsl:value-of select="$column"/>
      <xsl:text> </xsl:text>
      <xsl:value-of select="dbms-data-type"/>
        <xsl:if test="string-length(default-value) &gt; 0">
          <xsl:text> DEFAULT </xsl:text>
          <xsl:value-of select="default-value"/>
        </xsl:if>
      <xsl:if test="$nullable = 'false'">
        <xsl:text> NOT NULL</xsl:text>
      </xsl:if>
      <xsl:text>;</xsl:text>
      <xsl:value-of select="$newline"/>
    </xsl:for-each>
  </xsl:template>

<!-- Process the modify-column part -->
  <xsl:template match="modify-column">
    <xsl:param name="table"/>

    <xsl:variable name="column">
      <xsl:call-template name="write-object-name">
        <xsl:with-param name="objectname" select="@name"/>
      </xsl:call-template>
    </xsl:variable>

    <xsl:if test="string-length(dbms-data-type) &gt; 0">
      <xsl:text>ALTER TABLE </xsl:text>
      <xsl:value-of select="$table"/>
      <xsl:text> ALTER COLUMN </xsl:text>
      <xsl:value-of select="$column"/>
      <xsl:text> TYPE </xsl:text>
      <xsl:value-of select="dbms-data-type"/>
      <xsl:text>;</xsl:text>
      <xsl:value-of select="$newline"/>
    </xsl:if>

    <xsl:if test="nullable = 'true'">
      <xsl:text>ALTER TABLE </xsl:text>
      <xsl:value-of select="$table"/>
      <xsl:text> ALTER COLUMN </xsl:text>
      <xsl:value-of select="$column"/>
      <xsl:text> DROP NOT NULL;</xsl:text>
      <xsl:value-of select="$newline"/>
    </xsl:if>

    <xsl:if test="nullable = 'false'">
      <xsl:text>ALTER TABLE </xsl:text>
      <xsl:value-of select="$table"/>
      <xsl:text> ALTER COLUMN </xsl:text>
      <xsl:value-of select="$column"/>
      <xsl:text> SET NOT NULL;</xsl:text>
      <xsl:value-of select="$newline"/>
    </xsl:if>

    <xsl:if test="string-length(default-value) &gt; 0">
      <xsl:text>ALTER TABLE </xsl:text>
      <xsl:value-of select="$table"/>
      <xsl:text> ALTER COLUMN </xsl:text>
      <xsl:value-of select="$column"/>
      <xsl:text> SET DEFAULT </xsl:text>
      <xsl:value-of select="default-value"/>
      <xsl:text>;</xsl:text>
      <xsl:value-of select="$newline"/>
    </xsl:if>

    <xsl:if test="string-length(comment) &gt; 0">
      <xsl:text>COMMENT ON COLUMN </xsl:text>
      <xsl:value-of select="$table"/>
      <xsl:text>.</xsl:text>
      <xsl:value-of select="$column"/>
      <xsl:text> IS '</xsl:text>
      <xsl:value-of select="comment"/>
      <xsl:text>';</xsl:text>
      <xsl:value-of select="$newline"/>
    </xsl:if>

  </xsl:template>

<!-- Add primary keys -->
  <xsl:template match="add-primary-key">
    <xsl:param name="table"/>
    <xsl:text>ALTER TABLE </xsl:text>
    <xsl:value-of select="$table"/>
    <xsl:value-of select="$newline"/>
    <xsl:text>  ADD CONSTRAINT </xsl:text>
    <xsl:call-template name="write-object-name">
      <xsl:with-param name="objectname" select="@name"/>
    </xsl:call-template>
    <xsl:value-of select="$newline"/>
    <xsl:text>  PRIMARY KEY (</xsl:text>
    <xsl:for-each select="column-name">
      <xsl:call-template name="write-object-name">
        <xsl:with-param name="objectname" select="."/>
      </xsl:call-template>
      <xsl:if test="position() &lt; last()">
        <xsl:text>,</xsl:text>
      </xsl:if>
    </xsl:for-each>
    <xsl:text>);</xsl:text>
    <xsl:value-of select="$newline"/>
    <xsl:value-of select="$newline"/>
  </xsl:template>

<!-- Remove primary keys -->
  <xsl:template match="remove-primary-key">
    <xsl:param name="table"/>
    <xsl:text>ALTER TABLE </xsl:text>
    <xsl:value-of select="$table"/>
    <xsl:text> DROP PRIMARY KEY;</xsl:text>
    <xsl:value-of select="$newline"/>
  </xsl:template>

<!-- re-create a view -->
  <xsl:template match="view-def">
    <xsl:text>CREATE OR REPLACE VIEW </xsl:text>
    <xsl:call-template name="write-object-name">
      <xsl:with-param name="objectname" select="view-name"/>
    </xsl:call-template>
    <xsl:value-of select="$newline"/>
    <xsl:text>AS</xsl:text>
    <xsl:value-of select="$newline"/>
    <xsl:value-of select="view-source"/>
    <xsl:text>;</xsl:text>
    <xsl:value-of select="$newline"/>
    <xsl:if test="string-length(comment) &gt; 0">
      <xsl:text>COMMENT ON VIEW </xsl:text>
      <xsl:call-template name="write-object-name">
        <xsl:with-param name="objectname" select="view-name"/>
      </xsl:call-template>
      <xsl:text> IS '</xsl:text>
      <xsl:value-of select="comment"/>
      <xsl:text>';</xsl:text>
      <xsl:value-of select="$newline"/>
    </xsl:if>
    <xsl:value-of select="$newline"/>
  </xsl:template>

  <!-- Create Table Definition without foreign keys-->
  <xsl:template match="table-def">
    <xsl:variable name="tablename">
      <xsl:call-template name="write-object-name">
        <xsl:with-param name="objectname" select="table-name"/>
      </xsl:call-template>
    </xsl:variable>
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
      <xsl:text>  </xsl:text>
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
      <xsl:copy-of select="$colname"/>
      <xsl:text> </xsl:text>
      <xsl:value-of select="dbms-data-type"/>
      <xsl:value-of select="$nullable"/>
      <xsl:value-of select="$defaultvalue"/>
      <xsl:if test="position() &lt; last()">
        <xsl:text>,</xsl:text>
        <xsl:value-of select="$newline"/>
      </xsl:if>
    </xsl:for-each>
    <xsl:value-of select="$newline"/>
    <xsl:text>);</xsl:text>
    <xsl:value-of select="$newline"/>
    <xsl:value-of select="$newline"/>

    <xsl:variable name="pkcount">
      <xsl:value-of select="count(column-def[primary-key='true'])"/>
    </xsl:variable>

    <xsl:if test="$pkcount &gt; 0">
      <xsl:text>ALTER TABLE </xsl:text>
      <xsl:value-of select="$tablename"/>
      <xsl:value-of select="$newline"/>
      <xsl:text>  ADD CONSTRAINT </xsl:text>
      <xsl:value-of select="concat('pk_', $tablename)"/>
      <xsl:value-of select="$newline"/>
      <xsl:text>  PRIMARY KEY (</xsl:text>
      <xsl:for-each select="column-def[primary-key='true']">
        <xsl:value-of select="column-name"/>
        <xsl:if test="position() &lt; last()">
          <xsl:text>, </xsl:text>
        </xsl:if>
      </xsl:for-each>
      <xsl:text>);</xsl:text>
      <xsl:value-of select="$newline"/>
      <xsl:value-of select="$newline"/>
    </xsl:if>

  </xsl:template>

  <xsl:template name="drop-fk">
     <xsl:param name="tablename"/>
    <xsl:text>ALTER TABLE </xsl:text>
    <xsl:value-of select="$tablename"/>
    <xsl:value-of select="$newline"/>
    <xsl:text>  DROP CONSTRAINT </xsl:text>
    <xsl:call-template name="write-object-name">
      <xsl:with-param name="objectname" select="constraint-name"/>
    </xsl:call-template>
    <xsl:text>;</xsl:text>
    <xsl:value-of select="$newline"/>
  </xsl:template>

  <xsl:template name="add-fk">
     <xsl:param name="tablename"/>
    <xsl:text>ALTER TABLE </xsl:text>
    <xsl:value-of select="$tablename"/>
    <xsl:value-of select="$newline"/>
    <xsl:text>  ADD CONSTRAINT </xsl:text>
    <xsl:choose>
      <xsl:when test="contains(constraint-name,'.')">
        <xsl:call-template name="write-object-name">
          <xsl:with-param name="objectname" select="concat($quote,constraint-name,$quote)"/>
        </xsl:call-template>
      </xsl:when>
      <xsl:otherwise>
        <xsl:call-template name="write-object-name">
          <xsl:with-param name="objectname" select="constraint-name"/>
        </xsl:call-template>
      </xsl:otherwise>
    </xsl:choose>
    <xsl:value-of select="$newline"/>
    <xsl:text>  FOREIGN KEY (</xsl:text>
    <xsl:for-each select="source-columns/column">
      <xsl:call-template name="write-object-name">
        <xsl:with-param name="objectname" select="."/>
      </xsl:call-template>
      <xsl:if test="position() &lt; last()">
        <xsl:text>, </xsl:text>
      </xsl:if>
    </xsl:for-each>
    <xsl:text>)</xsl:text>
    <xsl:value-of select="$newline"/>
    <xsl:text>  REFERENCES </xsl:text>
    <xsl:call-template name="write-object-name">
      <xsl:with-param name="objectname" select="references/table-name"/>
    </xsl:call-template>
    <xsl:text> (</xsl:text>
    <xsl:for-each select="referenced-columns/column">
        <xsl:call-template name="write-object-name">
          <xsl:with-param name="objectname" select="."/>
        </xsl:call-template>
        <xsl:if test="position() &lt; last()">
          <xsl:text>, </xsl:text>
        </xsl:if>
    </xsl:for-each>
    <xsl:text>);</xsl:text>
    <xsl:value-of select="$newline"/>
  </xsl:template>

  <xsl:template name="create-index">
    <xsl:param name="tablename"/>
    <xsl:variable name="pk" select="primary-key"/>
    <xsl:if test="$pk = 'false'">
      <xsl:variable name="unique">
        <xsl:if test="unique='true'">UNIQUE </xsl:if>
      </xsl:variable>
      <xsl:text>CREATE </xsl:text>
      <xsl:value-of select="$unique"/>
      <xsl:text>INDEX </xsl:text>
      <xsl:call-template name="write-object-name">
        <xsl:with-param name="objectname" select="name"/>
      </xsl:call-template>
      <xsl:text> ON </xsl:text>
      <xsl:value-of select="$tablename"/>
      <xsl:text> (</xsl:text>
      <xsl:value-of select="index-expression"/>
      <xsl:text>);</xsl:text>
      <xsl:value-of select="$newline"/>
    </xsl:if>
  </xsl:template>

  <xsl:template name="create-trigger">
    <xsl:param name="tablename"/>
    <xsl:variable name="source" select="trigger-source"/>
    <xsl:value-of select="$source"/>
    <xsl:value-of select="$newline"/>
  </xsl:template>

  <xsl:template name="write-object-name">
    <xsl:param name="objectname"/>
    <xsl:variable name="lcletters">abcdefghijklmnopqrstuvwxyz</xsl:variable>
    <xsl:variable name="ucletters">ABCDEFGHIJKLMNOPQRSTUVWXYZ</xsl:variable>

    <xsl:variable name="lower-name">
      <xsl:value-of select="translate($objectname,$ucletters,$lcletters)"/>
    </xsl:variable>

    <xsl:choose>
      <xsl:when test="substring($objectname,1,1) = $quote and substring($objectname,string-length($objectname),1) = $quote">
        <xsl:value-of select="$objectname"/>
      </xsl:when>
      <!-- Support for common Postgres and SQL keywords. If you need another special one, just added it -->
      <xsl:when test="$objectname = 'order' or $objectname = 'name' or $objectname = 'type' or $objectname = 'limit' or $objectname = 'where' or $objectname = 'select' or $objectname = 'update' or $objectname = 'delete' or $objectname = 'alter' or $objectname = 'and' or $objectname = 'as' or $objectname = 'or' or $objectname = 'group' or $objectname = 'by' or $objectname = 'primary' or $objectname = 'foreign' or $objectname = 'rollback' or $objectname = 'commit' or $objectname = 'begin' or $objectname = 'return'">
        <xsl:value-of select="concat($quote, $objectname, $quote)"/>
      </xsl:when>
      <xsl:when test="contains($objectname,' ') or contains($objectname,'-')">
        <xsl:value-of select="concat($quote, $objectname, $quote)"/>
      </xsl:when>
      <xsl:when test="$objectname != $lower-name">
        <xsl:text>"</xsl:text><xsl:value-of select="$objectname"/><xsl:text>"</xsl:text>
      </xsl:when>
      <xsl:otherwise>
        <xsl:value-of select="$objectname"/>
      </xsl:otherwise>
    </xsl:choose>
  </xsl:template>

</xsl:stylesheet>
