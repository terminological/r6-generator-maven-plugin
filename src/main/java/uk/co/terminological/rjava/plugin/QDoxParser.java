package uk.co.terminological.rjava.plugin;


import java.io.File;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.logging.Log;
import org.reflections.Reflections;

import com.thoughtworks.qdox.JavaProjectBuilder;
import com.thoughtworks.qdox.library.ClassLibraryBuilder;
import com.thoughtworks.qdox.library.SortedClassLibraryBuilder;
import com.thoughtworks.qdox.model.JavaAnnotatedElement;
import com.thoughtworks.qdox.model.JavaAnnotation;
import com.thoughtworks.qdox.model.JavaClass;
import com.thoughtworks.qdox.model.JavaConstructor;
import com.thoughtworks.qdox.model.JavaMethod;
import com.thoughtworks.qdox.model.JavaParameter;
import com.thoughtworks.qdox.model.JavaType;
import com.thoughtworks.qdox.model.impl.DefaultJavaClass;
import com.thoughtworks.qdox.model.impl.DefaultJavaType;

import uk.co.terminological.rjava.RDataType;
import uk.co.terminological.rjava.types.RObject;




/** The business end of the library.
 * This scans source code using QDox using the static {@link QDoxParser.scanModel} method
 * which is called by the main maven plugin class
 * @author terminological
 *
 */
public class QDoxParser {

	private JavaProjectBuilder jpb;
	private ClassLibraryBuilder libraryBuilder;
	Log log;
	
	QDoxParser(Log log) {
		this.log= log;
		libraryBuilder = new SortedClassLibraryBuilder(); 
		libraryBuilder.appendDefaultClassLoaders();
		jpb = new JavaProjectBuilder( libraryBuilder );
		
	}

	public static Optional<RModel> scanModel(List<?> list, PackageData config, Log log) throws MojoExecutionException {
		QDoxParser out = new QDoxParser(log);
		return out.scanSourceModel(
			list.stream().map(o -> {
				if (o instanceof File) return (File) o;
				else return new File(o.toString());
			}).collect(Collectors.toList()),
			config
		);
		
	}
	
	public Optional<JavaClass> javaClassFor(Class<?> clazz) {
		return Optional.ofNullable(jpb.getClassByName(clazz.getCanonicalName()));
	}
	
	public Optional<Class<?>> reflectionClassFor(JavaType type) {
		try {
			return Optional.of(Class.forName(type.getCanonicalName()));
		} catch (ClassNotFoundException e) {
			// TODO Auto-generated catch block
			return Optional.empty();
		}
		
	}

	private Optional<RModel> scanSourceModel(List<File> sourceFolders, PackageData config) throws MojoExecutionException {
		RModel out = new RModel(config, this);
		
		// TODO: if we add more supported primitive types in {@link RType} we need to update
		// this here as well
		out.addDataType(RType.doubleType(out));
		out.addDataType(RType.voidType(out));
		out.addDataType(RType.intType(out));
		out.addDataType(RType.stringType(out));
		out.addDataType(RType.booleanType(out));
		
		Reflections refl = new Reflections("uk.co.terminological.rjava");
		for (Class<?> c :refl.getTypesAnnotatedWith(uk.co.terminological.rjava.RDataType.class)) {
			RType type = createType(c,out);
			out.addDataType(type);
		}
			
		jpb.addClassLoader( ClassLoader.getSystemClassLoader() );
		sourceFolders.forEach(sf -> jpb.addSourceTree(sf));
		jpb.getClasses().stream().map(c->c.getCanonicalName()).forEach(log::info);
		
		// set up all classes as types
		// This was done cos having RClass inherit RType turned out to be cumbersome
		// as types largely set up by reflection from different package
		// Types and classes are kept seperate.
		for (JavaClass clazz: jpb.getClasses()) {
			if (hasAnnotation(uk.co.terminological.rjava.RClass.class,clazz)) {
				RType type = createType(clazz,out);
				out.addDataType(type);
			}
		}
		
		log.debug("Setting up R classes");
		//set up classes
		for (JavaClass clazz: jpb.getClasses()) {
			if (hasAnnotation(uk.co.terminological.rjava.RClass.class,clazz)) {
				RClass clz = createClass(clazz,out);
				out.getClassTypes().add(clz);
			}
		}
		
		if (out.getClassTypes().isEmpty()) {
			return Optional.empty();
		}
		return Optional.ofNullable(out);
	}
	
