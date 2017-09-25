<?xml version="1.0" encoding="ISO-8859-1"?>

<xsl:transform
     version="1.0"
     xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
>
<xsl:output
  encoding="iso-8859-15"
  method="html"
  indent="yes"
  omit-xml-declaration="yes"
  doctype-public="-//W3C//DTD HTML 4.01 Transitional//EN"
/>

<xsl:variable name="ref-db" select="concat('Schema=', /schema-diff/compare-settings/reference-schema, ', User=', /schema-diff/reference-connection/database-user, ', url=', /schema-diff/reference-connection/jdbc-url)"/>
<xsl:variable name="target-db" select="concat('Schema=', /schema-diff/compare-settings/target-schema, ', User=', /schema-diff/target-connection/database-user, ', url=', /schema-diff/target-connection/jdbc-url)"/>

<xsl:template match="/">
  <html>
  <head>
    <style type="text/css">
     
      body {
        font-size:12px;
      }
      
      h1 {
        margin-bottom:10px;
        font-weight:bold;
        font-size:125%;
      }
      
      h2 {
        border-bottom: 2px solid gray;
        background-color:#F5F5FF;
        font-size:115%;
      }
      
      h3 {
        font-size:100%;
        font-style: bold;
      }

      .tableNameHeading {
           margin-top:1em;
           margin-bottom:1em;
           font-size:100%;
           font-weight:bold;
       }

      .tableComment {
        background-color:#e4efff; margin-bottom:20px;
      }

      .tableDefinition {
        padding:2px;
        border-collapse:collapse;
        margin-top:1em;
        font-size:12px;
      }

      .tdTableDefinition {
        padding-right:10px;
        vertical-align:top;
        border-bottom:1px solid #C0C0C0;
      }

      .tdColName {
        width:20em;
      }

      .tdDataType {
        width:15em;
      }

      .tdPkFlag {
        width:4em;
      }

      .tdNullFlag {
        width:9em;
      }

      .tdTableHeading {
        padding:2px;
        font-weight:bold;
        vertical-align:top;
        border-bottom: 1px solid #C0C0C0;
        background-color: rgb(240,240,240);
      }

      .source {
        font-family: monospace;
        background: #F0F0F0;
        border: 1px solid gray;
        padding: 2px;
        white-space: pre;
        width:90%;
      }
    </style>

    <script type="text/javascript">
      <![CDATA[
        function toggleElement(elementId)
        {
          element = document.getElementById(elementId);
          style = element.style.display;

          var newstyle = "block";
          if (style == "block")
          {
             newstyle = "none";
          }
          element.style.display=newstyle;
        }
      ]]>
    </script>
    <title>SQL Workbench/J - Schema Diff <xsl:value-of select="$ref-db"/> &#187; <xsl:value-of select="$target-db"/> </title>
  </head>
  <body>
    <h1>SQL Workbench/J - Schema Diff</h1>
    <xsl:text>Reference database: </xsl:text><xsl:value-of select="$ref-db"/><br/>
    <xsl:text>Target database: </xsl:text><xsl:value-of select="$target-db"/><br/>
    
    <xsl:variable name="tbl-mod-count" select="count(/schema-diff/modify-table)"/>
    <xsl:variable name="tbl-add-count" select="count(/schema-diff/add-table)"/>
    <xsl:variable name="tbl-drop-count" select="count(/schema-diff/drop-table)"/>
    
    <xsl:variable name="view-drop-count" select="count(/schema-diff/drop-view)"/>
    <xsl:variable name="view-mod-count" select="count(/schema-diff/update-view)"/>
    <xsl:variable name="view-add-count" select="count(/schema-diff/create-view)"/>

    <xsl:variable name="seq-drop-count" select="count(/schema-diff/drop-sequence)"/>
    <xsl:variable name="seq-mod-count" select="count(/schema-diff/update-sequence)"/>
    <xsl:variable name="seq-add-count" select="count(/schema-diff/create-sequence)"/>

    <xsl:variable name="proc-drop-count" select="count(/schema-diff/drop-procedure)"/>
    <xsl:variable name="proc-mod-count" select="count(/schema-diff/update-proc)"/>
    <xsl:variable name="proc-add-count" select="count(/schema-diff/create-proc)"/>

    <xsl:variable name="pkg-drop-count" select="count(/schema-diff/drop-package)"/>
    <xsl:variable name="pkg-mod-count" select="count(/schema-diff/update-package)"/>
    <xsl:variable name="pkg-add-count" select="count(/schema-diff/create-package)"/>
               
    <xsl:variable name="diff-count" 
                  select="$tbl-mod-count + $tbl-add-count + $tbl-drop-count + $view-drop-count + $view-mod-count + $view-add-count + $seq-drop-count + $seq-mod-count + $seq-add-count + $proc-drop-count + $proc-mod-count + $proc-add-count + $pkg-drop-count + $pkg-mod-count + $pkg-add-count"/>
  
    <xsl:if test="$diff-count = 0">
       No changes required
    </xsl:if>
    
    <xsl:if test="$diff-count > 0">
      <ul>
        <xsl:if test="$tbl-add-count > 0"><li><a href="#add-tables">Missing tables</a></li></xsl:if>
        <xsl:if test="$tbl-mod-count > 0"><li><a href="#modify-tables">Tables to update</a></li></xsl:if>
        <xsl:if test="$tbl-drop-count > 0"><li><a href="#drop-tables">Tables to drop</a></li></xsl:if>
      </ul>
        
      <ul>
        <xsl:if test="$view-add-count > 0"><li><a href="#add-views">Missing views</a></li></xsl:if>
        <xsl:if test="$view-mod-count > 0"><li><a href="#modify-views">Views to update</a></li></xsl:if>
        <xsl:if test="$view-drop-count > 0"><li><a href="#drop-views">Views to drop</a></li></xsl:if>
      </ul>
        
      <ul>
        <xsl:if test="$seq-add-count > 0"><li><a href="#add-seq">Missing sequences</a></li></xsl:if>
        <!-- <xsl:if test="$seq-mod-count > 0"><li><a href="#modify-seq">Sequences to update</a></li></xsl:if> -->
        <xsl:if test="$seq-drop-count > 0"><li><a href="#drop-seq">Sequences to drop</a></li></xsl:if>
      </ul>
        
      <ul>
        <xsl:if test="$proc-add-count > 0"><li><a href="#add-procs">Missing procedures</a></li></xsl:if>
        <xsl:if test="$proc-mod-count > 0"><li><a href="#mod-procs">Procedures to update</a></li></xsl:if>
        <xsl:if test="$proc-drop-count > 0"><li><a href="#drop-procs">Procedures to drop</a></li></xsl:if>
      </ul>
        
      <ul>
        <xsl:if test="$pkg-add-count > 0"><li><a href="#add-pkg">Missing packages</a></li></xsl:if>
        <xsl:if test="$pkg-mod-count > 0"><li><a href="#mod-pkg">Packages to update</a></li></xsl:if>
        <xsl:if test="$pkg-drop-count > 0"><li><a href="#drop-pkg">Packages to drop</a></li></xsl:if>
      </ul>
      
      <xsl:if test="$tbl-add-count > 0">
        <h2 id="add-tables">Tables to add in <xsl:value-of select="$target-db"/></h2>
        <xsl:call-template name="missing-tables"/>
      </xsl:if>

      <xsl:if test="$tbl-mod-count > 0">
        <h2 id="modify-tables">Tables to change in <xsl:value-of select="$target-db"/></h2>
        <xsl:call-template name="modify-tables"/>
      </xsl:if>

      <xsl:if test="$tbl-drop-count > 0">
        <h2 id="drop-tables">Tables to drop in <xsl:value-of select="$target-db"/></h2>
        <xsl:call-template name="drop-tables"/>
      </xsl:if>

      <xsl:if test="$view-add-count > 0">
        <h2 id="add-views">Views to add in <xsl:value-of select="$target-db"/></h2>
        <xsl:call-template name="create-views"/>
      </xsl:if>

      <xsl:if test="$view-mod-count > 0">
        <h2 id="modify-views">Views to re-create in <xsl:value-of select="$target-db"/></h2>
        <xsl:call-template name="update-views"/>
      </xsl:if>

      <xsl:if test="$view-drop-count > 0">
        <h2 id="drop-views">Views to drop in <xsl:value-of select="$target-db"/></h2>
        <xsl:call-template name="drop-views"/>
      </xsl:if>
      
      <xsl:if test="$seq-add-count > 0">
        <h2 id="add-seq">Sequences to add in <xsl:value-of select="$target-db"/></h2>
        <xsl:call-template name="create-sequences"/>
      </xsl:if>

      <!--
      <xsl:if test="$seq-mod-count > 0">
        <h2 id="modify-seq">Sequences to update in <xsl:value-of select="$target-db"/></h2>
        <xsl:call-template name="update-sequences"/>
      </xsl:if>
      -->

      <xsl:if test="$seq-drop-count > 0">
        <h2 id="drop-seq">Sequences to drop in <xsl:value-of select="$target-db"/></h2>
        <xsl:call-template name="drop-sequences"/>
      </xsl:if>

      <!-- procedures -->
      <xsl:if test="$proc-add-count > 0">
        <h2 id="add-procs">Procedures to create in <xsl:value-of select="$target-db"/></h2>
        <xsl:call-template name="create-procs"/>
      </xsl:if>

      <xsl:if test="$proc-drop-count > 0">
        <h2 id="mod-procs">Procedures to update in <xsl:value-of select="$target-db"/></h2>
        <xsl:call-template name="update-procs"/>
      </xsl:if>

      <xsl:if test="$proc-drop-count > 0">
        <h2 id="drop-procs">Procedures to drop in <xsl:value-of select="$target-db"/></h2>
        <xsl:call-template name="drop-procs"/>
      </xsl:if>
      
      <!-- packages -->
      <xsl:if test="$pkg-add-count > 0">
        <h2 id="add-pkg">Packages to create in <xsl:value-of select="$target-db"/></h2>
        <xsl:call-template name="create-pkg"/>
      </xsl:if>

      <xsl:if test="$pkg-drop-count > 0">
        <h2 id="mod-pkg">Packages to update in <xsl:value-of select="$target-db"/></h2>
        <xsl:call-template name="update-pkg"/>
      </xsl:if>

      <xsl:if test="$pkg-drop-count > 0">
        <h2 id="drop-pkg">Packages to drop in <xsl:value-of select="$target-db"/></h2>
        <xsl:call-template name="drop-pkg"/>
      </xsl:if>
      
    </xsl:if>
    
  </body>
  </html>
