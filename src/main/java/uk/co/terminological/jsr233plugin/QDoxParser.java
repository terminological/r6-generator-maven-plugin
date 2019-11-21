package uk.co.terminological.jsr233plugin;


import java.io.File;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.apache.maven.plugin.MojoExecutionException;

import com.thoughtworks.qdox.JavaProjectBuilder;
import com.thoughtworks.qdox.library.ClassLibraryBuilder;
import com.thoughtworks.qdox.library.SortedClassLibraryBuilder;
import com.thoughtworks.qdox.model.JavaAnnotatedElement;
import com.thoughtworks.qdox.model.JavaAnnotation;
import com.thoughtworks.qdox.model.JavaClass;
import com.thoughtworks.qdox.model.JavaConstructor;
import com.thoughtworks.qdox.model.JavaMethod;
import com.thoughtworks.qdox.model.JavaParameter;
import com.thoughtworks.qdox.model.JavaParameterizedType;

import uk.co.terminological.jsr223.RClass;
import uk.co.terminological.jsr223.RMethod;
import uk.co.terminological.jsr233plugin.RModel.Method;



public class QDoxParser {

	private JavaProjectBuilder jpb;
	ClassLibraryBuilder libraryBuilder;
	
	QDoxParser() {
		libraryBuilder = new SortedClassLibraryBuilder(); 
		libraryBuilder.appendDefaultClassLoaders();
		jpb = new JavaProjectBuilder( libraryBuilder );
		
	}

	public static Optional<RModel> scanModel(List<?> list, PackageData config) throws MojoExecutionException {
		QDoxParser out = new QDoxParser();
		return out.scanSourceModel(
			list.stream().map(o -> {
				if (o instanceof File) return (File) o;
				else return new File(o.toString());
			}).collect(Collectors.toList()),
			config
		);
		
	}

	private Optional<RModel> scanSourceModel(List<File> sourceFolders, PackageData config) throws MojoExecutionException {
		RModel out = new RModel(config);
		sourceFolders.forEach(sf -> jpb.addSourceTree(sf));
		jpb.getClasses().stream().map(c->c.getCanonicalName()).forEach(System.out::println);
		for (JavaClass clazz: jpb.getClasses()) {
			if (hasAnnotation(RClass.class,clazz)) {
				RModel.Type type = createClass(clazz,out);
				out.getClassTypes().add(type);
			}
		}
		if (out.getClassTypes().isEmpty()) {
			return Optional.empty();
		}
		return Optional.ofNullable(out);
	}
	

	private static boolean isSupported(JavaClass clazz) {
		String tmp1 = clazz.getCanonicalName().replace("[", "").replace("]", "");
		boolean tmp2 = clazz.isArray();
		List<String> tmp3 = ((JavaParameterizedType) clazz).getActualTypeArguments().stream().map(
				tp -> tp.getFullyQualifiedName()
		).collect(Collectors.toList());
		return RModel.isSupportedOutput(tmp1,tmp2,tmp3.toArray(new String[] {}));
	}
	
	private static boolean isSupported(JavaParameter param) {
		String tmp1 = param.getCanonicalName().replace("[", "").replace("]", "");
		boolean tmp2 = param.getJavaClass().isArray();
		List<String> tmp3 = ((JavaParameterizedType) param.getType()).getActualTypeArguments().stream().map(
				tp -> tp.getFullyQualifiedName()
		).collect(Collectors.toList());
		return RModel.isSupportedInput(tmp1,tmp2,tmp3.toArray(new String[] {}));
	}
	
	public RModel.Method createMethod(JavaMethod m, RModel model) {
		RModel.Method out = new RModel.Method(model);
		
		getAnnotation(RMethod.class,m).ifPresent(
				a -> a.getNamedParameterMap().forEach((k,v) ->
							out.mergeAnnotations(k, v)));
		
		out.setDescription(
				(m.getComment() == null ? "" :
					m.getComment().trim()));
		
		m.getTags().forEach(dt -> out.mergeAnnotations(dt));
				
		
		out.setName(m.getName());
		
		out.setByValue(isSupported(m.getReturns()));
		out.setReturnType(m.getReturns().getFullyQualifiedName());
		out.setReturnSimple(m.getReturns().getValue()); //
		out.setStatic(m.isStatic());
		
		m.getParameters().stream().forEach(
				jp -> {
					out.getParameterTypes().add(jp.getType().getGenericCanonicalName());
					out.getParameterNames().add(jp.getName());
					out.getParameterByValues().add(isSupported(jp));
					}
				);
		
		return out;

	}

