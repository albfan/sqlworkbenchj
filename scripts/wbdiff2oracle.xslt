<?xml version="1.0" encoding="ISO-8859-1"?>
<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform">

<xsl:output
  encoding="iso-8859-15"
  method="text"
  indent="no"
  standalone="yes"
  omit-xml-declaration="yes"
/>

<!--
  Convert the output of SQL Workbench's WbSchemaDiff command to SQL for Oracle
  Author: Thomas Kellerer
  Thanks to Etienne for his addition and bugfixes
-->

  <xsl:strip-space elements="*"/>

  <xsl:variable name="quote">
    <xsl:text>"</xsl:text>
  </xsl:variable>
  <xsl:variable name="newline">
    <xsl:text>&#10;</xsl:text>
  </xsl:variable>

  <xsl:template match="/">

    <xsl:apply-templates select="/schema-diff/add-table"/>

    <xsl:for-each select="/schema-diff/modify-table">

      <xsl:variable name="table" select="@name"/>

      <xsl:apply-templates select="add-column">
        <xsl:with-param name="table" select="$table"/>
      </xsl:apply-templates>

      <xsl:apply-templates select="remove-column">
        <xsl:with-param name="table" select="$table"/>
      </xsl:apply-templates>

      <xsl:apply-templates select="modify-column">
        <xsl:with-param name="table" select="$table"/>
      </xsl:apply-templates>

      <xsl:apply-templates select="remove-primary-key">
        <xsl:with-param name="table" select="$table"/>
      </xsl:apply-templates>

      <xsl:apply-templates select="add-primary-key">
        <xsl:with-param name="table" select="$table"/>
      </xsl:apply-templates>

      <xsl:apply-templates select="add-index">
        <xsl:with-param name="table" select="$table"/>
      </xsl:apply-templates>

      <xsl:apply-templates select="drop-index">
        <xsl:with-param name="table" select="$table"/>
      </xsl:apply-templates>

      <xsl:for-each select="table-constraints/drop-constraint/constraint-definition">
        <xsl:text>ALTER TABLE </xsl:text>
        <xsl:value-of select="$table"/>
        <xsl:text> DROP CONSTRAINT </xsl:text>
        <xsl:value-of select="@name"/>
        <xsl:text>;</xsl:text>
        <xsl:value-of select="$newline"/>
      </xsl:for-each>
      <xsl:value-of select="$newline"/>

      <xsl:for-each select="table-constraints/modify-constraint/constraint-definition">
        <xsl:text>ALTER TABLE </xsl:text>
        <xsl:value-of select="$table"/>
        <xsl:text> DROP CONSTRAINT </xsl:text>
        <xsl:value-of select="@name"/>
        <xsl:text>;</xsl:text>
        <xsl:value-of select="$newline"/>

        <xsl:text>ALTER TABLE </xsl:text>
        <xsl:value-of select="$table"/>
        <xsl:text> ADD</xsl:text>
        <xsl:if test="@generated-name='true'">
          <xsl:text> CONSTRAINT </xsl:text>
          <xsl:value-of select="@name"/>
        </xsl:if>
        <xsl:text> CHECK </xsl:text>
        <xsl:value-of select="normalize-space(.)"/>
        <xsl:text>;</xsl:text>
        <xsl:value-of select="$newline"/>
      </xsl:for-each>

      <xsl:for-each select="table-constraints/add-constraint/constraint-definition">
        <xsl:text>ALTER TABLE </xsl:text>
        <xsl:value-of select="$table"/>
        <xsl:text> ADD</xsl:text>
        <xsl:if test="@generated-name='true'">
          <xsl:text> CONSTRAINT </xsl:text>
          <xsl:value-of select="@name"/>
        </xsl:if>
        <xsl:text> CHECK </xsl:text>
        <xsl:value-of select="normalize-space(.)"/>
        <xsl:text>;</xsl:text>
        <xsl:value-of select="$newline"/>
      </xsl:for-each>

      <xsl:for-each select="drop-foreign-keys/foreign-key">
        <xsl:call-template name="drop-fk">
          <xsl:with-param name="tablename" select="$table"/>
        </xsl:call-template>
        <xsl:value-of select="$newline"/>
      </xsl:for-each>

      <xsl:for-each select="add-foreign-keys/foreign-key">
        <xsl:call-template name="add-fk">
          <xsl:with-param name="tablename" select="$table"/>
        </xsl:call-template>
        <xsl:value-of select="$newline"/>
      </xsl:for-each>

    </xsl:for-each>

    <xsl:for-each select="/schema-diff/drop-view">
      <xsl:text>DROP VIEW </xsl:text>
      <xsl:value-of select="view-name"/>
      <xsl:text>;</xsl:text>
        <xsl:value-of select="$newline"/>
    </xsl:for-each>

    <xsl:for-each select="/schema-diff/create-view[not(@type)]">
      <xsl:apply-templates select="view-def"/>
    </xsl:for-each>

    <xsl:for-each select="/schema-diff/create-view[@type='MATERIALIZED VIEW']">
        <xsl:call-template name="create-mview"/>
    </xsl:for-each>

    <xsl:for-each select="/schema-diff/update-view[not(@type)]">
      <xsl:apply-templates select="view-def"/>
    </xsl:for-each>

    <xsl:for-each select="/schema-diff/create-proc">
      <xsl:apply-templates select="proc-def"/>
    </xsl:for-each>

  </xsl:template>

  <xsl:template name="create-mview">
    <xsl:variable name="mview-source">
        <xsl:if test="contains(view-def/view-source/text(), 'OR REPLACE')">
            <xsl:value-of select="view-def/view-source"/>
        </xsl:if>
        <xsl:if test="not(contains(view-def/view-source/text(), 'OR REPLACE'))">
            <xsl:call-template name="_replace_text">
              <xsl:with-param name="text" select="view-def/view-source"/>
              <xsl:with-param name="replace" select="'CREATE MATERIALIZED VIEW'"/>
              <xsl:with-param name="by" select="'CREATE OR REPLACE MATERIALIZED VIEW'"/>
            </xsl:call-template>
        </xsl:if>
    </xsl:variable>
    <xsl:value-of select="$mview-source"/>
    <xsl:value-of select="$newline"/>
  </xsl:template>

  <xsl:template name="drop-fk">
     <xsl:param name="tablename"/>
    <xsl:text>ALTER TABLE </xsl:text>
    <xsl:value-of select="$tablename"/>
    <xsl:value-of select="$newline"/>
    <xsl:text>  DROP CONSTRAINT </xsl:text>
    <xsl:value-of select="constraint-name"/>
    <xsl:text>;</xsl:text>
    <xsl:value-of select="$newline"/>
  </xsl:template>

  <xsl:template name="add-fk">
     <xsl:param name="tablename"/>
    <xsl:text>ALTER TABLE </xsl:text>
    <xsl:value-of select="$tablename"/>
    <xsl:value-of select="$newline"/>
    <xsl:text>  ADD CONSTRAINT </xsl:text>
    <xsl:value-of select="constraint-name"/>
    <xsl:value-of select="$newline"/>
    <xsl:text>  FOREIGN KEY (</xsl:text>
    <xsl:for-each select="source-columns/column">
        <xsl:copy-of select="."/>
        <xsl:if test="position() &lt; last()">
          <xsl:text>, </xsl:text>
        </xsl:if>
    </xsl:for-each>
    <xsl:text>)</xsl:text>
    <xsl:value-of select="$newline"/>
    <xsl:text>  REFERENCES </xsl:text>
    <xsl:value-of select="references/table-name"/>
    <xsl:text> (</xsl:text>
    <xsl:for-each select="referenced-columns/column">
        <xsl:copy-of select="."/>
        <xsl:if test="position() &lt; last()">
          <xsl:text>, </xsl:text>
        </xsl:if>
    </xsl:for-each>
    <xsl:text>)</xsl:text>
    <xsl:call-template name="add-fk-action">
      <xsl:with-param name="event-name" select="'ON DELETE'"/>
      <xsl:with-param name="action" select="delete-rule"/>
    </xsl:call-template>
    <xsl:call-template name="add-defer-rule"/>
    <xsl:text>;</xsl:text>
    <xsl:value-of select="$newline"/>
  </xsl:template>

  <xsl:template name="add-fk-action">
    <xsl:param name="event-name"/>
    <xsl:param name="action"/>
    <xsl:if test="$action = 'DELETE' or $action = 'SET NULL'">
      <xsl:value-of select="$newline"/>
      <xsl:text>  </xsl:text>
      <xsl:value-of select="$event-name"/><xsl:text> </xsl:text><xsl:value-of select="$action"/>
    </xsl:if>
  </xsl:template>

  <xsl:template name="add-defer-rule">
    <xsl:variable name="defer" select="deferrable"/>
    <xsl:choose>
      <xsl:when test="$defer='INITIALLY DEFERRED'">
        <xsl:value-of select="$newline"/>
        <xsl:text>  DEFERRABLE INITIALLY DEFERRED</xsl:text>
      </xsl:when>
      <xsl:when test="$defer='INITIALLY IMMEDIATE'">
        <xsl:value-of select="$newline"/>
        <xsl:text>  DEFERRABLE INITIALLY IMMEDIATE</xsl:text>
      </xsl:when>
    </xsl:choose>
  </xsl:template>

  <xsl:template match="proc-def">
    <xsl:value-of select="proc-source"/>
    <xsl:value-of select="$newline"/>
    <xsl:text>/</xsl:text>
    <xsl:value-of select="$newline"/>
  </xsl:template>

  <xsl:template match="add-index">
    <xsl:param name="table"/>
    <xsl:for-each select="index-def">
      <xsl:call-template name="create-index">
        <xsl:with-param name="tablename" select="$table"/>
      </xsl:call-template>
    </xsl:for-each>
  </xsl:template>

  <xsl:template match="drop-index">
    <xsl:param name="table"/>
    <xsl:text>ALTER TABLE </xsl:text>
    <xsl:value-of select="$table"/>
    <xsl:text> DROP INDEX </xsl:text>
    <xsl:value-of select="."/>
    <xsl:text>;</xsl:text>
    <xsl:value-of select="$newline"/>
  </xsl:template>

  <xsl:template match="remove-column">
    <xsl:param name="table"/>
    <xsl:text>ALTER TABLE </xsl:text>
    <xsl:value-of select="$table"/>
    <xsl:text> DROP COLUMN </xsl:text>
    <xsl:value-of select="@name"/>
    <xsl:text>;</xsl:text>
    <xsl:value-of select="$newline"/>
  </xsl:template>

  <xsl:template match="add-column">
    <xsl:param name="table"/>
    <xsl:for-each select="column-def">
      <xsl:variable name="column" select="column-name"/>
      <xsl:variable name="nullable" select="nullable"/>
      <xsl:text>ALTER TABLE </xsl:text>
      <xsl:value-of select="$table"/>
      <xsl:text> ADD </xsl:text>
      <xsl:value-of select="$column"/>
      <xsl:text> </xsl:text>
      <xsl:value-of select="dbms-data-type"/>
      <xsl:if test="string-length(default-value) &gt; 0">
        <xsl:text> DEFAULT </xsl:text>
        <xsl:value-of select="default-value"/>
      </xsl:if>
      <xsl:if test="$nullable = 'false'">
        <xsl:text> NOT NULL</xsl:text>
      </xsl:if>
      <xsl:text>;</xsl:text>
      <xsl:value-of select="$newline"/>
    </xsl:for-each>
  </xsl:template>

