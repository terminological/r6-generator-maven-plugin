# Generated by r6-generator-maven-plugin: remove this line and make manual changes when you come to CRAN submission

## Test environments

# N.B. this is only true is you have uncommented the correct lines in github actions workflow files
(via GitHub actions)
* ubuntu-latest; R-release 
* windows-latest, oldrel-1
* windows-latest, release
* windows-latest, devel
* macOS-latest, oldrel-1
* macOS-latest, release
* macOS-latest, devel
* ubuntu-latest, oldrel-1
* ubuntu-latest, devel

## R CMD check results
# TODO: insert R CMD check results here before submission

## CRAN note justifications
 
* mvnw, mvnw.cmd, and pom.xml are necessary for compilation of java files from source, if the JAR files are >5Mb and too big to fit on CRAN. 
* R6 is a build time dependency.
* This is a new release.
