package uk.co.terminological.jsr233plugin;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import com.thoughtworks.qdox.model.DocletTag;

import uk.co.terminological.jsr223.ROutput;

public class RModel {

	private List<Type> types = new ArrayList<>();
	private PackageData config;
	
	private static List<String> supportedLengthOneOutputs = Arrays.asList(
			Integer.class.getCanonicalName(), 
			int.class.getCanonicalName(),
			String.class.getCanonicalName(),
			Byte.class.getCanonicalName(), 
			byte.class.getCanonicalName(),
			Character.class.getCanonicalName(), 
			char.class.getCanonicalName(),
			Double.class.getCanonicalName(), 
			double.class.getCanonicalName(),
			Float.class.getCanonicalName(), 
			float.class.getCanonicalName(),
			BigDecimal.class.getCanonicalName(),
			BigInteger.class.getCanonicalName(),
			Short.class.getCanonicalName(), 
			short.class.getCanonicalName(),
			Long.class.getCanonicalName(), 
			long.class.getCanonicalName(),
			ROutput.Dataframe.class.getCanonicalName(),
			ROutput.ColMajorDataframe.class.getCanonicalName()
	);
	
	private static List<String> supportedLengthOneInputs = Arrays.asList(
			Double.class.getCanonicalName(),
			Integer.class.getCanonicalName(), 
			String.class.getCanonicalName(),
			Boolean.class.getCanonicalName(), 
			Byte.class.getCanonicalName(),
			double.class.getCanonicalName(),
			int.class.getCanonicalName(), 
			boolean.class.getCanonicalName(), 
			byte.class.getCanonicalName(),
			"double",
			"int", 
			"boolean", 
			"byte"
	);
	private static List<String> supportedArrayInputs = Arrays.asList(
			double.class.getCanonicalName(), 
			int.class.getCanonicalName(), 
			String.class.getCanonicalName(), 
			boolean.class.getCanonicalName(), 
			byte.class.getCanonicalName()
	);
	
	public static boolean isSupportedInput(String fqn, boolean isArray, String[] genericTypes) {
		return isSupportedInput(fqn,isArray, genericTypes,true); //defaults to rowMajor
	}
	
	public static boolean isSupportedInput(String fqn, boolean isArray, String[] genericTypes, boolean rowMajor) {
		if (!isArray && genericTypes.length == 0) {
			if (supportedLengthOneInputs.contains(fqn)) return true;
			else return false;
		} else {
			if (isArray) {
				return supportedArrayInputs.contains(fqn);
			}
			//Generic type
			if (fqn.contains("Set")) {
				return supportedLengthOneInputs.contains(genericTypes[0]);
			}
			if (fqn.contains("List")) {
				if (rowMajor && genericTypes[0].contains("Map")) return true; //list of maps possible from data frame
				//TODO: log something if this is a col major.
				return supportedLengthOneInputs.contains(genericTypes[0]);
			}
			if (fqn.contains("Map")) {
				if (
					!rowMajor
					&& genericTypes[0].equals("java.lang.String")
					&& genericTypes[1].contains("Object[]")
					//TODO: log something if this is a row major.
				) return true; //map<String,Object[]> possible from data frame
				if (
					genericTypes[0].equals("java.lang.String")
					&& supportedLengthOneInputs.contains(genericTypes[1])
				) return true;
			}
		}
		return false;
	}
	
	//fqn here is either Object.class.getCanonicalName())
	// or if array Object[].class.getComponentType().getCanonicalName()
	public static boolean isSupportedOutput(String fqn, boolean isArray, String[] genericTypes) {
		if (genericTypes.length == 0) {
			if (supportedLengthOneOutputs.contains(fqn)) return true;
			else return false;
		} else {
			if (fqn.contains("Set")) {
				return supportedLengthOneOutputs.contains(genericTypes[0]);
			}
			if (fqn.contains("List")) {
				if (genericTypes[0].contains("Map")) return true; //list of maps possible from data frame
				return supportedLengthOneOutputs.contains(genericTypes[0]);
			}
			if (fqn.contains("Map")) {
				if (
					genericTypes[0].equals("java.lang.String")
					&& genericTypes[1].contains("Object")
				) return true; //map<String,Object[]> possible from data frame
				if (
					genericTypes[0].equals("java.lang.String")
					&& supportedLengthOneOutputs.contains(genericTypes[1])
				) return true;
			}
		}
		return false;
	}
	
	public RModel(PackageData config) {
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
	
	public PackageData getConfig() {
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

	public boolean definesClass(String classname) {
		return this.types.stream().map(t -> t.getClassName()).anyMatch(cn -> cn.equals(classname));
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
		private List<Boolean> parameterByValue = new ArrayList<>();
		private String returnType;
		private boolean isStatic = false;
		private boolean byValue = false;
		private String description;
		private String returnSimple;
		private RModel model;
		
		public void setByValue(boolean byValue) {
			this.byValue = byValue;
		}
		public boolean byValue() {
			return this.byValue;
		}
		public Method(RModel model) {
			this.model = model;
		}
		public String getReturnSimple() {
			if (byValue || 
					model.types.stream().map(s -> s.getClassName()).anyMatch(s2 -> s2.equals(this.getReturnType()))) {
				return returnSimple;
			} else return "Object";
		}
		public void setReturnSimple(String returnSimple) {
			this.returnSimple = returnSimple;
		}
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
		public boolean isFactory() {
			return model.types.stream().map(s -> s.getClassName()).anyMatch(s2 -> s2.equals(this.getReturnType()));
		}
		public String getDescription() {
			return description;
		}
		public void setDescription(String string) {
			this.description = string;
		}
		public String explicitCast() {
			if(this.returnType.contains("Dataframe")) return "(List<Map<String,Object>>)";
			return("");
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
		
		public boolean getParameterByValue(String paramName) {
			return getParameterByValues().get(getParameterNames().indexOf(paramName));
		}
		
		public List<Boolean> getParameterByValues() {
			return parameterByValue;
		}

		public void setStatic(boolean static1) {
			this.isStatic = static1;
		}
		public boolean isStatic() {
			return isStatic;
		}
		
		public String getParameterCsv() {
			return getParameterNames().stream().collect(Collectors.joining(", "));
		}
		public String getParameterCsv(String pre) {
			return getParameterNames().stream().map(s->pre+s).collect(Collectors.joining(", "));
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
		private RModel model;
		
		public Type(RModel model) {
			this.setModel(model);
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
		public List<Method> getConstructors() {
			return Collections.singletonList(constructor); //TODO: extend to multiple constructors
		}
		public void setConstructor(Method constructor) {
			this.constructor = constructor;
		}
		public List<Method> getConstructorAndMethods() {
			List<Method> out = new ArrayList<Method>();
			out.addAll(getConstructors());
			out.addAll(getMethods());
			return out;
		}
		public List<Method> getConstructorsAndStaticMethods() {
			List<Method> out = new ArrayList<Method>();
			out.addAll(getConstructors());
			out.addAll(getStaticMethods());
			return out;
		}
		
		public List<Method> getStaticMethods() {
			return getMethods().stream().filter(m -> m.isStatic()).collect(Collectors.toList());
		}
		public boolean hasStaticMethods() {
			return !getStaticMethods().isEmpty();
		}
		
		public List<Method> getInstanceMethods() {
			return getMethods().stream().filter(m -> !m.isStatic()).collect(Collectors.toList());
		}

		public RModel getModel() {
			return model;
		}

		public void setModel(RModel model) {
			this.model = model;
		}
	}

	

}
