package uk.co.terminological.rjava.plugin;
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
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.ResolutionScope;


/**
 * The root of the maven plugin. This is the entry point called by maven.
 */
@Mojo( name = "compile-java-library", defaultPhase = LifecyclePhase.PACKAGE, requiresDependencyResolution = ResolutionScope.RUNTIME )
public class JarCompilerPlugin extends PluginBase {

	public void execute() throws MojoExecutionException {
		
		getLog().info("Executing R6 generator");
		setupPaths();
		rmContents(jarDir);
		
		
		getLog().debug("Assembling fat jar.");
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
		
		File targetDir = new File(mavenProject.getModel().getBuild().getDirectory());
		
		
		// copy the logging jars into the new project.
		getLog().info("Copying library files.");
		try {
			
			Files.createDirectories(rootDir);
			Files.copy(
					Paths.get(targetDir.getAbsolutePath(), jarFile), 
					jarLoc, StandardCopyOption.REPLACE_EXISTING);
			
		} catch (IOException e) {
			throw new MojoExecutionException("Couldn't move fat jar",e);
		}
		
		
	}
	
}
