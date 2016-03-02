package edu.uab.ccts.nlp.shared_task.semeval2015.uima;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.HiddenFileFilter;
import org.apache.ctakes.typesystem.type.structured.DocumentID;
import org.apache.uima.UimaContext;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.cas.CASException;
import org.apache.uima.collection.CollectionException;
import org.apache.uima.jcas.JCas;
import org.apache.uima.resource.ResourceInitializationException;
import org.apache.uima.util.Progress;
import org.apache.uima.util.ProgressImpl;
import org.cleartk.util.ViewUriUtil;

import edu.uab.ccts.nlp.shared_task.semeval2015.SemEval2015Constants;

import org.apache.uima.fit.component.JCasCollectionReader_ImplBase;
import org.apache.uima.fit.component.ViewCreatorAnnotator;
import org.apache.uima.fit.descriptor.ConfigurationParameter;

public class SemEval2015CorpusCollectionReader extends
JCasCollectionReader_ImplBase {

	public static final String TEXT_SUFFIX = "txt";
	public static final String PARAM_FILES = "fileroot";
	public static String unsupervised_corpus_root_path = 
			"/home/ozborn/semeval-2014-unlabeled-mimic-notes.v1";
	@ConfigurationParameter(
			name = PARAM_FILES,
			description = "points to a semeval-2014-unlabeled-mimic-notes.v1")
	protected String fileroot;

	protected List<File> textFiles = new ArrayList<>();
	protected int totalFiles = 0;

	public static void collectFiles(File directory, Collection<File> files) throws IOException
	{
		File[] dirFiles = directory.listFiles((FileFilter) HiddenFileFilter.VISIBLE);
		for (File f : dirFiles)
		{
			if (f.isDirectory())
			{
				collectFiles(f, files);
			} else if (f.getPath().endsWith(TEXT_SUFFIX))
			{
				files.add(f);
			}
		}
	}

	public void initialize(UimaContext context) throws ResourceInitializationException
	{
		String[] trainExtension = {SemEval2015CorpusCollectionReader.TEXT_SUFFIX};
		Collection<File> files = FileUtils.listFiles(
				new File(fileroot),
				trainExtension, true);

		for (File f : files)
		{
			File textFile = new File(f.getPath());
			if (textFile.exists())
			{
				textFiles.add(textFile);
			}
		}
		totalFiles = textFiles.size();
	}

	public void getNext(JCas jCas) throws IOException, CollectionException
	{
		try {
			File textFile = textFiles.remove(0);
			String fileText = FileUtils.readFileToString(textFile);
			String first_line = fileText.split("\n")[0];
			String thedocid = textFile.getName().replaceAll("."+TEXT_SUFFIX, "")+"-"+first_line.split("\t")[6];
			setDocumentID(jCas, thedocid, SemEval2015Constants.APP_VIEW);
			setDocumentID(jCas, thedocid, jCas.getViewName());
			jCas.setDocumentText(fileText);
			ViewUriUtil.setURI(jCas, textFile.toURI());
		} catch (Exception e) {
			throw(new CollectionException(e));
		}
	}

	public boolean hasNext() throws IOException, CollectionException
	{
		return (textFiles.size() > 0);
	}

	public Progress[] getProgress()
	{
		return new Progress[]{
				new ProgressImpl(totalFiles - textFiles.size(),
						totalFiles,
						Progress.ENTITIES)};
	}

	
	void setDocumentID(JCas jcasToSet, String docid, String viewname) throws
	AnalysisEngineProcessException, CASException{
		DocumentID clear_docid = new DocumentID(jcasToSet);
		clear_docid.setDocumentID(docid);
		ViewCreatorAnnotator.createViewSafely(jcasToSet, viewname);
		JCas theview = jcasToSet.getView(viewname);
		clear_docid.addToIndexes(theview);
	}

}
