package uk.co.terminological.jsr233plugin;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import java.util.Collections;

public class RModel {

	private Type type;
	
	public void setClassType(Type type) {
		this.type=type;
	}
	
	public Type getClassType() {
		return type;
	}
	
	public static class Method {
		
		private Map<String,Object> annotations = new HashMap<>(); 
		private String methodName;
		private List<String> parameterNames = new ArrayList<>();
		private List<String> parameterTypes = new ArrayList<>();
		private String returnType;
		private boolean isFluent = false;
		
		public String getReturnType() {
			return returnType;
		}
		public String getName() {
			return methodName;
		}
		public List<String> getParameterNames() {
			return parameterNames;
		}
		public List<String> getParameterTypes() {
			return parameterTypes;
		}
		public void setName(String methodName) {
			this.methodName = methodName;
		}
		public Map<String, Object> getAnnotations() {
			return annotations;
		}
		public void setReturnType(String canonicalName) {
			this.returnType = canonicalName;	
		}
		public boolean isFluent() {
			return isFluent;
		}
		public void setFluent(boolean isFluent) {
			this.isFluent = isFluent;
		}
		
		public String getValue(String s) {
			return annotations.get(s) == null ? "" : stripQuotes((String) annotations.get(s));
		}
		
		@SuppressWarnings("unchecked")
		public List<String> getList(String s) {
			return annotations.get(s) == null ? 
					Collections.emptyList() : 
						((List<String>) annotations.get(s)).stream().map(RModel::stripQuotes)
						.collect(Collectors.toList()) ;
		}
		
	}
	
	private static String stripQuotes(String s) {
		return s.replace("\"", "");
	}
	
	public static class Type {
		
		private Map<String,Object> annotations = new HashMap<>();
		private String className;
		private List<Method> methods = new ArrayList<>();
		private String name;
		
		public Map<String, Object> getAnnotations() {
			return annotations;
		}
		public String getClassName() {
			return className;
		}
		public List<Method> getMethods() {
			return methods;
		}
		public void setClassName(String className) {
			this.className = className;
		}
		public void addMethod(Method method) {
			methods.add(method);
		}
		public String getName() {
			return name;
		}
		public void setName(String name) {
			this.name = name;
		}
		
		public Set<String> getMethodImports() {
			return getMethods().stream().flatMap(m ->  
				(m.getAnnotations().get("imports") != null) ?
				Stream.of((String[]) m.getAnnotations().get("imports")) :
				Stream.empty()
			).collect(Collectors.toSet());
		}
		
		public String getValue(String s) {
			return annotations.get(s) == null ? "" : stripQuotes((String) annotations.get(s));
		}
		
		@SuppressWarnings("unchecked")
		public List<String> getList(String s) {
			return annotations.get(s) == null ? 
					Collections.emptyList() : 
						((List<String>) annotations.get(s)).stream().map(RModel::stripQuotes)
						.collect(Collectors.toList()) ;
		}
	}

	

}
