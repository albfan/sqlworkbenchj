<?xml version="1.0" encoding="ISO-8859-1"?>
<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform" xml:space="preserve"> 
<!--
  Convert a SQL Workbench/J schema report (http://www.sql-workbench.net)
  into a benerator definition 

  Author: Thomas Kellerer
-->

<xsl:output encoding="UTF-8" method="xml" indent="yes" standalone="yes" omit-xml-declaration="yes"/>
<xsl:preserve-space elements="setup generate consumer"/>

<xsl:variable name="quote"><xsl:text>"</xsl:text></xsl:variable>
<xsl:variable name="newline"><xsl:text>&#10;</xsl:text></xsl:variable>
<xsl:variable name="squote"><xsl:text>&#39;</xsl:text></xsl:variable>
<xsl:variable name="dsquote"><xsl:text>&#39;&#39;</xsl:text></xsl:variable>

<xsl:variable name="lcletters">abcdefghijklmnopqrstuvwxyz</xsl:variable>
<xsl:variable name="ucletters">ABCDEFGHIJKLMNOPQRSTUVWXYZ</xsl:variable>

<xsl:template match="/">
<setup xmlns="http://databene.org/benerator/0.8.0"
		xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
		xsi:schemaLocation="http://databene.org/benerator/0.8.0 http://databene.org/benerator-0.8.0.xsd">

  <import defaults="true"/>
  <import platforms="csv"/>

  <xsl:for-each select="/schema-report/table-def">
    <xsl:variable name="tablename">
      <xsl:value-of select="translate(table-name,$ucletters,$lcletters)"/>
    </xsl:variable>
    <setting name="{$tablename}_count" value="100"/> 
  </xsl:for-each>
  
  <xsl:apply-templates select="/schema-report/table-def"/>
  
</setup>
  
</xsl:template>

<xsl:template match="table-def">
  <xsl:variable name="tablename">
    <xsl:value-of select="translate(table-name,$ucletters,$lcletters)"/>
  </xsl:variable>
  
  <generate type="{$tablename}" count="{$tablename}_count">
      <consumer class="CSVEntityExporter">
          <property name="uri" value="{$tablename}.csv" />
          <property name="separator" value="\t"/>
          <property name="lineSeparator" value="\n"/>
          <property name="encoding" value="ISO-8859-1"/>
      </consumer>
      <xsl:value-of select="$newline"/>
      
      <xsl:for-each select="column-def">
        <xsl:sort select="dbms-position"/>
        <xsl:variable name="colname">
          <xsl:value-of select="translate(column-name,$ucletters,$lcletters)"/>
        </xsl:variable>
        
        <xsl:variable name="dbms-type">
          <xsl:value-of select="translate(dbms-data-type,$ucletters,$lcletters)"/>
        </xsl:variable>
        
        <xsl:variable name="datatype">
          <xsl:choose>
            <xsl:when test="dbms-type = 'datetime'">
              <xsl:value-of select="'timestamp'"/>
            </xsl:when>
            <xsl:when test="java-sql-type-name = 'VARCHAR'">
              <xsl:value-of select="'string'"/>
            </xsl:when>
            <xsl:when test="java-sql-type-name = 'BIGINT'">
              <xsl:value-of select="'big_integer'"/>
            </xsl:when>
            <xsl:when test="substring($dbms-type,1,7) = 'number('">
              <xsl:value-of select="'big_decimal'"/>
            </xsl:when>
            <xsl:when test="substring($dbms-type,1,7) = 'numeric('">
              <xsl:value-of select="'big_decimal'"/>
            </xsl:when>
            <xsl:when test="substring($dbms-type,1,7) = 'decimal('">
              <xsl:value-of select="'big_decimal'"/>
            </xsl:when>
            <xsl:when test="dbms-type = 'number'">
              <xsl:value-of select="'big_integer'"/>
            </xsl:when>
            <xsl:otherwise>
              <xsl:value-of select="dbms-type"/>
            </xsl:otherwise>
          </xsl:choose>
        </xsl:variable>
        
        <attribute name="{$colname}" type="{$datatype}" />
        
      </xsl:for-each>
  </generate>
    
</xsl:template> <!-- table-def -->

</xsl:stylesheet>

