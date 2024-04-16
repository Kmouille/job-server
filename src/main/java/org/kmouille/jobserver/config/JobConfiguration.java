package org.kmouille.jobserver.config;

import java.io.File;
import java.nio.file.Paths;

import javax.sql.DataSource;

import org.h2.jdbcx.JdbcDataSource;
import org.jobrunr.jobs.mappers.JobMapper;
import org.jobrunr.storage.StorageProvider;
import org.jobrunr.storage.sql.common.SqlStorageProviderFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class JobConfiguration {

	@Value("${image.destination.folder:/tmp/JobRunr/out}")
	private File destinationFolder;

	@Value("${pdf.source.folder:/tmp/JobRunr/in}")
	private File sourceFolder;

	@Bean
	public StorageProvider storageProvider(JobMapper jobMapper, DataSource dataSource) {
		// var storageProvider = new InMemoryStorageProvider();
		var storageProvider = SqlStorageProviderFactory.using(dataSource);
		storageProvider.setJobMapper(jobMapper);
		return storageProvider;
	}

	@Bean
	public DataSource dataSource() {
		var ds = new JdbcDataSource();
		ds.setURL("jdbc:h2:" + Paths.get(("C:/tmp/JobRunr/h2"), "JobRunr"));
		ds.setUser("sa");
		ds.setPassword("sa");
		return ds;
	}

	public File getDestinationFolder() {
		return destinationFolder;
	}

	public void setDestinationFolder(File destinationFolder) {
		this.destinationFolder = destinationFolder;
	}

	public File getSourceFolder() {
		return sourceFolder;
	}

	public void setSourceFolder(File sourceFolder) {
		this.sourceFolder = sourceFolder;
	}

}
