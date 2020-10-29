# maven-r6-generator-plugin

Maven plugin and annotation processor to write glue code to allow correctly annotated java class to be used within R as an set of R6 classes.

## Rationale

R can use RJava or jsr223 to communicate with java. R has a class system called R6.

If you want to use a java library in R there is potentially a lot of glue code needed, and R library specific packaging configuration required.

However if you don't mind writing an R-centric API in Java you can generate all of this glue code using a few java annotations and the normal javadoc annotations. This plugin aims to provide an annotation processor that writes that glue code and creates a fairly transparent connection between java code and R code, with a minimum of hard work. The focus of this is streamlining the creation of R libraries by Java developers, rather than allowing access to arbitrary Java code from R.

The ultimate aim of this plugin to allow java developers to provide simple APIs for their libraries, package their library using maven, push it to github and for that to become seamlessly available as an R library, with a minimal amount of fuss.

## Basic usage

### write a java api:

```Java
package 

import uk.co.terminological.rjava.RClass;
import uk.co.terminological.rjava.RMethod;

/**
 * A test of the r6 templating
 * 
 * this is a details comment 
 * @author joe tester joe.tester@example.com ORCIDID
 * 
 */
@RClass
public class HelloWorld {

	/**
	 * Description of a hello world function
	 * @return this java method returns a String
	 */
	@RMethod(examples = {
					"message('An example')",
					"message('Spans many lines')"
	})
	public static String greet() {
		return "Hello R world. Love from Java."
	}
}
```

Key points:
* You can annotate multiple classes with @RClass.
* Only public methods annotated with @RMethod will feature in the R library
* you cannot overload methods or constructors. Only one method with a given name is supported, and only one annotated constructor.
* static and non static java methods are supported.
* Objects that can be translated into R are returned by value
* Other objects are passed around in R by reference as R6 Objects mirroring the layout of the java code.
* Such objects can interact with each other in the same java api engine (see below)

### package it:

Maven jitpack runtime and plugin repoistory

```XML

...
	<!-- Resolve runtime library on github -->
	<repositories>
		<repository>
		    <id>jitpack.io</id>
		    <url>https://jitpack.io</url>
		</repository>
	</repositories>
...
	<!-- Resolve maven plugin on github -->
	<pluginRepositories>
		<pluginRepository>
		    <id>jitpack.io</id>
		    <url>https://jitpack.io</url>
		</pluginRepository>
	</pluginRepositories>
...
```

Required Maven runtime dependency

```XML
...
	<properties>
		...
		<r6.version>master-SNAPSHOT</r6.version>
	</properties>
...
		<dependency>
			<groupId>com.github.terminological</groupId>
			<artifactId>r6-generator-runtime</artifactId>
			<version>${r6.version}</version>
		</dependency>
...
```

Maven plugin example configuration - generates a R library into the directory "r-library" in the source tree:

```XML
...
	<build>
		<plugins>
			...
			<plugin>
				<groupId>com.github.terminological</groupId>
				<artifactId>r6-generator-maven-plugin</artifactId>
				<version>${r6.version}</version>
				<configuration>
					<outputDirectory>${project.basedir}/r-library</outputDirectory>
					<packageData>
						<title>A test library</title>
						<version>0.01</version>
						<packageName>myRpackage</packageName>
						<license>MIT</license>
						<debug>true</debug> <!-- enables remote dubugging -->
						<rjavaOpts>
							<rjavaOpt>-Xmx256M</rjavaOpt>
						</rjavaOpts>
						<description>An optional long description of the package</description>
						<maintainerName>test forename</maintainerName>
						<maintainerFamilyName>optional surname</maintainerFamilyName>
						<maintainerEmail>test@example.com</maintainerEmail>
					</packageData>
				</configuration>
				<executions>
					<execution>
						<id>generate-r-library</id>
						<goals>
							<goal>generate-r-library</goal>
						</goals>
					</execution>
				</executions>
			</plugin>
		</plugins>
	</build>
...
```

And with this in place, a call to mvn package or mvn install will create your R library by adding files to your java source tree in the directory.
Push your java source tree to github (Optional). 

### run it from R:


```R
library(devtools)
# if you are using locally:
load_all("~/Git/your-project-id/r-library")
# OR if you pushed the project to github
# install_github("your-github-name/your-project-id",subdir="r-library")

# a basic smoke test

# the JavaApi class is the entry point for R to your Java code.
J <- myRpackage::JavaApi$get()

# all the API classes and methods are classes attached to the J java api object
J$HelloWorld$greet()
?myRpackage::HelloWorld
```

For more options about the : 
https://github.com/terminological/r6-generator-runtime

For a more complete working example and further documentation see: 
https://github.com/terminological/r6-generator-maven-plugin-test

