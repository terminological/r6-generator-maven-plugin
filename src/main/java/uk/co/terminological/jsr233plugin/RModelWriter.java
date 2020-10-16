package uk.co.terminological.jsr233plugin;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.Writer;
import java.util.HashMap;
import java.util.Map;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.logging.Log;

import freemarker.template.Configuration;
import freemarker.template.DefaultObjectWrapperBuilder;
import freemarker.template.Template;
import freemarker.template.TemplateException;
import freemarker.template.TemplateExceptionHandler;

public class RModelWriter {

	private Configuration cfg;
	private RModel model;
	private File target;
	private String jarFileName;
	private Log log;

	public RModelWriter(RModel model, File target, String jarFileName, Log log) {
		this.model = model;
		this.target = target;
		this.jarFileName = jarFileName;
		this.log=log;
	}

	public void write() throws MojoExecutionException {

		if (target == null) throw new MojoExecutionException("No target directory has been set");

		// Freemarker stuff
		cfg = new Configuration(Configuration.VERSION_2_3_25);
		cfg.setObjectWrapper(new DefaultObjectWrapperBuilder(Configuration.VERSION_2_3_25).build());
		cfg.setDefaultEncoding("UTF-8");
		cfg.setTemplateExceptionHandler(TemplateExceptionHandler.DEBUG_HANDLER);
		cfg.setClassForTemplateLoading(RModelWriter.class,"");

		
		File rDir = new File(target,"R");
		rDir.mkdirs();
		
		File manDir = new File(target,"man");
		manDir.mkdirs();
		
		Map<String,Object> typeRoot = new HashMap<>();
		typeRoot.put("model", model);
		typeRoot.put("jarFileName", jarFileName);
	
		doGenerate(new File(target,"NAMESPACE"),getTemplate("/rjavaNamespace.ftl"),typeRoot);
		doGenerate(new File(target,"DESCRIPTION"),getTemplate("/rjavaDescription.ftl"),typeRoot);
		doGenerate(new File(manDir,"JavaApi.Rd"),getTemplate("/rjavaApiRd.ftl"),typeRoot);
		doGenerate(new File(manDir,model.getConfig().getPackageName()+"-package.Rd"),getTemplate("/rjavaPackageRd.ftl"),typeRoot);
		doGenerate(new File(rDir,"JavaApi.R"),getTemplate("/rjavaApiR.ftl"),typeRoot);
		doGenerate(new File(rDir,"zzz.R"),getTemplate("/rjavaZzz.ftl"),typeRoot);
		
		for (RClass type: model.getClassTypes()) {
			
			typeRoot.put("class", type);
			
			doGenerate(new File(manDir,type.getSimpleName()+".Rd"),getTemplate("/rjavaRd.ftl"),typeRoot);
			doGenerate(new File(rDir,type.getSimpleName()+".R"),getTemplate("/rjavaClassR.ftl"),typeRoot);
			
		}
		
	}

	private Template getTemplate(String name) throws MojoExecutionException {
		Template tmp;
		try {
			tmp = cfg.getTemplate(name);
		} catch (IOException e) {
			throw new MojoExecutionException("Couldn't load template "+name,e);
		}
		log.debug("Using freemarker template: "+name);
		return tmp;
	}

	// Utility to handle the freemarker generation mechanics.
	private void doGenerate(File file, Template tmp, Map<String,Object> root) throws MojoExecutionException {

		try {

			Writer out;

			out = new PrintWriter(new FileOutputStream(file));
			log.info("Writing file: "+file.getAbsolutePath());
			tmp.process(root, out);
			out.close();

		} catch (IOException e) {
			e.printStackTrace();
			throw new MojoExecutionException("Couldn't write source file", e);
			// this should not happen. 

		} catch (TemplateException e) {
			
			throw new MojoExecutionException("Error in freemarker template: "+tmp.getName(),e);
		}
	}

}