	public RModel.Type createClass(JavaClass clazz, RModel model) throws MojoExecutionException {
		RModel.Type out = new RModel.Type(model);
		
		getAnnotation(RClass.class,clazz).ifPresent(
				a -> a.getNamedParameterMap().forEach((k,v) ->
							out.mergeAnnotations(k, v)));
		
		out.setDescription(
				(clazz.getComment() == null ? "" :
					clazz.getComment().trim().split("\n")[0]));
		out.setDetails(
				(clazz.getComment() == null  ? "" :
					(clazz.getComment().isEmpty() ? "" :
						clazz.getComment().trim().substring(
								Math.max(0, clazz.getComment().trim().indexOf("\n"))))));
		
		out.setClassName(clazz.getCanonicalName());
		out.setName(clazz.getName());
		clazz.getTags().forEach(dt -> out.mergeAnnotations(dt));
		
		if (clazz.getConstructors().isEmpty()) {
			// Use a default no-arg constructor if there are none defined
			out.setConstructor(this.noargsConstructor(clazz,model));
		} else {
			// Use the first annotated construcotr
			clazz.getConstructors().stream().filter(c ->
				c.isPublic() && hasAnnotation(RMethod.class,c)
					).findFirst().ifPresent(c ->
						out.setConstructor(createConstructor(c,model))
					);
			// Otherwise ise the first no-args constructor
			if (out.getConstructor() == null) {
				clazz.getConstructors().stream().filter(c ->
						c.isPublic() && c.getParameters().isEmpty()
						).findFirst().ifPresent(c ->
							out.setConstructor(createConstructor(c,model))
								);
			}
			//Otherwise use the first public
			if (out.getConstructor() == null) {
				clazz.getConstructors().stream().filter(c ->
						c.isPublic()
						).findFirst().ifPresent(c ->
							out.setConstructor(createConstructor(c,model))
								);
			}			
			//Otherwise there is no appropriate constructor.
			if (out.getConstructor() == null) {
				throw new MojoExecutionException("No appropriate constructor defined for "+clazz.getCanonicalName());
			}
		}
		
		
		for (JavaMethod m: clazz.getMethods(true)) {
			if (m.isPublic() && hasAnnotation(RMethod.class,m)) {
				RModel.Method method = createMethod(m,model);
				out.addMethod(method);
			}
		}
		
		return out;
	}

	private Method noargsConstructor(JavaClass c, RModel model) {
		RModel.Method out = new RModel.Method(model);
		out.setName("new");
		out.setDescription("the default no-args constructor");
		out.setStatic(true);
		out.setByValue(false);
		out.setReturnType(c.getFullyQualifiedName());
		out.setReturnSimple(c.getValue());
		return out;
	}
			
	private Method createConstructor(JavaConstructor m, RModel model) {
		RModel.Method out = new RModel.Method(model);
		
		getAnnotation(RMethod.class,m).ifPresent(
				a -> a.getNamedParameterMap().forEach((k,v) ->
							out.mergeAnnotations(k, v)));
		
		out.setDescription(
				(m.getComment() == null ? "" :
					m.getComment().trim()));
		
		m.getTags().forEach(dt -> out.mergeAnnotations(dt));
		
		out.setName("new");
		out.setByValue(false);
		out.setReturnType(m.getDeclaringClass().getFullyQualifiedName());
		out.setReturnSimple(m.getDeclaringClass().getValue());
		out.setStatic(true);
		
		m.getParameters().stream().forEach(
				jp -> {
					out.getParameterTypes().add(jp.getType().getGenericCanonicalName());
					out.getParameterNames().add(jp.getName());
					out.getParameterByValues().add(isSupported(jp));
					}
				);
		
		return out;
	}

	private static boolean hasAnnotation(Class<? extends java.lang.annotation.Annotation> ann, JavaAnnotatedElement elem) {
		return getAnnotation(ann,elem).isPresent();
	}

	private static Optional<JavaAnnotation> getAnnotation(Class<? extends java.lang.annotation.Annotation> ann, JavaAnnotatedElement elem) {
		
		for (JavaAnnotation a: elem.getAnnotations()) {
			String aname = a.getType().getValue();
			String jname = ann.getCanonicalName();
			if (jname.endsWith(aname)) return Optional.of(a);
		}
		return Optional.empty();
		
	}

	

	
}
