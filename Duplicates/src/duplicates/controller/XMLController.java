package duplicates.controller;

import duplicates.model.DuplicateSearchOptionsModel;
import duplicates.model.FileSearchModel;
import duplicates.model.FileSearchOptionsModel;

import javax.xml.stream.*;
import java.io.*;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.function.Consumer;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import javax.xml.parsers.*;
import javax.xml.transform.*;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.File;
import java.time.LocalDate;

/**
 * XMLController: Settings (bestehend) + Streaming von Dateisuche-Ergebnissen (neu).
 */
public class XMLController {

    // --- bestehende Settings-Pfade ---
    private static String dsSettingsPath = "settings/DSsettings.xml";
    private static String fsSettingsPath = "settings/FSsettings.xml";

    // --- neue Pfade/Handles für Ergebnis-Streaming ---
    private static XMLStreamWriter RESULTS_WRITER;
    private static File RESULTS_FILE;

    private static final DateTimeFormatter TS_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");

    // ======== NEU: Ergebnisse als XML streamen (speicherschonend) ========

    /**
     * Beginnt eine neue Ergebnisdatei.
     * Schreibt Header + <FileSearchResults><Meta>…</Meta><Files>
     */
    public static synchronized void beginSearchResults(String resultFilePath,
                                                       FileSearchOptionsModel options) {
        try {
            RESULTS_FILE = new File(resultFilePath);
            File parent = RESULTS_FILE.getParentFile();
            if (parent != null) parent.mkdirs();

            OutputStream os = new BufferedOutputStream(new FileOutputStream(RESULTS_FILE, false));
            XMLOutputFactory of = XMLOutputFactory.newInstance();
            RESULTS_WRITER = of.createXMLStreamWriter(os, "UTF-8");

            RESULTS_WRITER.writeStartDocument("UTF-8", "1.0");
            RESULTS_WRITER.writeCharacters("\n");
            RESULTS_WRITER.writeStartElement("FileSearchResults");
            RESULTS_WRITER.writeAttribute("timestamp", LocalDateTime.now().format(TS_FMT));

            // Meta-Block (optional hilfreich zum Reproduzieren)
            RESULTS_WRITER.writeCharacters("\n  ");
            RESULTS_WRITER.writeStartElement("Meta");
            writeAttr("MinFileSizeMB", String.valueOf(options.getMinFileSize()));
            writeAttr("MaxFileSizeMB", String.valueOf(options.getMaxFileSize()));
            writeAttr("FileExtensions", safe(options.getFileExtention()));
            writeAttr("FileNameContains", safe(options.getFileNameString()));
            writeAttr("CreationDateOperator", safe(options.getFileCreationDateOperator()));
            writeAttr("CreationDate", options.getCreationDate() != null ? options.getCreationDate().toString() : "");
            writeAttr("ModificationDateOperator", safe(options.getFileModificationDateOperator()));
            writeAttr("ModificationDate", options.getModificationDate() != null ? options.getModificationDate().toString() : "");
            writeAttr("IncludeSubfolders", String.valueOf(options.isSubFolderBoo()));
            RESULTS_WRITER.writeEndElement(); // </Meta>

            // Dateien-Container
            RESULTS_WRITER.writeCharacters("\n  ");
            RESULTS_WRITER.writeStartElement("Files");
            RESULTS_WRITER.writeCharacters("\n");
            RESULTS_WRITER.flush();
        } catch (Exception e) {
            closeQuietly();
            throw new RuntimeException("beginSearchResults fehlgeschlagen: " + e.getMessage(), e);
        }
    }

