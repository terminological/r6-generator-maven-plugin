# Generated by r6-generator-maven-plugin: do not edit by hand
#' This is the main entry point of the ${model.getConfig().getPackageName()} generated R library.
#'
#' @description
#' ${model.getConfig().getTitle()}
#'
#' Version: ${model.getConfig().getVersion()}
#'
#' Generated: ${model.getConfig().getDate()}
#'
#' Contact: ${model.getConfig().getMaintainerEmail()}
<#list model.getImports() as import>
#' @import ${import}
</#list>	
#' @import rJava
#' @export
JavaApi = R6::R6Class("JavaApi", public=list( 
	#### fields ----
	#' @field .log a pointer to the java logging system
	.log = NULL,
	#' @field .fromJava a set of type conversion functions from Java to R
	.fromJava = NULL,
	#' @field .toJava a set of type conversion functions from R to Java
	.toJava = NULL,
	#' @field .reg the list of references to java objects created by this API 
	.reg = list(),
<#list model.getClassTypes() as class>
	${class.getSimpleName()} = NULL,
</#list>

	#' @description
    #' change the java logging level
    #' @param logLevel A string such as "DEBUG", "INFO", "WARN"
    #' @return nothing
  	changeLogLevel = function(logLevel) {
  		.jcall("uk/co/terminological/rjava/LogController", returnSig = "V", method = "changeLogLevel" , logLevel)
  		invisible(NULL)
    },
	
	#' @description
    #' change the java logging level using a log4j configuration file
    #' @param log4jproperties An absolute filepath to the log4j propertied file
    #' @return nothing
	reconfigureLog = function(log4jproperties) {
  		.jcall("uk/co/terminological/rjava/LogController", returnSig = "V", method = "reconfigureLog" , log4jproperties)
  		invisible(NULL)
    },
	
	#' @description
    #' print java system messages to the R console and flush the message cache. This is generally called automatically,
    #' @return nothing
	printMessages = function() {
		# check = FALSE here to stop exceptions being cleared from the stack.
		cat(.jcall("uk/co/terminological/rjava/LogController", returnSig = "Ljava/lang/String;", method = "getSystemMessages", check=FALSE))
		invisible(NULL)
	},
	
	<#-- 
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
	-->
	
 	#### constructor ----
 	#' @description
 	#' Create the R6 api library class. This is the entry point to all Java related classes and methods in this package.
    #' @param logLevel One of "OFF", "FATAL", "ERROR", "WARN", "INFO", "DEBUG", "TRACE", "ALL". (defaults to "${model.getConfig().getDefaultLogLevel()}") 
    #' @examples
    #' \dontrun{
    #' J = ${model.getConfig().getPackageName()}::JavaApi$get();
	#' }
    #' @return nothing
 	initialize = function(logLevel = <#if model.getConfig().getDebugMode()>"DEBUG"<#else>"${model.getConfig().getDefaultLogLevel()}"</#if>) {
 		if (is.null(JavaApi$singleton)) stop("Startup the java api with JavaApi$get() rather than using this constructor directly")
 	
 		message("Initialising ${model.getConfig().getTitle()}")
 		message("Version: ${model.getConfig().getVersion()}")
		message("Generated: ${model.getConfig().getDate()}")
 	
	 	<#if model.getConfig().getDebugMode()>
		# pass in debug options
		if (!.jniInitialized) {
			.jinit(parameters=c(getOption("java.parameters"),"-Xdebug","-Xrunjdwp:transport=dt_socket,address=8998,server=y,suspend=n"), silent = TRUE, force.init = TRUE)
			message("java debugging initialised on port 8998")
		}
		<#else>
		if (!.jniInitialized) 
	        .jinit(parameters=getOption("java.parameters"),silent = TRUE, force.init = FALSE)
		</#if>
		
		# add in all the jars that come with the library
	    classes <- system.file("java", package = "${model.getConfig().getPackageName()}")
	    if (nchar(classes)) {
	        .jaddClassPath(classes)
	        jars <- grep(".*\\.jar", list.files(classes, full.names = TRUE), TRUE, value = TRUE)
	        message(paste0("Adding to classpath: ",jars,collapse='\n'))
	        .jaddClassPath(jars)
	    }
	    
	    # configure logging
 		.jcall("uk/co/terminological/rjava/LogController", returnSig = "V", method = "setupRConsole")
 		.jcall("uk/co/terminological/rjava/LogController", returnSig = "V", method = "configureLog" , logLevel)
 		# TODO: this is the library build date code byut it requires testing
 		buildDate = .jcall("uk/co/terminological/rjava/LogController", returnSig = "S", method = "getClassBuildTime")
    	self$.log = .jcall("org/slf4j/LoggerFactory", returnSig = "Lorg/slf4j/Logger;", method = "getLogger", "${model.getConfig().getPackageName()}");
    	.jcall(self$.log,returnSig = "V",method = "info","Initialised ${model.getConfig().getPackageName()}");
		.jcall(self$.log,returnSig = "V",method = "debug","Version: ${model.getConfig().getVersion()}");
		.jcall(self$.log,returnSig = "V",method = "debug","R package generated: ${model.getConfig().getDate()}");
		.jcall(self$.log,returnSig = "V",method = "debug",paste0("Java library compiled: ",buildDate));
		.jcall(self$.log,returnSig = "V",method = "debug","Contact: ${model.getConfig().getMaintainerEmail()}");
		self$printMessages()
		
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
			new = function(${method.getFunctionParameterCsv()}) {
				# constructor
				# convert parameters to java
	<#list method.getParameterNames() as param>
				tmp_${param} = self$.toJava$${method.getParameterType(param).getSimpleName()}(${param});
	</#list>
				# invoke constructor method
				tmp_out = .jnew("${class.getJNIName()}" ${method.getParameterCsv("tmp_")}, check=FALSE);
				self$printMessages()
				.jcheck() 
				# convert result back to R (should be a identity conversion)
				tmp_r6 = ${class.getSimpleName()}$new(
					self$.fromJava$${method.getReturnType().getSimpleName()}(tmp_out),
					self
				);
				return(tmp_r6)
			}<#if (class.hasStaticMethods())>,</#if>
	<#list class.getStaticMethods() as method>
			${method.getName()} = function(${method.getFunctionParameterCsv()}) {
				# copy parameters
			<#list method.getParameterNames() as param>
				tmp_${param} = self$.toJava$${method.getParameterType(param).getSimpleName()}(${param});
			</#list>
				#execute static call
				tmp_out = .jcall("${class.getJNIName()}", returnSig = "${method.getReturnType().getJNIType()}", method="${method.getName()}" ${method.getParameterCsv("tmp_")}, check=FALSE);
				self$printMessages()
				.jcheck() 
			<#if method.isFactory()>
				<#-- 
				# get object if it already exists
				if(self$isRegistered(tmp_out)) return(self$getRegistered(tmp_out))
				 -->
				# wrap return java object in R6 class 
				out = ${class.getSimpleName()}$new(
					self$.fromJava$${method.getReturnType().getSimpleName()}(tmp_out),
					self
				);
				return(out)
			<#else>
				# convert java object back to R
				out = self$.fromJava$${method.getReturnType().getSimpleName()}(tmp_out);
				if(is.null(out)) return(invisible(out))
				return(out)
			</#if>
			}<#sep>,
	</#list>
	)
</#list>
	}
))

JavaApi$singleton = NULL

JavaApi$get = function(logLevel = <#if model.getConfig().getDebugMode()>"DEBUG"<#else>"${model.getConfig().getDefaultLogLevel()}"</#if>) {
	if (is.null(JavaApi$singleton)) {
		# set to non-null so that R6 constructor will work
		JavaApi$singleton = FALSE 
		JavaApi$singleton = JavaApi$new(logLevel)
	}
	return(JavaApi$singleton)
}

