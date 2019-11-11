package uk.co.terminological.jsr233plugin;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.Writer;
import java.util.HashMap;
import java.util.Map;

import org.apache.maven.plugin.MojoExecutionException;

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

	public RModelWriter(RModel model, File target, String jarFileName) {
		this.model = model;
		this.target = target;
		this.jarFileName = jarFileName;
	}

	public void write() throws MojoExecutionException {

		
		if (target == null) throw new RuntimeException("No target directory has been set");

		cfg = new Configuration(Configuration.VERSION_2_3_25);
		cfg.setObjectWrapper(new DefaultObjectWrapperBuilder(Configuration.VERSION_2_3_25).build());
		cfg.setDefaultEncoding("UTF-8");
		cfg.setTemplateExceptionHandler(TemplateExceptionHandler.DEBUG_HANDLER);
		cfg.setClassForTemplateLoading(RModelWriter.class,"");

		
		File rDir = new File(target,"R");
		rDir.mkdirs();
		
		File manDir = new File(target,"man");
		manDir.mkdirs();
		
		RModel.Type type = model.getClassType();
			
			Map<String,Object> typeRoot = new HashMap<>();
			typeRoot.put("class", type);
			typeRoot.put("jarFileName", jarFileName);
			
			doGenerate(new File(target,"NAMESPACE"),getTemplate("/namespace.ftl"),typeRoot);
			doGenerate(new File(target,"DESCRIPTION"),getTemplate("/description.ftl"),typeRoot);
			doGenerate(new File(rDir,type.getName()+".R"),getTemplate("/r.ftl"),typeRoot);
			doGenerate(new File(manDir,"java"+type.getName()+".Rd"),getTemplate("/rd.ftl"),typeRoot);
			
			for (RModel.Method method: type.getMethods()) {
				
				Map<String,Object> methodRoot = new HashMap<>();
				methodRoot.put("method", method);
				doGenerate(new File(manDir,method.getName()+".Rd"),getTemplate("/man.ftl"),methodRoot);
				
			}
			
		
	}

	private Template getTemplate(String name) throws MojoExecutionException {
		Template tmp;
		try {
			tmp = cfg.getTemplate(name);
		} catch (IOException e) {
			throw new MojoExecutionException("Couldn't load template "+name,e);
		}
		System.out.println("using template: "+name);
		return tmp;
	}

	// Utility to handle the freemarker generation mechanics.
	private void doGenerate(File file, Template tmp, Map<String,Object> root) throws MojoExecutionException {

		try {

			Writer out;

			out = new PrintWriter(new FileOutputStream(file));
			System.out.println("Writing file: "+file.getAbsolutePath());
			tmp.process(root, out);
			out.close();

		} catch (IOException e) {
			e.printStackTrace();
			throw new RuntimeException(e);
			// this should not happen. 

		} catch (TemplateException e) {
			
			throw new MojoExecutionException("Error in freemarker template: "+tmp.getName(),e);
		}
	}

}
