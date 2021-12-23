package uk.co.terminological.rjava.plugin;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.maven.plugin.MojoExecutionException;

import com.thoughtworks.qdox.model.JavaClass;
import com.thoughtworks.qdox.model.JavaType;

public class RModel {

	List<RClass> types = new ArrayList<>();
	Map<String,RType> datatypes = new HashMap<>();
	private PackageData config;
	private QDoxParser parser;

	public Optional<JavaClass> javaClassFor(Class<?> clazz) {
		return parser.javaClassFor(clazz);
	}
	
	public RModel(PackageData config, QDoxParser parser) {
		this.config = config;
		this.parser = parser;
	}

	public List<RClass> getClassTypes() {
		return types;
	}
	
	public Set<String> getImports() {
		return getClassTypes().stream().flatMap(m ->  
			m.getAnnotationList("imports").stream()
		).collect(Collectors.toSet());
	}
	
	public Set<String> getSuggests() {
		return getClassTypes().stream().flatMap(m ->  
			m.getAnnotationList("suggests").stream()
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
				getConfig().getMaintainerEmail(),
				getConfig().getMaintainerORCID()
		});
		tmp.addAll(this.getClassTypes().stream().map(t -> t.getAuthor()).filter(a -> (
				a != null && a.length > 0
		)).collect(Collectors.toSet()));
		return tmp;
	}

	public RType getRTypeOrThrowError(JavaType type) throws MojoExecutionException {
		return datatypes.values()
				.stream()
				.filter(rt -> rt.getCanonicalName().equals(type.getCanonicalName()))
				.findFirst()
				.orElseThrow(() -> new MojoExecutionException("Not a valid input or output type: "+type));
	}

	public Collection<RType> getDataTypes() {
		return datatypes.values();
	}

	public void addDataType(RType out) {
		this.datatypes.put(out.getCanonicalName(), out);
	}

	

	

}