</xsl:template>

<xsl:template name="modify-tables">
    <xsl:for-each select="/schema-diff/modify-table">

      <xsl:sort select="table-name"/>
      <xsl:variable name="table" select="@name"/>
      
      <xsl:variable name="add-count" select="count(add-column)"/>
      <xsl:variable name="mod-count" select="count(modify-column)"/>
      <xsl:variable name="drop-count" select="count(drop-column)"/>
      <xsl:variable name="drop-fk-count" select="count(drop-foreign-keys)"/>
      <xsl:variable name="add-fk-count" select="count(add-foreign-keys)"/>
      <xsl:variable name="add-idx-count" select="count(add-index)"/>
      <xsl:variable name="drop-idx-count" select="count(drop-index)"/>
      <xsl:variable name="mod-idx-count" select="count(modify-index)"/>
      <xsl:variable name="mod-cons-count" select="count(table-constraints/modify-constraint)"/>
      <xsl:variable name="drop-cons-count" select="count(table-constraints/drop-constraint)"/>
      <xsl:variable name="add-cons-count" select="count(table-constraints/add-constraint)"/>
      
      <xsl:variable name="change-count" 
                    select="$add-count + $mod-count + $drop-count + $add-fk-count + $drop-fk-count + $add-idx-count + $drop-idx-count + $mod-idx-count + $mod-cons-count + $drop-cons-count + $add-cons-count"/>
      
      <!-- ignore changes to "extended attributes, e.g. NOLOGGING for Oracle -->
      <xsl:if test="$change-count &gt; 0">
      
        <div class="tableNameHeading">
          <a name="{$table}" href="javascript:toggleElement('{$table}_det')">
            <xsl:value-of select="$table"/>
          </a>
        </div>

        <div id="{$table}_det" style="display:none">
          <xsl:if test="count(add-column) &gt; 0">
            <h3>Columns to add</h3>
            <ul>
              <xsl:for-each select="add-column/column-def">
                <li><xsl:call-template name="column-definition-simple"/></li>
              </xsl:for-each>
            </ul>
          </xsl:if>

          <xsl:if test="count(drop-column) &gt; 0">
            <h3>Columns to drop</h3>
            <ul>
              <xsl:for-each select="remove-column">
                <li><xsl:value-of select="@name"/></li>
              </xsl:for-each>
            </ul>
          </xsl:if>
          
          <xsl:if test="count(modify-column) &gt; 0">
            <h3>Columns to update</h3>
            <ul>
              <xsl:for-each select="modify-column/reference-column-definition">
                <li>
                  <xsl:value-of select="../@name"/><xsl:call-template name="column-definition-simple"/><br/>
                  <xsl:text>Changed attributes:</xsl:text><br/>
                  <ul>
                    <xsl:for-each select="../new-column-attributes/*">
                      <li><xsl:value-of select="local-name(.)"/><xsl:text>: </xsl:text><xsl:value-of select="."/></li>
                    </xsl:for-each>
                  </ul>
                </li>
              </xsl:for-each>
            </ul>
          </xsl:if>

          <xsl:if test="$add-fk-count &gt; 0">
            <h3>Foreign keys to add</h3>
            <ul>
              <xsl:for-each select="add-foreign-keys/foreign-key">
                <li>
                  <xsl:value-of select="constraint-name"/>
                  <xsl:text> referencing </xsl:text><xsl:value-of select="references/table-name"/>
                </li>
              </xsl:for-each>
            </ul>
          </xsl:if>

          <xsl:if test="$drop-fk-count &gt; 0">
            <h3>Foreign keys to drop</h3>
            <ul>
              <xsl:for-each select="drop-foreign-keys/foreign-key">
                <li>
                  <xsl:value-of select="constraint-name"/>
                  <xsl:text> referencing </xsl:text><xsl:value-of select="references/table-name"/>
                </li>
              </xsl:for-each>
            </ul>
          </xsl:if>
          
          <xsl:if test="$mod-cons-count &gt; 0">
            <h3>Check constraints to update</h3>
            <ul>
              <xsl:for-each select="table-constraints/modify-constraint">
                <li>
                  <xsl:value-of select="constraint-definition/@name"/>
                  <xsl:text>: </xsl:text><xsl:value-of select="constraint-definition/text()"/>
                </li>
              </xsl:for-each>
            </ul>
          </xsl:if>
          
          <xsl:if test="$drop-cons-count &gt; 0">
            <h3>Check constraints to drop</h3>
            <ul>
              <xsl:for-each select="table-constraints/drop-constraint">
                <li>
                  <xsl:value-of select="constraint-definition/@name"/>
                  <xsl:text>: </xsl:text><xsl:value-of select="constraint-definition/text()"/>
                </li>
              </xsl:for-each>
            </ul>
          </xsl:if>

          <xsl:if test="$add-cons-count &gt; 0">
            <h3>Check constraints to add</h3>
            <ul>
              <xsl:for-each select="table-constraints/add-constraint">
                <li>
                  <xsl:value-of select="constraint-definition/@name"/>
                  <xsl:text>: </xsl:text><xsl:value-of select="constraint-definition/text()"/>
                </li>
              </xsl:for-each>
            </ul>
          </xsl:if>

          <xsl:if test="$add-idx-count &gt; 0">
            <h3>Indexes to create</h3>
            <ul>
              <xsl:for-each select="add-index/index-def">
                <li>
                  <xsl:value-of select="name"/><xsl:text> (</xsl:text><xsl:value-of select="index-expression"/><xsl:text>)</xsl:text>
                  <span style="display:block;font-size:85%;margin-top:0.5em;margin-bottom:0.5em">
                    <xsl:if test="unique='true'"><xsl:text> UNIQUE</xsl:text></xsl:if>
                    <xsl:if test="type='NORMAL/REV'"><xsl:text> REVERSE</xsl:text></xsl:if>
                    <xsl:if test="primary-key='true'"><xsl:text> PRIMARY KEY</xsl:text></xsl:if>
                  </span>
                </li>
              </xsl:for-each>
            </ul>
          </xsl:if>

          <xsl:if test="$mod-idx-count &gt; 0">
            <h3>Indexes to update</h3>
            <ul>
              <xsl:for-each select="modify-index/modified/type">
                <li>
                  <xsl:value-of select="../../reference-index/name"/><xsl:text>: Change type from </xsl:text><xsl:value-of select="@oldvalue"/><xsl:text> to </xsl:text><xsl:value-of select="@newvalue"/>
                </li>
              </xsl:for-each>
              <xsl:for-each select="modify-index/modified/name">
                <li>
                  <xsl:value-of select="@oldvalue"/><xsl:text>: rename to: </xsl:text><xsl:value-of select="@newvalue"/>
                </li>
              </xsl:for-each>
            </ul>
          </xsl:if>
                                                                                
          <xsl:if test="$drop-idx-count &gt; 0">
            <h3>Indexes to drop</h3>
            <ul>
              <xsl:for-each select="drop-index">
                <li>
                  <xsl:value-of select="."/>
                </li>
              </xsl:for-each>
            </ul>
          </xsl:if>

          <!--
          <xsl:if test="count(add-options) &gt; 0">
            <h3>Modify attribute</h3>
            <ul>
              <xsl:for-each select="add-options/option">
                <li><xsl:value-of select="@type"/><xsl:text>: </xsl:text><xsl:value-of select="definition"/></li>
              </xsl:for-each>
            </ul>
          </xsl:if>
          -->
        </div>
      </xsl:if>
    </xsl:for-each>
