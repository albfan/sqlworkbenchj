<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
        xmlns="http://www.liquibase.org/xml/ns/dbchangelog"
        xmlns:wb-string-util="workbench.util.StringUtil">

<xsl:template name="create-table">
  <xsl:variable name="squote"><xsl:text>&#39;</xsl:text></xsl:variable>
  <xsl:variable name="dsquote"><xsl:text>&#39;&#39;</xsl:text></xsl:variable>

  <xsl:variable name="table-name" select="table-name"/>
  <xsl:variable name="pk-col-count">
    <xsl:value-of select="count(column-def[primary-key='true'])"/>
  </xsl:variable>

  <xsl:element name="createTable">
    <xsl:attribute name="tableName"><xsl:value-of select="$table-name"/></xsl:attribute>
    <xsl:if test="string-length($schema.owner) &gt; 0">
      <xsl:attribute name="schemaName">
         <xsl:value-of select="$schema.owner"/>
      </xsl:attribute>
    </xsl:if>
    <xsl:if test="string-length($tablespace.table) &gt; 0">
      <xsl:attribute name="tablespace">
         <xsl:value-of select="$tablespace.table"/>
      </xsl:attribute>
    </xsl:if>

    <xsl:if test="string-length(comment) &gt; 0">
      <xsl:attribute name="remarks">
        <xsl:call-template name="_replace_text">
            <xsl:with-param name="text" select="comment"/>
            <xsl:with-param name="replace" select="$squote"/>
            <xsl:with-param name="by" select="$dsquote"/>
        </xsl:call-template>
      </xsl:attribute>
    </xsl:if>

    <!-- find PK name -->
    <xsl:variable name="pk-name">
      <xsl:value-of select="index-def[primary-key='true']/name"/>
    </xsl:variable>

    <xsl:for-each select="column-def">
      <xsl:sort select="dbms-position"/>

      <xsl:variable name="column-name" select="@name"/>

      <xsl:variable name="data-type">
        <xsl:if test="$useJdbcTypes = 'true'">
          <xsl:call-template name="write-data-type">
            <xsl:with-param name="type-id" select="java-sql-type"/>
            <xsl:with-param name="dbms-type" select="dbms-data-type"/>
            <xsl:with-param name="precision" select="dbms-data-size"/>
            <xsl:with-param name="scale" select="dbms-data-digits"/>
          </xsl:call-template>
        </xsl:if>

        <xsl:if test="$useJdbcTypes = 'false'">
          <xsl:value-of select="dbms-data-type"/>
        </xsl:if>
      </xsl:variable>

      <xsl:element name="column">
        <xsl:attribute name="name"><xsl:value-of select="$column-name"/></xsl:attribute>
        <xsl:attribute name="type"><xsl:value-of select="$data-type"/></xsl:attribute>

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
              <xsl:value-of select="wb-string-util:trimQuotes(default-value)"/>
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
              <xsl:value-of select="default-value"/>
            </xsl:attribute>
          </xsl:if>
        </xsl:if>

        <!-- only write remarks if they are defined -->
        <xsl:if test="string-length(comment) &gt; 0">
          <xsl:attribute name="remarks">
            <xsl:call-template name="_replace_text">
               <xsl:with-param name="text" select="comment"/>
               <xsl:with-param name="replace" select="$squote"/>
               <xsl:with-param name="by" select="$dsquote"/>
            </xsl:call-template>
          </xsl:attribute>
        </xsl:if>

        <xsl:if test="$useJdbcTypes = 'true'">
            <xsl:if test="auto-increment = 'true'">
              <xsl:attribute name="autoIncrement">
                <xsl:value-of select="'true'"/>
              </xsl:attribute>
            </xsl:if>
        </xsl:if>

        <xsl:if test="($pk-flag = 'true' and $pk-col-count = 1) or $nullable = 'false'">
          <xsl:element name="constraints">

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
          </xsl:element>
        </xsl:if>

      </xsl:element>

    </xsl:for-each> <!-- columns -->

  </xsl:element>

  <!--
    now process all index definitions for this table
  -->
  <xsl:for-each select="index-def">
    <!-- There is no way to create an Oracle DOMAIN index with Liquibase -->
    <xsl:if test="type != 'DOMAIN'">
      <xsl:call-template name="create-index">
         <xsl:with-param name="table-name" select="$table-name"/>
         <xsl:with-param name="pk-col-count" select="$pk-col-count"/>
      </xsl:call-template>
    </xsl:if>
  </xsl:for-each> <!-- index-def -->

  <xsl:for-each select="table-constraints/constraint-definition[@type='check']">
    <xsl:variable name="condition">
        <xsl:value-of select="normalize-space(.)"/>
    </xsl:variable>
    <xsl:element name="sql">ALTER TABLE <xsl:value-of select="$table-name"/> ADD CONSTRAINT <xsl:value-of select="@name"/> CHECK <xsl:value-of select="normalize-space(.)"/></xsl:element>
  </xsl:for-each>

</xsl:template>

