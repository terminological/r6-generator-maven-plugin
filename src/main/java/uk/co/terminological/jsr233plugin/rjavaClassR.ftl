# Generated by maven-r-jsr223-plugin: do not edit by hand
# a java class R6 wrapper
${class.getSimpleName()} = R6::R6Class("${class.getSimpleName()}", public=list(
	.api = NULL,
	.jobj = NULL,
	.ptr = NULL,
	initialize = function(jobj,api){
		self$.jobj = jobj;
		self$.api = api;
		self$.ptr = xptr::xptr_address(jobj@jobj)
		api$register(self)
	},
	
	<#list class.getInstanceMethods() as method>
	${method.getName()} = function(${method.getParameterCsv()}) {
		# copy parameters
		<#list method.getParameterNames() as param>
		tmp_${param} = self$.api$.toJava$${method.getParameterType(param).getSimpleName()}(${param});
		</#list>
		#execute method call
		tmp_out = .jcall(self$.jobj, returnSig = "${method.getReturnType().getJNIType()}", method="${method.getName()}" ${method.getParameterCsv("tmp_")}); 
		<#if method.isFactory()>
		# get object if it already exists
		if(self$.api$isRegistered(tmp_out)) return(self$.api$getRegistered(tmp_out))
		# wrap return java object in R6 class  
		return(${method.getReturnType().getSimpleName()}$new(
			self$.api$.fromJava$${method.getReturnType().getSimpleName()}(tmp_out),
			self$.api
		));
		<#else>
		# convert java object back to R
		return(self$.api$.fromJava$${method.getReturnType().getSimpleName()}(tmp_out));
		</#if>
	},
	</#list>
	
	print = function() {
		tmp_out = .jcall(self$.jobj, returnSig = "Ljava/lang/String;", method="toString");
		print(tmp_out)
		invisible(self)
	}
))