    /**
     * Hängt einen Treffer an. Sehr leichtgewichtig (keine DOM-Bäume).
     */
    public static synchronized void appendSearchResult(FileSearchModel m) {
        if (RESULTS_WRITER == null) return;
        try {
            java.io.File f = m.getFile();
            String fullPath = f.getAbsolutePath();

            RESULTS_WRITER.writeCharacters("    ");
            RESULTS_WRITER.writeStartElement("File");
            writeAttr("fullPath", fullPath);
            writeAttr("name", m.getFileName());
            writeAttr("parent", f.getParent() != null ? f.getParent() : "");
            writeAttr("sizeBytes", String.valueOf(m.getFileSizeBytes()));
            writeAttr("type", safe(m.getDisplayType()));
            writeAttr("created", m.getCreationDate() != null ? m.getCreationDate().toString() : "");
            writeAttr("modified", m.getModificationDate() != null ? m.getModificationDate().toString() : "");
            writeAttr("hidden", String.valueOf(f.isHidden()));
            writeAttr("readOnly", String.valueOf(f.isFile() && !f.canWrite()));
            RESULTS_WRITER.writeEndElement(); // </File>
            RESULTS_WRITER.writeCharacters("\n");
        } catch (Exception e) {
            throw new RuntimeException("appendSearchResult fehlgeschlagen: " + e.getMessage(), e);
        }
    }

    /**
     * Schließt die Ergebnisdatei (</Files></FileSearchResults> + XML-Ende).
     */
    public static synchronized void endSearchResults() {
        if (RESULTS_WRITER == null) return;
        try {
            RESULTS_WRITER.writeCharacters("  ");
            RESULTS_WRITER.writeEndElement(); // </Files>
            RESULTS_WRITER.writeCharacters("\n");
            RESULTS_WRITER.writeEndElement(); // </FileSearchResults>
            RESULTS_WRITER.writeCharacters("\n");
            RESULTS_WRITER.writeEndDocument();
            RESULTS_WRITER.flush();
        } catch (Exception e) {
            throw new RuntimeException("endSearchResults fehlgeschlagen: " + e.getMessage(), e);
        } finally {
            closeQuietly();
        }
    }

    private static void writeAttr(String name, String value) throws XMLStreamException {
        RESULTS_WRITER.writeAttribute(name, value != null ? value : "");
    }
    private static String safe(String s) { return s == null ? "" : s; }

    private static void closeQuietly() {
        try {
            if (RESULTS_WRITER != null) {
                RESULTS_WRITER.close();
            }
        } catch (Exception ignored) {}
        RESULTS_WRITER = null;
        RESULTS_FILE = null;
    }

    /**
     * Liest alle <File>-Elemente aus resultFilePath und ruft für jeden Eintrag den Consumer.
     * Schlank: streamt mit StAX, kein großer Speicherverbrauch.
     *
     * Hinweis: Für maximale Geschwindigkeit kann man direkt die im XML gespeicherten Attribute
     * ins TableModel übernehmen. Wenn du lieber ein FileSearchModel möchtest, kannst du hier
     * auch aus fullPath ein File bauen und ein Model konstruieren.
     */
    public static void readSearchResults(String resultFilePath,
                                         Consumer<XMLFileRecord> consumer) {
        File f = new File(resultFilePath);
        if (!f.exists()) return;

        try (InputStream is = new BufferedInputStream(new FileInputStream(f))) {
            XMLInputFactory inf = XMLInputFactory.newInstance();
            XMLStreamReader r = inf.createXMLStreamReader(is, "UTF-8");

            while (r.hasNext()) {
                int evt = r.next();
                if (evt == XMLStreamConstants.START_ELEMENT && "File".equals(r.getLocalName())) {
                    XMLFileRecord rec = new XMLFileRecord();
                    rec.fullPath = attr(r, "fullPath");
                    rec.name = attr(r, "name");
                    rec.parent = attr(r, "parent");
                    rec.sizeBytes = parseLong(attr(r, "sizeBytes"), 0L);
                    rec.type = attr(r, "type");
                    rec.created = attr(r, "created");
                    rec.modified = attr(r, "modified");
                    rec.hidden = Boolean.parseBoolean(attr(r, "hidden"));
                    rec.readOnly = Boolean.parseBoolean(attr(r, "readOnly"));
                    consumer.accept(rec);
                }
            }
            r.close();
        } catch (Exception e) {
            throw new RuntimeException("readSearchResults fehlgeschlagen: " + e.getMessage(), e);
        }
    }

