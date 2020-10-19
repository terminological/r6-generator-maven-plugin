package uk.co.terminological.rjava.plugin;

import java.io.File;
import java.sql.Date;
import java.time.LocalDate;
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
	
	@Parameter(required=true)
	private String maintainerEmail;
	
	@Parameter(defaultValue="",required=true)
	private String description;
	
	@Parameter(required=true)
	private String license;
	
	@Parameter(required=false)
	private String[] rjavaOpts;
	
	@Parameter
	private Boolean debug;
	
	public String getMaintainerName() {
		return maintainerName;
	}

	public String getMaintainerFamilyName() {
		return maintainerFamilyName;
	}

	public String getMaintainerEmail() {
		return maintainerEmail;
	}
	
	/** {@code <debugMode>true</debugMode>}
	 * @return
	 */
	public boolean getDebugMode() {
		return debug != null && debug; 
	}
	
	public String getYear() {
		return Integer.toString(LocalDate.now().getYear());
	}
	
	public boolean hasRJavaOpts() {
		return getRJavaOpts().isEmpty();
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
		return description;
	}

	public String getLicense() {
		return license;
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

	
	
	
}
