package uk.co.terminological.rjava.plugin;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import com.thoughtworks.qdox.model.JavaType;


import uk.co.terminological.rjava.types.*;

/** This is used for parameters and for return types.
 * Originally it supported generics etc but latterly only supports the {@link RObject} 
 * class hierachy plus a few convenience types defined here.
 * 
 * @author terminological
 *
 * 
 * 
 */
public class RType {
	
	// TODO: could this be moved to the runtime package and would that be beneficial
	// there is a high degree of interdependence between this and the runtime package annotations
	
	//TODO: this is not used and coudl be stripped out
	//private RModel model;
	protected String simpleName;
	protected String canonicalName;
	private List<String> inputRCode;
	private List<String> outputRCode;
	private String JNIType;
	//List<RType> typeParameters = new ArrayList<>();
	
	
	// QDox derived types. This might not be used anymore
	public RType(RModel model, List<String> inputRCode, List<String> outputRCode, String JNIType, JavaType type, String simpleName) {
		//this.model = model;
		this.simpleName = simpleName;
		this.inputRCode = inputRCode;
		this.outputRCode = outputRCode;
		this.JNIType = JNIType;
		this.canonicalName = type.getCanonicalName();
		// DEAL with empty
	}
	
	// Reflection based constructor which will use classes from the runtime class
	// and manually defined ones 
	public RType(RModel model, List<String> inputRCode, List<String> outputRCode, String JNIType, Class<?> type) {
		//this.model = model;
		this.simpleName = type.getSimpleName();
		this.inputRCode = inputRCode;
		this.outputRCode = outputRCode;
		this.JNIType = JNIType;
		this.canonicalName = type.getCanonicalName()==null ? "NULL" : type.getCanonicalName();
		// DEAL with empty
	}

	static String jni(Class<?> c) {
		return c.getCanonicalName().replace(".", "/");
	}
	
	// Assumes the implementation details from rjavaApiR.ftl
	// Be good to do all of this as a ftl macro really
	static String conv(Class<?> c) {
		return "self$.toJava$"+c.getSimpleName();
	}
	
	//likewise this as a macro
	//these are conveniences for me when creating the annotations in the
	//runtime package.
	static Map<String,String> aliases = new HashMap<String,String>();
	{
		aliases.put("~ROBJECT~", jni(RObject.class));
		aliases.put("~RNULL~", jni(RNull.class));
		
		aliases.put("~RCHARACTER~", jni(RCharacter.class));
		aliases.put("~RCHARACTERVECTOR~", jni(RCharacterVector.class));
		aliases.put("~RNUMERIC~", jni(RNumeric.class));
		aliases.put("~RNUMERICVECTOR~", jni(RNumericVector.class));
		aliases.put("~RFACTOR~", jni(RFactor.class));
		aliases.put("~RFACTORVECTOR~", jni(RFactorVector.class));
		aliases.put("~RLOGICAL~", jni(RLogical.class));
		aliases.put("~RLOGICALVECTOR~", jni(RLogicalVector.class));
		aliases.put("~RINTEGER~", jni(RInteger.class));
		aliases.put("~RINTEGERVECTOR~", jni(RIntegerVector.class));
		aliases.put("~RDATE~", jni(RDate.class));
		aliases.put("~RDATEVECTOR~", jni(RDateVector.class));
		aliases.put("~RUNTYPEDNA~", jni(RUntypedNa.class));
		aliases.put("~RUNTYPEDNAVECTOR~", jni(RUntypedNaVector.class));
		
		aliases.put("~RDATAFRAME~", jni(RDataframe.class));
		aliases.put("~RVECTOR~", jni(RVector.class));
		
		aliases.put("~RNUMERICARRAY~", jni(RNumericArray.class));
		
		aliases.put("~RLIST~", jni(RList.class));
		aliases.put("~RNAMEDLIST~", jni(RNamedList.class));
		
		aliases.put("~TO_RCHARACTERVECTOR~", conv(RCharacterVector.class));
		aliases.put("~TO_RNUMERICVECTOR~", conv(RNumericVector.class));
		aliases.put("~TO_RFACTORVECTOR~", conv(RFactorVector.class));
		aliases.put("~TO_RLOGICALVECTOR~", conv(RLogicalVector.class));
		aliases.put("~TO_RINTEGERVECTOR~", conv(RIntegerVector.class));
		aliases.put("~TO_RDATEVECTOR~", conv(RDateVector.class));
		aliases.put("~TO_RUNTYPEDNAVECTOR~", conv(RUntypedNaVector.class));
		
		aliases.put("~TO_RCHARACTER~", conv(RCharacter.class));
		aliases.put("~TO_RNUMERIC~", conv(RNumeric.class));
		aliases.put("~TO_RFACTOR~", conv(RFactor.class));
		aliases.put("~TO_RLOGICAL~", conv(RLogical.class));
		aliases.put("~TO_RINTEGER~", conv(RInteger.class));
		aliases.put("~TO_RDATE~", conv(RDate.class));
		aliases.put("~TO_RUNTYPEDNA~", conv(RUntypedNa.class));
		
		aliases.put("~TO_RDATAFRAME~", conv(RDataframe.class));
		aliases.put("~TO_RLIST~", conv(RList.class));
		aliases.put("~TO_RNAMEDLIST~", conv(RNamedList.class));
		aliases.put("~TO_RNULL~", conv(RNull.class));
		
		aliases.put("~TO_RNUMERICARRAY~", conv(RNumericArray.class));
		
	}
	
