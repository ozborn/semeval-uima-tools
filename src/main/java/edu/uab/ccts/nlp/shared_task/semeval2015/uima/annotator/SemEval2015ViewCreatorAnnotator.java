package edu.uab.ccts.nlp.shared_task.semeval2015.uima.annotator;

import java.io.File;
import java.io.IOException;

import org.apache.commons.io.FileUtils;
import org.apache.uima.analysis_engine.AnalysisEngineDescription;
import org.cleartk.util.ViewUriUtil;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.fit.component.JCasAnnotator_ImplBase;
import org.apache.uima.fit.factory.AnalysisEngineFactory;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;
import org.apache.uima.resource.ResourceInitializationException;
import org.apache.uima.util.Level;
import org.apache.uima.util.Logger;
import org.uimafit.descriptor.ConfigurationParameter;

import edu.uab.ccts.nlp.shared_task.semeval2015.SemEval2015Constants;


/**
 * This file reads in piped annotation SemEval 2015 files into the PIPED_VIEW 
 * and the text into default/GOLD_VIEW
 * @author ozborn
 *
 */
public class SemEval2015ViewCreatorAnnotator extends JCasAnnotator_ImplBase {


	public static final String PARAM_TRAINING_PATH = "SemEval2015TrainingPath";
	@ConfigurationParameter(
			name = PARAM_TRAINING_PATH,
			description = "path to training directory"
	)
	private String SemEval2015TrainingPath = SemEval2015Constants.oldTrainingPath; //Needed default not working FIXME
	private Logger LOG = null; 


	@Override
	public void process(JCas jcas) throws AnalysisEngineProcessException {
		LOG = this.getContext().getLogger();
		JCas pipedView = null, semevalTextView = null;
		pipedView = JCasUtil.getView(jcas, SemEval2015Constants.PIPED_VIEW, true);
		semevalTextView = JCasUtil.getView(jcas, SemEval2015Constants.GOLD_VIEW, true);
		String name = new File(ViewUriUtil.getURI(jcas).getPath()).getName();
		LOG.log(Level.FINE,"Determining SemEval2015 prefix and type from URI:"+
		ViewUriUtil.getURI(jcas).getPath());
		String[] bits = name.split("-");
		String prefix = bits[0]+"-"+bits[1];
		LOG.log(Level.FINEST,"Prefix was:"+prefix);
		String type = "discharge"; //Default type
		if(bits.length>=3){
			type = bits[2];
			if(type.toLowerCase().startsWith("discharge")) type="discharge";
			if(type.toLowerCase().startsWith("ecg")) type="ecg";
			if(type.toLowerCase().startsWith("echo")) type="echo";
			if(type.toLowerCase().startsWith("radio")) type="radiology";
		} 
		LOG.log(Level.FINEST,"Type:"+type+" Prefix:"+prefix);
		LOG.log(Level.FINE,"SemEval2015 Training Path:"+SemEval2015TrainingPath);
		String original_textfile_name = SemEval2015TrainingPath+File.separator+type+
				File.separator+prefix+"."+SemEval2015Constants.SEMEVAL_TEXT_FILE_EXTENSION;
		String pipefilename = SemEval2015TrainingPath+File.separator+type+
				File.separator+prefix+"."+SemEval2015Constants.SEMEVAL_PIPED_EXTENSION;
		try {
			File ofile = new File(original_textfile_name);
			if(ofile.exists()) {
				String otext = FileUtils.readFileToString(ofile);
				semevalTextView.setDocumentText(otext);
			} else {
				LOG.log(Level.INFO,"Did not find training text file:"+ofile+", trying devel file formatting");
				ofile = new File(SemEval2015Constants.defaultDevelPath+File.separator+type+
						File.separator+prefix.split("\\.")[0]+"."+SemEval2015Constants.SEMEVAL_TEXT_FILE_EXTENSION);
				if(ofile.exists()){
					String otext = FileUtils.readFileToString(ofile);
					semevalTextView.setDocumentText(otext);
				} else {
					LOG.log(Level.WARNING,"Could not find expected devel text file:"+ofile.getPath());
				}
			}
			File pfile = new File(pipefilename);
			if(pfile.exists()) {
				String ptext = FileUtils.readFileToString(pfile);
				pipedView.setDocumentText(ptext);
			} else {
				//System.out.println("Could not find expected pipe file:"+pfile.getPath());
				pfile = new File(SemEval2015Constants.defaultDevelPath+File.separator+type+
						File.separator+prefix.split("\\.")[0]+"."+SemEval2015Constants.SEMEVAL_PIPED_EXTENSION);
				if(pfile.exists()) {
					String ptext = FileUtils.readFileToString(pfile);
					pipedView.setDocumentText(ptext);
				} else {
					LOG.log(Level.WARNING,"Could not find expected pipe file:"+pfile.getPath());
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}


	public static AnalysisEngineDescription createAnnotatorDescription(String training_path)
			throws ResourceInitializationException {

		return AnalysisEngineFactory.createEngineDescription(
				SemEval2015ViewCreatorAnnotator.class,
				SemEval2015ViewCreatorAnnotator.PARAM_TRAINING_PATH,
				training_path
				);
	}


}
