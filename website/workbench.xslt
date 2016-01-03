<?xml version="1.0" encoding="utf-8"?>
<!DOCTYPE xslt [
    <!ENTITY space "&#32;">
    <!ENTITY nbsp "&#160;">
    <!ENTITY raquo "&#187;">
    <!ENTITY copy "&#169;">
    <!ENTITY reg "&#174;">
]>
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform" version="1.0"
                xmlns:redirect="http://xml.apache.org/xalan/redirect"
                extension-element-prefixes="redirect"
                >

  <xsl:output encoding="utf-8"
          method="html"
          omit-xml-declaration="yes"
          indent="no"
          standalone="yes"
          doctype-public="-//W3C//DTD HTML 4.01 Transitional//EN"
          doctype-system="http://www.w3.org/TR/html4/loose.dtd"
          />

  <xsl:variable name="siteName" select="/site/@name"/>
  <xsl:param name="filedir"/>
  <xsl:param name="buildNumber"/>
  <xsl:param name="buildDate"/>
  <xsl:param name="devBuildDate"/>
  <xsl:param name="devBuildNumber"/>
  <xsl:param name="currentDate"/>
  <xsl:param name="includeDev" select="'0'"/>
  <xsl:variable name="fdir">
    <xsl:value-of select="concat($filedir, '\')"/>
  </xsl:variable>


<!--
  Generate the table of content which will be displayed for each
  page in the left hand navigation
-->
<!-- <xsl:variable name="toc"> -->
  <xsl:template name="make-toc">
    <xsl:param name="currentPage"/>

    <div id="navigation">
      <ul class="toc">
        <xsl:for-each select="/site/page[@toc='main']">
            <xsl:call-template name="toc-entry">
              <xsl:with-param name="currentPage" select="$currentPage"/>
            </xsl:call-template>
        </xsl:for-each>
        <li class="toclist" id="manual">
          <a class="list" title="Display the online manual of the current stable release" href="manual/workbench-manual.html">Manual</a>
        </li>
        <li class="toclist" id="problems">
          <a class="list" title="FAQ, Problems" href="manual/troubleshooting.html">Common Problems</a>
        </li>
      </ul>

      <ul class="toc" id="google">
        <li class="toclist">
          <a class="list" href="http://groups.google.com/group/sql-workbench" title="Jump to forum">Forum@Google
            <img src="images/outlink.gif" alt="Link Arrow" title="Jump to forum" border="0"/>
          </a>
        </li>
      </ul>

      <ul class="toc" id="smalltext">
        <li class="toclist">
          <a class="list" href="wb_news.xml">RSS Feed</a>
        </li>

        <xsl:for-each select="/site/page[@toc='small']">
          <xsl:call-template name="toc-entry"/>
        </xsl:for-each>
      </ul>

      <div id="ttip">
        <a target="_blank" href="https://www.campact.de/ttip-ebi/">
          <img border="0" style="margin-top:50px;" src="ttip.jpg" alt="Stop TTIP"/>
        </a>
      </div>

    </div>

  </xsl:template>
<!-- </xsl:variable> -->

  <xsl:template name="toc-entry">
    <xsl:param name="currentPage"/>

    <xsl:variable name="pageName" select="@name"/>
    <xsl:variable name="pageTitle" select="@title"/>
    <xsl:variable name="notoc" select="@notoc"/>

    <xsl:variable name="ref">
      <xsl:if test="@name">
        <xsl:value-of select="concat(@name,'.html')"/>
      </xsl:if>
      <xsl:if test="@link">
        <xsl:value-of select="@link"/>
      </xsl:if>
    </xsl:variable>

    <li class="toclist">
      <xsl:if test="$currentPage = $pageName">
        <xsl:attribute name="id">
          <xsl:value-of select="'current'"/>
        </xsl:attribute>
      </xsl:if>

      <A class="list" href="{$ref}">
        <xsl:if test="$currentPage = $pageName">
          <xsl:attribute name="id">
            <xsl:value-of select="'active'"/>
          </xsl:attribute>
        </xsl:if>
        <xsl:value-of select="$pageTitle"/>
      </A>
    </li>
  </xsl:template>

