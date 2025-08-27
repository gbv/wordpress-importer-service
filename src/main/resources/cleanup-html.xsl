<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
                xmlns:html="http://www.w3.org/1999/xhtml"
                xmlns:xalan="http://xml.apache.org/xalan"
                xmlns:util="xalan://de.vzg.wis.Utils"
                exclude-result-prefixes="xalan util">

  <xsl:output method="xml" />

  <xsl:template match="@*|node()">
    <xsl:copy>
      <xsl:apply-templates select="@*|node()" />
    </xsl:copy>
  </xsl:template>

  <xsl:key name="names" match="@name" use="." />

  <xsl:variable name="lowercase" select="'abcdefghijklmnopqrstuvwxyz'" />
  <xsl:variable name="uppercase" select="'ABCDEFGHIJKLMNOPQRSTUVWXYZ'" />

  <xsl:template match="@text-align[translate(text(), $uppercase, $lowercase) = 'justify']">
    <!-- remove this -->
  </xsl:template>

  <xsl:template match="html:script">
    <!-- remove scripts -->
  </xsl:template>

  <xsl:template match="@*[translate(local-name(), $uppercase, $lowercase) = 'onclick']">
    <!-- remove onclick -->
  </xsl:template>

  <xsl:template match="html:*[contains(@style, 'display') and contains(@style, 'none')]">
    <!-- remove this -->
  </xsl:template>

  <xsl:template match="@id">
    <!-- remove all ids -->
  </xsl:template>



  <xsl:template match="html:table[contains(@class,'footnote-reference-container')]">
    <xsl:if test="html:tbody/html:tr">
      <html:ul>
        <xsl:for-each select="html:tbody/html:tr">
          <html:li>
            <xsl:value-of select="concat(html:td[1], ' ', html:td[3])" />
            <br />
          </html:li>
        </xsl:for-each>
      </html:ul>
    </xsl:if>
  </xsl:template>

  <xsl:template match="html:a[@href]">
    <xsl:copy>
      <xsl:variable name="href" select="util:encodeURI(@href)" />
      <xsl:if test="string-length($href) &gt; 0">
        <xsl:attribute name="href">
          <xsl:value-of select="$href" />
        </xsl:attribute>
      </xsl:if>

      <xsl:apply-templates select="@*|node()" />
    </xsl:copy>
  </xsl:template>

  <xsl:template match="html:a/@href"></xsl:template>

  <xsl:template match="@name">
    <xsl:if test="generate-id(.)=generate-id(key('names', .)[1])">
      <xsl:copy-of select="." />
    </xsl:if>
  </xsl:template>

  <xsl:template match="@color[contains(., '!important')]"><!-- Entfernt !important aus color-Attribut -->
    <xsl:attribute name="color">
      <xsl:value-of select="normalize-space(substring-before(., '!important'))" />
    </xsl:attribute>
  </xsl:template>

  <!-- Entfernt alle Attribute, deren Name mit - beginnt (CSS Custom Properties) -->
  <xsl:template match="@*[starts-with(local-name(), '--')]"/>

  <!-- Entfernt opacity, display und CSS-Variablen aus style-Attributen -->
  <xsl:template match="@style">
    <xsl:attribute name="style">
      <xsl:variable name="styleString" select="."/>
      <xsl:for-each select="str:tokenize($styleString, ';')" xmlns:str="http://exslt.org/strings">
        <xsl:variable name="item" select="normalize-space(.)"/>
        <xsl:if test="not(starts-with(translate($item, 'OPACITY', 'opacity'), 'opacity')) and not(starts-with(translate($item, 'DISPLAY', 'display'), 'display')) and not(starts-with($item, '--')) and string-length($item) &gt; 0">
          <xsl:value-of select="$item"/>
          <xsl:if test="position() != last()">; </xsl:if>
        </xsl:if>
      </xsl:for-each>
    </xsl:attribute>
  </xsl:template>

</xsl:stylesheet>
