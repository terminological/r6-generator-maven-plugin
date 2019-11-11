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
import com.thoughtworks.qdox.model.JavaMethod;

import uk.co.terminological.jsr223.RClass;
import uk.co.terminological.jsr223.RMethod;



public class QDoxParser {

	private JavaProjectBuilder jpb;
	ClassLibraryBuilder libraryBuilder;
	
	QDoxParser() {
		libraryBuilder = new SortedClassLibraryBuilder(); 
		libraryBuilder.appendDefaultClassLoaders();
		jpb = new JavaProjectBuilder( libraryBuilder );
		
	}

	public static Optional<RModel> scanModel(List<?> list) throws MojoExecutionException {
		QDoxParser out = new QDoxParser();
		return out.scanSourceModel(
			list.stream().map(o -> {
				if (o instanceof File) return (File) o;
				else return new File(o.toString());
			}).collect(Collectors.toList())
		);
		
	}

	private Optional<RModel> scanSourceModel(List<File> sourceFolders) throws MojoExecutionException {
		RModel out = null;
		sourceFolders.forEach(sf -> jpb.addSourceTree(sf));
		jpb.getClasses().stream().map(c->c.getCanonicalName()).forEach(System.out::println);
		for (JavaClass clazz: jpb.getClasses()) {
			if (hasAnnotation(RClass.class,clazz)) {
				RModel.Type type = createClass(clazz);
				if (out != null) throw new MojoExecutionException("More than one R API classes annotated");
				out = new RModel();
				out.setClassType(type);
			}
		}
		return Optional.ofNullable(out);
	}

	public RModel.Method createMethod(JavaMethod m) {
		RModel.Method out = new RModel.Method();
		
		getAnnotation(RMethod.class,m).ifPresent(
				a -> a.getNamedParameterMap().forEach((k,v) ->
							out.getAnnotations().put(k, v)));
		
		out.setName(m.getName());
		out.setReturnType(m.getReturns().getCanonicalName());
		out.setFluent(m.getReturns().equals(m.getDeclaringClass()));
		
		m.getParameters().stream().forEach(
				jp -> {
					out.getParameterTypes().add(jp.getType().getGenericCanonicalName());
					out.getParameterNames().add(jp.getName());}
				);
		
		return out;

	}

	public RModel.Type createClass(JavaClass clazz) {
		RModel.Type out = new RModel.Type();
		
		getAnnotation(RClass.class,clazz).ifPresent(
				a -> a.getNamedParameterMap().forEach((k,v) ->
							out.getAnnotations().put(k, v)));
		
		out.setClassName(clazz.getCanonicalName());
		out.setName(clazz.getName());
		
		for (JavaMethod m: clazz.getMethods(true)) {
			if (hasAnnotation(RMethod.class,m)) {
				RModel.Method method = createMethod(m);
				out.addMethod(method);
			}
		}
		
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
