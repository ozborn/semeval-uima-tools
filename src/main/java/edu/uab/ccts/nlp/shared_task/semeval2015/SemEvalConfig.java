package edu.uab.ccts.nlp.shared_task.semeval2015;

import java.io.IOException;
import java.net.URL;
import java.util.HashSet;
import java.util.Properties;
import java.util.Set;
import java.util.regex.Pattern;

import org.apache.uima.UIMAFramework;
import org.apache.uima.resource.ResourceInitializationException;
import org.apache.uima.util.Level;

public class SemEvalConfig {

	Properties semeval2umls;

	public SemEvalConfig() throws ResourceInitializationException {
		semeval2umls = new Properties();
		try {
			URL u1 = this.getClass().getResource("/semeval2umls.properties");
			semeval2umls.load(u1.openStream());
		} catch (IOException ioe) { ioe.printStackTrace(); throw new ResourceInitializationException();}
		UIMAFramework.getLogger().log(Level.CONFIG, "Loaded semeval2umls.properties");
	}

	
	public Set<String> semevalNorm2Cui(String type, String norm) {
		HashSet<String>thecuis = new HashSet<String>();

		if(norm==null) {
			UIMAFramework.getLogger().log(Level.WARNING, "Received null norm");
			return thecuis;
		}
		String cui = norm;
		if(! (	cui.matches("C\\d\\d\\d\\d\\d\\d\\d") 
				// || cui.equalsIgnoreCase("cui-less")
			)) {
			if(type.equalsIgnoreCase(SemEval2015Constants.UNCERTAINTY_RELATION)
					&& norm.equalsIgnoreCase("yes"))
				cui = semeval2umls.getProperty(SemEval2015Constants.UNCERTAINTY_RELATION.toLowerCase());
			else if(type.equalsIgnoreCase(SemEval2015Constants.NEGATION_RELATION)
					&& norm.equalsIgnoreCase("yes"))
				cui = semeval2umls.getProperty("negative");
			else if(type.equalsIgnoreCase(SemEval2015Constants.GENERIC_RELATION)
					&& norm.equalsIgnoreCase("true"))
				cui = semeval2umls.getProperty(SemEval2015Constants.GENERIC_RELATION.toLowerCase());
			else if(type.equalsIgnoreCase(SemEval2015Constants.CONDITIONAL_RELATION)
					&& norm.equalsIgnoreCase("true"))
				cui = semeval2umls.getProperty(SemEval2015Constants.CONDITIONAL_RELATION.toLowerCase());
			else if(type.equalsIgnoreCase(SemEval2015Constants.SEVERITY_RELATION))
				cui = semeval2umls.getProperty(norm.toLowerCase());
			else if(type.equalsIgnoreCase(SemEval2015Constants.COURSE_RELATION))
				cui = semeval2umls.getProperty(norm.toLowerCase());
			else if(type.equalsIgnoreCase(SemEval2015Constants.SUBJECT_RELATION))
				cui = semeval2umls.getProperty(norm.toLowerCase());
			else if(type.equalsIgnoreCase(SemEval2015Constants.BODY_RELATION)) {
				boolean bad = norm.matches("\\d\\d\\d\\d\\d\\d\\d*");
				if(bad) cui = "C"+norm;
				if(norm.equalsIgnoreCase("cuiless")) cui = "CUI-less";
			} else {
				UIMAFramework.getLogger().log(Level.WARNING,"Unknown relation for norm:"+norm+" in type:"+type);
				cui = semeval2umls.getProperty(norm.toLowerCase());
				if(cui!=null) { 
					UIMAFramework.getLogger().log(Level.WARNING
					,"Unknown relation for norm:"+norm+" had CUI set to :"+cui);
				}
			}
			if(cui==null) {
				UIMAFramework.getLogger().log(Level.WARNING,"Null CUI for norm:"+norm+" in type:"+type);
			}
		}
		thecuis.add(cui);
		return thecuis;
	}

}
