Package: ${model.getConfig().getPackageName()}
Title: ${model.getConfig().getTitle()}
Version: ${model.getConfig().getVersion()}
Authors@R: c(
	<#list model.getAuthors() as author>
    person(given = "${author[0]}",<#if author[1]??>family = "${author[1]}",</#if><#if author[2]??>email = "${author[2]}",</#if><#if author[3]??>comment = structure("${author[3]}", .Names = "ORCID"),</#if>role = <#if author?is_first>c("aut", "cre")<#else>"aut"</#if>)<#sep>,
	</#list>
)
Description: ${model.getConfig().getDescription()!}
License: ${model.getConfig().getLicense()!}
Encoding: UTF-8
LazyData: true
VignetteBuilder: knitr
Suggests: 
<#list model.getSuggests() as suggest>	${suggest},${"\n"}</#list>	knitr,
	rmarkdown,
	testthat
Imports:
<#list model.getImports() as import>	${import},${"\n"}</#list>	rJava,
	R6,
	fs
