<?xml version="1.0" encoding="ISO-8859-1"?>
<!-- 
  Convert a SQL Workbench/J schema report (http://www.sql-workbench.net) 
  to an initial LiquiBase (http://www.liquibase.org) changeset.
  
  The change set's author will be "sql-workbench" and the id will be 1
  Everything will be put into a single changeset
-->
<xsl:transform version="2.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform">

<xsl:output encoding="UTF-8" method="xml" indent="yes" standalone="no"/>

<xsl:param name="includeSequences">true</xsl:param>

<xsl:variable name="newline"><xsl:text>&#10;</xsl:text></xsl:variable>
<xsl:variable name="tab"><xsl:text>&#x09;</xsl:text></xsl:variable>

<xsl:template match="/">
  <databaseChangeLog 
       xmlns="http://www.liquibase.org/xml/ns/dbchangelog/1.9" 
       xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" 
       xsi:schemaLocation="http://www.liquibase.org/xml/ns/dbchangelog/1.9 http://www.liquibase.org/xml/ns/dbchangelog/dbchangelog-1.9.xsd">

  <!-- create a single changeset for the initial setup -->
  <changeSet author="sql-workbench" id="1">
    <xsl:for-each select="/schema-report/table-def">
    
      <xsl:sort select="@physicalName"/>
      <xsl:variable name="table-name" select="table-name"/>
      
      <createTable tableName="{$table-name}">
      
        <xsl:if test="string-length(comment) &gt; 0">
          <xsl:attribute name="remarks">
            <xsl:value-of select="comment"/>
          </xsl:attribute>
        </xsl:if>

        <!-- find PK name -->
        <xsl:variable name="pk-name">
          <xsl:value-of select="index-def/name[primary-key='true']"/>
        </xsl:variable>
    
        <xsl:variable name="pk-col-count">
          <xsl:value-of select="count(ndex-def/name[primary-key='true']/olumn-list/column)"/>
        </xsl:variable>
        
        <xsl:for-each select="column-def">
          <xsl:sort select="dbms-position"/>
          <xsl:variable name="column-name" select="column-name"/>
          
          <xsl:variable name="data-type">
            <xsl:call-template name="write-data-type">
              <xsl:with-param name="type-id" select="java-sql-type"/>
              <xsl:with-param name="precision" select="dbms-data-size"/>
              <xsl:with-param name="scale" select="dbms-data-digits"/>
            </xsl:call-template>
          </xsl:variable>
          
          <!-- As an alternative use the "native" datatype as reported by the JDBC driver... -->
          <!-- <xsl:variable name="data-type" select="dbms-data-type"/> -->
          
          <column name="{$column-name}" type="{$data-type}">
          
            <xsl:variable name="pk-flag" select="primary-key"/>
            <xsl:variable name="nullable" select="nullable"/>
            <xsl:variable name="type-id" select="java-sql-type"/>

            <xsl:if test="string-length(default-value) &gt; 0">
            
              <!-- defaults for character columns go into a different attribute than other values -->
              <xsl:variable name="character-types">
                <xsl:value-of select="'12;-9;-15;1;'"/>
              </xsl:variable>
              <xsl:if test="contains($character-types, concat($type-id,';'))">
                <xsl:attribute name="defaultValue"> 
                  <xsl:value-of select="default-value"/>
                </xsl:attribute>
              </xsl:if>
              
              <xsl:variable name="numeric-types">
                <xsl:value-of select="'-5;3;4;5;6;7;8;'"/>
              </xsl:variable>
              <xsl:if test="contains($numeric-types, concat($type-id,';'))">
                <xsl:attribute name="defaultValueNumeric"> 
                  <xsl:value-of select="translate(default-value, ' *?[]()ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz', '')"/>
                </xsl:attribute>
              </xsl:if>

              <xsl:variable name="boolean-types">
                <xsl:value-of select="'16;-7;'"/>
              </xsl:variable>
              <xsl:if test="contains($boolean-types, concat($type-id,';'))">
                <xsl:attribute name="defaultValueBoolean"> 
                  <xsl:value-of select="translate(default-value, ' *?[]()ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz', '')"/>
                </xsl:attribute>
              </xsl:if>
            </xsl:if>
            
            <!-- only write remarks if they are defined -->
            <xsl:if test="string-length(comment) &gt; 0">
              <xsl:attribute name="remarks">
                <xsl:value-of select="comment"/>
              </xsl:attribute>
            </xsl:if>
            
            <!-- only write pk flag if it's true -->
            <xsl:if test="($pk-flag = 'true' and $pk-col-count = 1) or $nullable = 'false'">
              <constraints>
              
                <xsl:if test="$nullable = 'false'">
                  <xsl:attribute name="nullable">
                    <xsl:value-of select="$nullable"/>
                  </xsl:attribute>
                </xsl:if>
                
                <xsl:if test="$pk-flag = 'true' and $pk-col-count = 1">
                  <xsl:attribute name="primaryKey">
                    <xsl:value-of select="'true'"/>
                  </xsl:attribute>
                  <xsl:attribute name="primaryKeyName">
                    <xsl:value-of select="$pk-name"/>
                  </xsl:attribute>
                </xsl:if>
              
              </constraints>
            </xsl:if>
            
          </column>
          
        </xsl:for-each> <!-- columns -->
          
      </createTable>

      <!-- 
        now process all index definitions for this table 
      -->
      <xsl:for-each select="index-def">
      
        <xsl:variable name="index-name">
          <xsl:value-of select="name"/>
        </xsl:variable>
        
        <xsl:variable name="unique-flag">
          <xsl:value-of select="unique"/>
        </xsl:variable>

        <!-- 
           Primary keys with a single column are already defined in the table itself
           so we only need to take care of those with more than one column
        -->
        <xsl:if test="primary-key='true' and count(column-list/column) &gt; 1">
          <xsl:variable name="pk-columns">
            <xsl:for-each select="column-list/column">
              <xsl:value-of select="@name"/>
              <xsl:if test="position() &lt; last()"><xsl:text>,</xsl:text></xsl:if>
            </xsl:for-each> 
          </xsl:variable>
          
          <addPrimaryKey tableName="{$table-name}" columnNames="{$pk-columns}" constraintName="{$index-name}"/>
        </xsl:if>
                
        <xsl:if test="primary-key='false'">
          <createIndex indexName="{$index-name}" tableName="{$table-name}" unique="{$unique-flag}">
          
            <xsl:for-each select="column-list/column">
              <column>
                <xsl:attribute name="name">
                  <!-- 
                    Power*Architect stores the logical name of the column in the index definition's physical name attribute
                    As the physical name cannot be used for the index definition (might not be a legal column name) we 
                    need to retrieve the real physical name from the table's column definition
                  -->
                  <xsl:value-of select="@name"/>
                </xsl:attribute>
              </column>
            </xsl:for-each> <!-- index columns -->
            
          </createIndex>
       </xsl:if>
        
      </xsl:for-each> <!-- table index -->
      
    </xsl:for-each>  <!-- tables -->
    
    <!-- now process all foreign keys -->
    <xsl:for-each select="/schema-report/table-def/foreign-keys/foreign-key">
    
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

<!-- 
  Map jdbc data types (from java.sql.Types) to a proper data type
  using scale and precision where approriate.
-->
<xsl:template name="write-data-type">
  <xsl:param name="type-id"/>
  <xsl:param name="precision"/>
  <xsl:param name="scale"/>
  <xsl:choose>
    <xsl:when test="$type-id = 2005">
      <xsl:text>CLOB</xsl:text>
    </xsl:when>
    <xsl:when test="$type-id = 2011">
      <xsl:text>NCLOB</xsl:text>
    </xsl:when>
    <xsl:when test="$type-id = 2004">
      <xsl:text>BLOB</xsl:text>
    </xsl:when>
    <xsl:when test="$type-id = -3">
      <xsl:text>VARBINARY</xsl:text>
    </xsl:when>
    <xsl:when test="$type-id = -4">
      <xsl:text>LONGVARBINARY</xsl:text>
    </xsl:when>
    <xsl:when test="$type-id = -1">
      <xsl:text>LONGVARCHAR</xsl:text>
    </xsl:when>
    <xsl:when test="$type-id = 93">
      <xsl:text>TIMESTAMP</xsl:text>
    </xsl:when>
    <xsl:when test="$type-id = 92">
      <xsl:text>TIME</xsl:text>
    </xsl:when>
    <xsl:when test="$type-id = 91">
      <xsl:text>DATE</xsl:text>
    </xsl:when>
    <xsl:when test="$type-id = 1">
      <xsl:text>CHAR</xsl:text>
    </xsl:when>
    <xsl:when test="$type-id = -15">
      <xsl:text>NCHAR</xsl:text>
    </xsl:when>
    <xsl:when test="$type-id = 4">
      <xsl:text>INTEGER</xsl:text>
    </xsl:when>
    <xsl:when test="$type-id = -5">
      <xsl:text>BIGINT</xsl:text>
    </xsl:when>
    <xsl:when test="$type-id = 5">
      <xsl:text>SMALLINT</xsl:text>
    </xsl:when>
    <xsl:when test="$type-id = -6">
      <xsl:text>TINYINT</xsl:text>
    </xsl:when>
    <xsl:when test="$type-id = 8">
      <xsl:text>DOUBLE</xsl:text>
    </xsl:when>
    <xsl:when test="$type-id = 7">
      <xsl:text>REAL</xsl:text>
    </xsl:when>
    <xsl:when test="$type-id = 6">
      <xsl:text>FLOAT</xsl:text>
    </xsl:when>
    <xsl:when test="$type-id = 16">
      <xsl:text>BOOLEAN</xsl:text>
    </xsl:when>
    <xsl:when test="$type-id = -7">
      <xsl:text>BIT</xsl:text>
    </xsl:when>
    <xsl:when test="$type-id = 2">
      <xsl:text>NUMERIC(</xsl:text><xsl:value-of select="$precision"/><xsl:text>,</xsl:text><xsl:value-of select="$scale"/><xsl:text>)</xsl:text>
    </xsl:when>
    <xsl:when test="$type-id = 3">
      <xsl:text>DECIMAL(</xsl:text><xsl:value-of select="$precision"/><xsl:text>,</xsl:text><xsl:value-of select="$scale"/><xsl:text>)</xsl:text>
    </xsl:when>
    <xsl:when test="$type-id = 12">
      <xsl:text>VARCHAR(</xsl:text><xsl:value-of select="$precision"/><xsl:text>)</xsl:text>
    </xsl:when>
    <xsl:when test="$type-id = -9">
      <xsl:text>NVARCHAR(</xsl:text><xsl:value-of select="$precision"/><xsl:text>)</xsl:text>
    </xsl:when>
    <xsl:otherwise>
        <xsl:text>[</xsl:text><xsl:value-of select="$type-id"/><xsl:text>]</xsl:text>
    </xsl:otherwise>  
  </xsl:choose>
</xsl:template>


</xsl:transform>  