	public List<String> getInputRCode() {
		for (Entry<String,String> kv: aliases.entrySet()) {
			List<String> tmpList = new ArrayList<>();
			for (int i=0;i<inputRCode.size(); i++) {
				
				String tmp = inputRCode.get(i).replace(kv.getKey(), kv.getValue());
				tmpList.add(tmp);
				
			}
			inputRCode = tmpList;
		}
		return inputRCode;
	}

	public List<String> getOutputRCode() {
		for (Entry<String,String> kv: aliases.entrySet()) {
			List<String> tmpList = new ArrayList<>();
			for (int i=0;i<outputRCode.size(); i++) {
				
				String tmp = outputRCode.get(i).replace(kv.getKey(), kv.getValue());
				tmpList.add(tmp);
				
			}
			outputRCode = tmpList;
		}
		return outputRCode;
	}

	public String getJNIType() {
		return JNIType;
	}

	public String getSimpleName() {
		return simpleName;
	}
	
	public String getCanonicalName() {
		return canonicalName;
	}
	
	// The types that are supported but are not RObject classes from the runtime package
	// are here. Adding in extras here should not require any additional things to be updated
	// Other that they need to be added at the beginning of the parse to {@link RModel}. This
	// happens in the {@link QDoxParser}
	// Each type defined the r code mapping it to r from jni, r to jni and the wire type
	public static RType voidType(RModel model) {
		return new RType(model, 
				Arrays.asList("function(rObj) stop('no input expected')"), 
				Arrays.asList("function(jObj) invisible(NULL)"),
				"V",
				void.class);
	}
	
	public static RType stringType(RModel model) {
		return new RType(model, 
				Arrays.asList("function(rObj) return(as.character(rObj))"), 
				Arrays.asList("function(jObj) return(as.character(jObj))"),
				"Ljava/lang/String;",
				String.class);
	}
	
	public static RType intType(RModel model) {
		return new RType(model, 
				Arrays.asList(
						"function(rObj) {",
						"    if (is.na(rObj)) stop('cant use NA as input to java int')",
						"    if (length(rObj) > 1) stop('input too long')", 
						"    if (as.integer(rObj)[[1]]!=rObj[[1]]) stop('not an integer')",
						"    return(as.integer(rObj[[1]]))",
						"}"), 
				Arrays.asList("function(jObj) return(as.integer(jObj))"),
				"I",
				int.class);
	}
	
	public static RType doubleType(RModel model) {
		return new RType(model, 
				Arrays.asList(
						"function(rObj) {",
						"    if (is.na(rObj)) stop('cant use NA as input to java double')",
						"    if (length(rObj) > 1) stop('input too long')", 
						"    if (!is.numeric(rObj)) stop('not an double')",
						"    return(as.numeric(rObj[[1]]))",
						"}"), 
				Arrays.asList("function(jObj) return(as.numeric(jObj))"),
				"D",
				double.class);
	}
	
	public static RType booleanType(RModel model) {
		return new RType(model, 
				Arrays.asList(
						"function(rObj) {",
						"    if (is.na(rObj)) stop('cant use NA as input to java boolean')",
						"    if (length(rObj) > 1) stop('input too long')", 
						"    if (!is.logical(rObj)) stop('not a logical')",
						"    return(as.logical(rObj[[1]]))",
						"}"), 
				Arrays.asList("function(jObj) return(as.logical(jObj))"),
				"Z",
				boolean.class);
	}
	
}