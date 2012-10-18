<?xml version="1.0" encoding="ISO-8859-1"?>
<!--
  Convert a SQL Workbench/J schema report (http://www.sql-workbench.net)
  to an initial LiquiBase (http://www.liquibase.org) changeset.

  The change set's author will be "sql-workbench" (but can be changed by setting
  the XSLT parameter "authorName" and the changeSet's id will be 1

  Everything will be put into a single changeset
-->
<xsl:transform version="2.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform">

<xsl:output encoding="UTF-8" method="xml" indent="yes" cdata-section-elements="createView"/>

<xsl:preserve-space elements="*"/>
<xsl:strip-space elements="createView"/>

<xsl:param name="schema.owner"/>
<xsl:param name="tablespace.table"/>
<xsl:param name="tablespace.index"/>
<xsl:param name="authorName">sql-workbench</xsl:param>
<xsl:param name="useJdbcTypes">false</xsl:param>

<xsl:import href="liquibase_common.xslt"/>

<xsl:template match="/schema-report">

  <databaseChangeLog
       xmlns="http://www.liquibase.org/xml/ns/dbchangelog"
       xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
       xsi:schemaLocation="http://www.liquibase.org/xml/ns/dbchangelog http://www.liquibase.org/xml/ns/dbchangelog/dbchangelog-2.0.xsd">

  <!-- create a single changeset for the initial setup -->
  <changeSet author="{$authorName}" id="1">

    <xsl:for-each select="table-def">
      <xsl:sort select="table-name"/>

      <!-- the create-table template is defined in liquibase_common.xslt -->
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

    <xsl:for-each select="proc-def">
      <changeSet>
        <createProcedure>
           <xsl:text disable-output-escaping="yes">&lt;![CDATA[</xsl:text>
           <xsl:value-of disable-output-escaping="yes" select="proc-source"/>
           <xsl:text disable-output-escaping="yes">]]&gt;</xsl:text>
        </createProcedure>
      </changeSet>
    </xsl:for-each>

    <xsl:for-each select="view-def">
      <xsl:variable name="view-name" select="@name"/>
      <createView viewName="{$view-name}">
        <xsl:if test="string-length($schema.owner) &gt; 0">
          <xsl:attribute name="schemaName">
            <xsl:value-of select="$schema.owner"/>
          </xsl:attribute>
        </xsl:if>
        <xsl:value-of disable-output-escaping="yes" select="view-source"/>
      </createView>
    </xsl:for-each>

    <xsl:for-each select="sequence-def">
      <xsl:variable name="seq-name" select="@name"/>
      <createSequence sequenceName="{$seq-name}">
        <xsl:if test="string-length($schema.owner) &gt; 0">
          <xsl:attribute name="schemaName">
            <xsl:value-of select="$schema.owner"/>
          </xsl:attribute>
        </xsl:if>
      </createSequence>
    </xsl:for-each>

  </changeSet>

  </databaseChangeLog>

<xsl:message>
**************************************************
*** You might want to adjust the changeset id! ***
**************************************************
</xsl:message>

</xsl:template>


</xsl:transform>
