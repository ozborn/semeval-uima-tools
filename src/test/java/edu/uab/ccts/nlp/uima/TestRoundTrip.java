package edu.uab.ccts.nlp.uima;

import java.io.File;
import java.util.Collection;

import org.apache.commons.io.FileUtils;
import org.apache.uima.collection.CollectionReaderDescription;
import org.apache.uima.fit.factory.AggregateBuilder;
import org.apache.uima.fit.factory.CollectionReaderFactory;
import org.apache.uima.fit.pipeline.SimplePipeline;
import org.apache.uima.jcas.JCas;
import org.apache.uima.resource.ResourceInitializationException;

import edu.uab.ccts.nlp.shared_task.semeval2015.SemEval2015Constants;
import edu.uab.ccts.nlp.shared_task.semeval2015.uima.SemEval2015CollectionReader;
import edu.uab.ccts.nlp.shared_task.semeval2015.uima.annotator.SemEval2015GoldAttributeParserAnnotator;
import edu.uab.ccts.nlp.shared_task.semeval2015.uima.annotator.SemEval2015Task2Consumer;

import static org.junit.Assume.*;
import org.junit.Test;

public class TestRoundTrip {
	
	@Test 
	public void testRoundTrip() throws ResourceInitializationException{
		assumeTrue((new File(SemEval2015Constants.defaultDevelPath)).isDirectory());
		runRoundTrip(SemEval2015Constants.defaultDevelPath,"target/test/roundtrip/devel");
		//runRoundTrip(SemEval2015Constants.defaultTrainingPath,"target/test/roundtrip/train");
		//runRoundTrip(SemEval2015Constants.updatedTrainingPath,"target/test/roundtrip/updated_train");
	}
	
	
	public boolean runRoundTrip(String input_dir, String output_dir) throws ResourceInitializationException{
		
		boolean passed=false;
		String textfilepostfix[] = {SemEval2015Constants.SEMEVAL_PIPED_EXTENSION};
		Collection<File> trainFiles = FileUtils.listFiles(
				new File(input_dir),
				textfilepostfix, true);
		
		CollectionReaderDescription crd = CollectionReaderFactory.createReaderDescription(
				SemEval2015CollectionReader.class,
				SemEval2015CollectionReader.PARAM_FILES,trainFiles
			);
		
		AggregateBuilder builder = new AggregateBuilder();
		builder.add(SemEval2015GoldAttributeParserAnnotator.getTestingDescription());
		builder.add(SemEval2015Task2Consumer.getDescription(output_dir));
		for (@SuppressWarnings("unused") JCas jcas : SimplePipeline.iteratePipeline(crd, builder.createAggregateDescription())) {}
		return passed;
	}
	

}
