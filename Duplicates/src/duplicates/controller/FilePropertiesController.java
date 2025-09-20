package duplicates.controller;

import com.drew.imaging.ImageMetadataReader;
import com.drew.metadata.Metadata;
import com.drew.metadata.Directory;
import com.drew.metadata.Tag;
import com.drew.metadata.exif.GpsDirectory;

import java.io.File;
import java.util.*;

public class FilePropertiesController {

    public static Map<String, String> getImageMetadata(File file) {
        Map<String, String> props = new LinkedHashMap<>();
        try {
            Metadata metadata = ImageMetadataReader.readMetadata(file);

            // --- Basisdaten ---
            props.put("Dateiname", file.getName());
            props.put("Pfad", file.getAbsolutePath());
            props.put("Größe (Byte)", String.valueOf(file.length()));

            // --- Alle Metadaten iterieren ---
            for (Directory dir : metadata.getDirectories()) {
                for (Tag tag : dir.getTags()) {
                    String key = dir.getName() + " - " + tag.getTagName();
                    props.put(key, tag.getDescription());
                }
            }

            // --- GPS separat ---
            GpsDirectory gpsDir = metadata.getFirstDirectoryOfType(GpsDirectory.class);
            if (gpsDir != null && gpsDir.getGeoLocation() != null) {
                props.put("GPS Latitude", String.valueOf(gpsDir.getGeoLocation().getLatitude()));
                props.put("GPS Longitude", String.valueOf(gpsDir.getGeoLocation().getLongitude()));
            } else {
                props.put("GPS Latitude", "–");
                props.put("GPS Longitude", "–");
            }

        } catch (Exception e) {
            props.put("Fehler beim Lesen der Metadaten", e.getMessage());
        }
        return props;
    }
}
