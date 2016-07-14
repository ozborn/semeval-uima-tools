package edu.uab.ccts.nlp.shared_task.semeval2015.uima.annotator;

import org.apache.commons.io.FileUtils;
import org.apache.ctakes.typesystem.type.relation.RelationArgument;
import org.apache.ctakes.typesystem.type.structured.DocumentID;
import org.apache.ctakes.typesystem.type.textspan.Sentence;
import org.apache.uima.UimaContext;
import org.apache.uima.analysis_engine.AnalysisEngineDescription;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.cas.CASException;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.cas.FSArray;
import org.apache.uima.jcas.cas.StringArray;
import org.apache.uima.resource.ResourceInitializationException;

import org.cleartk.semeval2015.type.DiseaseDisorder;
import org.cleartk.semeval2015.type.DiseaseDisorderAttribute;
import org.cleartk.semeval2015.type.DisorderRelation;
import org.cleartk.semeval2015.type.DisorderSpan;
import org.cleartk.semeval2015.type.DisorderSpanRelation;

import edu.uab.ccts.nlp.shared_task.semeval2015.SemEval2015Constants;

import org.apache.uima.fit.component.JCasAnnotator_ImplBase;
import org.apache.uima.fit.descriptor.ConfigurationParameter;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.fit.factory.AnalysisEngineFactory;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * This should be the parser of choice to read in
 * the multiCUI/multi attribute annotations of SemEval2015
 * (not SemEval2015TaskCGoldAnnotator which is deprecated)
 * The prev_disease component does work though
 * 		-relevant even in the updated data set
 *      -problem is need  fixing this also requires fixing output to put it back in SemEval format
 *      -will make new type joining DiseaseDisorderTogether -> JoinedDisease
 * @author ozborn
 *
 */
public class SemEval2015GoldAttributeParserAnnotator extends JCasAnnotator_ImplBase
{
	public static final String PARAM_TRAINING = "training";
	public static final String PARAM_CUI_MAP = "cuiMap";
	public static final int dd_doc = 0;
	public static final int dd_spans = 1;
	public static final int dd_cui = 2;
	public static final int ni_norm = 3;
	public static final int ni_cue = 4;
	public static final int sc_norm = 5;
	public static final int sc_cue = 6;
	public static final int ui_norm = 7;
	public static final int ui_cue = 8;
	public static final int cc_norm = 9;
	public static final int cc_cue = 10;
	public static final int sv_norm = 11;
	public static final int sv_cue = 12;
	public static final int co_norm = 13;
	public static final int co_cue = 14;
	public static final int gc_norm = 15;
	public static final int gc_cue = 16;
	public static final int bl_norm = 17;
	public static final int bl_cue = 18;
	public static final int dt_norm = 19;
	public static final int te_norm = 20;
	public static final int te_cue = 21;
	public static final int totalFields = SemEval2015Constants.TOTAL_FIELDS;
	public static boolean VERBOSE = false;
	public static final String DISJOINT_SPAN = "dspan";
	public static HashMap<String, String> stringCUIMap;
	@ConfigurationParameter(
			name = PARAM_TRAINING,
			description = "indicates whether we should build a CUI map using this annotator")
	protected boolean training = true;
	@ConfigurationParameter(
			name = PARAM_CUI_MAP,
			description = "file to read CUI map from in testing and application")
	protected String cuiMap = null;
	
	
	public static void writeMapToFile(HashMap<String, String> stringCUIMap, File outputFile)
	{
		StringBuilder output = new StringBuilder();
		for (String key : stringCUIMap.keySet())
			output.append(key).append("\t").append(stringCUIMap.get(key)).append("\n");
		try
		{
			FileUtils.writeStringToFile(outputFile, output.toString());
		} catch (IOException e)
		{
			e.printStackTrace();
		}
	}
	
	
	public static HashMap<String, String> getMap(File outputFile)
	{
		String input;
		HashMap<String, String> cuiMap = new HashMap<>();
		try
		{
			input = FileUtils.readFileToString(outputFile);
		} catch (IOException e)
		{
			System.err.println("Unable to read text-cui map file: " + outputFile.getPath());
			return cuiMap;
		}
		for (String line : input.split("\n"))
		{
			String[] fields = line.split("\t");
			cuiMap.put(fields[0], fields[1]);
		}
		return cuiMap;
	}
	
	
	