<!-- Process the modify-column part -->
  <xsl:template match="modify-column">
    <xsl:param name="table"/>
    <xsl:apply-templates select="new-column-attributes">
      <xsl:with-param name="table" select="$table"/>
      <xsl:with-param name="column" select="@name"/>
      <xsl:with-param name="oldDefault" select="reference-column-definition/default-value"/>
    </xsl:apply-templates>
  </xsl:template>

  <xsl:template match="new-column-attributes">
    <xsl:param name="table"/>
    <xsl:param name="column"/>
    <xsl:param name="oldDefault"/>
    <xsl:if test="string-length(dbms-data-type) &gt; 0">
      <xsl:text>ALTER TABLE </xsl:text>
      <xsl:value-of select="$table"/>
      <xsl:text> MODIFY </xsl:text>
      <xsl:value-of select="$column"/>
      <xsl:text> </xsl:text>
      <xsl:value-of select="dbms-data-type"/>
      <xsl:text>;</xsl:text>
      <xsl:value-of select="$newline"/>
    </xsl:if>
    <xsl:if test="nullable = 'true'">
      <xsl:text>ALTER TABLE </xsl:text>
      <xsl:value-of select="$table"/>
      <xsl:text> MODIFY </xsl:text>
      <xsl:value-of select="$column"/>
      <xsl:text> NULL;</xsl:text>
      <xsl:value-of select="$newline"/>
    </xsl:if>
    <xsl:if test="nullable = 'false'">
      <xsl:text>ALTER TABLE </xsl:text>
      <xsl:value-of select="$table"/>
      <xsl:text> MODIFY </xsl:text>
      <xsl:value-of select="$column"/>
      <xsl:text> NOT NULL;</xsl:text>
      <xsl:value-of select="$newline"/>
    </xsl:if>
    <xsl:if test="string-length(default-value) &gt; 0">
      <xsl:text>ALTER TABLE </xsl:text>
      <xsl:value-of select="$table"/>
      <xsl:text> MODIFY </xsl:text>
      <xsl:value-of select="$column"/>
      <xsl:text> DEFAULT </xsl:text>
      <xsl:value-of select="default-value"/>
      <xsl:text>;</xsl:text>
      <xsl:value-of select="$newline"/>
    </xsl:if>
    <xsl:if test="default-value/@remove = 'true' and not($oldDefault = 'null') ">
      <xsl:text>ALTER TABLE </xsl:text>
      <xsl:value-of select="$table"/>
      <xsl:text> ALTER COLUMN </xsl:text>
      <xsl:value-of select="$column"/>
      <xsl:text> SET DEFAULT NULL;</xsl:text>
      <xsl:value-of select="$newline"/>
    </xsl:if>
    <xsl:if test="string-length(comment) &gt; 0">
      <xsl:text>COMMENT ON COLUMN </xsl:text>
      <xsl:value-of select="$table"/>
      <xsl:text>.</xsl:text>
      <xsl:value-of select="$column"/>
      <xsl:text> IS '</xsl:text>
      <xsl:value-of select="comment"/>
      <xsl:text>';</xsl:text>
      <xsl:value-of select="$newline"/>
    </xsl:if>
  </xsl:template>

