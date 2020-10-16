# Generated by maven-r-jsr223-plugin: do not edit by hand
# returns a reference to a java class in a jsr223 engine
JavaApi = R6::R6Class("JavaApi", public=list( 
	#### fields ----
	.log = NULL,
	.fromJava = NULL,
	.toJava = NULL,
	.reg = list(),
<#list model.getClassTypes() as class>
	${class.getSimpleName()} = NULL,
</#list>
  
  	changeLogLevel = function(logLevel) {
  		.jcall("uk/co/terminological/rjava/LogController", returnSig = "V", method = "changeLogLevel" , logLevel)
    },
	
	reconfigureLog = function(log4jproperties) {
  		.jcall("uk/co/terminological/rjava/LogController", returnSig = "V", method = "reconfigureLog" , log4jproperties)
    },
	
	#### registration functions
	
	register = function(r6obj) {
		ptr = xptr::xptr_address(r6obj$.jobj@jobj)
		self$.reg[[ptr]] = r6obj
	},
	
	isRegistered = function(jobj) {
		ptr = xptr::xptr_address(jobj@jobj)
		return(!is.null(self$.reg[[ptr]]))
	},
	
	getRegistered = function(jobj) {
		ptr = xptr::xptr_address(jobj@jobj)
		return(self$.reg[[ptr]])
	},
	
 	#### constructor ----
 	initialize = function(logLevel = "INFO") {
 	
 		.jcall("uk/co/terminological/rjava/LogController", returnSig = "V", method = "configureLog" , logLevel)
    	self$.log = .jcall("org/slf4j/LoggerFactory", returnSig = "Lorg/slf4j/Logger;", method = "getLogger", "${model.getConfig().getPackageName()}");
		.jcall(self$.log,returnSig = "V",method = "info","${model.getConfig().getPackageName()}: JavaApi initialised");
	
		# initialise type conversion functions
		
		self$.toJava = list(
<#list model.getDataTypes() as data>
			${data.getSimpleName()}=<#list data.getInputRCode() as fnLine>${fnLine}<#sep>${'\n'}			</#list><#sep>,
</#list>
		)
		
		self$.fromJava = list(
<#list model.getDataTypes() as data>
			${data.getSimpleName()}=<#list data.getOutputRCode() as fnLine>${fnLine}<#sep>${'\n'}			</#list><#sep>,
</#list>
		)
	
		# initialise java class constructors and static method definitions
		
<#list model.getClassTypes() as class>
		self$${class.getSimpleName()} = list(
	<#assign method=class.getConstructor()>
			new = function(${method.getParameterCsv()}) {
				# constructor
				# convert parameters to java
	<#list method.getParameterNames() as param>
				tmp_${param} = self$.toJava$${method.getParameterType(param).getSimpleName()}(${param});
	</#list>
				# invoke constructor method
				tmp_out = .jnew("${class.getJNIName()}" ${method.getParameterCsv("tmp_")}); 
				# convert result back to R (should be a identity conversion)
				tmp_r6 = ${class.getSimpleName()}$new(
					self$.fromJava$${method.getReturnType().getSimpleName()}(tmp_out),
					self
				);
			}<#if (class.hasStaticMethods())>,</#if>
	<#list class.getStaticMethods() as method>
			${method.getName()} = function(${method.getParameterCsv()}) {
				# copy parameters
			<#list method.getParameterNames() as param>
				tmp_${param} = self$.toJava$${method.getParameterType(param).getSimpleName()}(${param});
			</#list>
				#execute static call
				tmp_out = .jcall("${class.getJNIName()}", returnSig = "${method.getReturnType().getJNIType()}", method="${method.getName()}" ${method.getParameterCsv("tmp_")}); 
			<#if method.isFactory()>
				# get object if it already exists
				if(self$isRegistered(tmp_out)) return(self$getRegistered(tmp_out))
				# wrap return java object in R6 class 
				return(${class.getSimpleName()}$new(
					self$.fromJava$${method.getReturnType().getSimpleName()}(tmp_out),
					self
				));
			<#else>
				# convert java object back to R
				return(self$.fromJava$${method.getReturnType().getSimpleName()}(tmp_out));
			</#if>
			}<#sep>,
	</#list>
	)
</#list>
	}
))

