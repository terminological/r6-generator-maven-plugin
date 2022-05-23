package uk.co.terminological.rjava.plugin;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.commons.text.StringEscapeUtils;

public class RMethod extends RAnnotated {
			
	private String methodName;
	private LinkedHashMap<String,RType> parameters = new LinkedHashMap<>();
	private LinkedHashMap<String,String> defaults = new LinkedHashMap<>();
	private RType returnType;
	private boolean isStatic;
	private String description;
	
	public RMethod(RModel model, Map<String,Object> annotations, String mname, String description, boolean isStatic) {
		super(model, annotations, null, mname);
		this.methodName = mname;
		this.description = description;
		this.isStatic = isStatic;
	}
	
	public void setReturnType(RType returnType) {this.returnType = returnType;}
	public void addParameter(String name, RType parameterType, String defaultExpr) {
		parameters.put(name, parameterType);
		defaults.put(name, defaultExpr);
	}
	
	public RType getReturnType() {
		return returnType;
	}
	public String getName() {
		return methodName;
	}
	public List<String> getParameterNames() {
		return new ArrayList<String>(parameters.keySet());
	}
	public List<RType> getParameterTypes() {
		return new ArrayList<RType>(parameters.values());
	}
	
	public boolean isFactory() {
		return this.getModel().getClassTypes().stream()
				.anyMatch(s2 -> s2.getCanonicalName().equals(this.getReturnType().getCanonicalName()));
	}
	public String getDescription() {
		return description == null || description.isEmpty() ? "no description" : description;
	}
	public String getParameterDescription(String paramName) {
		if (this.getAnnotations().get("param") == null) return paramName;
		String tmp = this.getAnnotations().get("param").stream()
			.filter(val -> val.trim().startsWith(paramName))
			.collect(Collectors.joining());
		String defaultValue = defaults.get(paramName);
		defaultValue = defaultValue == null ? null : defaultValue.replaceAll("^\\s*\"|\"\\s*$", "").replaceAll("\\\\", "\\\\\\\\");
		defaultValue = defaultValue == null ? "" : " - (defaulting to "+defaultValue+")";  
		return tmp.isEmpty() ? paramName+defaultValue : tmp+defaultValue;
	}
	public RType getParameterType(String paramName) {
		return parameters.get(paramName);
	}
	public boolean isStatic() {
		return isStatic;
	}
	
	public String getParameterCsv() {
		return getParameterNames().stream().collect(Collectors.joining(", "));
	}
	public String getParameterCsv(String pre) {
		if (this.parameters.size() == 0) return "";
		return ", "+getParameterNames().stream().map(s->pre+s).collect(Collectors.joining(", "));
	}
	public String getFunctionParameterCsv() {
		return defaults.entrySet().stream()
				.map(kv -> {
					String tmp = kv.getKey();
					if (kv.getValue() != null) {
						tmp = tmp + "="+
								StringEscapeUtils.unescapeJava(kv.getValue()).replaceAll("^\\s*\"|\"\\s*$", "");
					}
					return tmp;		
				})
				.collect(Collectors.joining(", "));
	}
	
}