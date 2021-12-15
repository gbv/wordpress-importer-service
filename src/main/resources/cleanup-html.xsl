<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
                xmlns:html="http://www.w3.org/1999/xhtml"
                xmlns:xalan="http://xml.apache.org/xalan"
                xmlns:util="xalan://de.vzg.service.Utils"
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
      <xsl:attribute name="href">
        <xsl:value-of select="util:encodeURI(@href)" />
      </xsl:attribute>

      <xsl:apply-templates select="@*|node()" />
    </xsl:copy>
  </xsl:template>

  <xsl:template match="html:a/@href"></xsl:template>

  <xsl:template match="@name">
    <xsl:if test="generate-id(.)=generate-id(key('names', .)[1])">
      <xsl:copy-of select="." />
    </xsl:if>
  </xsl:template>

</xsl:stylesheet>
