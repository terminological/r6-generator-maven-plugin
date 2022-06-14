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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.ArtifactUtils;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.ResolutionScope;


/**
 * The root of the maven plugin. This is the entry point called by maven.
 */
@Mojo( name = "generate-r-library", defaultPhase = LifecyclePhase.INSTALL, requiresDependencyResolution = ResolutionScope.RUNTIME )
public class R6GeneratorPlugin extends PluginBase {

	public void execute() throws MojoExecutionException {
		
		getLog().info("Executing R6 generator");
		setupPaths();
		getLog().debug("Wiping previous files.");
		// Find additional exports in non-generated R files 
		List<String> additionalExports = scanDirectoryForExports(rDir);
		
		rmContents(docs);
		rmContents(manDir);
		rmGenerated(workflows);
		rmGenerated(rDir);
		
		
		// Assemble and build the fat jar for R plugin
		// This has been moved to JarCompilerPlugin 
		
		getLog().debug("Scanning source code.");

		// build the model of the code we are going to use to build the API
		Optional<RModel> model = QDoxParser.scanModel(mavenProject.getCompileSourceRoots(), packageData, getLog());
		if (model.isPresent()) {
				
			RModel m = model.get();
			
			String key = ArtifactUtils.versionlessKey("com.github.terminological","r6-generator-maven-plugin");
			Artifact pluginVersion = (Artifact) mavenProject.getPluginArtifactMap().get(key);
			// TODO: get git commit verison information
			// https://www.baeldung.com/spring-git-information
			m.setPluginMetadata(pluginVersion);
			m.setMavenMetadata(mavenProject);
			m.setRelativePath(mavenProject,rootDir);
					
			
			//write the code to the desired location.
			getLog().debug("Writing R6 library code.");
			RModelWriter writer = new RModelWriter(
					m.withAdditionalExports(additionalExports), 
					outputDirectory,
					jarFile,
					rToPomPath,
					getLog()
					);
			writer.write();
			
		}
		
		if (packageData.useRoxygen2() && !packageData.getDebugMode()) {
			
			// must be an array to stop java tokenising it
			String rCMD[] = {"R","-e","devtools::document(pkg = '"+outputDirectory+"')"};
			getLog().info("Generating roxygen configuration.");
			getLog().debug(Arrays.stream(rCMD).collect(Collectors.joining(" ")));
			// Runtime run = Runtime.getRuntime();
			try {
				ProcessBuilder processBuilder = new ProcessBuilder(rCMD);
				processBuilder.redirectErrorStream(true);
				Process pr = processBuilder.start();
				int res = pr.waitFor();
				BufferedReader buf = new BufferedReader(new InputStreamReader(pr.getInputStream()));
				String line = "";
				if(res != 0) {
					while ((line=buf.readLine())!=null) {
						getLog().error(line);
					}
					throw new MojoExecutionException("ROxygen did not complete normally. The package is probably in an inconsistent state.");
				} else {
					while ((line=buf.readLine())!=null) {
						getLog().info(line);
					}
				}
			} catch (IOException | InterruptedException e) {
				throw new MojoExecutionException("Failed to execute pkgdown", e);
			}
		}
	
		if (packageData.usePkgdown() && !packageData.getDebugMode()) {
			
			// must be an array to stop java tokenising it
			String rCMD[] = {"R","-e","pkgdown::build_site(pkg = '"+outputDirectory+"')"};
			getLog().info("Generating pkgdown site - please be patient");
			getLog().debug(Arrays.stream(rCMD).collect(Collectors.joining(" ")));
			// Runtime run = Runtime.getRuntime();
			try {
				ProcessBuilder processBuilder = new ProcessBuilder(rCMD);
				processBuilder.redirectErrorStream(true);
				Process pr = processBuilder.start();
				int res = pr.waitFor();
				BufferedReader buf = new BufferedReader(new InputStreamReader(pr.getInputStream()));
				String line = "";
				if(res != 0) {
					while ((line=buf.readLine())!=null) {
						getLog().error(line);
					}
					getLog().error("Pkgdown did not complete normally. Details in the log file.");
				} else {
					while ((line=buf.readLine())!=null) {
						getLog().info(line);
					}
				}
			} catch (IOException | InterruptedException e) {
				throw new MojoExecutionException("Failed to execute pkgdown", e);
			}
		}
    
		// Set up maven wrapper
		// Using a fixed version of maven
		getLog().info("Setup maven wrapper scripts");
		executeMojo(
				plugin(
					groupId("org.apache.maven.plugins"),
					artifactId("maven-wrapper-plugin"),
					version("3.1.1")),
				goal("wrapper"),
				configuration(
						element(name("distributionType"),"script"),
						element(name("mavenVersion"),"3.3.9")
				),
				executionEnvironment(
						mavenProject,
						mavenSession,
						pluginManager));
		
		// Generate java docs (for pkgdown site / github pages)
		if (packageData.useJavadoc() && !packageData.getDebugMode()) {
			getLog().info("Generating javadocs");
			executeMojo(
					plugin(
						groupId("org.apache.maven.plugins"),
						artifactId("maven-javadoc-plugin"),
						version("3.2.0")),
					goal("javadoc"),
					configuration(
							element(name("reportOutputDirectory"),docs.toString()),
							element(name("destDir"),"javadoc"),
							element(name("javadocExecutable"),packageData.getJavadocExecutable()),
							element(name("additionalOptions"),
									element(name("additionalOption"),"-header '<script type=\"text/javascript\" src=\"http://cdn.mathjax.org/mathjax/latest/MathJax.js?config=TeX-AMS-MML_HTMLorMML\"></script>'"),
									element(name("additionalOption"),"--allow-script-in-comments")),
							element(name("failOnError"),"false")
					),
					executionEnvironment(
							mavenProject,
							mavenSession,
							pluginManager));
		}
		
	}
	
}
