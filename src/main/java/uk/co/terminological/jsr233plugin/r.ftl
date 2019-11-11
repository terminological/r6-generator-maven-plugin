# returns a reference to a jsr223 engine
java${class.getName()} <- function() {
	class.path <- c(
	  system.file("java", "groovy-all-2.4.17.jar", package="${class.getValue("packageName")}"),
	  system.file("java", "${jarFileName}", package="${class.getValue("packageName")}")
	)	
	api <- jsr223::ScriptEngine$new("groovy", class.path)
	api$setDataFrameRowMajor(FALSE)
	api %@% '
		import ${class.getClassName()};
		x = new ${class.getName()}();
	'
	return(api)
}

<#list class.getMethods() as method>
# method: ${method.getName()}
# returns: ${method.getReturnType()}
${method.getName()} <- function(api<#list method.getParameterNames() as param>, ${param}</#list>) {
<#if method.isFluent()>
	<#list method.getParameterNames() as param>
	api$tmp_${param} = ${param};
	</#list>
	api %@% 'x = x.${method.getName()}(<#list method.getParameterNames() as param>tmp_${param}<#sep>, </#list>);';
	<#list method.getParameterNames() as param>
	api$remove("tmp_${param}")
	</#list>
	return(api);
<#else>
	return(api$invokeMethod("x", "${method.getName()}"<#list method.getParameterNames() as param>, ${param}</#list>))
</#if>
}
</#list>

