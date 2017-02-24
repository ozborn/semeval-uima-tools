package edu.uab.ccts.nlp.shared_task.semeval2015.uima.annotator;

import org.apache.ctakes.typesystem.type.structured.DocumentID;
import org.apache.uima.UimaContext;
import org.apache.uima.analysis_engine.AnalysisEngineDescription;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.cas.FSArray;
import org.apache.uima.resource.ResourceInitializationException;
import org.apache.uima.util.Level;
import org.apache.uima.util.Logger;
import org.cleartk.semeval2015.type.DiseaseDisorder;
import org.cleartk.semeval2015.type.DiseaseDisorderAttribute;
import org.cleartk.semeval2015.type.DisorderRelation;
import org.cleartk.semeval2015.type.DisorderSpan;

import edu.uab.ccts.nlp.shared_task.semeval2015.SemEval2015Constants;
import edu.uab.ccts.nlp.shared_task.semeval2015.SemEvalConfig;

import org.apache.uima.fit.component.JCasAnnotator_ImplBase;
import org.apache.uima.fit.descriptor.ConfigurationParameter;
import org.apache.uima.fit.factory.AnalysisEngineFactory;
import org.apache.uima.fit.util.JCasUtil;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Properties;
import java.util.Set;

/**
 * Counts the number of attributes in a disorder in SemEval format files
 * @author ozborn
 *
 */
public class SemEval2015AttributeCounter extends JCasAnnotator_ImplBase {

	public static final String PARAM_OUTPUT_DIRECTORY = "outputDir";
	@ConfigurationParameter(
			name = PARAM_OUTPUT_DIRECTORY,
			description = "Path to the output directory for SemEval Counting",
			defaultValue="target/")
	private String outputDir = "target/Semeval2015CountResults";

	public static final String PARAM_OUTPUT_FILENAME = "allData";
	@ConfigurationParameter(
			name = PARAM_OUTPUT_FILENAME,
			description = "Filename in output directory for SemEval Counting",
			defaultValue="allCounts.txt")
	private String allData;

	String docid = null;
	SemEvalConfig semconfig;


	@Override
	public void initialize(UimaContext context) throws ResourceInitializationException
	{
		super.initialize(context);
		semconfig = new SemEvalConfig();
	}


	@Override
	public void process(JCas aJCas) throws AnalysisEngineProcessException
	{
		JCas appView = null;
		String filepath = null;
		this.getContext().getLogger().log(Level.FINE,"Writing to directory:"+outputDir);
		this.getContext().getLogger().log(Level.FINE,"Writing globally to filename:"+allData);
		try
		{
			appView = JCasUtil.getView(aJCas,SemEval2015Constants.GOLD_VIEW,false);
			//appView = JCasUtil.getView(aJCas,SemEval2015Constants.APP_VIEW,aJCas.getView(SemEval2015Constants.GOLD_VIEW));
			if(!JCasUtil.exists(appView, DiseaseDisorder.class)) {
				this.getContext().getLogger().log(Level.WARNING,
						"No DiseaseDisorders found in GOLD_VIEW/default");
			}

			for (DocumentID di : JCasUtil.select(appView, DocumentID.class))
			{
				docid = di.getDocumentID();
				break;
			}
			if(docid==null) {
				this.getContext().getLogger().log(Level.WARNING,
						"No DocumentID found in either APP_VIEW or GOLD_VIEW/default");
				return;
			}
			filepath = outputDir + File.separator +
					docid.substring(0, docid.length() - 4) + "pipe";

		} catch (Exception e)
		{
			e.printStackTrace();
			throw new AnalysisEngineProcessException(e);
		}

		try (Writer allwriter = new FileWriter(allData,true)){
			Writer writer = new FileWriter(filepath);
			for (DiseaseDisorder ds : JCasUtil.select(appView, DiseaseDisorder.class))
			{
				associateSpans(appView, ds);
				String results = getDiseaseDisorderSemEval2015Counts(docid, ds);
				this.getContext().getLogger().log(Level.FINER,results);
				writer.write(results + "\n"); 
				allwriter.write(results + "\n"); 
			}
			writer.close();
		} catch (Exception e)
		{
			e.printStackTrace();
		}
	}

