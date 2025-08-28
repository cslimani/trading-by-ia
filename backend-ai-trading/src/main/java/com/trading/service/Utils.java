package com.trading.service;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

public class Utils {

	public static void backupFile(String sourcePath, String targetFolder) {
		File source = new File(sourcePath);

		if (!source.exists()) {
			System.out.println("File does not exist " + sourcePath);
			return;
		}

		// Formatter la date et l'heure avec LocalDateTime
		DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");
		String horodatage = LocalDateTime.now().format(formatter);

		// Extraire nom et extension
		String fileName = source.getName();
		int pointIndex = fileName.lastIndexOf(".");
		String nomSansExt = (pointIndex > 0) ? fileName.substring(0, pointIndex) : fileName;
		String extension = (pointIndex > 0) ? fileName.substring(pointIndex) : "";

		// Nouveau nom avec horodatage
		String newName = nomSansExt + "_" + horodatage + extension;

		// Créer le chemin du fichier de destination
		File target = new File(targetFolder + File.separator + newName);

		try {
			Files.copy(source.toPath(), target.toPath(), StandardCopyOption.REPLACE_EXISTING);
			Files.deleteIfExists(source.toPath());
			System.out.println("Fichier copié et renommé : " + target.getAbsolutePath());
		} catch (IOException e) {
			System.out.println("Erreur lors de la copie : " + e.getMessage());
		}
	}

	public static Double getTimestamp(LocalDateTime date) {
		return Double.valueOf(date.atZone(ZoneId.systemDefault())
                .toInstant()                
                .getEpochSecond());
	}
}