<!-- entry point template to select the whole site document -->
  <xsl:template match="site">

    <xsl:for-each select="page">
      <xsl:if test="@name">

        <xsl:variable name="pageName" select="@name"/>
        <xsl:variable name="pageTitle" select="@title"/>

        <xsl:variable name="fname">
          <xsl:value-of select="concat($pageName,'.html')"/>
        </xsl:variable>

        <xsl:variable name="filename">
          <xsl:value-of select="concat($fdir, $fname)"/>
        </xsl:variable>

        <redirect:write file="{$filename}">
          <xsl:call-template name="main">
            <xsl:with-param name="pageTitle" select="$pageTitle"/>
            <xsl:with-param name="pageName" select="@name"/>
          </xsl:call-template>
        </redirect:write>

      </xsl:if>
    </xsl:for-each>

    <xsl:if test="$includeDev = 1">
      <xsl:for-each select="/site/page[@id='dev-history']">

        <xsl:variable name="pageName" select="@id"/>
        <xsl:variable name="pageTitle" select="@title"/>

        <xsl:variable name="fname">
          <xsl:value-of select="concat($pageName,'.html')"/>
        </xsl:variable>

        <xsl:variable name="filename">
          <xsl:value-of select="concat($fdir, $fname)"/>
        </xsl:variable>

        <redirect:write file="{$filename}">
          <xsl:call-template name="main">
            <xsl:with-param name="pageTitle" select="$pageTitle"/>
            <xsl:with-param name="pageName" select="@name"/>
          </xsl:call-template>
        </redirect:write>

      </xsl:for-each>
    </xsl:if>

  </xsl:template>

  <xsl:template name="main">
    <xsl:param name="pageName"/>
    <xsl:param name="pageTitle"/>
    <xsl:param name="imageName"/>
    <xsl:param name="imageTitle"/>
    <xsl:param name="subTitle"/>
    <xsl:param name="description"/>
    <xsl:param name="back"/>
    <xsl:param name="next"/>

    <html>
      <head>
        <title>
          <xsl:value-of select="$siteName"/>&nbsp;-&nbsp;
          <xsl:value-of select="@title"/>
        </title>
        <meta http-equiv="Pragma" CONTENT="no-cache"/>
        <meta http-equiv="Expires" content="-1"/>
        <link rel="SHORTCUT ICON" href="favicon.ico"/>
        <link rel="alternate" type="application/rss+xml" title="SQL Workbench/J" href="/wb_news.xml"/>
        <xsl:if test="$pageName='index'">
            <meta name="description" content="A free SQL query tool various DBMS (PostgreSQL, Oracle, DB2, HyberSQL and others)"/>
        </xsl:if>
        <xsl:if test="$pageName != 'index'">
            <meta name="description">
                <xsl:attribute name="content">
                    <xsl:value-of select="concat('SQL Workbench/J - ', @title)"/>
                </xsl:attribute>
            </meta>
        </xsl:if>
        <meta name="keywords" lang="en" content="sql,query,tool,analyzer,gui,jdbc,database,isql,viewer,frontend,java,dbms,oracle,postgres,h2database,h2,firebirdsql,hsql,hsqldb,sqlplus,replacement,import,export,csv,unload,convert,insert,blob,clob,xml,etl,migrate,compare,diff,structure,table"/>
        <meta name="date">
          <xsl:attribute name="content">
            <xsl:value-of select="$currentDate"/>
          </xsl:attribute>
        </meta>
        <meta name="robots" content="follow"/>
        <link href="wb.css" rel="stylesheet" type="text/css"></link>
      </head>
      <body>
        <!--
        <a target="new" href="http://www.mgm-tp.com/home/index.html" title="mgm technology partners GmbH">
          <span id="mgm"></span>
        </a>
        -->

        <div id="top"></div>

        <a href="index.html" title="home">
          <span id="head"></span>
        </a>

        <div id="left">
          <xsl:call-template name="make-toc">
            <xsl:with-param name="currentPage" select="$pageName"/>
          </xsl:call-template>
        </div>

        <div id="main">
          <xsl:if test="$imageName">
          
            <h3 style="padding-top:1em"><xsl:value-of select="@title"/></h3>
             <div style="float:left">
                 <xsl:if test="$next">
                    <a href="{$next}"><img src="{$imageName}"/></a>
                 </xsl:if>
                 <xsl:if test="$next = ''">
                    <img src="{$imageName}"/>
                 </xsl:if>
                 <br/>
                 <div style="padding-top:1.5em">
                   <xsl:if test="$back and $back != 'screenshots.html'">
                     <div style="float:left"><a href="{$back}">Previous</a></div>
                   </xsl:if>
                   <xsl:if test="$back = 'screenshots.html'">
                     <span><a href="{$back}">Screenshots</a></span>
                   </xsl:if>
                   <xsl:if test="$next and $next != 'screenshots.html'">
                     <div style="float:right"><a href="{$next}">Next</a></div>
                   </xsl:if>
                   <xsl:if test="$next = 'screenshots.html'">
                     <div style="float:right"><a href="screenshots.html">Screenshots</a></div>
                   </xsl:if>
                 </div>
             </div>
          </xsl:if>
          <xsl:if test="not($imageName)">
            <div class="content">
              <xsl:apply-templates select="content"/>
            </div>
          </xsl:if>
        </div>

      </body>
    </html>
  </xsl:template>

  <xsl:template match="full-history">
    <h1>Release History</h1>

    <script type="text/javascript">
      <![CDATA[
        function toggleTOC()
        {
          element = document.getElementById("toc-wrapper");
          style = element.style.display;
          var newstyle = "block";
          if (style == "block")
          {
             newstyle = "none";
          }
          element.style.display=newstyle;

          toggleElement = document.getElementById("toc-toggle");

          if (newstyle == "none")
          {
            toggleElement.innerHTML = "Show list of all builds";
          }
          if (newstyle == "block")
          {
            toggleElement.innerHTML = "Hide list of all builds";
          }
        }
      ]]>
    </script>
    <a id="toc-toggle" href="javascript:toggleTOC()">Show list of all builds</a>
    <div id="toc-wrapper" style="display:none;margin-top:5px">
      <xsl:for-each select="document('../scripts/history.xml')/history/release[@build != '-1']">
        <xsl:variable name="build-nr" select="@build"/>
          <a href="#build_{$build-nr}">Build <xsl:value-of select="@build"/></a>&nbsp;
      </xsl:for-each>
    </div>
    <br/>
    <br/>
    <xsl:for-each select="document('../scripts/history.xml')/history/release[@build != '-1']">
      <xsl:variable name="build-nr" select="@build"/>
      <h1 id="build_{$build-nr}" class="build-nr">Build <xsl:value-of select="@build"/> (<xsl:value-of select="@date"/>)</h1>

      <xsl:if test="count(entry[@type='enh']) &gt; 0">
        <h3 class="history-entry">Enhancements</h3>
        <ul>
          <xsl:for-each select="entry[@type='enh']">
            <xsl:sort select="@dev-build" order="descending" data-type="number"/>
            <li><xsl:copy-of select="text()"/></li>
          </xsl:for-each>
        </ul>
      </xsl:if>

      <xsl:if test="count(entry[@type='fix']) &gt; 0">
        <h3 class="history-entry">Bug fixes</h3>
        <ul>
        <xsl:for-each select="entry[@type='fix']">
          <xsl:sort select="@dev-build" order="descending"  data-type="number"/>
          <li><xsl:copy-of select="text()"/></li>
        </xsl:for-each>
        </ul>
      </xsl:if>
    </xsl:for-each>

  </xsl:template>

  <xsl:template match="dev-history">
    <xsl:variable name="dev-build-nr" select="document('../scripts/history.xml')/history/release[2]/@build"/>

    <xsl:variable name="dev-build-minor">
      <xsl:for-each select="document('../scripts/history.xml')/history/release[1]/entry">
        <xsl:sort select="@dev-build" order="descending"  data-type="number"/>
        <xsl:if test="position()=1">
          <xsl:value-of select="@dev-build" />
        </xsl:if>
      </xsl:for-each>
    </xsl:variable>

    <h1>Changelog for Build <xsl:value-of select="concat($dev-build-nr,'.',$dev-build-minor)"/></h1>

    <xsl:for-each select="document('../scripts/history.xml')/history/release[1]">

      <h2 class="history-entry">Enhancements</h2>
      <ul>
      <xsl:for-each select="entry[@type='enh']">
        <xsl:sort select="@dev-build" order="descending"  data-type="number"/>
        <li>(<xsl:value-of select="$dev-build-nr"/>.<xsl:value-of select="@dev-build"/>) <xsl:value-of select="."/></li>
      </xsl:for-each>
      </ul>
      <h2 class="history-entry">Bug fixes</h2>
      <ul>
      <xsl:for-each select="entry[@type='fix']">
        <xsl:sort select="@dev-build" order="descending" data-type="number"/>
        <li>(<xsl:value-of select="$dev-build-nr"/>.<xsl:value-of select="@dev-build"/>) <xsl:value-of select="."/></li>
      </xsl:for-each>
      </ul>

    </xsl:for-each>
  </xsl:template>

  <xsl:template match="@*">
    <xsl:copy-of select="."/>
  </xsl:template>

  <xsl:template match="source-link">
    <a href="WorkbenchSrc-Build{$buildNumber}.zip">Source code</a>
  </xsl:template>

  <xsl:template match="zip-link">
    <a href="Workbench-Build{$buildNumber}.zip">Download generic package for all systems</a>
  </xsl:template>

  <xsl:template match="zip-office-link">
    <a href="Workbench-Build{$buildNumber}-with-office-libs.zip">Generic package including libraries to read and write office (ods,xls,xlsx) documents</a>
  </xsl:template>

  <xsl:template match="mac-link">
    <a href="Workbench-Build{$buildNumber}-MacJava8.tgz">Download package for MacOS</a>
  </xsl:template>

  <xsl:template match="mac-office-link">
    <a href="Workbench-Build{$buildNumber}-MacJava7-with-office-libs.tgz">MacOS including libraries to read and write office (ods,xls,xlsx) documents</a>
  </xsl:template>

  <xsl:template match="build-number">
    <xsl:value-of select="$buildNumber"/>
  </xsl:template>

  <xsl:template match="build-date">
    <xsl:value-of select="$buildDate"/>
  </xsl:template>

  <xsl:template match="dev-build">
    <xsl:if test="$includeDev = 1">
      <p>Current dev-build: <xsl:value-of select="$devBuildNumber"/>,&nbsp;<xsl:value-of select="$devBuildDate"/> (<a href="dev-history.html">Change Log</a>)</p>
      <ul>
        <li>
          <a href="Workbench-Build{$devBuildNumber}.zip">Download development build</a>
        </li>
        <li>
          <a href="Workbench-Build{$devBuildNumber}-with-office-libs.zip">Download development build including office libraries</a>
        </li>
        <li>
          <a href="WorkbenchSrc-Build{$devBuildNumber}.zip">Source code</a>
        </li>
        <li>
          <a href="/devmanual">Online Manual for the current dev build</a>
        </li>
      </ul>
    </xsl:if>
    <xsl:if test="$includeDev = 0 ">
      <p style="color:gray">
      (Currently there is no development build available)
      </p>
    </xsl:if>


  </xsl:template>

  <xsl:template match="dev-build-info">
    <xsl:if test="$includeDev = 1">
      <br/>Current development build:
      <span style="font-weight:bold">
        <a href="dev-download.html">
          <xsl:value-of select="$devBuildNumber"/> (
          <xsl:value-of select="$devBuildDate"/>)
        </a>
      </span>
      <br/>
    </xsl:if>
  </xsl:template>

  <xsl:template match="image-link">
    <xsl:variable name="imageName" select="@name"/>
    <xsl:variable name="imageTitle" select="@title"/>
    <xsl:variable name="back" select="@back"/>
    <xsl:variable name="next" select="@next"/>
    <xsl:variable name="fname" select="concat(translate(@name,'.','_'),'.html')"/>
    <xsl:variable name="imgFile" select="concat($fdir, $fname)"/>
    <xsl:variable name="backLink">
      <xsl:if test="$back">
        <xsl:value-of select="concat(translate($back,'.','_'),'.html')"/>
      </xsl:if>
    </xsl:variable>
    
    <xsl:variable name="nextLink">
      <xsl:if test="$next">
        <xsl:value-of select="concat(translate($next,'.','_'),'.html')"/>
      </xsl:if>
    </xsl:variable>
    
    <a href="{$fname}">
      <xsl:value-of select="."/>
    </a>
    <redirect:write file="{$imgFile}">
      <xsl:call-template name="main">
        <xsl:with-param name="imageName" select="$imageName"/>
        <xsl:with-param name="pageTitle" select="'SQL Workbench/J'"/>
        <xsl:with-param name="subTitle" select="$imageTitle"/>
        <xsl:with-param name="imageTitle" select="$imageTitle"/>
        <xsl:with-param name="back" select="$backLink"/>
        <xsl:with-param name="next" select="$nextLink"/>
      </xsl:call-template>
    </redirect:write>
  </xsl:template>

  <xsl:template match="not-mysql">
    <p class="mysql-hint">
      Please note that SQL Workbench/J has no relation to the product <a title="Visit Oracle's product page" href="http://www.mysql.com/products/workbench/"><b>My</b>SQL Workbench</a>
      <img src="images/outlink.gif" alt="Link Arrow" border="0"/>&nbsp;which is maintained and owned by Oracle. <br/>
      If you are looking for <b>My</b>SQL Workbench support please contact Oracle.
    </p>
  </xsl:template>

  <xsl:template match="mail-to">
    <xsl:text disable-output-escaping="yes"><![CDATA[<a href=mailto:&#115;&#117;&#112;&#112;&#111;&#114;&#116;&#64;&#115;&#113;&#108;&#45;&#119;&#111;&#114;&#107;&#98;&#101;&#110;&#99;&#104;&#46;&#110;&#101;&#116;>&#115;&#117;&#112;&#112;&#111;&#114;&#116;&#64;&#115;&#113;&#108;&#45;&#119;&#111;&#114;&#107;&#98;&#101;&#110;&#99;&#104;&#46;&#110;&#101;&#116;</a>]]>
    </xsl:text>
  </xsl:template>

<!-- <xsl:template match="content"><xsl:value-of select="."/></xsl:template> -->
  <xsl:template match="img">
    <xsl:copy>
      <xsl:apply-templates select="@*"/>
      <xsl:apply-templates/>
    </xsl:copy>
  </xsl:template>
  <xsl:template match="ul">
    <xsl:copy>
      <xsl:apply-templates select="@*"/>
      <xsl:apply-templates/>
    </xsl:copy>
  </xsl:template>
  <xsl:template match="ol">
    <xsl:copy>
      <xsl:apply-templates select="@*"/>
      <xsl:apply-templates/>
    </xsl:copy>
  </xsl:template>
  <xsl:template match="li">
    <xsl:copy>
      <xsl:apply-templates select="@*"/>
      <xsl:apply-templates/>
    </xsl:copy>
  </xsl:template>
  <xsl:template match="p">
    <xsl:copy>
      <xsl:apply-templates select="@*"/>
      <xsl:apply-templates/>
    </xsl:copy>
  </xsl:template>
  <xsl:template match="b">
    <xsl:copy>
      <xsl:apply-templates select="@*"/>
      <xsl:apply-templates/>
    </xsl:copy>
  </xsl:template>
  <xsl:template match="i">
    <xsl:copy>
      <xsl:apply-templates select="@*"/>
      <xsl:apply-templates/>
    </xsl:copy>
  </xsl:template>
  <xsl:template match="br">
    <xsl:copy>
      <xsl:apply-templates select="@*"/>
      <xsl:apply-templates/>
    </xsl:copy>
  </xsl:template>
  <xsl:template match="span">
    <xsl:copy>
      <xsl:apply-templates select="@*"/>
      <xsl:apply-templates/>
    </xsl:copy>
  </xsl:template>
  <xsl:template match="a">
    <xsl:variable name="ref" select="@href"/>
    <xsl:copy>
      <xsl:apply-templates select="@*"/>
      <xsl:apply-templates/>
    </xsl:copy>
    <xsl:if test="substring($ref,1,4) = 'http'">
      <img src="images/outlink.gif" alt="Link Arrow" title="{$ref}" border="0"/>
    </xsl:if>
  </xsl:template>
  <xsl:template match="tt">
    <xsl:copy>
      <xsl:apply-templates select="@*"/>
      <xsl:apply-templates/>
    </xsl:copy>
  </xsl:template>
  <xsl:template match="h1">
    <xsl:copy>
      <xsl:apply-templates select="@*"/>
      <xsl:apply-templates/>
    </xsl:copy>
  </xsl:template>
  <xsl:template match="h2">
    <xsl:copy>
      <xsl:apply-templates select="@*"/>
      <xsl:apply-templates/>
    </xsl:copy>
  </xsl:template>
  <xsl:template match="h3">
    <xsl:copy>
      <xsl:apply-templates select="@*"/>
      <xsl:apply-templates/>
    </xsl:copy>
  </xsl:template>
  <xsl:template match="h4">
    <xsl:copy>
      <xsl:apply-templates select="@*"/>
      <xsl:apply-templates/>
    </xsl:copy>
  </xsl:template>

  <xsl:template match="div">
    <xsl:copy>
      <xsl:apply-templates select="@*"/>
      <xsl:apply-templates/>
    </xsl:copy>
  </xsl:template>
  <xsl:template match="span">
    <xsl:copy>
      <xsl:apply-templates select="@*"/>
      <xsl:apply-templates/>
    </xsl:copy>
  </xsl:template>

</xsl:stylesheet>
