package edu.uab.ccts.nlp.shared_task.semeval2015.uima.annotator;

import org.apache.ctakes.typesystem.type.structured.DocumentID;
import org.apache.uima.UimaContext;
import org.apache.uima.analysis_engine.AnalysisEngineDescription;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.cas.FSArray;
import org.apache.uima.resource.ResourceInitializationException;
import org.apache.uima.util.Level;
import org.cleartk.semeval2015.type.DiseaseDisorder;
import org.cleartk.semeval2015.type.DiseaseDisorderAttribute;
import org.cleartk.semeval2015.type.DisorderRelation;
import org.cleartk.semeval2015.type.DisorderSpan;

import edu.uab.ccts.nlp.shared_task.semeval2015.SemEval2015Constants;

import org.apache.uima.fit.component.JCasAnnotator_ImplBase;
import org.apache.uima.fit.descriptor.ConfigurationParameter;
import org.apache.uima.fit.factory.AnalysisEngineFactory;
import org.apache.uima.fit.util.JCasUtil;

import java.io.File;
import java.io.FileWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.List;

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
	private String outputDir = "target/Semeval2015CountResults.tsv";


	public static boolean VERBOSE = false;

	public void initialize(UimaContext context) throws ResourceInitializationException
	{
		super.initialize(context);
		try {
			File out = new File(outputDir);
			if (!out.exists())
			{
				if (!out.mkdirs()) System.out.println("Could not make directory " + outputDir);
			} else
			{
				if (VERBOSE) System.out.println(outputDir + " exists!");
			}
		} catch (Exception e)
		{
			e.printStackTrace();
		}
	}

	@Override
	public void process(JCas aJCas) throws AnalysisEngineProcessException
	{
		JCas appView = null;
		String docid = null, filepath = null;
		try
		{
			appView = JCasUtil.getView(aJCas,SemEval2015Constants.APP_VIEW,aJCas.getView(SemEval2015Constants.GOLD_VIEW));
			if(!JCasUtil.exists(appView, DiseaseDisorder.class)) {
				this.getContext().getLogger().log(Level.WARNING,
						"No DiseaseDisorders found in either APP_VIEW or GOLD_VIEW/default");
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
		try
		{
			Writer writer = new FileWriter(filepath);
			//TreeSet<String> ordered_doc = new TreeSet<String>();
			for (DiseaseDisorder ds : JCasUtil.select(appView, DiseaseDisorder.class))
			{
				associateSpans(appView, ds);
				String results = getDiseaseDisorderSemEval2015Counts(docid, ds);
				//ordered_doc.add(results);
				if (VERBOSE) System.out.println(results);
				writer.write(results + "\n"); 
			}
			//for(String s : ordered_doc) { writer.write(s + "\n"); }
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
		/*
		for(int i=0;i<dd.getCuis().size();i++){
			if(i!=dd.getCuis().size()-1) output_lines.append(dd.getCuis(i)+" ");
			else output_lines.append(dd.getCuis(i));
		}
		*/
		output_lines.append(SemEval2015Constants.OUTPUT_SEPERATOR);
		FSArray atts = dd.getAttributes();
		output_lines.append(fetchAttributeCount(atts, SemEval2015Constants.NEGATION_RELATION));
		output_lines.append(fetchAttributeCount(atts, SemEval2015Constants.SUBJECT_RELATION));
		output_lines.append(fetchAttributeCount(atts, SemEval2015Constants.UNCERTAINITY_RELATION));
		output_lines.append(fetchAttributeCount(atts, SemEval2015Constants.COURSE_RELATION));
		output_lines.append(fetchAttributeCount(atts, SemEval2015Constants.SEVERITY_RELATION));
		output_lines.append(fetchAttributeCount(atts, SemEval2015Constants.CONDITIONAL_RELATION));
		output_lines.append(fetchAttributeCount(atts, SemEval2015Constants.GENERIC_RELATION));
		output_lines.append(fetchAttributeCount(atts, SemEval2015Constants.BODY_RELATION));
		//output_lines.append(fetchAttributeCount(atts, SemEval2015Constants.DOCTIME_RELATION));
		//output_lines.append(fetchAttributeCount(atts, SemEval2015Constants.TEMPORAL_RELATION));
		//		System.out.println();
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
				}
			}
		}
		String out = Integer.toString(theattcount);
		return out;
	}

	
	
	public static FSArray associateSpans(JCas jCas, DiseaseDisorder dd)
	{
		List<DiseaseDisorderAttribute> atts = new ArrayList<>();
		if(!JCasUtil.exists(jCas, DisorderRelation.class)) System.out.println("No disorder relations!");
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
		if(VERBOSE) System.out.println("Found "+atts.size()+" attributes in last annotator.");
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


	public static AnalysisEngineDescription getDescription() throws ResourceInitializationException {
		return AnalysisEngineFactory.createEngineDescription(SemEval2015Task2Consumer.class);
	}	


	/**
	 * Returns a descriptor for a Consumer that writes annotations to target directory
	 * @param target_directory
	 * @return
	 * @throws ResourceInitializationException
	 */
	public static AnalysisEngineDescription getCuilessDescription(String target_directory) throws ResourceInitializationException {
		return AnalysisEngineFactory.createEngineDescription(
				SemEval2015Task2Consumer.class
				,PARAM_OUTPUT_DIRECTORY
				,target_directory
				);
	}	

}
