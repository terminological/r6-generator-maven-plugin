# Generated by r6-geenrator-maven-plugin: do not edit by hand
# This is a class of the ${model.getConfig().getPackageName()} generated R library.
#
# ${model.getConfig().getTitle()}
# Version: ${model.getConfig().getVersion()}
# Generated: ${model.getConfig().getDate()}
# Contact: ${model.getConfig().getMaintainerEmail()}
${class.getSimpleName()} = R6::R6Class("${class.getSimpleName()}", public=list(
	.api = NULL,
	.jobj = NULL,
	<#-- .ptr = NULL, -->
	initialize = function(jobj,api){
		self$.jobj = jobj;
		self$.api = api;
		<#-- 
		self$.ptr = xptr::xptr_address(jobj@jobj)
		api$register(self)
		 -->
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
		# is this a fluent method?
		# if(.jcall(self$.jobj, returnSig="Z", method="equals", .jcast(tmp_out))) {
		if(self$.jobj$equals(tmp_out)) {
			# return fluent method
			self$.api$printMessages()
			invisible(self)
		} else {
			# wrap return java object in R6 class  
			out = ${method.getReturnType().getSimpleName()}$new(
				self$.api$.fromJava$${method.getReturnType().getSimpleName()}(tmp_out),
				self$.api
			);
			self$.api$printMessages()
			return(out);
		}
		<#else>
		# convert java object back to R
		out = self$.api$.fromJava$${method.getReturnType().getSimpleName()}(tmp_out);
		self$.api$printMessages()
		if(is.null(out)) return(invisible(out))
		return(out);
		</#if>
	},
	</#list>
	
	print = function() {
		tmp_out = .jcall(self$.jobj, returnSig = "Ljava/lang/String;", method="toString");
		self$.api$printMessages()
		print(tmp_out)
		invisible(self)
	},
	
	equals = function(object) {
		if (is.null(object$.jobj)) return(FALSE)
		return(self$.jobj$equals(object$.jobj))
	}
))
