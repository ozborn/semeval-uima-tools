package edu.uab.ccts.nlp.uima.collection_reader;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.HiddenFileFilter;
import org.apache.uima.UimaContext;
import org.apache.uima.cas.CASException;
import org.apache.uima.collection.CollectionException;
import org.apache.uima.jcas.JCas;
import org.apache.uima.resource.ResourceInitializationException;
import org.apache.uima.util.Progress;
import org.apache.uima.util.ProgressImpl;
import org.cleartk.util.ViewUriUtil;
import org.apache.uima.fit.component.JCasCollectionReader_ImplBase;
import org.apache.uima.fit.descriptor.ConfigurationParameter;

import edu.uab.ccts.nlp.shared_task.SemEval2015Constants;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Based almost entirely on ClearClinical by James Gung, updated for use with ClearClinical 2.0
 * @author jgung, ozborn
 *
 */
public class SemEval2015CollectionReader extends JCasCollectionReader_ImplBase
{


	public static final String PARAM_FILES = "files";
	@ConfigurationParameter(
			name = PARAM_FILES,
			description = "points to a semeval-2014-task-7 data directory")
	protected Collection<File> files;
	
	/*
	 * 	@ConfigurationParameter(
			name = PARAM_DIR,
			description = "points to a semeval-2015-task-14 data dir",
			defaultValue="/Users/ozborn/Dropbox/Public_NLP_Data/semeval-2015-task-14_updated/data/devel")
	//Can not get this working with files as a input parameter
	String inputdir;
	 */
	

	protected List<File> pipedFiles = new ArrayList<>();
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
			} else if (f.getPath().endsWith(SemEval2015Constants.SEMEVAL_PIPED_EXTENSION))
			{
				files.add(f);
			}
		}
	}

	public void initialize(UimaContext context) throws ResourceInitializationException
	{
		for (File f : files)
		{
			String path = f.getPath().replace(SemEval2015Constants.SEMEVAL_PIPED_EXTENSION, 
					SemEval2015Constants.SEMEVAL_TEXT_FILE_EXTENSION);
			File textFile = new File(path);
			if (textFile.exists())
			{
				pipedFiles.add(f);
				textFiles.add(textFile);
			}
		}
		totalFiles = pipedFiles.size();
	}

	public void getNext(JCas jCas) throws IOException, CollectionException
	{
		JCas pipedView,appView;
		try
		{
			pipedView = jCas.createView(SemEval2015Constants.PIPED_VIEW);
			appView = jCas.createView(SemEval2015Constants.APP_VIEW);
		} catch (CASException ce)
		{
			throw new CollectionException(ce);
		}

		File pipeFile = pipedFiles.remove(0);
		String annotations = FileUtils.readFileToString(pipeFile);
		File textFile = textFiles.remove(0);
		String fileText = FileUtils.readFileToString(textFile);

		jCas.setDocumentText(fileText);
		appView.setDocumentText(fileText);
		ViewUriUtil.setURI(jCas, textFile.toURI());
		pipedView.setDocumentText(annotations);
	}

	public boolean hasNext() throws IOException, CollectionException
	{
		return (pipedFiles.size() > 0);
	}

	public Progress[] getProgress()
	{
		return new Progress[]{
				new ProgressImpl(totalFiles - pipedFiles.size(),
						totalFiles,
						Progress.ENTITIES)};
	}

}
