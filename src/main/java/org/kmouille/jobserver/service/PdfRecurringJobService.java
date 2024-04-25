package org.kmouille.jobserver.service;

import java.io.File;
import java.util.Arrays;

import org.apache.commons.io.IOCase;
import org.apache.commons.io.filefilter.SuffixFileFilter;
import org.jobrunr.jobs.annotations.Job;
import org.jobrunr.jobs.annotations.Recurring;
import org.jobrunr.jobs.context.JobContext;
import org.jobrunr.jobs.context.JobDashboardProgressBar;
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

	@Recurring(id = "scan-folder-job-images", cron = "*/20 * * * *")
	@Job(name = "Convert pages of files to images")
	public void generateImages(JobContext jobContext) {
		var srcFolder = new File(jobConfiguration.getSourceFolder(), "toImages");
		srcFolder.mkdirs();
		var filePath = srcFolder.getAbsolutePath();
		var filenames = srcFolder.list(new SuffixFileFilter(".pdf", IOCase.INSENSITIVE));
		jobLogger.info("[IMAGES] Launching job for {}", Arrays.toString(filenames));
		for (var filename : filenames) {
			jobLogger.info("Launching job {}", new File(filePath, filename).getAbsolutePath());
			var enqueuedJobId = jobScheduler
					.enqueue(() -> {
						pdfService.generateImages(JobContext.Null, filePath, filename, 72);
					});
			jobLogger.info("[IMAGES] Job {} enqueued for {}", enqueuedJobId, filename);
		}
	}

	/**
	 * Example of a recurring job that doesn't trigger subjobs.
	 * 
	 * @param jobContext
	 *            injected by the JobScheduler
	 */
	@Recurring(id = "scan-folder-job-pdfa", cron = "*/5 * * * *")
	@Job(name = "Convert files to PDFA")
	public void convertToPdfa(JobContext jobContext) {
		var srcFolder = new File(jobConfiguration.getSourceFolder(), "toPdfa");
		srcFolder.mkdirs();
		var filePath = srcFolder.getAbsolutePath();
		var filenames = srcFolder.list(new SuffixFileFilter(".pdf", IOCase.INSENSITIVE));

		var destFolder = new File(jobConfiguration.getDestinationFolder(), "PDFA");
		var subFolder = new File(destFolder, jobContext.getJobId().toString());
		subFolder.mkdirs();

		var progressBar = jobContext.progressBar(filenames.length);
		jobLogger.info("[PDFA] Launching job for {} files", filenames.length);
		for (var filename : filenames) {
			var file = new File(filePath, filename);
			jobLogger.info("Converting {}", file.getAbsolutePath());
			try {
				iceBluePdfService.convertToPdfa(file, subFolder);
				increaseByOne(progressBar, true);
			} catch (Exception e) {
				// Exception cause I don't know the iceBlue types
				log.error("Conversion failed {}", filename, e);
				jobLogger.warn("Conversion failed {}: {}", file.getName(), e.getMessage());
				increaseByOne(progressBar, false);
			}
		}
	}

	private void increaseByOne(JobDashboardProgressBar progressBar, boolean succeeded) {
		progressBar.setProgress(progressBar.getTotalAmount(),
				progressBar.getSucceededAmount() + (succeeded ? 1 : 0),
				progressBar.getFailedAmount() + (succeeded ? 0 : 1));

	}

	@Recurring(id = "scan-folder-job-docx", cron = "*/15 * * * *")
	@Job(name = "Convert files to DOCX")
	public void convertToDocx(JobContext jobContext) {
		var srcFolder = new File(jobConfiguration.getSourceFolder(), "toDocx");
		srcFolder.mkdirs();
		var filePath = srcFolder.getAbsolutePath();
		var filenames = srcFolder.list(new SuffixFileFilter(".pdf", IOCase.INSENSITIVE));
		jobLogger.info("[DOCX] Launching job for {} files", filenames.length);
		for (var filename : filenames) {
			jobLogger.info("Launching job {}", new File(filePath, filename).getAbsolutePath());
			var enqueuedJobId = jobScheduler
					.enqueue(() -> {
						iceBluePdfService.convertToDocx(JobContext.Null, filePath, filename);
					});
			jobLogger.info("[DOCX] Job {} enqueued for {}", enqueuedJobId, filename);
		}
	}

}
