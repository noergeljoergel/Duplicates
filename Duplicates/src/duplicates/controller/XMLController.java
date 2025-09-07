package duplicates.controller;

import java.io.File;
import java.time.LocalDate;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import duplicates.model.FileSearchOptionsModel;
import duplicates.model.DuplicateSearchOptionsModel;


// stores saveGame-Data in XML-format
public class XMLController {
	// Filepaths
	private static String dsSettingsPath = "settings/DSsettings.xml";
	private static String fsSettingsPath = "settings/FSsettings.xml";
//	private GameState gameState;

//	public XMLController(GameState gameState) {
//		this.gameState = gameState;
//	}

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