	/**
	 */
	private String getDiseaseDisorderSemEval2015Counts(String docid, DiseaseDisorder dd )
	{
		Logger logger = this.getContext().getLogger();
		int countall = 0;
		StringBuffer output_lines = new StringBuffer(2000);
		output_lines.append(docid);
		output_lines.append(SemEval2015Constants.OUTPUT_SEPERATOR);
		FSArray spans = dd.getSpans();
		for (int i = 0; i < spans.size(); i++)
		{
			DisorderSpan ds = (DisorderSpan) spans.get(i);
			output_lines.append(ds.getBegin() + "-" + ds.getEnd());
			if (i != spans.size() - 1) output_lines.append(",");
		}
		output_lines.append(SemEval2015Constants.OUTPUT_SEPERATOR);
		output_lines.append(dd.getCuis().size());
		countall = dd.getCuis().size();
		HashSet<String>cuiSet = new HashSet<String>();
		for(int i=0;i<dd.getCuis().size();i++){
			this.getContext().getLogger().log(Level.FINER,"CUI@i"+i+":"+dd.getCuis(i));
			cuiSet.add(dd.getCuis(i));
		}
		output_lines.append(SemEval2015Constants.OUTPUT_SEPERATOR);
		FSArray atts = dd.getAttributes();

		cuiSet.addAll(fetchAttributeCuis(atts,SemEval2015Constants.NEGATION_RELATION));
		cuiSet.addAll(fetchAttributeCuis(atts,SemEval2015Constants.SUBJECT_RELATION));
		cuiSet.addAll(fetchAttributeCuis(atts,SemEval2015Constants.UNCERTAINTY_RELATION));
		cuiSet.addAll(fetchAttributeCuis(atts,SemEval2015Constants.COURSE_RELATION));
		cuiSet.addAll(fetchAttributeCuis(atts,SemEval2015Constants.SEVERITY_RELATION));
		cuiSet.addAll(fetchAttributeCuis(atts,SemEval2015Constants.CONDITIONAL_RELATION));
		cuiSet.addAll(fetchAttributeCuis(atts,SemEval2015Constants.GENERIC_RELATION));
		cuiSet.addAll(fetchAttributeCuis(atts,SemEval2015Constants.BODY_RELATION));

		if( fetchAttributeCount(atts, SemEval2015Constants.NEGATION_RELATION).indexOf("0")==-1) countall++;
		if( fetchAttributeCount(atts, SemEval2015Constants.SUBJECT_RELATION).indexOf("0")==-1) countall++;
		if( fetchAttributeCount(atts, SemEval2015Constants.UNCERTAINTY_RELATION).indexOf("0")==-1) countall++;
		if( fetchAttributeCount(atts, SemEval2015Constants.COURSE_RELATION).indexOf("0")==-1) countall++;
		if( fetchAttributeCount(atts, SemEval2015Constants.SEVERITY_RELATION).indexOf("0")==-1) countall++;
		if( fetchAttributeCount(atts, SemEval2015Constants.CONDITIONAL_RELATION).indexOf("0")==-1) countall++;
		if( fetchAttributeCount(atts, SemEval2015Constants.GENERIC_RELATION).indexOf("0")==-1) countall++;
		if( fetchAttributeCount(atts, SemEval2015Constants.BODY_RELATION).indexOf("0")==-1) countall++;

		output_lines.append(fetchAttributeCount(atts, SemEval2015Constants.NEGATION_RELATION));
		output_lines.append(fetchAttributeCount(atts, SemEval2015Constants.SUBJECT_RELATION));
		output_lines.append(fetchAttributeCount(atts, SemEval2015Constants.UNCERTAINTY_RELATION));
		output_lines.append(fetchAttributeCount(atts, SemEval2015Constants.COURSE_RELATION));
		output_lines.append(fetchAttributeCount(atts, SemEval2015Constants.SEVERITY_RELATION));
		output_lines.append(fetchAttributeCount(atts, SemEval2015Constants.CONDITIONAL_RELATION));
		output_lines.append(fetchAttributeCount(atts, SemEval2015Constants.GENERIC_RELATION));
		output_lines.append(fetchAttributeCount(atts, SemEval2015Constants.BODY_RELATION));
		//output_lines.append(fetchAttributeCount(atts, SemEval2015Constants.DOCTIME_RELATION));
		//output_lines.append(fetchAttributeCount(atts, SemEval2015Constants.TEMPORAL_RELATION));
		output_lines.append(countall+"|");
		output_lines.append(cuiSet.size()+"|");
		if(countall!=cuiSet.size()) logger.log(Level.WARNING,"Disease CUI Counts not matching ("+
				cuiSet.toString()+") for:"+output_lines);
		return output_lines.toString();
	}

