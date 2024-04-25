package org.kmouille.jobserver.service;

import static org.apache.commons.io.FileUtils.byteCountToDisplaySize;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.jobrunr.jobs.annotations.Job;
import org.jobrunr.jobs.context.JobContext;
import org.jobrunr.jobs.context.JobRunrDashboardLogger;
import org.kmouille.jobserver.config.JobConfiguration;
import org.slf4j.Logger;
import org.springframework.stereotype.Service;

import com.spire.pdf.FileFormat;
import com.spire.pdf.PdfDocument;
import com.spire.pdf.conversion.PdfStandardsConverter;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class IceBluePdfService {

	private static final Logger jobLogger = new JobRunrDashboardLogger(log);

	private final JobConfiguration jobConfiguration;

	@Job(name = "Convert %2 to PDFA", retries = 2)
	public void convertToPdfa(JobContext jobContext, String filePath, String fileName) throws IOException {
		var startMs = System.currentTimeMillis();

		var srcFile = new File(filePath, fileName);
		var fileSize = FileUtils.sizeOf(srcFile);
		jobLogger.info("Converting {} ({})", srcFile.getAbsolutePath(), byteCountToDisplaySize(fileSize));

		var destFolder = new File(jobConfiguration.getDestinationFolder(), "PDFA");
		var subFolder = new File(destFolder, jobContext.getJobId().toString());
		subFolder.mkdirs();

		var outFile = convertToPdfa(srcFile, subFolder);
		var outFileSize = FileUtils.sizeOf(outFile);
		jobLogger.info("Output file: {} ({})", outFile.getAbsolutePath(), byteCountToDisplaySize(outFileSize));
		var endMs = System.currentTimeMillis();
		jobLogger.info("Job duration: {}s", (endMs - startMs) / 1000.);
	}

	public File convertToPdfa(File srcFile, File outFolder) throws IOException, FileNotFoundException {
		var outFile = new File(outFolder, FilenameUtils.getBaseName(srcFile.getName()) + ".pdfa.iceblue.pdf");
		try (var is = new FileInputStream(srcFile);
				var os = new FileOutputStream(outFile)) {
			new PdfStandardsConverter(is).toPdfA3B(os);
		}
		return outFile;
	}

	@Job(name = "Convert %2 to DOCX", retries = 2)
	public void convertToDocx(JobContext jobContext, String filePath, String fileName) throws IOException {
		var startMs = System.currentTimeMillis();

		var srcFile = new File(filePath, fileName);
		var fileSize = FileUtils.sizeOf(srcFile);
		jobLogger.info("Converting {} ({})", srcFile.getAbsolutePath(), byteCountToDisplaySize(fileSize));

		var destFolder = new File(jobConfiguration.getDestinationFolder(), "DOCX");
		var subFolder = new File(destFolder, jobContext.getJobId().toString());
		subFolder.mkdirs();

		var outFile = convertToDocx(srcFile, subFolder);
		var endMs = System.currentTimeMillis();
		var outFileSize = FileUtils.sizeOf(outFile);
		jobLogger.info("Output file: {} ({})", outFile.getAbsolutePath(), byteCountToDisplaySize(outFileSize));
		jobLogger.info("Job duration: {}s", (endMs - startMs) / 1000.);
	}

	public File convertToDocx(File srcFile, File outFolder) throws IOException, FileNotFoundException {
		var outFile = new File(outFolder, FilenameUtils.getBaseName(srcFile.getName()) + ".iceblue.pdf.docx");
		try (var is = new FileInputStream(srcFile);
				var os = new FileOutputStream(outFile)) {
			var pdfDoc = new PdfDocument();
			pdfDoc.loadFromStream(is);
			pdfDoc.saveToStream(os, FileFormat.DOCX);
			pdfDoc.close();
		}
		return outFile;
	}

}