	/** The biggest part of this is parsing the class files
	 * @param clazz
	 * @param model
	 * @return
	 * @throws MojoExecutionException
	 */
	public RClass createClass(JavaClass clazz, RModel model) throws MojoExecutionException {
		try {
			//Annotation parsing is painful with QDox but you can get all the taglets
			Map<String,Object> annotationMap = getAnnotation(uk.co.terminological.rjava.RClass.class,clazz).map(a -> a.getNamedParameterMap()).get();
			String description = (clazz.getComment() == null ? "" :
						clazz.getComment().trim().split("\n")[0]);
			String details = (clazz.getComment() == null  ? "" :
						(clazz.getComment().isEmpty() ? "" :
							clazz.getComment().trim().substring(
									Math.max(0, clazz.getComment().trim().indexOf("\n")))));
			
			RClass out = new RClass(
					model,
					annotationMap,
					clazz,
					clazz.getName(),
					0,
					description,details);
			
			clazz.getTags().forEach(dt -> out.mergeAnnotations(dt.getName(), dt.getValue()));
			
			// Identify constructors
			if (clazz.getConstructors().isEmpty()) {
				// Use a default no-arg constructor if there are none defined
				out.setConstructor(this.noargsConstructor(clazz,model));
			} else {
				// Use the first annotated construcotr
				{
					Optional<JavaConstructor> tmp = clazz.getConstructors().stream().filter(c ->
						c.isPublic() && hasAnnotation(uk.co.terminological.rjava.RMethod.class,c)
							).findFirst();
					if(tmp.isPresent()) {
						out.setConstructor(createConstructor(tmp.get(),model));
					};
				}
				// Otherwise ise the first no-args constructor
				if (out.getConstructor() == null) {
					Optional<JavaConstructor> tmp = clazz.getConstructors().stream().filter(c ->
							c.isPublic() && c.getParameters().isEmpty()
							).findFirst();
					if (tmp.isPresent()) out.setConstructor(createConstructor(tmp.get(),model));
				}
				//Otherwise use the first public
				if (out.getConstructor() == null) {
					Optional<JavaConstructor> tmp = clazz.getConstructors().stream().filter(c ->
							c.isPublic()
							).findFirst();
					if (tmp.isPresent()) out.setConstructor(createConstructor(tmp.get(),model));
				}
				//Otherwise there is no appropriate constructor.
				if (out.getConstructor() == null) {
					throw new MojoExecutionException("No appropriate constructor defined for "+clazz.getCanonicalName());
				}
			}
			
			//Identify methods
			for (JavaMethod m: clazz.getMethods(true)) {
				if (m.isPublic() && hasAnnotation(uk.co.terminological.rjava.RMethod.class,m)) {
					RMethod method = createMethod(m,model);
					out.addMethod(method);
				}
			}
			
			//Identify methods
			for (JavaMethod m: clazz.getMethods(true)) {
				boolean foundAlready = false;
				if (m.isPublic() && hasAnnotation(uk.co.terminological.rjava.RFinalize.class,m)) {
					if(!m.getReturnType().equals(JavaType.VOID)) 
						throw new MojoExecutionException("Finalizers methods mut be void: "+clazz.getCanonicalName());
					if(foundAlready)
						throw new MojoExecutionException("Only one method can be a finalizers: "+clazz.getCanonicalName());
					out.addFinalizer(m.getName());
					foundAlready = true;
				}
			}
			
			return out;
		} catch (MojoExecutionException e) {
			log.error("Error in: "+clazz.getCanonicalName());
			throw new MojoExecutionException("Error in: "+clazz.getCanonicalName(),e);
		}
	}