<xsl:template name="create-index">
  <xsl:param name="table-name"/>
  <xsl:param name="pk-col-count"/>

  <xsl:variable name="index-name">
    <xsl:value-of select="name"/>
  </xsl:variable>

  <xsl:variable name="unique-flag">
    <xsl:value-of select="unique"/>
  </xsl:variable>

  <!--
     Primary keys with a single column are already defined in the table itself
     so we only need to take care of those with more than one column.

     Oracle supports (non-unique) index definitions that have more columns than the PK
     to be used for enforcing the PK. No addPrimaryKey should be generated for those indexes.

     Only indexes that are marked as PK and have the same number of columns as the PK itself should be considered
     as the index for the PK.

     Checking for PK indexes ensures that a unique constraint is not added twice:
     once for the PK columns and once for the unique index that is present
  -->
  <xsl:if test="primary-key='true' and count(column-list/column) &gt; 1 and count(column-list/column) = $pk-col-count">
    <xsl:variable name="pk-columns">
      <xsl:for-each select="column-list/column">
        <xsl:value-of select="@name"/>
        <xsl:if test="position() &lt; last()"><xsl:text>,</xsl:text></xsl:if>
      </xsl:for-each>
    </xsl:variable>

    <xsl:element name="addPrimaryKey">
      <xsl:attribute name="tableName"><xsl:value-of select="$table-name"/></xsl:attribute>
      <xsl:attribute name="columnNames"><xsl:value-of select="$pk-columns"/></xsl:attribute>
      <xsl:attribute name="constraintName"><xsl:value-of select="$index-name"/></xsl:attribute>
    </xsl:element>
  </xsl:if>

  <xsl:if test="primary-key='false' or count(column-list/column) != $pk-col-count">
    <xsl:element name="createIndex">
      <xsl:attribute name="indexName"><xsl:value-of select="$index-name"/></xsl:attribute>
      <xsl:attribute name="tableName"><xsl:value-of select="$table-name"/></xsl:attribute>
      <xsl:attribute name="unique"><xsl:value-of select="$unique-flag"/></xsl:attribute>
      <xsl:if test="string-length($schema.owner) &gt; 0">
        <xsl:attribute name="schemaName">
           <xsl:value-of select="$schema.owner"/>
        </xsl:attribute>
      </xsl:if>
      <xsl:if test="string-length($tablespace.index) &gt; 0">
        <xsl:attribute name="tablespace">
           <xsl:value-of select="$tablespace.index"/>
        </xsl:attribute>
      </xsl:if>

      <xsl:for-each select="column-list/column">
        <xsl:element name="column">
          <xsl:attribute name="name">
            <xsl:value-of select="@name"/>
          </xsl:attribute>
        </xsl:element>
      </xsl:for-each> <!-- index columns -->
    </xsl:element>
  </xsl:if>
</xsl:template>

<xsl:template name="add-fk">
  <xsl:param name="tablename"/>

  <xsl:variable name="fk-name" select="constraint-name"/>
  <xsl:variable name="referenced-table" select="references/table-name"/>

  <xsl:variable name="base-columns">
    <xsl:for-each select="source-columns/column">
      <xsl:copy-of select="."/>
      <xsl:if test="position() &lt; last()"><xsl:text>,</xsl:text></xsl:if>
    </xsl:for-each>
  </xsl:variable>

  <xsl:variable name="referenced-columns">
    <xsl:for-each select="referenced-columns/column">
      <xsl:copy-of select="."/>
      <xsl:if test="position() &lt; last()"><xsl:text>,</xsl:text></xsl:if>
    </xsl:for-each>
  </xsl:variable>

  <xsl:variable name="update-rule" select="update-rule/text()"/>
  <xsl:variable name="delete-rule" select="delete-rule/text()"/>
  <xsl:variable name="deferrable-value" select="deferrable/@jdbcValue"/>

  <xsl:element name="addForeignKeyConstraint">
    <xsl:attribute name="constraintName"><xsl:value-of select="$fk-name"/></xsl:attribute>
    <xsl:attribute name="baseTableName"><xsl:value-of select="$tablename"/></xsl:attribute>
    <xsl:attribute name="baseColumnNames"><xsl:value-of select="$base-columns"/></xsl:attribute>
    <xsl:attribute name="referencedTableName"><xsl:value-of select="$referenced-table"/></xsl:attribute>
    <xsl:attribute name="referencedColumnNames"><xsl:value-of select="$referenced-columns"/></xsl:attribute>

    <xsl:if test="$delete-rule != 'NO ACTION' and $delete-rule != 'RESTRICT'">
      <xsl:attribute name="onDelete">
        <xsl:value-of select="$delete-rule"/>
      </xsl:attribute>
    </xsl:if>
    <xsl:if test="$update-rule != 'NO ACTION' and $update-rule != 'RESTRICT'">
      <xsl:attribute name="onUpdate">
        <xsl:value-of select="$update-rule"/>
      </xsl:attribute>
    </xsl:if>
    <!-- constant values for the deferrability:
         7 = not deferrable
         6 = initially immediate
         5 = initially deferred
    -->
    <xsl:if test="$deferrable-value = 5 or $deferrable-value = 6">
      <xsl:attribute name="deferrable">
        <xsl:value-of select="'true'"/>
      </xsl:attribute>
      <xsl:if test="$deferrable-value = 5">
        <xsl:attribute name="initiallyDeferred">
          <xsl:value-of select="'true'"/>
        </xsl:attribute>
      </xsl:if>
      <xsl:if test="$deferrable-value = 6">
        <xsl:attribute name="initiallyDeferred">
          <xsl:value-of select="'false'"/>
        </xsl:attribute>
      </xsl:if>
    </xsl:if>
  </xsl:element>
