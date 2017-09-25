<?xml version="1.0" encoding="ISO-8859-1"?>
<!--
  Convert a SQL Workbench/J schema report (http://www.sql-workbench.net)
  to a LiquiBase (http://www.liquibase.org) changeLog.

  Each table will be placed in a separate changeSet
  All FKs will be in a single changeSet
  Each view will be placed into a separate changeSet
  Each sequence will be placed into a separate changeSet

-->
<xsl:transform version="2.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform">

<xsl:output encoding="UTF-8" method="xml" indent="yes" cdata-section-elements="createView"/>

<xsl:preserve-space elements="*"/>
<xsl:strip-space elements="createView"/>

<xsl:param name="schema.owner"/>
<xsl:param name="tablespace.table"/>
<xsl:param name="tablespace.index"/>
<xsl:param name="authorName">sql-workbench</xsl:param>
<xsl:param name="idPrefix">initial-</xsl:param>
<xsl:param name="useJdbcTypes">true</xsl:param>
<xsl:param name="mapXMLToClob">true</xsl:param>
<xsl:param name="useOrderedSequence">false</xsl:param>

<xsl:variable name="newline">
  <xsl:text>&#10;</xsl:text>
</xsl:variable>

<xsl:import href="liquibase_common.xslt"/>

<xsl:template match="/schema-report">

  <databaseChangeLog
       xmlns="http://www.liquibase.org/xml/ns/dbchangelog"
       xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
       xsi:schemaLocation="http://www.liquibase.org/xml/ns/dbchangelog http://www.liquibase.org/xml/ns/dbchangelog/dbchangelog-3.0.xsd">

    <xsl:for-each select="table-def">
      <xsl:sort select="table-name"/>

      <xsl:variable name="id" select="position()"/>
      <!-- one changeset for each table -->
      <changeSet author="{$authorName}" id="{$idPrefix}{$id}">
      <!-- the create-table template is defined in liquibase_common.xslt -->
        <xsl:call-template name="create-table"/>
      </changeSet>

    </xsl:for-each>  <!-- tables -->

    <xsl:for-each select="table-def/foreign-keys/foreign-key">
      <xsl:variable name="id" select="position()"/>
      <changeSet author="{$authorName}" id="{$idPrefix}fk-{$id}">
        <xsl:call-template name="add-fk">
          <xsl:with-param name="tablename" select="../../table-name"/>
        </xsl:call-template>
      </changeSet>
    </xsl:for-each>


    <xsl:for-each select="proc-def">
      <xsl:variable name="id" select="position()"/>
      <changeSet author="{$authorName}" id="{$idPrefix}proc-{$id}">
        <createProcedure>
           <xsl:text disable-output-escaping="yes">&lt;![CDATA[</xsl:text>
           <xsl:value-of disable-output-escaping="yes" select="proc-source"/>
           <xsl:text disable-output-escaping="yes">]]&gt;</xsl:text>
        </createProcedure>
      </changeSet>
    </xsl:for-each>

    <xsl:for-each select="view-def">
      <xsl:variable name="view-name" select="@name"/>
      <xsl:variable name="id" select="position()"/>
      <!-- one changeset for each table -->
      <changeSet author="{$authorName}" id="{$idPrefix}view-{$id}">
        <createView viewName="{$view-name}">
          <xsl:if test="string-length($schema.owner) &gt; 0">
            <xsl:attribute name="schemaName">
              <xsl:value-of select="$schema.owner"/>
            </xsl:attribute>
          </xsl:if>
          <xsl:value-of select="$newline"/>
          <xsl:value-of disable-output-escaping="yes" select="view-source"/>
        </createView>
      </changeSet>
    </xsl:for-each>

    <xsl:for-each select="sequence-def">
      <xsl:variable name="id" select="position()"/>
      <!-- one changeset for each table -->
      <changeSet author="{$authorName}" id="{$idPrefix}sequence-{$id}">
        <xsl:apply-templates select="."/>
      </changeSet>
    </xsl:for-each>

  </databaseChangeLog>

<xsl:message>
**************************************************
*** You might want to adjust the changeset id! ***
**************************************************
</xsl:message>

</xsl:template>


</xsl:transform>