</xsl:template>

<xsl:template name="column-definition-simple">
  <xsl:value-of select="column-name"/><xsl:text>: </xsl:text><xsl:value-of select="dbms-data-type"/>
  <xsl:choose>
    <xsl:when test="nullable = 'false'"><xsl:text> NOT NULL</xsl:text></xsl:when>
    <xsl:otherwise><xsl:text> NULL</xsl:text></xsl:otherwise>
  </xsl:choose>
</xsl:template>

<xsl:template name="missing-tables">

    <xsl:for-each select="/schema-diff/add-table/table-def">

      <xsl:sort select="table-name"/>
      <xsl:variable name="table" select="table-name"/>

      <div class="tableNameHeading">
        <a name="{$table}" href="javascript:toggleElement('{$table}_cols')">
          <xsl:value-of select="$table"/>
        </a>
      </div>
      <div id="{$table}_cols" style="display:none">
        <xsl:call-template name="column-list"/>
      </div>
    </xsl:for-each>

</xsl:template>

<xsl:template name="drop-tables">

    <xsl:for-each select="/schema-diff/drop-table/table-name">
      <xsl:sort select="."/>
      <ul>
        <li>
          <xsl:value-of select="."/>
        </li>
      </ul>
    </xsl:for-each>

</xsl:template>

