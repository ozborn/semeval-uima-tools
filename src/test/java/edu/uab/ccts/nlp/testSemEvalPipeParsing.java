package edu.uab.ccts.nlp;

import static org.junit.Assume.*;

import java.io.File;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.junit.Test;

import edu.uab.ccts.nlp.shared_task.semeval2015.SemEval2015Constants;

public class testSemEvalPipeParsing {

	@Test
	public void test() {
		assumeTrue((new File(SemEval2015Constants.defaultDevelPath)).isDirectory());
		String testPath = SemEval2015Constants.defaultDevelPath+File.separator+"discharge/18541-017142.pipe";
		File testFile = new File(testPath);
		assumeTrue(testFile.exists());
		String ptext = null;
		String regex = "(.)*\\d\\|[Cc][Uu][Ii]-less\\|(.)*";
		try (Stream<String> stream = Files.lines(Paths.get(testPath),Charset.forName("UTF-8"))) {
			ptext = stream
			.filter(line -> line.matches(regex))
			.distinct()
			.collect(Collectors.joining("\n"))
			;
		} catch (Exception e) { System.out.println("Failure!");e.printStackTrace(); }
		int linesFound = ptext.split("\n").length;
		assumeTrue(linesFound==24);
		System.out.println(testPath+" produced expected "+linesFound+" non-duplicate CUI-less lines.");
		System.out.println();
	}

}