	public static void createDiseaseAttributeRelation(JCas jCas, DiseaseDisorder disease, DiseaseDisorderAttribute att)
	{
		for (DisorderSpan span : JCasUtil.select(disease.getSpans(), DisorderSpan.class))
		{
			createAttributeRelation(jCas, att, span, att.getAttributeType());
		}
	}
	
	
	public void initialize(UimaContext context) throws ResourceInitializationException
	{
		super.initialize(context);
		if (training)
			stringCUIMap = new HashMap<>();
		else
			stringCUIMap = getMap(new File(cuiMap));
	}
	

	/**
	 * Creates SemEval/ClearClinical annotations in PIPED_VIEW (used to use default view)
	 */
	public void process(JCas jcas) throws AnalysisEngineProcessException
	{
		JCas pipedView = null, goldTextView = null;
		try
		{
			pipedView = jcas.getView(SemEval2015Constants.PIPED_VIEW);
			goldTextView = jcas.getView(SemEval2015Constants.GOLD_VIEW);
		} catch (CASException e)
		{
			e.printStackTrace();
			System.exit(0);
		}
		
		//Set up document level holders for disease
		ArrayList<DiseaseDisorder> docdiseases = new ArrayList<DiseaseDisorder>(pipedView.getDocumentText().split("\n").length);
		List<List<DisorderSpan>> disjointSpans = new ArrayList<>(); //Keep track of to add in relation
		String docId = "";
		for (String line : pipedView.getDocumentText().split("\n"))
		{
			List<DiseaseDisorderAttribute> diseaseAtts = new ArrayList<>();
			String[] fields = line.split("\\|");
			if (fields.length < totalFields)
			{
				System.out.println("Wrong format ("+fields.length+"): " + line);
				continue;
			}
			docId = fields[dd_doc];
			String cui_string = fields[dd_cui];
			
			//Handle Discontinuous Spans
			String[] ddSpans = fields[dd_spans].split(",");
			ArrayList<DisorderSpan> cur_spans = new ArrayList<>();
			FSArray prev_spans = null;
			String text = "";
			try
			{
				if(docdiseases.size()!=0) {
					DiseaseDisorder prev_disease = docdiseases.get(docdiseases.size()-1);
					prev_spans = prev_disease.getSpans();
				}
				for (String ddSpan : ddSpans) {
					String[] startBegin = ddSpan.split("-");
					int begin = Integer.parseInt(startBegin[0]);
					int end = Integer.parseInt(startBegin[1]);
					DisorderSpan dspan = new DisorderSpan(goldTextView, begin, end);
					dspan.setChunk("");
					dspan.setCui(cui_string);
					dspan.addToIndexes(goldTextView);
					cur_spans.add(dspan);
					String disorderText = dspan.getCoveredText().trim().toLowerCase();
					text += disorderText + " "; //Used only for CUI map
				}
				text = text.trim();
				if (training)
				{
					text = text.replaceAll("[\\s\\r\\n]", " "); // replace any newline characters and whatnot
					text = text.replaceAll("\\s+", " ");
					stringCUIMap.put(text, cui_string);
				}
				//Determine if we have seen this disease before 
				//Required to handle situation where next line is NOT a new disease but an additional anatomical mapping
				//Doesn't appear to be needed with updated data, still seen with updated data (10 times in devel)
				boolean seen_before = true;
				for(int i=0;i<cur_spans.size();i++){
					DisorderSpan cur = cur_spans.get(i);
					if(prev_spans==null || !spanSeenBefore(cur,prev_spans)){
						seen_before = false;
						break;
					}
				}
				if(seen_before) {
					System.out.println("This line was seen before:"+line); System.out.flush();
				}
				if (cur_spans.size() > 1) /* multi-span disorder */
				{
					disjointSpans.add(cur_spans);
				}
				//Set up disease
				DiseaseDisorder disease = new DiseaseDisorder(goldTextView);
				FSArray relSpans = new FSArray(goldTextView, cur_spans.size());
				int min_begin = -1, max_end = -1;
				for (int i = 0; i < cur_spans.size(); i++)
				{
					DisorderSpan ds = cur_spans.get(i);
					if (ds.getBegin() < min_begin || min_begin == -1) min_begin = ds.getBegin();
					if (ds.getEnd() > max_end) max_end = ds.getEnd();
					relSpans.set(i, ds);
				}
				disease.setSpans(relSpans);
				disease.setBegin(min_begin);
				disease.setEnd(max_end);
				String[] cuis = cui_string.split(" ");
				StringArray usa = new StringArray(goldTextView,cuis.length);
				for(int i=0;i<cuis.length;i++){ usa.set(i,cuis[i]); }
				usa.addToIndexes();
				disease.setCuis(usa);

				docdiseases.add(disease);
				disease.addToIndexes(goldTextView);
				/* Extract attributes */
				extractAttribute(goldTextView, diseaseAtts, fields,
						bl_norm, bl_cue, SemEval2015Constants.BODY_RELATION, disease);
				extractAttribute(goldTextView, diseaseAtts, fields,
						co_norm, co_cue, SemEval2015Constants.CONDITIONAL_RELATION, disease);
				extractAttribute(goldTextView, diseaseAtts, fields,
						gc_norm, gc_cue, SemEval2015Constants.GENERIC_RELATION, disease);
				extractAttribute(goldTextView, diseaseAtts, fields,
						ni_norm, ni_cue, SemEval2015Constants.NEGATION_RELATION, disease);
				extractAttribute(goldTextView, diseaseAtts, fields,
						sv_norm, sv_cue, SemEval2015Constants.SEVERITY_RELATION, disease);
				extractAttribute(goldTextView, diseaseAtts, fields,
						sc_norm, sc_cue, SemEval2015Constants.SUBJECT_RELATION, disease);
				extractAttribute(goldTextView, diseaseAtts, fields,
						ui_norm, ui_cue, SemEval2015Constants.UNCERTAINITY_RELATION, disease);
				extractAttribute(goldTextView, diseaseAtts, fields,
						cc_norm, cc_cue, SemEval2015Constants.COURSE_RELATION, disease);
				/*
	String ccNorm = fields[cc_norm];
	String ccOffsets = fields[cc_cue];
	//Hack to handle errors in training discharge task 2 data (03087-026480.pipe, 17644-017974.pipe,15230-012950.pipe )
	if(ccOffsets.equals("nul") || ccOffsets.equals("unmarked")) ccOffsets="null";
	if (!ccOffsets.equals("null"))
	{
	String[] offsets = ccOffsets.split("-");
	int begin = Integer.parseInt(offsets[0]);
	int end = Integer.parseInt(offsets[1]);
	DiseaseDisorderAttribute cc = new DiseaseDisorderAttribute(goldTextView, begin, end);
	cc.setNorm(ccNorm);
	cc.setAttributeType(SemEval2015Constants.COURSE_RELATION);
	cc.addToIndexes();
	diseaseAtts.add(cc);
	createAttributeRelation(goldTextView, disease, cc);
	}
				 */
				if (totalFields > 19)
				{
					extractAttribute(goldTextView, diseaseAtts, fields,
							te_norm, te_cue, SemEval2015Constants.TEMPORAL_RELATION, disease);
					String dtNorm = fields[dt_norm];
					if (!dtNorm.equals(SemEval2015Constants.defaultNorms.get(SemEval2015Constants.DOCTIME_RELATION)))
					{
						int begin = 1;
						int end = goldTextView.getDocumentText().length() - 1;
						DiseaseDisorderAttribute dt = new DiseaseDisorderAttribute(goldTextView, begin, end);
						dt.setNorm(dtNorm);
						dt.setAttributeType(SemEval2015Constants.DOCTIME_RELATION);
						dt.addToIndexes();
						diseaseAtts.add(dt);
					}
				}
				FSArray diseaseAttributes = new FSArray(goldTextView, diseaseAtts.size());
				for (int i = 0; i < diseaseAtts.size(); i++)
				{
					diseaseAttributes.set(i, diseaseAtts.get(i));
				}
				disease.setAttributes(diseaseAttributes);
				//System.out.println("Disease ("+fields[1]+") in "+fields[0]+" set "+diseaseAttributes.size()+" attributes.");
			} catch (NumberFormatException e)
			{
				System.out.println("Piped format error in line: " + line);
				e.printStackTrace();
			}
		}
		/* Add relations for multi-span disorders */
		for (List<DisorderSpan> multiSpanDisorder : disjointSpans)
		{
			while (multiSpanDisorder.size() > 1)
			{
				DisorderSpan arg1 = multiSpanDisorder.remove(0);
				DisorderSpan arg2 = multiSpanDisorder.get(0);
				createDisjointSpanRelation(goldTextView, arg1, arg2, DISJOINT_SPAN);
				if (VERBOSE)
					System.out.println("Added relation: " + arg1.getCoveredText() + "--" + arg2.getCoveredText());
			}
		}
		/* add doc id for output purposes */
		DocumentID id = new DocumentID(pipedView);
		id.setDocumentID(docId);
        id.addToIndexes(pipedView); id.addToIndexes(goldTextView);
	}
	private void extractAttribute(JCas jCas,
			List<DiseaseDisorderAttribute> dAtts, String[] fields,
			int input_norm, int input_cue, String relation, DiseaseDisorder disease)
	{
		String blNorm = fields[input_norm];
		String blOffsets = fields[input_cue];
		if (!blOffsets.equals("null"))
		{
			DiseaseDisorderAttribute bl = new DiseaseDisorderAttribute(jCas);
			String[] commasplit = blOffsets.split(",");
			FSArray attspans = new FSArray(jCas,commasplit.length);
			for(int i=0;i<commasplit.length;i++){
				String[] offsets = commasplit[i].split("-");
				int begin = Integer.parseInt(offsets[0]);
				int end = Integer.parseInt(offsets[1]);
				if(i==0)bl.setBegin(begin);
				bl.setEnd(end);
				DisorderSpan ds = new DisorderSpan(jCas, begin, end);
				attspans.set(i, ds);
			}
			bl.setNorm(blNorm);
			bl.setAttributeType(relation);
			bl.setSpans(attspans);
			bl.addToIndexes();
			dAtts.add(bl);
			createDiseaseAttributeRelation(jCas, disease, bl);
		}
	}
	/**
	 * Determine if span ds is present in prevSpans, if yes, return true
	 * @param ds
	 * @param prevSpans
	 * @return
	 */
	private boolean spanSeenBefore(DisorderSpan ds, FSArray prevSpans){
		for(int j=0;j<prevSpans.size();j++){
			DisorderSpan old = (DisorderSpan) prevSpans.get(j);
			if(ds.getBegin()==old.getBegin() && ds.getEnd()==old.getEnd()) {
				return true;
			}
		}
		return false;
	}