<xsl:template name="column-list">
    <table class="tableDefinition" width="100%">
      <tr>
        <td class="tdTableHeading tdColName"><xsl:text>Column</xsl:text></td>
        <td class="tdTableHeading tdDataType"><xsl:text>Type</xsl:text></td>
        <td class="tdTableHeading tdPkFlag"><xsl:text>PK</xsl:text></td>
        <td class="tdTableHeading tdNullFlag"><xsl:text>Nullable</xsl:text></td>
        <td class="tdTableHeading"><xsl:text>Comment</xsl:text></td>
      </tr>

      <xsl:for-each select="column-def">
        <xsl:sort select="dbms-position"/>
        <tr>
          <td class="tdTableDefinition">
            <xsl:value-of select="column-name"/>
            <xsl:if test="count(references) &gt; 0">
              <xsl:variable name="targetTable" select="references/table-name"/>
              &#160;(<a href="#{$targetTable}"><xsl:value-of select="'FK'"/></a>)
            </xsl:if>
          </td>
          <td class="tdTableDefinition">
            <xsl:value-of select="dbms-data-type"/>
          </td>
          <td class="tdTableDefinition" nowrap="nowrap">
            <xsl:if test="primary-key='true'">
              <xsl:text>PK</xsl:text>
            </xsl:if>
          </td>
          <td class="tdTableDefinition" nowrap="nowrap">
            <xsl:if test="nullable='false'">
              <xsl:text>NOT NULL</xsl:text>
            </xsl:if>
          </td>
          <td class="tdTableDefinition">
            <xsl:value-of select="comment"/>
          </td>
        </tr>
      </xsl:for-each>
    </table>
