package uk.co.terminological.rjava.plugin;

import java.io.File;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.apache.maven.plugins.annotations.Parameter;



/** Holds all the options for the maven plugin
 * @author terminological
 *
 */
public class PackageData {

	@Parameter
	private File[] inputFiles;
	
	@Parameter(required=true)
	private String title;
	
	@Parameter(required=true)
	private String version;

	@Parameter(required=true)
	private String packageName;
	
	@Parameter(required=true)
	private String maintainerName;
	
	@Parameter(defaultValue="",required=true)
	private String maintainerFamilyName;
	
	@Parameter(defaultValue="Unknown",required=true)
	private String maintainerOrganisation;
	
	@Parameter(required=true)
	private String maintainerEmail;
	
	@Parameter()
	private String maintainerORCID;
	
	@Parameter(required=true)
	private String description;
	
	@Parameter(required=true)
	private String license;
	
	@Parameter(required=false)
	private String[] rjavaOpts;
	
	@Parameter(property="defaultLogLevel",defaultValue="INFO",required=true)
	private String defaultLogLevel;
	
	@Parameter
	private Boolean debug;
	
	@Parameter
	private Boolean usePkgdown;
	
	@Parameter
	private Boolean useRoxygen2;
	
	@Parameter
	private Boolean useJavadoc;

	@Parameter
	private String doi;

	@Parameter
	private String url;
	
	@Parameter(defaultValue="${java.home}/bin/javadoc")
	private String javadocExecutable;
	
	public String getMaintainerName() {
		return maintainerName;
	}

	public String getMaintainerFamilyName() {
		return maintainerFamilyName;
	}

	public String getMaintainerOrganisation() {
		return maintainerOrganisation;
	}
	
	public String getMaintainerEmail() {
		return maintainerEmail;
	}
	
	public String getMaintainerORCID() {
		return maintainerORCID;
	}

	/** {@code <debugMode>true</debugMode>}
	 * @return
	 */
	public boolean getDebugMode() {
		return debug != null && debug.booleanValue(); 
	}
	
	/** {@code <defaultLogLevel>WARN</defaultLogLevel>}
	 * @return
	 */
	public String getDefaultLogLevel() {
		return defaultLogLevel == null ? "INFO" : defaultLogLevel;
	};
	
	/** {@code <usePkgdown>true</usePkgdown>}
	 * 
	 * build machine must have R and pkgdown installed
	 * @return
	 */
	public boolean usePkgdown() {
		return usePkgdown != null && usePkgdown.booleanValue(); 
	}
	
	/** {@code <usePkgdown>true</usePkgdown>}
	 * 
	 * build machine must have R and devtools installed
	 * @return
	 */
	public boolean useRoxygen2() {
		return useRoxygen2 != null && useRoxygen2.booleanValue(); 
	}
	
	
	/** {@code <useJavadoc>true</useJavadoc>}
	 * 
	 * build machine must have R and pkgdown installed
	 * @return
	 */
	public boolean useJavadoc() {
		return useJavadoc != null && useJavadoc.booleanValue(); 
	}
	
	public String getJavadocExecutable() {
		return this.javadocExecutable;
	}
	
	public boolean needsLicense() {
		return Arrays.asList(
				"MIT","BSD_2_clause","BSD_3_clause"
		).contains(license);
	}
	
	public String getYear() {
		return Integer.toString(LocalDate.now().getYear());
	}
	
	public String getDate() {
		return LocalDateTime.now().toString();
	}
	
	public boolean hasRJavaOpts() {
		return !getRJavaOpts().isEmpty();
	}
	
	/** For example {@code <rJavaOpts><rJavaOpt>-Xmx2048M</rJavaOpt></rJavaOpts>}
	*/
	public List<String> getRJavaOpts() {
		if (this.rjavaOpts == null) return Collections.emptyList();
		return Arrays.asList(this.rjavaOpts);
	}

	public void setMaintainerName(String maintainerName) {
		this.maintainerName = maintainerName;
	}

	public void setMaintainerFamilyName(String maintainerFamilyName) {
		this.maintainerFamilyName = maintainerFamilyName;
	}

	public void setMaintainerEmail(String maintainerEmail) {
		this.maintainerEmail = maintainerEmail;
	}

	public String getDescription() {
		return description.replaceAll("\\s+", " ");
	}

	public String getLicense() {
		return license + (needsLicense() ? " + file LICENSE" : "");
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public void setLicense(String license) {
		this.license = license;
	}
	
	public File[] getInputFiles() {
		return inputFiles;
	}

	public void setInputFiles(File[] inputFiles) {
		this.inputFiles = inputFiles;
	}
	
	public String getPackageName() {
		return packageName;
	}

	public String getTitle() {
		return title;
	}

	public String getVersion() {
		return version;
	}

	public void setPackageName(String packageName) {
		this.packageName = packageName;
	}

	public void setTitle(String title) {
		this.title = title;
	}

	public void setVersion(String version) {
		this.version = version;
	}

	public void setDefaultLogLevel(String defaultLogLevel) {
		this.defaultLogLevel = defaultLogLevel;
	}
	
	public String getDoi() {
		return doi;
	}

	public void setDoi(String doi) {
		this.doi = doi;
	}

	public String getUrl() {
		return url;
	}

	public void setUrl(String url) {
		this.url = url;
	}
	
}