    private static String attr(XMLStreamReader r, String name) {
        String v = r.getAttributeValue(null, name);
        return v == null ? "" : v;
    }
    private static long parseLong(String s, long def) {
        try { return Long.parseLong(s); } catch (Exception e) { return def; }
    }

    // Kleines DTO, damit die View ohne teure Re-Stat-Abfragen schnell befüllen kann.
    public static class XMLFileRecord {
        public String fullPath;
        public String name;
        public String parent;
        public long sizeBytes;
        public String type;
        public String created;
        public String modified;
        public boolean hidden;
        public boolean readOnly;
    }

    // ======== Bestehende Settings-Methoden (unverändert) ========

    // --- (Bestehender Code aus deiner Datei, unverändert) ---
    // saveDSSettingsToXML(...)
    // saveFSSettingsToXML(...)
    // readDSSettingsFromXML()
    // readFSSettingsFromXML()

    //  >>> Füge hier den kompletten bestehenden Settings-Code unverändert wieder ein <<<
    //  (Um Platz zu sparen, habe ich ihn hier weggelassen – bitte deine vorhandenen
    //  Methoden 1:1 behalten. Die neuen Methoden stehen zusätzlich daneben.)


	/**
	 * saves settings in XML file settings.xml, 
	 * @Example XMLController.saveSettingsToXML(0.1f, 0.2f, 1024, 768, true);
	 * 
	 * @param minFileSize - float - Volume for Sound
	 * @param maxFileSize - float - Volume for SFX 
	 * @param resX - int - horizontal screen resolution  
	 * @param resY - int - vertical screen resolution
	 * @param fileSizeBoo - boolean - true if full screen mode is used
	 * @author Jörg
	 */
	