</xsl:template>

<!--
  *******************************************
  ********************* VIEWS ***************
  *******************************************
-->
  
<xsl:template name="create-views">
    <xsl:for-each select="/schema-diff/create-view">
      <xsl:sort select="view-def/@name"/>
      
      <xsl:call-template name="view-definition"/>
    </xsl:for-each>
</xsl:template>

<xsl:template name="update-views">
    <xsl:for-each select="/schema-diff/update-view">
      <xsl:sort select="view-def/@name"/>
      <xsl:call-template name="view-definition"/>
    </xsl:for-each>
</xsl:template>

<xsl:template name="drop-views">
  <ul>
    <xsl:for-each select="/schema-diff/drop-view">
      <xsl:sort select="view-def/@name"/>
      <li><xsl:value-of select="view-def/@name"/></li>
    </xsl:for-each>
  </ul>
</xsl:template>

<xsl:template name="view-definition">
  <xsl:variable name="view" select="view-def/@name"/>
  <div class="tableNameHeading">
    <a name="v_{$view}" href="javascript:toggleElement('{$view}_src')">
      <xsl:value-of select="$view"/>
    </a>
  </div>
  <div id="{$view}_src" style="display:none" class="source">
    <xsl:copy-of select="view-def/view-source"/>
  </div>
