package org.kmouille.jobserver.service;

import static org.kmouille.jobserver.service.PdfMetadataMapper.pdfFileInfoToString;
import static org.kmouille.jobserver.service.PdfMetadataMapper.signatureInfoToString;

import java.io.File;
import java.io.IOException;

import javax.imageio.ImageIO;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.rendering.ImageType;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.jobrunr.jobs.annotations.Job;
import org.jobrunr.jobs.context.JobContext;
import org.jobrunr.jobs.context.JobRunrDashboardLogger;
import org.kmouille.jobserver.config.JobConfiguration;
import org.slf4j.Logger;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class PdfService {

	private static final Logger jobLogger = new JobRunrDashboardLogger(log);

	private final JobConfiguration jobConfiguration;

	public void printMetadata(File file) throws IOException {
		try (var pdfDoc = Loader.loadPDF(file)) {
			var infoString = pdfFileInfoToString(pdfDoc);
			var signatureInfoString = signatureInfoToString(pdfDoc);
			log.info("File info:\n{}", infoString);
			log.info("Signature info:\n{}", signatureInfoString);
		}
	}

	@Job(name = "Generate %3dpi images for %2", retries = 2)
	public void generateImages(JobContext jobContext, String filePath, String fileName, int dpi) throws IOException {
		var startMs = System.currentTimeMillis();
		var srcFile = new File(filePath, fileName);
		var srcFileBasename = FilenameUtils.getBaseName(fileName);

		var fileSize = FileUtils.sizeOf(srcFile);
		jobLogger.info("Rendering {} ({})", srcFile.getAbsolutePath(), FileUtils.byteCountToDisplaySize(fileSize));

		var destFolder = new File(jobConfiguration.getDestinationFolder(), "IMG");
		var subFolder = new File(new File(destFolder, jobContext.getJobId().toString()), srcFileBasename);
		subFolder.mkdirs();

		try (var pdfDoc = Loader.loadPDF(srcFile)) {
			jobLogger.info("PDF info:\n{}", pdfFileInfoToString(pdfDoc));
			var pdfRenderer = new PDFRenderer(pdfDoc);
			var numPages = pdfDoc.getNumberOfPages();
			jobLogger.info("Found {} pages for {}", numPages, srcFile.getAbsolutePath());
			var progressBar = jobContext.progressBar(numPages);
			for (int page = 0; page < numPages; ++page) {
				var outputFile = new File(subFolder, srcFileBasename + "-" + (page + 1) + ".png");
				jobLogger.trace("Rendering page {}/{}: {}", page, numPages, outputFile.getName());

				var bufferedImg = pdfRenderer.renderImageWithDPI(page, dpi, ImageType.RGB);
				ImageIO.write(bufferedImg, "PNG", outputFile);

				progressBar.increaseByOne();
				if (page % 10 == 0) {
					jobLogger.info("{} pages rendered so far...", page);
				}

				if (numPages == 1) {
					jobLogger.info("Output image file: {} ({})",
							outputFile.getAbsolutePath(),
							FileUtils.byteCountToDisplaySize(FileUtils.sizeOf(outputFile)));
				}
			}
			var endMs = System.currentTimeMillis();
			jobLogger.info("Job duration: {}s", (endMs - startMs) / 1000.);
		}
	}

}