	/**
	 * Speicher die Duplicate-Suche Einstellungen in XML Datei
	 * @param options
	 * @author Jörg
	 */
	public static void saveDSSettingsToXML(DuplicateSearchOptionsModel options) {
	    new File(dsSettingsPath);

	    DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory.newInstance();
	    try {
	        DocumentBuilder documentBuilder = documentBuilderFactory.newDocumentBuilder();
	        Document doc = documentBuilder.newDocument();

	        Element rootElement = doc.createElement("Settings");
	        doc.appendChild(rootElement);

	        // 1: Min / Max Size
	        Element minSize = doc.createElement("MinFileSize");
	        minSize.setAttribute("MB", String.valueOf(options.getMinFileSize()));
	        rootElement.appendChild(minSize);

	        Element maxSize = doc.createElement("MaxFileSize");
	        maxSize.setAttribute("MB", String.valueOf(options.getMaxFileSize()));
	        rootElement.appendChild(maxSize);

	        // 2: File Extensions & Name
	        Element fExt = doc.createElement("FileExtensions");
	        fExt.setAttribute("ext", options.getFileExtention() != null ? options.getFileExtention() : "");
	        rootElement.appendChild(fExt);

	        Element fNameString = doc.createElement("FileNameString");
	        fNameString.setAttribute("value", options.getFileNameString() != null ? options.getFileNameString() : "");
	        rootElement.appendChild(fNameString);

	        // 3: Creation Date Operator + Date
	        Element createdOp = doc.createElement("CreationDateOperator");
	        createdOp.setAttribute("op", options.getFileCreationDateOperator() != null ? options.getFileCreationDateOperator() : "");
	        rootElement.appendChild(createdOp);

	        Element created = doc.createElement("CreationDate");
	        created.setAttribute("value", options.getCreationDate() != null ? options.getCreationDate().toString() : "");
	        rootElement.appendChild(created);

	        // 4: Modification Date Operator + Date
	        Element modifiedOp = doc.createElement("ModificationDateOperator");
	        modifiedOp.setAttribute("op", options.getFileModificationDateOperator() != null ? options.getFileModificationDateOperator() : "");
	        rootElement.appendChild(modifiedOp);

	        Element modified = doc.createElement("ModificationDate");
	        modified.setAttribute("value", options.getModificationDate() != null ? options.getModificationDate().toString() : "");
	        rootElement.appendChild(modified);

	        // 5: Booleans
	        Element fSize = doc.createElement("FileSize");
	        fSize.setAttribute("On", String.valueOf(options.isFileSizeBoo()));
	        rootElement.appendChild(fSize);

	        Element fName = doc.createElement("FileName");
	        fName.setAttribute("On", String.valueOf(options.isFileNameBoo()));
	        rootElement.appendChild(fName);

	        Element fExtBool = doc.createElement("FileExtensionBoolean");
	        fExtBool.setAttribute("On", String.valueOf(options.isFileExtentionBoo()));
	        rootElement.appendChild(fExtBool);

	        Element sFolder = doc.createElement("SubFolder");
	        sFolder.setAttribute("On", String.valueOf(options.isSubFolderBoo()));
	        rootElement.appendChild(sFolder);

	        // XML schreiben
	        TransformerFactory tf = TransformerFactory.newInstance();
	        Transformer transformer = tf.newTransformer();
	        transformer.setOutputProperty(OutputKeys.INDENT, "yes");
	        transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");

	        File file = new File(dsSettingsPath);
	        file.getParentFile().mkdirs();
	        transformer.transform(new DOMSource(doc), new StreamResult(file));

	        System.out.println("Settings gespeichert unter: " + file.getAbsolutePath());

	    } catch (ParserConfigurationException | TransformerException e) {
	        e.printStackTrace();
	    }
	}

	
	
	
	public static void saveFSSettingsToXML(FileSearchOptionsModel options) {
	    new File(fsSettingsPath);

	    DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory.newInstance();
	    try {
	        DocumentBuilder documentBuilder = documentBuilderFactory.newDocumentBuilder();
	        Document doc = documentBuilder.newDocument();

	        Element rootElement = doc.createElement("Settings");
	        doc.appendChild(rootElement);

	        // 1: Min / Max Size
	        Element minSize = doc.createElement("MinFileSize");
	        minSize.setAttribute("MB", String.valueOf(options.getMinFileSize()));
	        rootElement.appendChild(minSize);

	        Element maxSize = doc.createElement("MaxFileSize");
	        maxSize.setAttribute("MB", String.valueOf(options.getMaxFileSize()));
	        rootElement.appendChild(maxSize);

	        // 2: File Extensions & Name
	        Element fExt = doc.createElement("FileExtensions");
	        fExt.setAttribute("ext", options.getFileExtention() != null ? options.getFileExtention() : "");
	        rootElement.appendChild(fExt);

	        Element fNameString = doc.createElement("FileNameString");
	        fNameString.setAttribute("value", options.getFileNameString() != null ? options.getFileNameString() : "");
	        rootElement.appendChild(fNameString);

	        // 3: Creation Date Operator + Date
	        Element createdOp = doc.createElement("CreationDateOperator");
	        createdOp.setAttribute("op", options.getFileCreationDateOperator() != null ? options.getFileCreationDateOperator() : "");
	        rootElement.appendChild(createdOp);

	        Element created = doc.createElement("CreationDate");
	        created.setAttribute("value", options.getCreationDate() != null ? options.getCreationDate().toString() : "");
	        rootElement.appendChild(created);

	        // 4: Modification Date Operator + Date
	        Element modifiedOp = doc.createElement("ModificationDateOperator");
	        modifiedOp.setAttribute("op", options.getFileModificationDateOperator() != null ? options.getFileModificationDateOperator() : "");
	        rootElement.appendChild(modifiedOp);

	        Element modified = doc.createElement("ModificationDate");
	        modified.setAttribute("value", options.getModificationDate() != null ? options.getModificationDate().toString() : "");
	        rootElement.appendChild(modified);

	        // 5: Include Subfolders
	        Element sFolder = doc.createElement("SubFolder");
	        sFolder.setAttribute("On", String.valueOf(options.isSubFolderBoo()));
	        rootElement.appendChild(sFolder);

	        // XML schreiben
	        TransformerFactory tf = TransformerFactory.newInstance();
	        Transformer transformer = tf.newTransformer();
	        transformer.setOutputProperty(OutputKeys.INDENT, "yes");
	        transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");

	        File file = new File(fsSettingsPath);
	        file.getParentFile().mkdirs();
	        transformer.transform(new DOMSource(doc), new StreamResult(file));

	        System.out.println("Dateisuche-Settings gespeichert unter: " + file.getAbsolutePath());

	    } catch (ParserConfigurationException | TransformerException e) {
	        e.printStackTrace();
	    }
	}
	
	
	
	
	
