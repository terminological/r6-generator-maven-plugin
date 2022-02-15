package uk.co.terminological.rjava.plugin;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.thoughtworks.qdox.model.JavaType;

public class RClass extends RAnnotated {
	
	private List<RMethod> methods = new ArrayList<>();
	private String description;
	private String details;
	private RMethod constructor;
	private RModel model;
	private String finalizer;
	
	public RClass(RModel model, Map<String,Object> annotations, JavaType type, String name, int dimensions, String description, String details) {
		super(model,annotations,type,name);
		this.description = description;
		this.details = details;
	}
	
	public void setConstructor(RMethod constructor) {
		this.constructor = constructor;
	}
	
	public void addMethod(RMethod method) {
		methods.add(method);
	}
	
	public List<RMethod> getMethods() {
		return methods;
	}
	public String getTitle() {
		return (this.getAnnotationValue("title").isEmpty() ? this.getSimpleName() :
			this.getAnnotationValue("title"));
	}
	
	public String getDetails() {
		return details == null || details.isEmpty() ? "no details" : details;
	}
	public String getDescription() {
		return description == null || description.isEmpty() ? "missing description" : description;
	}
	public String[] getAuthor() {
		if (this.getAnnotationValue("author").isEmpty()) return new String[] {};
		return this.getAnnotationValue("author").split("\\s");
	}
	public RMethod getConstructor() {
		return constructor;
	}
	public List<RMethod> getConstructorAndMethods() {
		List<RMethod> out = new ArrayList<RMethod>();
		out.add(getConstructor());
		out.addAll(getMethods());
		return out;
	}
	public List<RMethod> getConstructorsAndStaticMethods() {
		List<RMethod> out = new ArrayList<RMethod>();
		out.add(getConstructor());
		out.addAll(getStaticMethods());
		return out;
	}
	public List<RMethod> getStaticMethods() {
		return getMethods().stream().filter(m -> m.isStatic()).collect(Collectors.toList());
	}
	
	public boolean hasStaticMethods() {
		return !getStaticMethods().isEmpty();
	}
	public List<RMethod> getInstanceMethods() {
		return getMethods().stream().filter(m -> !m.isStatic()).collect(Collectors.toList());
	}
	public RModel getModel() {
		return model;
	}

	public void addFinalizer(String name) {
		this.finalizer = name;		
	}

	public String getFinalizer() {
		return this.finalizer;		
	}
	
	public boolean hasFinalizer() {
		return (this.finalizer != null);
	}
}