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
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Optional;
import java.util.stream.Collectors;

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
 * The root of the maven plugin. This is the entry point called by maven.
 */
@Mojo( name = "generate-r-library", defaultPhase = LifecyclePhase.PACKAGE, requiresDependencyResolution = ResolutionScope.RUNTIME )
public class R6GeneratorPlugin extends AbstractMojo {

	@Component
	private MavenProject mavenProject;

	@Component
	private MavenSession mavenSession;

	@Component
	private BuildPluginManager pluginManager;
	
	@Parameter(required=true)
	private PackageData packageData;
	
	@Parameter(required=true)
	private File outputDirectory;
	
	private void delete(Path t) {
		try {
			Files.delete(t);
		} catch (IOException e1) {
			getLog().warn("couldn't remove: "+t.toString());
		}
	}
	
	public void execute() throws MojoExecutionException {
		
		getLog().info("Executing R6 generator");
		// wipe all the directories to prevent build up of old versions
		Path rootDir = outputDirectory.toPath();
		Path jarDir = rootDir.resolve("inst").resolve("java");
		Path rDir = rootDir.resolve("R");
		Path manDir = rootDir.resolve("man");
		Path docs = rootDir.resolve("docs");
		getLog().debug("Wiping previous files.");
		
		try {
			Files.createDirectories(jarDir);
			Files.createDirectories(rDir);
			Files.createDirectories(manDir);
			Files.createDirectories(docs);
			Files.walk(docs).sorted(Comparator.reverseOrder()).forEach(this::delete);
			Files.walk(jarDir).sorted(Comparator.reverseOrder()).filter(f -> !f.equals(jarDir)).forEach(this::delete);
			Files.walk(rDir).sorted(Comparator.reverseOrder()).filter(f -> !f.equals(rDir)).forEach(this::delete);
			Files.walk(manDir).sorted(Comparator.reverseOrder()).filter(f -> !f.equals(manDir)).forEach(this::delete);
			
		} catch (IOException e1) {
			throw new MojoExecutionException("Problem tidying up previous runs",e1);
		}
		
		// Assemble and build the fat jar for R plugin
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
		String jarFile = mavenProject.getModel().getBuild().getFinalName()+"-jar-with-dependencies.jar";
		File targetDir = new File(mavenProject.getModel().getBuild().getDirectory());
		Path jarLoc = jarDir.resolve(jarFile);
		
		// copy the logging jars into the new project.
		// I don;t know why this didn;t work as a maven dependencey of the 
		// runtime project. maybe it would have done.
		getLog().info("Copying library files.");
		try {
			
			Files.createDirectories(rootDir);
			
			
//			for (String fileName: Arrays.asList(
//					"slf4j-api-1.7.22.jar", 
//					"log4j-api-2.13.3.jar", 
//					"log4j-core-2.13.3.jar",
//					"log4j-slf4j-impl-2.13.3.jar"
//					)) {
//				URI uri = R6GeneratorPlugin.class.getResource("/"+fileName).toURI();
//				String[] array = uri.toString().split("!");
//				FileSystem fs = FileSystems.newFileSystem(URI.create(array[0]), new HashMap<>());
//				Path groovyLoc =fs.getPath(array[1]);
//				Files.copy(
//						groovyLoc, 
//						jarDir.resolve(fileName),
//						StandardCopyOption.REPLACE_EXISTING);
//				
//				fs.close();
//			}
			
			Files.copy(
					Paths.get(targetDir.getAbsolutePath(), jarFile), 
					jarLoc, StandardCopyOption.REPLACE_EXISTING);
			
		} catch (IOException e) {
			throw new MojoExecutionException("Couldn't move fat jar",e);
		}
		
		getLog().debug("Scanning source code.");
		// build the model of the code we are going to use to build the API
		Optional<RModel> model = QDoxParser.scanModel(mavenProject.getCompileSourceRoots(), packageData, getLog());
		if (model.isPresent()) {
			
			//write the code to the desired location.
			getLog().debug("Writing R6 library code.");
			RModelWriter writer = new RModelWriter(
					model.get(), 
					outputDirectory,
					jarFile,
					getLog()
					);
			writer.write();
			
		}
		
		if (packageData.usePkgdown() && !packageData.getDebugMode()) {
			
			// must be an array to stop java tokenising it
			String rCMD[] = {"R","-e","pkgdown::build_site(pkg = '"+outputDirectory+"')"};
			getLog().info("Generating pkgdown site - please be patient");
			getLog().debug(Arrays.stream(rCMD).collect(Collectors.joining(" ")));
			Runtime run = Runtime.getRuntime();
			try {
				Process pr = run.exec(rCMD);
				int res = pr.waitFor();
				BufferedReader buf = new BufferedReader(new InputStreamReader(pr.getInputStream()));
				String line = "";
				while ((line=buf.readLine())!=null) {
					getLog().info(line);
				}
				if(res != 0) throw new MojoExecutionException("Failed to execute pkgdown");
			} catch (IOException | InterruptedException e) {
				throw new MojoExecutionException("Failed to execute pkgdown", e);
			}
		}
	
		
    
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
