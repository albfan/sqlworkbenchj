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

      <xsl:call-template name="add-fk">
        <xsl:with-param name="tablename" select="../../table-name"/>
      </xsl:call-template>
      
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
        <xsl:if test="string-length(sequence-properties/property[@name='INCREMENT']/@value) &gt; 0">
          <xsl:attribute name="incrementBy">
            <xsl:value-of select="sequence-properties/property[@name='INCREMENT']/@value"/>
          </xsl:attribute>
        </xsl:if>
        <xsl:if test="string-length(sequence-properties/property[@name='CYCLE']/@value) &gt; 0">
          <xsl:attribute name="cycle">
            <xsl:value-of select="sequence-properties/property[@name='CYCLE']/@value"/>
          </xsl:attribute>
        </xsl:if>
        <xsl:if test="string-length(sequence-properties/property[@name='MIN_VALUE']/@value) &gt; 0">
          <xsl:attribute name="minValue">
            <xsl:value-of select="sequence-properties/property[@name='MIN_VALUE']/@value"/>
          </xsl:attribute>
        </xsl:if>
        <xsl:if test="string-length(sequence-properties/property[@name='ORDERED']/@value) &gt; 0">
          <xsl:attribute name="ordered">
            <xsl:value-of select="sequence-properties/property[@name='ORDERED']/@value"/>
          </xsl:attribute>
        </xsl:if>
      </createSequence>
      <xsl:if test="string-length(sequence-properties/property[@name='OWNED_BY']/@value) &gt; 0">
        <sql dbms="postgresql">
          <xsl:text>ALTER SEQUENCE </xsl:text><xsl:value-of select="$seq-name"/><xsl:text> OWNED BY </xsl:text><xsl:value-of select="sequence-properties/property[@name='OWNED_BY']/@value"/><xsl:text>;</xsl:text>
        </sql>
      </xsl:if>
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