</xsl:template>

<xsl:template match="sequence-def">
  <xsl:variable name="seq-name" select="@name"/>
  <xsl:variable name="max-value" select="sequence-properties/property[@name='MAX_VALUE']/@value"/>

  <xsl:element name="createSequence">
    <xsl:attribute name="sequenceName"><xsl:value-of select="$seq-name"/></xsl:attribute>
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
    <xsl:if test="string-length($max-value) &gt; 0 and $max-value != '9223372036854775807' and substring($max-value, 1, 27) != '999999999999999999999999999'">
      <xsl:attribute name="maxValue">
        <xsl:value-of select="sequence-properties/property[@name='MAX_VALUE']/@value"/>
      </xsl:attribute>
    </xsl:if>
    <xsl:if test="$useOrderedSequence = 'true'">
      <xsl:if test="string-length(sequence-properties/property[@name='ORDERED']/@value) &gt; 0">
        <xsl:attribute name="ordered">
          <xsl:value-of select="sequence-properties/property[@name='ORDERED']/@value"/>
        </xsl:attribute>
      </xsl:if>
    </xsl:if>
  </xsl:element>
  <xsl:if test="string-length(sequence-properties/property[@name='OWNED_BY']/@value) &gt; 0">
    <xsl:element name="sql">
      <xsl:attribute name="dbms">postgresql</xsl:attribute>
      <xsl:text>ALTER SEQUENCE </xsl:text><xsl:value-of select="$seq-name"/><xsl:text> OWNED BY </xsl:text><xsl:value-of select="sequence-properties/property[@name='OWNED_BY']/@value"/><xsl:text>;</xsl:text>
    </xsl:element>
  </xsl:if>
</xsl:template>

<!--
  Map jdbc data types (from java.sql.Types) to a proper data type
  using scale and precision where approriate.
-->
<xsl:template name="write-data-type">
  <xsl:param name="type-id"/>
  <xsl:param name="dbms-type"/>
  <xsl:param name="precision"/>
  <xsl:param name="scale"/>
  <xsl:choose>
    <xsl:when test="$type-id = 2005 or ($type-id = 12 and $precision = 2147483647)">
      <xsl:text>CLOB</xsl:text>
    </xsl:when>
    <xsl:when test="$type-id = 2011">
      <xsl:text>NCLOB</xsl:text>
    </xsl:when>
    <xsl:when test="$type-id = 2004 or $type-id = -2">
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
      <xsl:text>CHAR(</xsl:text><xsl:value-of select="$precision"/><xsl:text>)</xsl:text>
    </xsl:when>
    <xsl:when test="$type-id = -15">
      <xsl:text>NCHAR(</xsl:text><xsl:value-of select="$precision"/><xsl:text>)</xsl:text>
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
    <xsl:when test="$type-id = 3 or $type-id = 2">
      <xsl:if test="$scale &gt; 0">
        <xsl:text>DECIMAL(</xsl:text><xsl:value-of select="$precision"/><xsl:text>,</xsl:text><xsl:value-of select="$scale"/><xsl:text>)</xsl:text>
      </xsl:if>
      <xsl:if test="$scale = 0 and $precision &gt; 10">
        <xsl:text>BIGINT</xsl:text>
      </xsl:if>
      <xsl:if test="$scale = 0 and $precision &lt;= 10">
        <xsl:text>INTEGER</xsl:text>
      </xsl:if>
    </xsl:when>
    <xsl:when test="$type-id = 12">
      <xsl:text>VARCHAR(</xsl:text><xsl:value-of select="$precision"/><xsl:text>)</xsl:text>
    </xsl:when>
    <xsl:when test="$type-id = -9">
      <xsl:text>NVARCHAR(</xsl:text><xsl:value-of select="$precision"/><xsl:text>)</xsl:text>
    </xsl:when>
    <xsl:when test="$mapXMLToClob = 'true' and $type-id = 1111 and ($dbms-type='XML' or $dbms-type = 'XMLTYPE')">
      <xsl:text>CLOB</xsl:text>
    </xsl:when>
    <xsl:when test="$mapXMLToClob = 'false' and $type-id = 1111 and ($dbms-type='XML' or $dbms-type = 'XMLTYPE')">
      <xsl:value-of select="$dbms-type"/>
    </xsl:when>
    <xsl:otherwise>
        <xsl:text>$dbms-type</xsl:text>
    </xsl:otherwise>
  </xsl:choose>
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