</xsl:template>

<!--
  ************************************************
  ********************* Procedures ***************
  ************************************************
-->
<xsl:template name="create-procs">
    <xsl:for-each select="/schema-diff/create-proc">
      <xsl:sort select="proc-def/proc-name"/>
      
      <xsl:call-template name="proc-definition"/>
    </xsl:for-each>
</xsl:template>

<xsl:template name="update-procs">
    <xsl:for-each select="/schema-diff/update-proc">
      <xsl:sort select="proc-def/@name"/>
      <xsl:call-template name="proc-definition"/>
    </xsl:for-each>
</xsl:template>

<xsl:template name="drop-procs">
  <ul>
    <xsl:for-each select="/schema-diff/drop-procedure">
      <xsl:sort select="proc-def/proc-name"/>
      <li><xsl:value-of select="proc-def/proc-name"/></li>
    </xsl:for-each>
  </ul>
</xsl:template>

<xsl:template name="proc-definition">
  <xsl:variable name="proc" select="proc-def/proc-name"/>
  <div class="tableNameHeading">
    <a name="pr_{$proc}" href="javascript:toggleElement('{$proc}_psrc')">
      <xsl:value-of select="$proc"/>
    </a>
  </div>
  <div id="{$proc}_psrc" style="display:none" class="source">
    <xsl:copy-of select="proc-def/proc-source"/>
  </div>
</xsl:template>

<!--
  ************************************************
  ********************* Procedures ***************
  ************************************************
-->
<xsl:template name="create-pkg">
    <xsl:for-each select="/schema-diff/create-package">
      <xsl:sort select="package-def/package-name"/>
      
      <xsl:call-template name="package-definition"/>
    </xsl:for-each>
</xsl:template>

<xsl:template name="update-pkg">
    <xsl:for-each select="/schema-diff/update-package">
      <xsl:sort select="package-def/package-name"/>
      
      <xsl:call-template name="package-definition"/>
    </xsl:for-each>
</xsl:template>

<xsl:template name="drop-pkg">
  <ul>
    <xsl:for-each select="/schema-diff/drop-package">
      <xsl:sort select="proc-def/package-name"/>
      <li><xsl:value-of select="package-def/package-name"/></li>
    </xsl:for-each>
  </ul>
</xsl:template>

<xsl:template name="package-definition">
  <xsl:variable name="pkg" select="package-def/package-name"/>
  <div class="tableNameHeading">
    <a name="pkg_{$pkg}" href="javascript:toggleElement('{$pkg}_pkgsrc')">
      <xsl:value-of select="$pkg"/>
    </a>
  </div>
  <div id="{$pkg}_pkgsrc" style="display:none" class="source">
    <xsl:copy-of select="package-def/package-source"/>
  </div>
</xsl:template>

<!--
  ***********************************************
  ********************* Sequences ***************
  ***********************************************
-->

<xsl:template name="create-sequences">
    <xsl:for-each select="/schema-diff/create-sequence">
      <xsl:sort select="@name"/>
      <ul>
        <li>
          <xsl:value-of select="sequence-def/sequence-name"/>
        </li>
      </ul>
    </xsl:for-each>
</xsl:template>

<xsl:template name="drop-sequences">
    <xsl:for-each select="/schema-diff/drop-sequence/sequence-name">
      <xsl:sort select="."/>
      <ul>
        <li>
          <xsl:value-of select="."/>
        </li>
      </ul>
    </xsl:for-each>
</xsl:template>

<xsl:template name="update-sequences">
    <xsl:for-each select="/schema-diff/update-sequence">
      <xsl:sort select="@name"/>
      <ul>
        <li>
          <xsl:value-of select="sequence-def/sequence-name"/>
          <span style="display:block;margin-top:0.5em">
          Changed properties:<br/>
          <ul>
            <xsl:for-each select="modify-properties/property">
              <li>
                <xsl:value-of select="@name"/><xsl:text>: </xsl:text><xsl:value-of select="@value"/>
              </li>
            </xsl:for-each>
          </ul>
          </span>
        </li>
      </ul>
    </xsl:for-each>
</xsl:template>

</xsl:transform>
