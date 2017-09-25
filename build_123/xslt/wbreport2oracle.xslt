<?xml version="1.0" encoding="ISO-8859-1"?>
<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform">
<!--
  Convert a SQL Workbench/J schema report (http://www.sql-workbench.net)
  into a SQL script for Oracle (http://www.oracle.com)

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
<xsl:variable name="quote"><xsl:text>"</xsl:text></xsl:variable>
<xsl:variable name="newline"><xsl:text>&#10;</xsl:text></xsl:variable>
  <xsl:variable name="squote"><xsl:text>&#39;</xsl:text></xsl:variable>
  <xsl:variable name="dsquote"><xsl:text>&#39;&#39;</xsl:text></xsl:variable>

<xsl:template match="/">
  <xsl:apply-templates select="/schema-report/sequence-def"/>
  <xsl:apply-templates select="/schema-report/table-def"/>
  <xsl:apply-templates select="/schema-report/view-def"/>
  <xsl:call-template name="process-fk"/>
</xsl:template>

<xsl:template match="table-def">

  <xsl:variable name="tablename" select="table-name"/>
  <xsl:text>DROP TABLE </xsl:text>
  <xsl:value-of select="table-name"/>
  <xsl:text> CASCADE CONSTRAINTS;</xsl:text>
  <xsl:value-of select="$newline"/>

  <xsl:text>CREATE TABLE </xsl:text><xsl:value-of select="table-name"/>
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
      <xsl:if test="nullable = 'false'"> NOT NULL</xsl:if>
    </xsl:variable>

    <xsl:variable name="defaultvalue">
      <xsl:if test="string-length(default-value) &gt; 0">
        <xsl:text> </xsl:text>DEFAULT <xsl:value-of select="default-value"/>
      </xsl:if>
    </xsl:variable>

    <xsl:variable name="datatype">
      <xsl:choose>
        <xsl:when test="dbms-data-type = 'datetime'">
          <xsl:value-of select="'TIMESTAMP'"/>
        </xsl:when>
        <xsl:when test="dbms-data-type = 'text'">
          <xsl:value-of select="'VARCHAR2(4000)'"/>
        </xsl:when>
        <xsl:when test="dbms-data-type = 'int'">
          <xsl:value-of select="'INTEGER'"/>
        </xsl:when>
        <xsl:when test="java-sql-type-name = 'VARCHAR'">
          <xsl:value-of select="'VARCHAR2('"/><xsl:value-of select="dbms-data-size"/><xsl:value-of select="')'"/>
        </xsl:when>
        <xsl:when test="java-sql-type-name = 'BIGINT'">
          <xsl:value-of select="'INTEGER'"/>
        </xsl:when>
        <xsl:otherwise>
          <xsl:value-of select="dbms-data-type"/>
        </xsl:otherwise>
      </xsl:choose>
    </xsl:variable>

    <xsl:text>  </xsl:text>
    <xsl:copy-of select="$colname"/>
    <xsl:text> </xsl:text>
    <xsl:value-of select="$datatype"/>
    <xsl:value-of select="$defaultvalue"/>
    <xsl:value-of select="$nullable"/>
    <xsl:if test="position() &lt; last()">
      <xsl:text>,</xsl:text><xsl:value-of select="$newline"/>
    </xsl:if>
  </xsl:for-each>

  <xsl:for-each select="table-constraints/constraint-definition">
    <xsl:text>,</xsl:text>
    <xsl:value-of select="$newline"/>
    <xsl:text>  </xsl:text>
    <xsl:if test="@generated-name = 'false'">
      <xsl:text>CONSTRAINT </xsl:text>
      <xsl:value-of select="@name"/>
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
    <xsl:text>ALTER TABLE </xsl:text><xsl:value-of select="$tablename"/>
    <xsl:value-of select="$newline"/>
	<xsl:variable name="constraintname">
    <xsl:if test="string-length(primary-key-name) &gt; 0">
      <xsl:value-of select="primary-key-name"/>
    </xsl:if>
    <xsl:if test="string-length(primary-key-name) = 0">
      <xsl:value-of select="concat('pk_', $tablename)"/>
    </xsl:if>
	</xsl:variable>
    <xsl:text>ADD CONSTRAINT </xsl:text><xsl:value-of select="$constraintname"/><xsl:text> PRIMARY KEY (</xsl:text>
    <xsl:for-each select="column-def[primary-key='true']">
      <xsl:value-of select="column-name"/>
      <xsl:if test="position() &lt; last()">
        <xsl:text>, </xsl:text>
      </xsl:if>
    </xsl:for-each>
    <xsl:text>);</xsl:text>
    <xsl:value-of select="$newline"/>
  </xsl:if>

  <xsl:if test="string-length(table-comment) &gt; 0">
    <xsl:text>COMMENT ON TABLE </xsl:text><xsl:value-of select="$tablename"/><xsl:text> IS '</xsl:text><xsl:value-of select="normalize-space(table-comment)"/><xsl:text>';</xsl:text>
    <xsl:value-of select="$newline"/>
  </xsl:if>

  <xsl:for-each select="column-def">
      <xsl:sort select="column-name"/>
      <xsl:if test="string-length(comment) &gt; 0">
        <xsl:text>COMMENT ON COLUMN </xsl:text>
        <xsl:value-of select="$tablename"/><xsl:text>.</xsl:text><xsl:value-of select="column-name"/>
        <xsl:text> IS '</xsl:text>
        <xsl:call-template name="_replace_text">
            <xsl:with-param name="text" select="normalize-space(comment)"/>
            <xsl:with-param name="replace" select="$squote"/>
            <xsl:with-param name="by" select="$dsquote"/>
        </xsl:call-template>
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
  <xsl:value-of select="$newline"/>

</xsl:template>

<xsl:template name="create-index">
  <xsl:param name="tablename"/>
  <xsl:variable name="pk" select="primary-key"/>
  <xsl:if test="$pk = 'false'">
    <xsl:variable name="unique">
      <xsl:if test="unique='true'">UNIQUE </xsl:if>
    </xsl:variable>
    <xsl:value-of select="$newline"/>
    <xsl:text>CREATE </xsl:text><xsl:value-of select="$unique"/>
    <xsl:text>INDEX </xsl:text><xsl:value-of select="name"/>
    <xsl:text> ON </xsl:text><xsl:value-of select="$tablename"/>
    <xsl:text>(</xsl:text>
    <xsl:for-each select="column-list/column">
        <xsl:value-of select="@name"/>
        <xsl:if test="position() &lt; last()">
          <xsl:text>, </xsl:text>
        </xsl:if>
    </xsl:for-each>
    <xsl:text>);</xsl:text>
  </xsl:if>
</xsl:template>

<xsl:template name="process-fk">
  <xsl:for-each select="/schema-report/table-def">
    <xsl:variable name="table" select="table-name"/>
    <xsl:if test="count(foreign-keys) &gt; 0">
      <xsl:for-each select="foreign-keys/foreign-key">
        <xsl:variable name="targetTable" select="references/table-name"/>
        <xsl:value-of select="$newline"/>
        <xsl:text>ALTER TABLE </xsl:text><xsl:value-of select="$table"/>
        <xsl:value-of select="$newline"/>
        <xsl:text> ADD CONSTRAINT </xsl:text><xsl:value-of select="constraint-name"/>
        <xsl:value-of select="$newline"/>
        <xsl:text> FOREIGN KEY (</xsl:text>
        <xsl:for-each select="source-columns/column">
          <xsl:value-of select="."/>
          <xsl:if test="position() &lt; last()">
            <xsl:text>,</xsl:text>
          </xsl:if>
        </xsl:for-each>
        <xsl:text>)</xsl:text>
        <xsl:value-of select="$newline"/>
        <xsl:text> REFERENCES </xsl:text><xsl:value-of select="$targetTable"/><xsl:text> (</xsl:text>
        <xsl:for-each select="referenced-columns/column">
          <xsl:value-of select="."/>
          <xsl:if test="position() &lt; last()">
            <xsl:text>,</xsl:text>
          </xsl:if>
        </xsl:for-each>
        <xsl:text>)</xsl:text>
        <xsl:call-template name="add-fk-action">
          <xsl:with-param name="event-name" select="'ON DELETE'"/>
          <xsl:with-param name="action" select="delete-rule"/>
        </xsl:call-template>
        <xsl:call-template name="add-defer-rule"/>
        <xsl:text>;</xsl:text>
        <xsl:value-of select="$newline"/>
      </xsl:for-each>
      <xsl:value-of select="$newline"/>
    </xsl:if>
  </xsl:for-each>
</xsl:template>

<xsl:template name="add-fk-action">
  <xsl:param name="event-name"/>
  <xsl:param name="action"/>
  <xsl:if test="$action = 'DELETE' or $action = 'SET NULL'">
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

<xsl:template match="sequence-def">
  <xsl:value-of select="sequence-source"/>
</xsl:template>

<xsl:template match="view-def">
  <xsl:variable name="quote"><xsl:text>"</xsl:text></xsl:variable>
  <xsl:variable name="backtick"><xsl:text>&#96;</xsl:text></xsl:variable>

  <xsl:value-of select="$newline"/>
  <xsl:text>CREATE OR REPLACE VIEW </xsl:text><xsl:value-of select="view-name"/>
  <xsl:value-of select="$newline"/>
  <xsl:text>(</xsl:text>
  <xsl:value-of select="$newline"/>

  <xsl:for-each select="column-def">
    <xsl:sort select="dbms-position"/>
    <xsl:variable name="orgname" select="column-name"/>
    <xsl:variable name="uppername">
    <xsl:value-of select="translate(column-name,
                                  'abcdefghijklmnopqrstuvwxyz`',
                                  'ABCDEFGHIJKLMNOPQRSTUVWXYZ')"/>
    </xsl:variable>
    <xsl:variable name="colname">
      <xsl:choose>
      <xsl:when test="contains(column-name,' ')">
        <xsl:value-of select="concat($quote,column-name,$quote)"/>
      </xsl:when>
      <xsl:otherwise>
          <xsl:value-of select="$uppername"/>
      </xsl:otherwise>
      </xsl:choose>
    </xsl:variable>
    <xsl:text>  </xsl:text><xsl:copy-of select="$colname"/>
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

</xsl:stylesheet>

