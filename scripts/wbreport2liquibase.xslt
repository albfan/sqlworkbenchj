<?xml version="1.0" encoding="ISO-8859-1"?>
<!-- 
  Convert a SQL Workbench/J schema report (http://www.sql-workbench.net) 
  to an initial LiquiBase (http://www.liquibase.org) changeset.
  
  The change set's author will be "sql-workbench" and the id will be 1
  Everything will be put into a single changeset
-->
<xsl:transform version="2.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform">

<xsl:output encoding="UTF-8" method="xml" indent="yes" standalone="no"/>
<xsl:preserve-space elements="*"/>

<xsl:param name="includeSequences">true</xsl:param>
<xsl:param name="authorName">sql-workbench</xsl:param>
<xsl:param name="useJdbcTypes">false</xsl:param>

<xsl:variable name="newline"><xsl:text>&#10;</xsl:text></xsl:variable>
<xsl:variable name="tab"><xsl:text>&#x09;</xsl:text></xsl:variable>
<xsl:variable name="squote"><xsl:text>&#39;</xsl:text></xsl:variable>
<xsl:variable name="dsquote"><xsl:text>&#39;&#39;</xsl:text></xsl:variable>

<xsl:import href="liquibase_createtable.xslt"/>

<xsl:template match="/schema-report">
  <databaseChangeLog 
       xmlns="http://www.liquibase.org/xml/ns/dbchangelog/1.9" 
       xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" 
       xsi:schemaLocation="http://www.liquibase.org/xml/ns/dbchangelog/1.9 http://www.liquibase.org/xml/ns/dbchangelog/dbchangelog-1.9.xsd">

  <!-- create a single changeset for the initial setup -->
  <changeSet author="{$authorName}" id="1">
  
    <xsl:for-each select="table-def">
      <xsl:sort select="table-name"/>
      
      <!-- the create-table template is defined in liquibase_createtable.xslt -->
      <xsl:call-template name="create-table"/>
      
    </xsl:for-each>  <!-- tables -->
    
    <!-- now process all foreign keys -->
    <xsl:for-each select="table-def/foreign-keys/foreign-key">
    
      <xsl:variable name="base-table" select="../../table-name"/>
      
      <xsl:variable name="target-table" select="references/table-name"/>
      
      <xsl:variable name="pk-columns">
        <xsl:for-each select="source-columns">
          <xsl:value-of select="column"/>
          <xsl:if test="position() &lt; last()"><xsl:text>,</xsl:text></xsl:if>
        </xsl:for-each>
      </xsl:variable>
      
      <xsl:variable name="fk-columns">
        <xsl:for-each select="referenced-columns">
          <xsl:value-of select="column"/>
          <xsl:if test="position() &lt; last()"><xsl:text>,</xsl:text></xsl:if>
        </xsl:for-each>
      </xsl:variable>
      
      <xsl:variable name="fk-name" select="constraint-name"/>
      <addForeignKeyConstraint constraintName="{$fk-name}"
                               baseTableName="{$base-table}" 
                               baseColumnNames="{$pk-columns}" 
                               referencedTableName="{$target-table}" 
                               referencedColumnNames="{$fk-columns}"/>
        
    </xsl:for-each>  <!-- foreign keys -->
    
  </changeSet>
  
  </databaseChangeLog>

</xsl:template>


</xsl:transform>  
