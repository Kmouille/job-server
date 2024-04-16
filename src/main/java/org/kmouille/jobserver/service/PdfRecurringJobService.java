package org.kmouille.jobserver.service;

import java.io.File;
import java.util.Arrays;

import org.jobrunr.jobs.annotations.Job;
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

	@Job(name = "Scan Folder Job")
	// @Recurring(id = "scan-folder-job", cron = "* * * * *")
	public void myRecurringMethod(JobContext jobContext) {
		var srcFolder = jobConfiguration.getSourceFolder();
		var filePath = srcFolder.getAbsolutePath();
		var filenames = srcFolder.list();
		jobLogger.info("Launching job for {}", Arrays.toString(filenames));
		for (var filename : filenames) {
			var file = new File(filePath, filename);
			var fileName = file.getName();
			jobLogger.info("Launching job {}/{}", filePath, fileName);
			var enqueuedJobId = jobScheduler
					.enqueue(() -> {
						pdfService.generateImages(JobContext.Null, filePath, fileName, 72);
					});
			jobLogger.info("Job {} enqueued for {}", enqueuedJobId, fileName);
		}
	}

}
