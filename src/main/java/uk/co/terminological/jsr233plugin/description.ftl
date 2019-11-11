Package: ${class.getValue("packageName")}
Title: ${class.getValue("title")}
Version: ${class.getValue("version")}
<#if class.getList("authors")[0]??>
Authors@R: 
    person(given = ${class.getList("authors")[0]!"anon"},
           family = ${class.getList("authors")[1]!"anon"},
           role = c("aut", "cre"),
           email = ${class.getList("authors")[2]!"anon@exmaple.com"},
           comment = structure(${class.getList("authors")[3]!"n/a"}, .Names = "ORCID"))
</#if>
Description: ${class.getValue("description")!}
License: ${class.getValue("license")!}
Encoding: UTF-8
LazyData: true
Suggests: 
<#list class.getList("suggests") as suggest>
	${suggest}<#sep>,
</#list>
Imports:
<#list class.getList("imports") as import>
	${import},
</#list>
	jsr223
