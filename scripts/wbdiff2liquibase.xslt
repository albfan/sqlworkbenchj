<?xml version="1.0" encoding="ISO-8859-1"?>
<!-- 
  Convert a SQL Workbench/J schema report (http://www.sql-workbench.net) 
  to an initial LiquiBase (http://www.liquibase.org) changeset.
  
  The change set's author will be "sql-workbench" and the id will be 1
  Everything will be put into a single changeset
-->
<xsl:transform version="2.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform">

<xsl:output encoding="UTF-8" method="xml" indent="yes" standalone="no"/>

<xsl:template match="/schema-diff">
  <databaseChangeLog 
       xmlns="http://www.liquibase.org/xml/ns/dbchangelog/1.9" 
       xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" 
       xsi:schemaLocation="http://www.liquibase.org/xml/ns/dbchangelog/1.9 http://www.liquibase.org/xml/ns/dbchangelog/dbchangelog-1.9.xsd">

      <!-- create a single changeset for the complete diff -->
      <changeSet author="sql-workbench" id="[change_me]">

        <xsl:import href="liquibase_createtable.xslt"/>
        
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
          
        </xsl:for-each> <!-- alter tables -->
        
        <xsl:for-each select="drop-table/table-name">
          <xsl:variable name="table-name" select="."/>
          <dropTable tableName="{$table-name}"/>
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