	/**
	 * Too bad UIMA doesn't have built in hashtables...
	 */
	private String fetchAttributeCount(FSArray atts, String type)
	{
		int theattcount = 0;
		if (atts != null)
		{
			for (int i = 0; i < atts.size(); i++)
			{
				DiseaseDisorderAttribute dda = (DiseaseDisorderAttribute) atts.get(i);
				if (type.equals(dda.getAttributeType()))
				{
					theattcount++;
					break;
				}
			}
		}
		String out = Integer.toString(theattcount)+SemEval2015Constants.OUTPUT_SEPERATOR;
		return out;
	}


	private Set<String> fetchAttributeCuis(FSArray atts, String type)
	{
		HashSet<String> thecuis = new HashSet<String>();
		if (atts != null)
		{
			for (int i = 0; i < atts.size(); i++)
			{
				DiseaseDisorderAttribute dda = (DiseaseDisorderAttribute) atts.get(i);
				if (type.equals(dda.getAttributeType()))
				{
					String norm = dda.getNorm();
					thecuis.addAll(semconfig.semevalNorm2Cui(type, norm));
				}
			}
		}
		return thecuis;
	}


	private FSArray associateSpans(JCas jCas, DiseaseDisorder dd)
	{
		Logger logger = this.getContext().getLogger();
		List<DiseaseDisorderAttribute> atts = new ArrayList<>();
		if(!JCasUtil.exists(jCas, DisorderRelation.class)) {
			logger.log(Level.FINE,"No disorder relations in "+docid+"|"
					+dd.getCoveredText()+"|"+dd.getBegin()+"-"+dd.getEnd());
		}
		for (DisorderRelation rel: JCasUtil.select(jCas, DisorderRelation.class))
		{
			DisorderSpan s = (DisorderSpan) rel.getArg2().getArgument();

			for (DisorderSpan span : JCasUtil.select(dd.getSpans(), DisorderSpan.class))
			{
				if (span == s)
				{
					atts.add((DiseaseDisorderAttribute) rel.getArg1().getArgument());
				}
			}
		}
		logger.log(Level.FINER,"Found "+atts.size()+" attributes in last annotator.");
		FSArray relSpans = new FSArray(jCas, atts.size());
		int min_begin = -1, max_end = -1;
		for (int i = 0; i < atts.size(); i++)
		{
			DiseaseDisorderAttribute ds = atts.get(i);
			if (ds.getBegin() < min_begin || min_begin == -1) min_begin = ds.getBegin();
			if (ds.getEnd() > max_end) max_end = ds.getEnd();
			relSpans.set(i, ds);
		}
		dd.setAttributes(relSpans);
		return relSpans;
	}


	public static AnalysisEngineDescription getDescription(String dir, String fn) 
			throws ResourceInitializationException {
		return AnalysisEngineFactory.createEngineDescription(
				SemEval2015AttributeCounter.class,
				PARAM_OUTPUT_DIRECTORY,dir,
				PARAM_OUTPUT_FILENAME,fn
				);
	}	


}
