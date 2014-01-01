<?xml version="1.0" encoding="ISO-8859-1"?>
<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform">

<!--
  Convert the output of SQL Workbench's WbSchemaDiff command to Microsoft SQL Server
  Author: Thomas Kellerer
-->
<xsl:output
  encoding="iso-8859-15"
  method="text"
  indent="no"
  standalone="yes"
  omit-xml-declaration="yes"
/>
  <xsl:strip-space elements="*"/>

  <xsl:variable name="quote">
    <xsl:text>"</xsl:text>
  </xsl:variable>
  <xsl:variable name="newline">
    <xsl:text>&#10;</xsl:text>
  </xsl:variable>

  <xsl:template match="/">
    <xsl:apply-templates select="/schema-diff/add-table"/>
    <xsl:value-of select="$newline"/>

    <xsl:for-each select="/schema-diff/modify-table">

      <xsl:variable name="table" select="@name"/>

      <xsl:apply-templates select="add-column">
        <xsl:with-param name="table" select="$table"/>
      </xsl:apply-templates>

      <xsl:apply-templates select="remove-column">
        <xsl:with-param name="table" select="$table"/>
      </xsl:apply-templates>

      <xsl:apply-templates select="modify-column">
        <xsl:with-param name="table" select="$table"/>
      </xsl:apply-templates>

      <xsl:apply-templates select="remove-primary-key">
        <xsl:with-param name="table" select="$table"/>
      </xsl:apply-templates>

      <xsl:apply-templates select="add-primary-key">
        <xsl:with-param name="table" select="$table"/>
      </xsl:apply-templates>

      <xsl:apply-templates select="add-index">
        <xsl:with-param name="table" select="$table"/>
      </xsl:apply-templates>

      <xsl:for-each select="table-constraints/drop-constraint/constraint-definition">
        <xsl:text>ALTER TABLE </xsl:text>
        <xsl:value-of select="$table"/>
        <xsl:text> DROP CONSTRAINT </xsl:text>
        <xsl:value-of select="@name"/>
        <xsl:text>;</xsl:text>
        <xsl:value-of select="$newline"/>
      </xsl:for-each>
      <xsl:value-of select="$newline"/>

      <xsl:for-each select="table-constraints/modify-constraint/constraint-definition">
        <xsl:text>ALTER TABLE </xsl:text>
        <xsl:value-of select="$table"/>
        <xsl:text> DROP CONSTRAINT </xsl:text>
        <xsl:value-of select="@name"/>
        <xsl:text>;</xsl:text>
        <xsl:value-of select="$newline"/>

        <xsl:text>ALTER TABLE </xsl:text>
        <xsl:value-of select="$table"/>
        <xsl:text> ADD</xsl:text>
        <xsl:if test="@generated-name='true'">
          <xsl:text> CONSTRAINT </xsl:text>
          <xsl:value-of select="@name"/>
        </xsl:if>
        <xsl:text> CHECK </xsl:text>
        <xsl:value-of select="normalize-space(.)"/>
        <xsl:text>;</xsl:text>
        <xsl:value-of select="$newline"/>
      </xsl:for-each>

      <xsl:for-each select="table-constraints/add-constraint/constraint-definition">
        <xsl:text>ALTER TABLE </xsl:text>
        <xsl:value-of select="$table"/>
        <xsl:text> ADD</xsl:text>
        <xsl:if test="@generated-name='true'">
          <xsl:text> CONSTRAINT </xsl:text>
          <xsl:value-of select="@name"/>
        </xsl:if>
        <xsl:text> CHECK </xsl:text>
        <xsl:value-of select="normalize-space(.)"/>
        <xsl:text>;</xsl:text>
        <xsl:value-of select="$newline"/>
      </xsl:for-each>

    </xsl:for-each>

    <xsl:for-each select="/schema-diff/create-view">
      <xsl:apply-templates select="view-def">
        <xsl:with-param name="mode" select="'create'"/>
      </xsl:apply-templates>
    </xsl:for-each>

    <xsl:for-each select="/schema-diff/update-view">
      <xsl:apply-templates select="view-def">
        <xsl:with-param name="mode" select="'update'"/>
      </xsl:apply-templates>
    </xsl:for-each>

    <xsl:for-each select="/schema-diff/create-proc">
      <xsl:apply-templates select="proc-def"/>
    </xsl:for-each>

    <xsl:value-of select="$newline"/>
    <xsl:text>COMMIT;</xsl:text>
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

  <xsl:template match="drop-index">
    <xsl:param name="table"/>
    <xsl:text>ALTER TABLE </xsl:text>
    <xsl:value-of select="$table"/>
    <xsl:text> DROP INDEX </xsl:text>
    <xsl:value-of select="."/>
    <xsl:text>;</xsl:text>
    <xsl:value-of select="$newline"/>
  </xsl:template>

  <xsl:template match="add-column">
    <xsl:param name="table"/>
    <xsl:for-each select="column-def">
      <xsl:variable name="column" select="column-name"/>
      <xsl:variable name="nullable" select="nullable"/>
      <xsl:text>ALTER TABLE </xsl:text>
      <xsl:value-of select="$table"/>
      <xsl:text> ADD </xsl:text>
      <xsl:value-of select="$column"/>
      <xsl:text> </xsl:text>
      <xsl:value-of select="dbms-data-type"/>
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
    <xsl:variable name="column" select="@name"/>
    <xsl:if test="string-length(dbms-data-type) &gt; 0">
      <xsl:text>ALTER TABLE </xsl:text>
      <xsl:value-of select="$table"/>
      <xsl:text> ALTER COLUMN </xsl:text>
      <xsl:value-of select="$column"/>
      <xsl:text> </xsl:text>
      <xsl:value-of select="dbms-data-type"/>
      <xsl:text>;</xsl:text>
      <xsl:value-of select="$newline"/>
    </xsl:if>

    <xsl:if test="nullable = 'true'">
      <xsl:text>ALTER TABLE </xsl:text>
      <xsl:value-of select="$table"/>
      <xsl:text> ALTER COLUMN </xsl:text>
      <xsl:value-of select="$column"/>
      <xsl:text> </xsl:text>
      <xsl:value-of select="dbms-data-type"/>
      <xsl:text> NULL;</xsl:text>
      <xsl:value-of select="$newline"/>
    </xsl:if>

    <xsl:if test="string-length(default-value) &gt; 0">
      <xsl:text>ALTER TABLE </xsl:text>
      <xsl:value-of select="$table"/>
      <xsl:text> ALTER COLUMN </xsl:text>
      <xsl:value-of select="$column"/>
      <xsl:text> DROP DEFAULT;</xsl:text>
      <xsl:value-of select="$newline"/>

      <xsl:text>ALTER TABLE </xsl:text>
      <xsl:value-of select="$table"/>
      <xsl:text> ADD DEFAULT </xsl:text>
      <xsl:value-of select="default-value"/>
      <xsl:text> FOR </xsl:text>
      <xsl:value-of select="$column"/>
      <xsl:text>;</xsl:text>
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
    <xsl:value-of select="@name"/>
    <xsl:value-of select="$newline"/>
    <xsl:text>  PRIMARY KEY (</xsl:text>
    <xsl:for-each select="column-name">
      <xsl:copy-of select="."/>
      <xsl:if test="position() &lt; last()">
        <xsl:text>,</xsl:text>
      </xsl:if>
    </xsl:for-each>
    <xsl:text>);</xsl:text>
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
    <xsl:param name="mode"/>
    <!--
    <xsl:if test="$mode = 'update'">
      <xsl:text>ALTER VIEW </xsl:text>
      <xsl:value-of select="view-name"/>
      <xsl:value-of select="$newline"/>
    </xsl:if>
    <xsl:if test="$mode = 'create'">
      <xsl:text>CREATE VIEW </xsl:text>
      <xsl:value-of select="view-name"/>
      <xsl:value-of select="$newline"/>
    </xsl:if>
    <xsl:text>AS</xsl:text>
    -->
    <xsl:value-of select="$newline"/>
    <xsl:copy-of select="view-source"/>
    <xsl:value-of select="$newline"/>
  </xsl:template>

  <xsl:template match="table-def">
    <xsl:variable name="tablename" select="table-name"/>
    <xsl:text>CREATE TABLE </xsl:text>
    <xsl:value-of select="table-name"/>
    <xsl:value-of select="$newline"/>
    <xsl:text>(</xsl:text>
    <xsl:value-of select="$newline"/>

    <xsl:for-each select="column-def">
      <xsl:sort select="dbms-position"/>
      <xsl:variable name="colname">
        <xsl:choose>
          <xsl:when test="contains(column-name,' ')">
            <xsl:value-of select="concat($quote,column-name,$quote)"/>
          </xsl:when>
          <xsl:otherwise>
            <xsl:value-of select="column-name"/>
          </xsl:otherwise>
        </xsl:choose>
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
      <xsl:text>  </xsl:text>
      <xsl:copy-of select="$colname"/>
      <xsl:text> </xsl:text>
      <xsl:value-of select="dbms-data-type"/>
      <xsl:value-of select="$nullable"/>
      <xsl:value-of select="$defaultvalue"/>
      <xsl:if test="position() &lt; last()">
        <xsl:text>,</xsl:text>
      </xsl:if>
      <xsl:value-of select="$newline"/>
    </xsl:for-each>
    <xsl:text>);</xsl:text>
    <xsl:value-of select="$newline"/>

    <xsl:variable name="pkcount">
      <xsl:value-of select="count(column-def[primary-key='true'])"/>
    </xsl:variable>

    <xsl:if test="$pkcount &gt; 0">
      <xsl:text>ALTER TABLE </xsl:text>
      <xsl:value-of select="$tablename"/>
      <xsl:value-of select="$newline"/>
      <xsl:text> ADD CONSTRAINT </xsl:text>
      <xsl:value-of select="concat('pk_', $tablename)"/>
      <xsl:value-of select="$newline"/>
      <xsl:text> PRIMARY KEY (</xsl:text>
      <xsl:for-each select="column-def[primary-key='true']">
        <xsl:value-of select="column-name"/>
        <xsl:if test="position() &lt; last()">
          <xsl:text>,</xsl:text>
        </xsl:if>
      </xsl:for-each>
      <xsl:text>);</xsl:text>
      <xsl:value-of select="$newline"/>
    </xsl:if>

    <xsl:for-each select="index-def">
      <xsl:call-template name="create-index">
        <xsl:with-param name="tablename" select="$tablename"/>
      </xsl:call-template>
    </xsl:for-each>

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
      <xsl:value-of select="name"/>
      <xsl:text> ON </xsl:text>
      <xsl:value-of select="$tablename"/>
      <xsl:text> (</xsl:text>
      <xsl:value-of select="index-expression"/>
      <xsl:text>);</xsl:text>
      <xsl:value-of select="$newline"/>
    </xsl:if>
  </xsl:template>

</xsl:stylesheet>