	public static DuplicateSearchOptionsModel readDSSettingsFromXML() {
	    File settingsFile = new File(dsSettingsPath);

	    try {
	        if (!settingsFile.exists()) {
	            System.err.println("Settings Datei nicht gefunden! " + settingsFile.getAbsolutePath());
	            return null;
	        }

	        DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory.newInstance();
	        DocumentBuilder documentBuilder = documentBuilderFactory.newDocumentBuilder();
	        Document doc = documentBuilder.parse(settingsFile);
	        doc.getDocumentElement().normalize();

	        Element rootElement = doc.getDocumentElement();

	        DuplicateSearchOptionsModel options = new DuplicateSearchOptionsModel();

	        // 1: Min / Max Size
	        Element minSize = (Element) rootElement.getElementsByTagName("MinFileSize").item(0);
	        options.setMinFileSize(Double.parseDouble(minSize.getAttribute("MB")));

	        Element maxSize = (Element) rootElement.getElementsByTagName("MaxFileSize").item(0);
	        options.setMaxFileSize(Double.parseDouble(maxSize.getAttribute("MB")));

	        // 2: File Extensions & Name
	        Element fExt = (Element) rootElement.getElementsByTagName("FileExtensions").item(0);
	        options.setFileExtention(fExt.getAttribute("ext"));

	        Element fNameString = (Element) rootElement.getElementsByTagName("FileNameString").item(0);
	        options.setFileNameString(fNameString.getAttribute("value"));

	        // 3: Creation Date Operator + Date
	        Element createdOp = (Element) rootElement.getElementsByTagName("CreationDateOperator").item(0);
	        options.setFileCreationDateOperator(createdOp != null ? createdOp.getAttribute("op") : "");

	        Element created = (Element) rootElement.getElementsByTagName("CreationDate").item(0);
	        String creationDateStr = created != null ? created.getAttribute("value") : "";
	        if (creationDateStr != null && !creationDateStr.isBlank()) {
	            options.setCreationDate(LocalDate.parse(creationDateStr));
	        }

	        // 4: Modification Date Operator + Date
	        Element modifiedOp = (Element) rootElement.getElementsByTagName("ModificationDateOperator").item(0);
	        options.setFileModificationDateOperator(modifiedOp != null ? modifiedOp.getAttribute("op") : "");

	        Element modified = (Element) rootElement.getElementsByTagName("ModificationDate").item(0);
	        String modificationDateStr = modified != null ? modified.getAttribute("value") : "";
	        if (modificationDateStr != null && !modificationDateStr.isBlank()) {
	            options.setModificationDate(LocalDate.parse(modificationDateStr));
	        }

	        // 5: Booleans
	        Element fSize = (Element) rootElement.getElementsByTagName("FileSize").item(0);
	        options.setFileSizeBoo(Boolean.parseBoolean(fSize.getAttribute("On")));

	        Element fName = (Element) rootElement.getElementsByTagName("FileName").item(0);
	        options.setFileNameBoo(Boolean.parseBoolean(fName.getAttribute("On")));

	        Element fExtBool = (Element) rootElement.getElementsByTagName("FileExtensionBoolean").item(0);
	        options.setFileExtentionBoo(Boolean.parseBoolean(fExtBool.getAttribute("On")));

	        Element sFolder = (Element) rootElement.getElementsByTagName("SubFolder").item(0);
	        options.setSubFolderBoo(Boolean.parseBoolean(sFolder.getAttribute("On")));

	        System.out.println("Einstellungen geladen von: " + settingsFile.getAbsolutePath());
	        return options;

	    } catch (Exception e) {
	        e.printStackTrace();
	        System.err.println("Fehler beim Lesen der Datei!");
	        return null;
	    }
	} 
	public static FileSearchOptionsModel readFSSettingsFromXML() {
	    File settingsFile = new File(fsSettingsPath);

	    try {
	        if (!settingsFile.exists()) {
	            System.err.println("FS-Settings Datei nicht gefunden! " + settingsFile.getAbsolutePath());
	            return null;
	        }

	        DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory.newInstance();
	        DocumentBuilder documentBuilder = documentBuilderFactory.newDocumentBuilder();
	        Document doc = documentBuilder.parse(settingsFile);
	        doc.getDocumentElement().normalize();

	        Element rootElement = doc.getDocumentElement();

	        FileSearchOptionsModel options = new FileSearchOptionsModel();

	        // 1: Min / Max Size
	        Element minSize = (Element) rootElement.getElementsByTagName("MinFileSize").item(0);
	        options.setMinFileSize(Double.parseDouble(minSize.getAttribute("MB")));

	        Element maxSize = (Element) rootElement.getElementsByTagName("MaxFileSize").item(0);
	        options.setMaxFileSize(Double.parseDouble(maxSize.getAttribute("MB")));

	        // 2: File Extensions & Name
	        Element fExt = (Element) rootElement.getElementsByTagName("FileExtensions").item(0);
	        options.setFileExtention(fExt.getAttribute("ext"));

	        Element fNameString = (Element) rootElement.getElementsByTagName("FileNameString").item(0);
	        options.setFileNameString(fNameString.getAttribute("value"));

	        // 3: Creation Date Operator + Date
	        Element createdOp = (Element) rootElement.getElementsByTagName("CreationDateOperator").item(0);
	        options.setFileCreationDateOperator(createdOp != null ? createdOp.getAttribute("op") : "");

	        Element created = (Element) rootElement.getElementsByTagName("CreationDate").item(0);
	        String creationDateStr = created != null ? created.getAttribute("value") : "";
	        if (creationDateStr != null && !creationDateStr.isBlank()) {
	            options.setCreationDate(LocalDate.parse(creationDateStr));
	        }

	        // 4: Modification Date Operator + Date
	        Element modifiedOp = (Element) rootElement.getElementsByTagName("ModificationDateOperator").item(0);
	        options.setFileModificationDateOperator(modifiedOp != null ? modifiedOp.getAttribute("op") : "");

	        Element modified = (Element) rootElement.getElementsByTagName("ModificationDate").item(0);
	        String modificationDateStr = modified != null ? modified.getAttribute("value") : "";
	        if (modificationDateStr != null && !modificationDateStr.isBlank()) {
	            options.setModificationDate(LocalDate.parse(modificationDateStr));
	        }

	        // 5: Include Subfolders
	        Element sFolder = (Element) rootElement.getElementsByTagName("SubFolder").item(0);
	        options.setSubFolderBoo(Boolean.parseBoolean(sFolder.getAttribute("On")));

	        System.out.println("FS-Einstellungen geladen von: " + settingsFile.getAbsolutePath());
	        return options;

	    } catch (Exception e) {
	        e.printStackTrace();
	        System.err.println("Fehler beim Lesen der FS-Settings Datei!");
	        return null;
	    }
	}
}
