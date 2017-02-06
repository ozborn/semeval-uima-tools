package edu.uab.ccts.nlp.shared_task.semeval2015.uima.annotator;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.stream.Collectors;
import java.util.stream.Stream;

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
import org.apache.uima.fit.descriptor.ConfigurationParameter;

import edu.uab.ccts.nlp.shared_task.semeval2015.SemEval2015Constants;


/**
 * This file reads in piped annotation SemEval 2015 files into the PIPED_VIEW 
 * and the text into default/GOLD_VIEW
 * @author ozborn
 *
 */
public class SemEval2015ViewCreatorAnnotator extends JCasAnnotator_ImplBase {


	public static final String PARAM_TRAINING_PATH = "FilesForViewPath";
	@ConfigurationParameter(
			name = PARAM_TRAINING_PATH,
			description="Path of SemEval 2015 files for which view will be created for ",
			mandatory=true
	)
	private String filesForViewPath; 
	
	public static final String PARAM_CUILESS_ONLY = "cuilessOnly";
	@ConfigurationParameter(
			name = PARAM_CUILESS_ONLY,
			description="True if only CUI-less disorders should be read into views ",
			mandatory=false,
			defaultValue="false"
	)
	private boolean cuilessOnly; 

	
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
		LOG.log(Level.FINER,"Type:"+type+" Prefix:"+prefix);
		
		
		LOG.log(Level.FINER,"SemEval2015 File Path:"+filesForViewPath);
		if(filesForViewPath==null) {
			LOG.log(Level.SEVERE,"Null value for filesForViewPath");
			throw new AnalysisEngineProcessException();
		}
		
		String original_textfile_name = filesForViewPath+File.separator+type+
				File.separator+prefix+"."+SemEval2015Constants.SEMEVAL_TEXT_FILE_EXTENSION;
		String pipefilename = filesForViewPath+File.separator+type+
				File.separator+prefix+"."+SemEval2015Constants.SEMEVAL_PIPED_EXTENSION;
		try {
			
			//Read in Text Files, Assume Training Files First
			File ofile = new File(original_textfile_name);
			if(ofile.exists()) {
				String otext = FileUtils.readFileToString(ofile);
				semevalTextView.setDocumentText(otext);
			} else {
				File dfile = new File(filesForViewPath+ File.separator+prefix.split("\\.")[0]+"."+
				SemEval2015Constants.SEMEVAL_TEXT_FILE_EXTENSION);
				LOG.log(Level.FINER,"Did not find training text file:"+ofile+", trying devel file "+dfile.toString());
				if(dfile.exists()){
					String otext = FileUtils.readFileToString(dfile);
					semevalTextView.setDocumentText(otext);
				} else {
					LOG.log(Level.WARNING,"Could not find expected devel text file:"+dfile.getPath());
				}
			}
			
			
			//Read in Piped Files, Assume Training Files First
			File pfile = new File(pipefilename);
			if(pfile.exists()) {
				pipedView.setDocumentText(readSemEvalFile(pipefilename,cuilessOnly));
			} else {
				pipefilename= SemEval2015Constants.defaultDevelPath+File.separator+type+
				File.separator+prefix.split("\\.")[0]+"."+SemEval2015Constants.SEMEVAL_PIPED_EXTENSION;
				LOG.log(Level.FINE,"Did not find training pipe file:"+pfile+", trying devel pipe file "+pipefilename);
				pfile = new File(pipefilename);
				if(pfile.exists()) {
					pipedView.setDocumentText(readSemEvalFile(pipefilename,cuilessOnly));
				} else {
					LOG.log(Level.WARNING,"Could not find expected pipe file:"+pfile.getPath());
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}


	/**
	 * @param pipedFilename
	 * @throws AnalysisEngineProcessException
	 */
	private String readSemEvalFile(String pipedFilename, boolean cuilessonly) throws AnalysisEngineProcessException {
		String ptext = null;
		try (Stream<String> stream = Files.lines(Paths.get(pipedFilename),Charset.forName("UTF-8"))) {
			if(cuilessonly) { 
					LOG.log(Level.FINE,"Doing CUILESS only");
					ptext = stream
					.filter(line -> line.indexOf("|CUI-less")!=-1)
					.collect(Collectors.joining("\n"));
			} else {
				ptext = stream.collect(Collectors.joining("\n"));
			}
			LOG.log(Level.FINE,"SemEval2015 file text is:"+ptext);
		} catch (IOException ioe) { ioe.printStackTrace(); throw new AnalysisEngineProcessException(ioe);}
		return ptext;
	}


	public static AnalysisEngineDescription createAnnotatorDescription(String thepath)
		throws ResourceInitializationException {
			return AnalysisEngineFactory.createEngineDescription(
				SemEval2015ViewCreatorAnnotator.class,
				SemEval2015ViewCreatorAnnotator.PARAM_TRAINING_PATH,
				thepath
			);
	}

	
	
	/**
	 * 
	 * @param thepath Path of SemEval 2015 data files
	 * @param createOnlyCuiless true if only CUI-less annotations should be read in
	 * @return
	 * @throws ResourceInitializationException
	 */
	public static AnalysisEngineDescription createDescription(String thepath,
			boolean createOnlyCuiless)
		throws ResourceInitializationException {
			return AnalysisEngineFactory.createEngineDescription(
				SemEval2015ViewCreatorAnnotator.class,
				SemEval2015ViewCreatorAnnotator.PARAM_TRAINING_PATH,
				thepath,
				SemEval2015ViewCreatorAnnotator.PARAM_CUILESS_ONLY,
				createOnlyCuiless
			);
	}


}