<!-- Add primary keys -->
  <xsl:template match="add-primary-key">
    <xsl:param name="table"/>
    <xsl:text>ALTER TABLE </xsl:text>
    <xsl:value-of select="$table"/>
    <xsl:value-of select="$newline"/>
    <xsl:text>  ADD CONSTRAINT </xsl:text>
    <xsl:value-of select="@name"/>
    <xsl:text>  PRIMARY KEY (</xsl:text>
    <xsl:for-each select="column-name">
      <xsl:copy-of select="."/>
      <xsl:if test="position() &lt; last()">
        <xsl:text>,</xsl:text>
      </xsl:if>
    </xsl:for-each>
    <xsl:text>);</xsl:text>
    <xsl:value-of select="$newline"/>
  </xsl:template>

<!-- Remove primary keys -->
  <xsl:template match="remove-primary-key">
    <xsl:param name="table"/>
    <xsl:text>ALTER TABLE </xsl:text>
    <xsl:value-of select="$table"/>
    <xsl:text> DROP PRIMARY KEY;</xsl:text>
    <xsl:value-of select="$newline"/>
  </xsl:template>

  <!-- re-create a view -->
  <xsl:template match="view-def">
    <xsl:text>CREATE OR REPLACE VIEW </xsl:text>
    <xsl:value-of select="view-name"/>
    <xsl:value-of select="$newline"/>
    <xsl:text>(</xsl:text>
    <xsl:value-of select="$newline"/>
    <xsl:for-each select="column-def">
      <xsl:sort select="dbms-position"/>
      <xsl:variable name="orgname" select="column-name"/>
      <xsl:variable name="uppername">
        <xsl:value-of select="translate(column-name,
                                  'abcdefghijklmnopqrstuvwxyz',
                                  'ABCDEFGHIJKLMNOPQRSTUVWXYZ')"/>
      </xsl:variable>
      <xsl:variable name="colname">
        <xsl:choose>
          <xsl:when test="contains(column-name,' ')">
            <xsl:value-of select="concat($quote,column-name,$quote)"/>
          </xsl:when>
          <xsl:when test="$uppername != column-name">
            <xsl:value-of select="concat($quote,column-name,$quote)"/>
          </xsl:when>
          <xsl:otherwise>
            <xsl:value-of select="column-name"/>
          </xsl:otherwise>
        </xsl:choose>
      </xsl:variable>
      <xsl:text>  </xsl:text>
      <xsl:copy-of select="$colname"/>
      <xsl:if test="position() &lt; last()">
        <xsl:text>,</xsl:text>
        <xsl:value-of select="$newline"/>
      </xsl:if>
    </xsl:for-each>
    <xsl:value-of select="$newline"/>
    <xsl:text>)</xsl:text>
    <xsl:value-of select="$newline"/>
    <xsl:text>AS </xsl:text>
    <xsl:copy-of select="normalize-space(view-source)"/>
    <xsl:text>;</xsl:text>
    <xsl:value-of select="$newline"/>
    <xsl:value-of select="$newline"/>
  </xsl:template>

  <xsl:template match="table-def">
    <xsl:variable name="tablename" select="table-name"/>
    <xsl:text>CREATE TABLE </xsl:text>
    <xsl:value-of select="table-name"/>
    <xsl:value-of select="$newline"/>
    <xsl:text>(</xsl:text>
    <xsl:value-of select="$newline"/>

    <xsl:for-each select="column-def">
      <xsl:sort select="dbms-position"/>
      <xsl:text>  </xsl:text>
      <xsl:variable name="colname">
        <xsl:choose>
          <xsl:when test="contains(column-name,' ')">
            <xsl:value-of select="concat($quote,column-name,$quote)"/>
          </xsl:when>
          <xsl:otherwise>
            <xsl:value-of select="column-name"/>
          </xsl:otherwise>
        </xsl:choose>
      </xsl:variable>
      <xsl:variable name="nullable">
        <xsl:if test="nullable = 'false'"> NOT NULL</xsl:if>
      </xsl:variable>
      <xsl:variable name="defaultvalue">
        <xsl:if test="string-length(default-value) &gt; 0">
          <xsl:text> DEFAULT </xsl:text>
          <xsl:value-of select="default-value"/>
        </xsl:if>
      </xsl:variable>
      <xsl:copy-of select="$colname"/>
      <xsl:text> </xsl:text>
      <xsl:value-of select="dbms-data-type"/>
      <xsl:value-of select="$defaultvalue"/>
      <xsl:value-of select="$nullable"/>
      <xsl:if test="position() &lt; last()">
        <xsl:text>,</xsl:text>
        <xsl:value-of select="$newline"/>
      </xsl:if>
    </xsl:for-each>
    <xsl:value-of select="$newline"/>
    <xsl:text>);</xsl:text>
    <xsl:value-of select="$newline"/>

    <xsl:variable name="pkcount">
      <xsl:value-of select="count(column-def[primary-key='true'])"/>
    </xsl:variable>

    <xsl:if test="$pkcount &gt; 0">
      <xsl:text>ALTER TABLE </xsl:text>
      <xsl:value-of select="$tablename"/>
      <xsl:value-of select="$newline"/>
      <xsl:text>  ADD CONSTRAINT </xsl:text>
      <xsl:value-of select="concat('pk_', $tablename)"/>
      <xsl:value-of select="$newline"/>
      <xsl:text>  PRIMARY KEY (</xsl:text>
      <xsl:for-each select="column-def[primary-key='true']">
        <xsl:value-of select="column-name"/>
        <xsl:if test="position() &lt; last()">
          <xsl:text>,</xsl:text>
        </xsl:if>
      </xsl:for-each>
      <xsl:text>);</xsl:text>
      <xsl:value-of select="$newline"/>
      <xsl:value-of select="$newline"/>
    </xsl:if>

    <xsl:for-each select="index-def">
      <xsl:call-template name="create-index">
        <xsl:with-param name="tablename" select="$tablename"/>
      </xsl:call-template>
    </xsl:for-each>
  </xsl:template>

  <xsl:template name="create-index">
    <xsl:param name="tablename"/>
    <xsl:variable name="pk" select="primary-key"/>
    <xsl:if test="$pk = 'false'">
      <xsl:variable name="unique">
        <xsl:if test="unique='true'">UNIQUE </xsl:if>
      </xsl:variable>
      <xsl:text>CREATE </xsl:text>
      <xsl:value-of select="$unique"/>
      <xsl:text>INDEX </xsl:text>
      <xsl:value-of select="name"/>
      <xsl:text> ON </xsl:text>
      <xsl:value-of select="$tablename"/>
      <xsl:text> (</xsl:text>
      <xsl:value-of select="index-expression"/>
      <xsl:text>);</xsl:text>
      <xsl:value-of select="$newline"/>
    </xsl:if>
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
