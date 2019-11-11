package uk.co.terminological.jsr233plugin;

import java.io.File;

import org.apache.maven.plugins.annotations.Parameter;

public class JS223Execution {

	@Parameter
	private File[] inputFiles;

	public File[] getInputFiles() {
		return inputFiles;
	}

	public void setInputFiles(File[] inputFiles) {
		this.inputFiles = inputFiles;
	}
}
