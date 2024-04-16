package org.kmouille.jobserver.service;

import static java.lang.String.format;
import static org.apache.commons.lang3.StringUtils.leftPad;
import static org.apache.commons.lang3.StringUtils.rightPad;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Calendar;
import java.util.stream.Collectors;

import org.apache.pdfbox.pdmodel.PDDocument;

public class PdfMetadataMapper {

	public static String pdfFileInfoToString(PDDocument pdfDoc) {

		var docInfo = pdfDoc.getDocumentInformation();
		var creationDate = toLocalDateTime(docInfo.getCreationDate());
		var modificationDate = toLocalDateTime(docInfo.getModificationDate());
		return ""
				+ "CreationDate:     " + creationDate + "\n"
				+ "Creator:          " + docInfo.getCreator() + "\n"
				+ "ModificationDate: " + modificationDate + "\n"
				+ "Producer:         " + docInfo.getProducer() + "\n"
				+ "Title:            " + docInfo.getTitle() + "\n"
				+ "Subject:          " + docInfo.getSubject() + "\n"
				+ "Author:           " + docInfo.getAuthor() + "\n"
				+ "MetadataKeys:     " + docInfo.getMetadataKeys() + "\n"
				+ "Pages:            " + pdfDoc.getNumberOfPages();
	}

	public static String signatureInfoToString(PDDocument pdfDoc) {
		return pdfDoc.getSignatureFields()
				.stream().map(signatureField -> {
					var pdAnnotationWidget = signatureField.getWidgets().get(0);
					var pdPage = pdAnnotationWidget.getPage();
					int pageNum = pdfDoc.getPages().indexOf(pdPage);

					var signatureZoneRect = pdAnnotationWidget.getRectangle();
					var signatureBBox = pdPage.getBBox();

					var signed = signatureField.getSignature() != null;
					var lowerLeftX = signatureZoneRect.getLowerLeftX();
					var lowerLeftY = signatureBBox.getHeight()
							- signatureZoneRect.getLowerLeftY() - signatureZoneRect.getHeight();

					return ""
							+ (signed ? "[V] " : "[X] ")
							+ "Field: " + rightPad(signatureField.getFullyQualifiedName(), 20) + " "
							+ "position(" + format("%.2f", lowerLeftX) + ";" + format("%.2f", lowerLeftY) + ") "
							+ "size(" + format("%.2f", signatureZoneRect.getWidth()) + ";"
							+ format("%.2f", signatureZoneRect.getHeight()) + ") "
							+ "Page #" + leftPad("" + pageNum, 3, '0') + " "
							+ "(rot " + pdPage.getRotation() + " "
							+ "size " + signatureBBox + ") ";
				}).collect(Collectors.joining("\n"));
	}

	static LocalDateTime toLocalDateTime(Calendar calendar) {
		return calendar != null ? LocalDateTime.ofInstant(calendar.toInstant(), ZoneId.of("UTC")) : null;
	}
}
