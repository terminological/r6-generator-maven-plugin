# returns a reference to a jsr223 engine
java${class.getName()} <- function() {
	class.path <- c("./${pathToJar}")
	api <- jsr223::ScriptEngine$new("groovy", class.path)
	api %@% '
		import ${class.getClassName()};
		${class.getName()} x = new ${class.getName()}();
	'
	return(api)
}

<#list class.getMethods() as method>
# method: ${method.getName()}
# returns: ${method.getReturnType()}
${method.getName()} <- function(api<#list method.getParameterNames() as param>, ${param}</#list>) {
<#if method.isFluent()>
	api %@% 'x = x.${method.getName()}(<#list method.getParameterNames() as param>${param}<#sep>,</#list>);'
<#else>
	return(api$invokeMethod("x", "${method.getName()}"<#list method.getParameterNames() as param>, ${param}</#list>))
</#if>
}
</#list>

