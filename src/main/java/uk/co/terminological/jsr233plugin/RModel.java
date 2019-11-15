package uk.co.terminological.jsr233plugin;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import com.thoughtworks.qdox.model.DocletTag;

import edu.emory.mathcs.backport.java.util.Arrays;

public class RModel {

	private List<Type> types = new ArrayList<>();
	private JS223Execution config;
		
	public RModel(JS223Execution config) {
		this.config = config;
	}

	public List<Type> getClassTypes() {
		return types;
	}
	
	public Set<String> getImports() {
		return getClassTypes().stream().flatMap(m ->  
			m.getList("imports").stream()
		).collect(Collectors.toSet());
	}
	
	public Set<String> getSuggests() {
		return getClassTypes().stream().flatMap(m ->  
			m.getList("suggests").stream()
		).collect(Collectors.toSet());
	}
	
	public JS223Execution getConfig() {
		return config;
	}
	
	public List<String[]> getAuthors() {
		ArrayList<String[]> tmp = new ArrayList<>();
		tmp.add(new String[] {
				getConfig().getMaintainerName(),
				getConfig().getMaintainerFamilyName(),
				getConfig().getMaintainerEmail()});
		tmp.add(new String[] {"r-jsr223-maven-plugin","terminological"});
		tmp.addAll(this.getClassTypes().stream().map(t -> t.getAuthor()).filter(a -> (
				a != null && a.length > 0
		)).collect(Collectors.toSet()));
		return tmp;
	}

	public static class Annotated {
		private Map<String,List<String>> annotations = new HashMap<>(); 
		public Map<String, List<String>> getAnnotations() {
			return annotations;
		}
		public String getValue(String s) {
			return getList(s).stream().collect(Collectors.joining("\n"));
		}
		
		public List<String> getList(String s) {
			return annotations.get(s) == null ? 
				Collections.emptyList() : 
					annotations.get(s).stream().map(RModel::stripQuotes)
					.collect(Collectors.toList()) ;
		}
		
		public void mergeAnnotations(DocletTag dt) {
			mergeAnnotations(dt.getName(), dt.getValue());
		}
		
		@SuppressWarnings("unchecked")
		public void mergeAnnotations(String key, Object value) {
			List<String> tmp;
			if (value instanceof Collection<?>) {
				tmp = ((Collection<?>) value).stream().map(o -> o.toString()).collect(Collectors.toList());
			} else if (value instanceof String[]) {
				tmp = Arrays.asList((String[]) value);
			} else {
				tmp = Collections.singletonList(value.toString());
			} 
			getAnnotations().merge(
				key, tmp, 
				(oldV,newV) -> {
					List<String> out = new ArrayList<>(oldV);
					out.addAll(newV);
					return out;
				}
			);
		}
	}
	
	public static class Method extends Annotated {
		
		
		private String methodName;
		private List<String> parameterNames = new ArrayList<>();
		private List<String> parameterTypes = new ArrayList<>();
		private String returnType;
		private boolean isFluent = false;
		private String description;
		
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
		public void setReturnType(String canonicalName) {
			this.returnType = canonicalName;	
		}
		public boolean isFluent() {
			return isFluent;
		}
		public void setFluent(boolean isFluent) {
			this.isFluent = isFluent;
		}
		public String getDescription() {
			return description;
		}
		public void setDescription(String string) {
			this.description = string;
		}
		public String getParameterDescription(String paramName) {
			if (this.getAnnotations().get("param") == null) return paramName;
			String tmp = this.getAnnotations().get("param").stream()
				.filter(val -> val.trim().startsWith(paramName))
				.collect(Collectors.joining());
			return tmp.isEmpty() ? paramName : tmp;
		}
		public String getParameterType(String paramName) {
			return getParameterTypes().get(getParameterNames().indexOf(paramName));
		}
		
	}
	
	private static String stripQuotes(String s) {
		return s.replace("\"", "");
	}
	
	public static class Type  extends Annotated {
		
		private String className;
		private List<Method> methods = new ArrayList<>();
		private String name;
		private String description;
		private String details;
		private Method constructor;
		
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
		public void setDescription(String string) {
			this.description = string;
		}
		public void setDetails(String substring) {
			this.details = substring;	
		}
		public String getDetails() {
			return details;
		}
		public String getDescription() {
			return description;
		}
		public String[] getAuthor() {
			if (this.getValue("author").isEmpty()) return new String[] {};
			return this.getValue("author").split("\\s");
		}
		public Method getConstructor() {
			return constructor;
		}
		public void setConstructor(Method constructor) {
			this.constructor = constructor;
		}
		public List<Method> getConstructorAndMethods() {
			List<Method> out = new ArrayList<Method>();
			out.add(getConstructor());
			out.addAll(getMethods());
			return out;
		}
	}

	

}