	/** Parse the method definition including parameter types and return types. 
	 * In general this will try and create a type if it doesn;t exist. This no longer
	 * supoprts collections and other complex types as these are constrained to be from the 
	 * {@link RObject} hierarchy now. This is largely checked by the fact that when creating a type 
	 * it has to have a @RClass or @RDatatype annotation. 
	 * @param m
	 * @param model
	 * @return
	 * @throws MojoExecutionException
	 */
	public RMethod createMethod(JavaMethod m, RModel model) throws MojoExecutionException {
		try {
			Map<String,Object> annotationMap = getAnnotation(uk.co.terminological.rjava.RMethod.class,m).map(a -> a.getNamedParameterMap()).get();
			String description = (m.getComment() == null ? "" :m.getComment().trim());
			RMethod out = new RMethod(model, annotationMap, m.getName(), description, m.isStatic());
			out.setReturnType( getOrCreateType(m.getReturns(), model));
			m.getTags().forEach(dt -> out.mergeAnnotations(dt.getName(),dt.getValue()));		
			for (JavaParameter jp : m.getParameters()) {
				String defaultValue = getAnnotation(uk.co.terminological.rjava.RDefault.class, jp).map(a -> a.getNamedParameter("rCode").toString()).orElse(null);
				out.addParameter(jp.getName(), getOrCreateType(jp.getType(), model), defaultValue);
			}
			return out;
		} catch (MojoExecutionException e) {
			log.error("Error in: "+m.getCallSignature());
			throw new MojoExecutionException("Error in: "+m.getCallSignature(),e);
		}
	}

	
	public RType getOrCreateType(JavaType type, RModel model) throws MojoExecutionException {
		// This is a bit of a hangover from when I was thinking about supporting user defined types
		// Where user is library author.
		// For the time being this is unnecessary and will manage the types in the runtime library
		// as there are too many dependencies on them to do it all at compile time.
		try {
			return model.getRTypeOrThrowError(type);
		} catch (MojoExecutionException e) {
			RType out = createType(type, model);
			model.addDataType(out);
			return out;
		}
	}
	
	public RType createType(Class<?> type, RModel model) throws MojoExecutionException {
		String[] j2r = type.getAnnotation(RDataType.class).JavaToR();
		String[] r2j = type.getAnnotation(RDataType.class).RtoJava();
		String jni = "L"+type.getCanonicalName().replace(".", "/")+";";
		return new RType(model,Arrays.asList(r2j),Arrays.asList(j2r),jni,type);
		
	}
	