	public static void createAttributeRelation(
			JCas jCas,
			DiseaseDisorderAttribute arg1,
			DisorderSpan arg2,
			String predictedCategory)
	{
		RelationArgument relArg1 = new RelationArgument(jCas);
		relArg1.setArgument(arg1);
		relArg1.setRole("arg1");
		RelationArgument relArg2 = new RelationArgument(jCas);
		relArg2.setArgument(arg2);
		relArg2.setRole("arg2");
		DisorderRelation relation = new DisorderRelation(jCas);
		List<Sentence> s = JCasUtil.selectCovering(jCas, Sentence.class, arg1.getBegin(), arg1.getEnd());
		List<Sentence> s2 = JCasUtil.selectCovering(jCas, Sentence.class, arg2.getBegin(), arg2.getEnd());
		for (Sentence sent : s)
		{
			if (!s2.contains(sent))
				return;
		}
		relArg1.addToIndexes();
		relArg2.addToIndexes();

		relation.setArg1(relArg1);
		relation.setArg2(relArg2);
		relation.setCategory(predictedCategory);

		relation.addToIndexes();
	}




	public static void createDisjointSpanRelation(
			JCas jCas,
			DisorderSpan arg1,
			DisorderSpan arg2,
			String predictedCategory)
	{
		RelationArgument relArg1 = new RelationArgument(jCas);
		relArg1.setArgument(arg1);
		relArg1.setRole("arg1");
		relArg1.addToIndexes();
		RelationArgument relArg2 = new RelationArgument(jCas);
		relArg2.setArgument(arg2);
		relArg2.setRole("arg2");
		relArg2.addToIndexes();
		DisorderSpanRelation relation = new DisorderSpanRelation(jCas);
		relation.setArg1(relArg1);
		relation.setArg2(relArg2);
		relation.setCategory(predictedCategory);
		relation.addToIndexes();
	}

	
	public static AnalysisEngineDescription getTrainingDescription() throws ResourceInitializationException {
		return AnalysisEngineFactory.createEngineDescription(SemEval2015GoldAttributeParserAnnotator.class,
				SemEval2015GoldAttributeParserAnnotator.PARAM_TRAINING, true,
				SemEval2015GoldAttributeParserAnnotator.PARAM_CUI_MAP,"src/main/resources/data/cuiMap.txt");
	}
	
	public static AnalysisEngineDescription getDescription() throws ResourceInitializationException {
		return AnalysisEngineFactory.createEngineDescription(SemEval2015GoldAttributeParserAnnotator.class
				);
	}
	
	
	public static AnalysisEngineDescription getTestingDescription() throws ResourceInitializationException {
		return AnalysisEngineFactory.createEngineDescription(SemEval2015GoldAttributeParserAnnotator.class,
				SemEval2015GoldAttributeParserAnnotator.PARAM_TRAINING, false,
				SemEval2015GoldAttributeParserAnnotator.PARAM_CUI_MAP,"src/main/resources/data/cuiMap.txt");
	}

}
