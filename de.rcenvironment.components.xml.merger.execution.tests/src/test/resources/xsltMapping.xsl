<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet version="1.0"  xmlns:xsl="http://www.w3.org/1999/XSL/Transform" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" exclude-result-prefixes="xsi">
	<xsl:output method="xml" version="1.0" encoding="UTF-8" indent="yes"/>
	
	
	<!--Define Variable for CPACS integrating file-->
	<xsl:variable name="integratingFile" select="'INTEGRATING_INPUT'"/>
	
	
	<!--Copy complete Source to Result -->
	<xsl:template match="@* | node()">
		<xsl:copy>
			<xsl:apply-templates select="@* | node()"/>
		</xsl:copy>
	</xsl:template>
	
	
	<xsl:template match="/cpacs/header">
		<header>
			<xsl:copy-of select="/cpacs/header/*"/> <!--Copy existing-->
		</header>
		<xsl:copy-of select="document($integratingFile)/cpacs/header"/> <!--Copy from integrating file-->
  	</xsl:template>
  	

</xsl:stylesheet>