	public RType createType(JavaType type, RModel model) throws MojoExecutionException {
		
		// Handle types annotated with RDataType
		if (hasAnnotation(uk.co.terminological.rjava.RDataType.class, (JavaAnnotatedElement) type)) {
			
			//Use reflection as Data types are imported into plugin project's classpath
			return this.reflectionClassFor(type).map(c -> {
				String[] j2r = c.getAnnotation(RDataType.class).JavaToR();
				String[] r2j = c.getAnnotation(RDataType.class).RtoJava();
				String jni = "L"+type.getCanonicalName().replace(".", "/")+";";
				return new RType(model,Arrays.asList(r2j),Arrays.asList(j2r),jni,type, ((DefaultJavaType) type).getName());
			}).orElseThrow(() -> new MojoExecutionException("Data type class not readable"));
			
		}
		
		// Handle types annotated with RClass
		else if (hasAnnotation(uk.co.terminological.rjava.RClass.class, (JavaAnnotatedElement) type)) {
			
			String jni = "L"+type.getCanonicalName().replace(".", "/")+";";
			// These are passed by RJava to R as a jobj ref which is wrapped in the 
			// R6 marshalling logic
			// They can be manipulated by R and passed back in to java as a robj which needs
			// unwrapping before being presented
			String[] j2r = {"function(jObj) return(jObj)"};
			String[] r2j = {"function(rObj) return(rObj$.jobj)"};
			return new RType(model,Arrays.asList(r2j),Arrays.asList(j2r),jni,type, ((DefaultJavaClass) type).getName());
		
		}
		
		// throw exception
		else {
			log.error("Not a supported data type: "+type.getCanonicalName());
			throw new MojoExecutionException("Not a supported data type: "+type.getCanonicalName());
		}
		
		// This dates from the time when I was trying to support any types
		// It contains useful code to create a type tree from arbitatry Java parameter
		// RDataType annotation contains custom R instructions to convert the data type to Java 
//		Map<String,Object> annotationMap = getAnnotation(uk.co.terminological.rjava.RDataType.class, (JavaAnnotatedElement) type)
//				.map(a -> a.getNamedParameterMap()
//			).orElse(Collections.emptyMap());
//		
//		 
//		
//		if (type instanceof DefaultJavaType) {
//			if (type instanceof JavaWildcardType) {
//				return new RType(model,annotationMap, type, ((DefaultJavaType) type).getName(), 0);
//			}
//			if (type instanceof JavaTypeVariable) {
//				List<JavaType> bounds = ((JavaTypeVariable<?>)type).getBounds(); 
//				if (bounds.size()==1) {
//					return getOrCreateType(bounds.get(0),model);
//				} else {
//					throw new MojoExecutionException("Variable type with multiple possibilities: "+bounds.toString());
//				}
//			}
//			RType out = new RType(model,annotationMap,type,type.getValue(), ((DefaultJavaType)type).getDimensions());
//			if (type instanceof DefaultJavaParameterizedType) {
//				for(JavaType param: ((DefaultJavaParameterizedType)type).getActualTypeArguments()) {
//					RType tmp = getOrCreateType(param,model);
//					out.addParameter(tmp);
//				}
//			}
//			model.addDataType(out);
//			return out;
//		} else if (type instanceof DefaultJavaClass) {
//			RType out = new RType(model,Collections.emptyMap(),type,type.getValue(), 0);
//			return(out);
//		} else {
//			throw new MojoExecutionException("Unknown type:"+ type.getCanonicalName());
//		}
	}
	

	// a default no-args construvtor implementation if the class does not define one. 
	private RMethod noargsConstructor(JavaClass c, RModel model) throws MojoExecutionException {
		RMethod out = new RMethod(model, Collections.emptyMap(),"new","the default no-args constructor",true);
		out.setReturnType(model.getRTypeOrThrowError(c));
		return out;
	}
			
	private RMethod createConstructor(JavaConstructor m, RModel model) throws MojoExecutionException {
		Map<String,Object> annotationMap = getAnnotation(uk.co.terminological.rjava.RMethod.class,m).map(a -> a.getNamedParameterMap()).orElse(Collections.emptyMap());
		String description = (m.getComment() == null ? "the default no-args constructor" :m.getComment().trim());
		RMethod out = new RMethod(model, annotationMap,"new",description,true);
		m.getTags().forEach(dt -> out.mergeAnnotations(dt.getName(), dt.getValue()));
		for (JavaParameter jp: m.getParameters()) {
			String defaultValue = getAnnotation(uk.co.terminological.rjava.RDefault.class, jp).map(a -> a.getNamedParameter("rCode").toString()).orElse(null);
			out.addParameter(jp.getName(), model.getRTypeOrThrowError(jp.getType()),defaultValue);
		}
		out.setReturnType(model.getRTypeOrThrowError(m.getDeclaringClass()));
		return out;
	}

	//QDox annotation handling 
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
