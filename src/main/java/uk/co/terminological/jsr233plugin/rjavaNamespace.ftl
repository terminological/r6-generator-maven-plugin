# Generated by maven-r-jsr223-plugin: do not edit by hand

export(JavaApi)
<#list model.getClassTypes() as class>
export(${class.getSimpleName()})
</#list>
<#list model.getImports() as import>
import(${import})
</#list>
import(rJava)
import(magrittr)