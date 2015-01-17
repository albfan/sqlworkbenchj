<?xml version="1.0" encoding="ISO-8859-1"?>
<!-- 
  Convert a SQL Workbench/J schema diff (http://www.sql-workbench.net) 
  to a LiquiBase (http://www.liquibase.org) changeset.
-->
<xsl:transform version="2.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform">

<xsl:output encoding="UTF-8" method="xml" indent="yes" standalone="no"/>

<xsl:param name="authorName">sql-workbench</xsl:param>
<xsl:param name="useJdbcTypes">false</xsl:param>

<xsl:variable name="schema-owner">${schema.owner}</xsl:variable>

<xsl:import href="liquibase_common.xslt"/>

<xsl:template match="/schema-diff">
  <databaseChangeLog 
       xmlns="http://www.liquibase.org/xml/ns/dbchangelog/1.9" 
       xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" 
       xsi:schemaLocation="http://www.liquibase.org/xml/ns/dbchangelog/1.9 http://www.liquibase.org/xml/ns/dbchangelog/dbchangelog-1.9.xsd">

      <!-- create a single changeset for the complete diff -->
      <changeSet author="{$authorName}" id="[change_me]">

        <xsl:for-each select="add-table/table-def">
          <xsl:call-template name="create-table"/>
        </xsl:for-each>
      
        <xsl:for-each select="modify-table">
        
          <xsl:sort select="@name"/>
          <xsl:variable name="table-name" select="@name"/>
          
          <xsl:for-each select="modify-column">
          
            <xsl:variable name="ref-datatype" select="reference-column-definition/dbms-data-type"/>
            <xsl:variable name="col-name" select="@name"/>
            <xsl:variable name="null-flag" select="nullable"/>

            <xsl:if test="count(dbms-data-type) &gt; 0">
              <modifyColumn tableName="{$table-name}">
                <column name="{$col-name}">
                  <xsl:attribute name="type">
                    <xsl:value-of select="dbms-data-type"/>
                  </xsl:attribute>
                </column>
              </modifyColumn>
            </xsl:if>
                
            <xsl:if test="$null-flag = 'false'">
              <dropNotNullConstraint tableName="{$table-name}" columnName="{$col-name}" columnDataType="{$ref-datatype}"/>
            </xsl:if>
            <xsl:if test="$null-flag = 'true'">
              <dropNotNullConstraint tableName="{$table-name}" columnName="{$col-name}" columnDataType="{$ref-datatype}"/>
            </xsl:if>
            
          </xsl:for-each> <!-- table columns -->

          <xsl:for-each select="drop-foreign-keys/foreign-key">
            <xsl:variable name="fk-name" select="constraint-name"/>
            <dropForeignKeyConstraint constraintName="{$fk-name}" baseTableName="{$table-name}"/>
          </xsl:for-each>

          <xsl:for-each select="add-foreign-keys/foreign-key">
            <xsl:call-template name="add-fk">
              <xsl:with-param name="tablename" select="$table-name"/>
            </xsl:call-template>
          </xsl:for-each>

        </xsl:for-each> <!-- alter tables -->
        
        <xsl:for-each select="drop-table/table-name">
          <xsl:variable name="table-name" select="."/>
          <dropTable schemaName="{$schema-owner}" tableName="{$table-name}"/>
        </xsl:for-each>

        <xsl:for-each select="create-sequence/sequence-def">
           <xsl:variable name="seq-name" select="@name"/>
           <createSequence schemaName="{$schema-owner}" sequenceName="{$seq-name}"/>
        </xsl:for-each>
        
        <xsl:for-each select="drop-sequence">
           <xsl:variable name="seq-name" select="@name"/>
           <dropSequence schemaName="{$schema-owner}" sequenceName="{$seq-name}"/>
        </xsl:for-each>

      </changeSet>

  </databaseChangeLog>

<xsl:message>
***************************************************
*** Please don't forget to adjust changeset id! ***
***************************************************
</xsl:message> 
  
</xsl:template>
  
</xsl:transform>