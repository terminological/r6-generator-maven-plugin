package uk.co.terminological.jsr233plugin;
import static org.twdata.maven.mojoexecutor.MojoExecutor.artifactId;
import static org.twdata.maven.mojoexecutor.MojoExecutor.configuration;
import static org.twdata.maven.mojoexecutor.MojoExecutor.element;
import static org.twdata.maven.mojoexecutor.MojoExecutor.executeMojo;
import static org.twdata.maven.mojoexecutor.MojoExecutor.executionEnvironment;
import static org.twdata.maven.mojoexecutor.MojoExecutor.goal;
import static org.twdata.maven.mojoexecutor.MojoExecutor.groupId;
import static org.twdata.maven.mojoexecutor.MojoExecutor.name;
import static org.twdata.maven.mojoexecutor.MojoExecutor.plugin;
import static org.twdata.maven.mojoexecutor.MojoExecutor.version;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.HashMap;
import java.util.Optional;

import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.BuildPluginManager;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.project.MavenProject;

/**
 */

@Mojo( name = "generate-r-library", defaultPhase = LifecyclePhase.PACKAGE, requiresDependencyResolution = ResolutionScope.RUNTIME )
public class JSR223Plugin extends AbstractMojo {

	@Component
	private MavenProject mavenProject;

	@Component
	private MavenSession mavenSession;

	@Component
	private BuildPluginManager pluginManager;
	
	@Parameter(required=true)
	private PackageData packageData;
	
	public void execute() throws MojoExecutionException {
		
		// Assemble and build the jar for R
		executeMojo(
				plugin(
					groupId("org.apache.maven.plugins"),
					artifactId("maven-assembly-plugin"),
					version("3.2.0")),
				goal("single"),
				configuration(
						element(name("descriptorRefs"), 
							element(name("descriptorRef"),"jar-with-dependencies")
						)),
				executionEnvironment(
						mavenProject,
						mavenSession,
						pluginManager));
		
		
		// Copy the jar the a lib directory
		String jarFile = mavenProject.getModel().getBuild().getFinalName()+"-jar-with-dependencies.jar";
		File targetDir = new File(mavenProject.getModel().getBuild().getDirectory());
		Path rootDir = mavenProject.getBasedir().toPath();
		Path jarLoc = rootDir.resolve("inst").resolve("java").resolve(jarFile);
		
		
				
		try {
			
			URI uri = JSR223Plugin.class.getResource("/groovy-all-2.4.17.jar").toURI();
			
			String[] array = uri.toString().split("!");
			FileSystem fs = FileSystems.newFileSystem(URI.create(array[0]), new HashMap<>());
			Path groovyLoc =fs.getPath(array[1]);
			
			Files.createDirectories(jarLoc.getParent());
			
			Files.copy(
					groovyLoc, 
					rootDir.resolve("inst").resolve("java").resolve("groovy-all-2.4.17.jar"),
					StandardCopyOption.REPLACE_EXISTING);
			
			fs.close();
			
			Files.copy(
					Paths.get(targetDir.getAbsolutePath(), jarFile), 
					jarLoc, StandardCopyOption.REPLACE_EXISTING);
			
		} catch (IOException | URISyntaxException e) {
			throw new MojoExecutionException("Couldn't move fat jar",e);
		}
		
		Optional<RModel> model = QDoxParser.scanModel(mavenProject.getCompileSourceRoots(), packageData);
		if (model.isPresent()) {
			
			RModelWriter writer = new RModelWriter(
					model.get(), 
					mavenProject.getBasedir(),
					jarFile
					);
			writer.write();
			
		}
	}
	
}
