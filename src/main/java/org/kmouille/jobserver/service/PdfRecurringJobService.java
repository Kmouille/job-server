package org.kmouille.jobserver.service;

import java.io.File;
import java.util.Arrays;

import org.apache.commons.io.IOCase;
import org.apache.commons.io.filefilter.SuffixFileFilter;
import org.jobrunr.jobs.annotations.Recurring;
import org.jobrunr.jobs.context.JobContext;
import org.jobrunr.jobs.context.JobRunrDashboardLogger;
import org.jobrunr.scheduling.JobScheduler;
import org.kmouille.jobserver.config.JobConfiguration;
import org.slf4j.Logger;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class PdfRecurringJobService {

	private static final Logger jobLogger = new JobRunrDashboardLogger(log);

	private final JobScheduler jobScheduler;

	private final JobConfiguration jobConfiguration;

	private final PdfService pdfService;

	private final IceBluePdfService iceBluePdfService;

	@Recurring(id = "scan-folder-job-images", cron = "*/2 * * * *")
	public void generateImages(JobContext jobContext) {
		var srcFolder = new File(jobConfiguration.getSourceFolder(), "toImages");
		srcFolder.mkdir();
		var filePath = srcFolder.getAbsolutePath();
		var filenames = srcFolder.list(new SuffixFileFilter(".pdf", IOCase.INSENSITIVE));
		jobLogger.info("[IMAGES] Launching job for {}", Arrays.toString(filenames));
		for (var filename : filenames) {
			var file = new File(filePath, filename);
			var fileName = file.getName();
			jobLogger.info("Launching job {}/{}", filePath, fileName);
			var enqueuedJobId = jobScheduler
					.enqueue(() -> {
						pdfService.generateImages(JobContext.Null, filePath, fileName, 72);
					});
			jobLogger.info("[IMAGES] Job {} enqueued for {}", enqueuedJobId, fileName);
		}
	}

	@Recurring(id = "scan-folder-job-pdfa", cron = "*/3 * * * *")
	public void convertToPdfa(JobContext jobContext) {
		var srcFolder = new File(jobConfiguration.getSourceFolder(), "toPdfa");
		srcFolder.mkdir();
		var filePath = srcFolder.getAbsolutePath();
		var filenames = srcFolder.list(new SuffixFileFilter(".pdf", IOCase.INSENSITIVE));
		jobLogger.info("[PDFA] Launching job for {} files", filenames.length);
		for (var filename : filenames) {
			var file = new File(filePath, filename);
			var fileName = file.getName();
			jobLogger.info("Launching job {}/{}", filePath, fileName);
			var enqueuedJobId = jobScheduler
					.enqueue(() -> {
						iceBluePdfService.convertToPdfa(JobContext.Null, filePath, fileName);
					});
			jobLogger.info("[PDFA] Job {} enqueued for {}", enqueuedJobId, fileName);
		}
	}

}
