<?xml version="1.0" encoding="ISO-8859-1"?>
<xsl:stylesheet
     version="1.0" 
     xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
>

<xsl:output 
  encoding="iso-8859-15" 
  method="xml" 
  indent="yes" 
  standalone="yes"	
  omit-xml-declaration="yes"
/>

<xsl:variable name="newline"><xsl:text>&#10;</xsl:text></xsl:variable>
<xsl:variable name="tab"><xsl:text>&#x09;</xsl:text></xsl:variable>

<xsl:template match="/">
  <xsl:variable name="table" select="/wb-export/table-def/table-name"/>
    
  <xsl:text>  </xsl:text><changeSet author="CHANGEME" id="CHANGEME">
  <xsl:value-of select="$newline"/>
  
  <!-- Write the data rows -->
  <xsl:for-each select="/wb-export/data/row-data">
    <xsl:text>    </xsl:text><insert tableName="{$table}">
    <xsl:value-of select="$newline"/>
        
    <xsl:for-each select="column-data">
    
      <xsl:variable name="col-index" select="@index"/>
      <xsl:variable name="column" select="/wb-export/table-def/column-def[@index=$col-index]/column-name"/>
      <xsl:variable name="type-name" select="/wb-export/table-def/column-def[@index=$col-index]/java-sql-type-name"/>
      
      <xsl:variable name="value">
        <xsl:choose>
          <xsl:when test="@null='true'">
            <xsl:value-of select="'NULL'"/>
          </xsl:when>
          <xsl:otherwise>
            <xsl:value-of select="."/>
          </xsl:otherwise>
        </xsl:choose>
      </xsl:variable> 
            
      <xsl:text>      </xsl:text>
      <xsl:choose>
        <xsl:when test="($type-name = 'VARCHAR') or ($type-name = 'CHAR') or ($type-name = 'NCHAR') or ($type-name = 'NVARCHAR') or ($type-name = 'CLOB') or ($type-name = 'NCLOB')">
          <column name="{$column}" value="{$value}"/>
        </xsl:when>
        <xsl:when test="($type-name = 'BOOLEAN')">
          <column name="{$column}" valueBoolean="{$value}"/>
        </xsl:when>
        <xsl:when test="($type-name = 'DATE') or ($type-name = 'TIMESTAMP')">
          <column name="{$column}" valueDate="{$value}"/>
        </xsl:when>
        <xsl:otherwise>
          <column name="{$column}" valueNumeric="{$value}"/>
        </xsl:otherwise>
      </xsl:choose>
      <xsl:value-of select="$newline"/>
      
    </xsl:for-each>
    
    <xsl:text>    </xsl:text></insert>
    <xsl:value-of select="$newline"/>
  </xsl:for-each>
    
  </changeSet>
    
</xsl:template>

</xsl:stylesheet>
