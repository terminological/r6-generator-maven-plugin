package uk.co.terminological.rjava.plugin;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.project.MavenProject;

import com.thoughtworks.qdox.model.JavaClass;
import com.thoughtworks.qdox.model.JavaType;

public class RModel {

	List<RClass> types = new ArrayList<>();
	Map<String,RType> datatypes = new HashMap<>();
	private PackageData config;
	private QDoxParser parser;
	private Collection<String> additionalExports;
	private Path rootPath;
	private Path relativePath;
	private MavenProject mavenProject;
	private Artifact pluginVersion;

	public Optional<JavaClass> javaClassFor(Class<?> clazz) {
		return parser.javaClassFor(clazz);
	}
	
	public RModel(PackageData config, QDoxParser parser) {
		this.config = config;
		this.parser = parser;
	}

	public RModel withAdditionalExports(Collection<String> exports) {
		this.additionalExports = exports;
		return this;
	}
	
	public List<RClass> getClassTypes() {
		return types;
	}
	
	public Set<String> getExports() {
		Set<String> out = getClassTypes().stream().map(c -> c.getSimpleName())
			.collect(Collectors.toSet());
		out.addAll(additionalExports);
		return out;
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

	public void setRelativePath(MavenProject mavenProject, Path rootDir) {
		rootPath = Paths.get(mavenProject.getBasedir().getPath());
		relativePath = rootPath.relativize(rootDir);
	}

	public String getRelativePath() {
		if (relativePath.toString().equals("")) return null;
		return relativePath.toString();
	}

	public String getRootPath() {
		return rootPath.toString();
	}

	public void setMavenMetadata(MavenProject mavenProject) {
		this.mavenProject = mavenProject;
	}

	public String getPluginVersion() {
		return
				this.pluginVersion.getGroupId()+":"+
				this.pluginVersion.getArtifactId()+":"+
				this.pluginVersion.getVersion();
	}

	public String getMavenVersion() {
		return
				this.mavenProject.getGroupId()+":"+
				this.mavenProject.getArtifactId()+":"+
				this.mavenProject.getVersion();
	}
	
	public void setPluginMetadata(Artifact pluginVersion) {
		this.pluginVersion = pluginVersion;
		
	}
	

}
