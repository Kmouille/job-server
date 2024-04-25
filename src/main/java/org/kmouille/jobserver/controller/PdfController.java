package org.kmouille.jobserver.controller;

import java.io.File;
import java.io.IOException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

import org.jobrunr.jobs.context.JobContext;
import org.jobrunr.scheduling.JobScheduler;
import org.jobrunr.storage.StorageProvider;
import org.kmouille.jobserver.service.IceBluePdfService;
import org.kmouille.jobserver.service.PdfService;
import org.springframework.http.MediaType;
import org.springframework.lang.NonNull;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping(path = "/pdf")
@RequiredArgsConstructor
public class PdfController {

	private final PdfService pdfService;
	private final IceBluePdfService iceBluePdfService;

	private final StorageProvider storageProvider;

	private final JobScheduler jobScheduler;

	@GetMapping(value = "metadata", produces = MediaType.APPLICATION_JSON_VALUE)
	public void getMetadata(@RequestParam(name = "path") String path) throws IOException {
		pdfService.printMetadata(new File(URLDecoder.decode(path, StandardCharsets.UTF_8)));
	}

	@GetMapping(value = "generateImage", produces = MediaType.APPLICATION_JSON_VALUE)
	public String generateImage(@RequestParam("path") @NonNull String path,
			@RequestParam(value = "dpi", defaultValue = "72") int dpi) {
		var file = new File(URLDecoder.decode(path, StandardCharsets.UTF_8));
		var filePath = file.getParent();
		var fileName = file.getName();
		// [JobRunr] Best practice to prepare the simplest lambda
		// Separating file name from location for easy job details rendering
		var enqueuedJobId = jobScheduler
				.enqueue(() -> pdfService.generateImages(JobContext.Null, filePath, fileName, dpi));
		return enqueuedJobId.toString();
	}

	@GetMapping(value = "pdfa", produces = MediaType.APPLICATION_JSON_VALUE)
	public String convertToPdfa(@RequestParam(name = "path") String path) throws IOException {
		var file = new File(URLDecoder.decode(path, StandardCharsets.UTF_8));
		var filePath = file.getParent();
		var fileName = file.getName();
		// [JobRunr] Best practice to prepare the simplest lambda
		// Separating file name from location for easy job details rendering
		var enqueuedJobId = jobScheduler
				.enqueue(() -> iceBluePdfService.convertToPdfa(JobContext.Null, filePath, fileName));
		return enqueuedJobId.toString();
	}

	/**
	 * Polling request to get job status.
	 * 
	 * @param jobId
	 * @return status of the job
	 * @throws IOException
	 */
	@GetMapping("/status/{jobId}")
	public String pollJob(@PathVariable String jobId) throws IOException {
		var jobById = storageProvider.getJobById(UUID.fromString(jobId));
		return jobById.getState().name();
	}

}
