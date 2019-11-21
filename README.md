# maven-r-jsr223-plugin

Maven plugin and annotation processor to write glue code to allow correctly annotated java class to be used as R function.

## Rationale

R can use RJava or jsr223 to communicate with java. R has a class system called R6.

If you want to use a java libray in R there is potentially a lot of glue code, and R library specific packaging configuration required.

However if you don't mind writing an API in Java you can generate all of this glue code using a few annotations and the normal javadoc annotations. This plugin aims to provide that glue code and write a fairly transparent connection between java code and R code, with a minimum of hard work.

The ultimate aim of this plugin to allow java developers to provide simple APIs for their libraries, package their library using maven, push it to github and for that to become seamlessly available as an R library.

## Basic usage

### write a java api:

```Java
/**
 * A test of the jsr223 templating
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
					"An example",
					"Spans many lines"
	})
	public static String greet() {
		return "Hello R world. Love from Java."
	}
}
```

Key points:
* You can annotate multiple classes with @RClass.
* Only public methods or constructors annotated with @RMethod will feature in the R library
* you cannot overload methods or constructors. Only one method with a given name is supported, and only one annotated constructor.
* static and non static java methods are supported.
* Objects that can be translated into R are returned by value
* Other objects are passed around in R by reference as R6 Objects mirroring the layout of the java code.
* Such objects can interact with each other in the same java api engine (see below)

### package it:

Maven jitpack runtime and plugin repoistory

```XML
	<!-- Resolve runtime library on github -->
	<repositories>
		<repository>
		    <id>jitpack.io</id>
		    <url>https://jitpack.io</url>
		</repository>
	</repositories>

	<!-- Resolve maven plugin on github -->
	<pluginRepositories>
		<pluginRepository>
		    <id>jitpack.io</id>
		    <url>https://jitpack.io</url>
		</pluginRepository>
	</pluginRepositories>

```

Required Maven runtime dependency

```XML
...
		<dependency>
			<groupId>com.github.terminological</groupId>
			<artifactId>r-jsr223-runtime</artifactId>
			<version>1.0</version>
		</dependency>
...
```

Maven plugin example configuration:

```XML
...
	<build>
		<plugins>
			...
			<plugin>
				<groupId>com.github.terminological</groupId>
				<artifactId>r-jsr223-maven-plugin</artifactId>
				<version>1.0</version>
				<configuration>
					<packageData>
						<title>A test library</title>
						<version>0.01</version>
						<packageName>myRpackage</packageName>
						<license>MIT</license>
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

And with this in place, a call to mvn package will create your R library by adding files to your java source tree.
Push your java source tree to github (Optional). 

### run it:


```R
library(devtools)
# if you are using locally:
# load_all("~/Git/your-project-id/")
# OR if you pushed the project to github
install_github("your-github-name/your-project-id")

# a basic smoke test

# the JavaApi class is the entry point for R to your Java code.
J <- myRpackage::JavaApi$new()

# all the API classes and methods are classes attached to the J java api object
J$HelloWorld$greet()
?myRpackage::HelloWorld
```



For a more complete working example see: 
https://github.com/terminological/r-jsr223-maven-plugin-test

