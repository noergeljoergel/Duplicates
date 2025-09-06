package duplicates.controller;

import java.io.File;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.w3c.dom.Document;
import org.w3c.dom.Element;


// stores saveGame-Data in XML-format
public class XMLController {
	// Filepaths
	private static String settingsPath = "settings/settings.xml";
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
	 * @param fileSize - boolean - true if full screen mode is used
	 * @author Jörg
	 */
	public static void saveSettingsToXML(double minFileSize, double maxFileSize, String fileExtention, boolean fileSize, boolean fileName, boolean subFolder, boolean fileExtentionBoolean) {
		new File(settingsPath);
	
		DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory.newInstance();
		DocumentBuilder documentBuilder;
		try {
			
			String outPut = minFileSize + " - " + maxFileSize + " - " + fileExtention + " - " + fileSize + " - " + fileName + " - " + subFolder + " - " + fileExtentionBoolean;
			System.out.println(outPut);
			
			
			documentBuilder = documentBuilderFactory.newDocumentBuilder();
			Document doc = documentBuilder.newDocument();

			// root element für settings
			Element rootElement = doc.createElement("Settings");
			doc.appendChild(rootElement);

			// minimale Dateigröße
			Element minSize = doc.createElement("minFileSize");
			minSize.setAttribute("MB", String.valueOf(minFileSize));
			rootElement.appendChild(minSize);

			// maximale Dateigröße
			Element maxSize = doc.createElement("maxFileSize");
			maxSize.setAttribute("MB", String.valueOf(maxFileSize));
			rootElement.appendChild(maxSize);

			// Liste der Dateierweiterungen als String
			Element fExt = doc.createElement("FileExtentions");
			fExt.setAttribute("ext", fileExtention);
			rootElement.appendChild(fExt);
			
			// FileSize
			Element fSize = doc.createElement("FileSize");
			fSize.setAttribute("On", String.valueOf(fileSize));
			rootElement.appendChild(fSize);
			
			// FileName
			Element fname = doc.createElement("FileName");
			fname.setAttribute("On", String.valueOf(fileName));
			rootElement.appendChild(fname);
			
			// SubFolder
			Element sFolder = doc.createElement("SubFolder");
			sFolder.setAttribute("On", String.valueOf(subFolder));
			rootElement.appendChild(sFolder);
			
			// FileExtention
			Element fExtention = doc.createElement("FileExtentionBoolean");
			fExtention.setAttribute("On", String.valueOf(fileExtentionBoolean));
			rootElement.appendChild(fExtention);			
			
		// write contents into file
			TransformerFactory tf = TransformerFactory.newInstance();
			Transformer transformer = tf.newTransformer();
			transformer.setOutputProperty(OutputKeys.INDENT, "yes");
			transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");

			File file = new File(settingsPath);
			file.getParentFile().mkdirs();
			transformer.transform(new DOMSource(doc), new StreamResult(file));

			System.out.println("Settings gespeichert unter: " + file.getAbsolutePath());

			// possible exceptions
		} catch (ParserConfigurationException e) {
			e.printStackTrace();
		} catch (TransformerConfigurationException e) {
			e.printStackTrace();
		} catch (TransformerException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	/**
	 * reads settings from settings/settings.xml  
	 * @Example 
	 * Object[] settings = xmlController.readSettingsFromXML();
	 * 
     *	if (settings != null) {
     *       float soundVolume   = (float) settings[0];
     *       float sfxVolume     = (float) settings[1];
     *       int resolutionX     = (int)   settings[2];
     *       int resolutionY     = (int)   settings[3];
     *       boolean fullscreen  = (boolean) settings[4];
     *       System.out.println("Sound Volume: " + soundVolume);
     *       System.out.println("SFX Volume: " + sfxVolume);
     *       System.out.println("Resolution: " + resolutionX + "x" + resolutionY);
     *       System.out.println("Fullscreen: " + fullscreen);
     *   }
     *   
     *
	 * @return Object[] { float soundVolume, float sfxVolume, int resolutionX, int resolutionY, boolean fullScreen }
	 * @author Christoph, Vladi, Jörg
	 */
	public static Object[] readSettingsFromXML() {
		File settingsFile = new File(settingsPath);

		try {
			if (!settingsFile.exists()) {
				System.err.println("Settings Datei nicht gefunden!" + settingsFile.getAbsolutePath());
				return null;
			}
			DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory.newInstance();
			DocumentBuilder documentBuilder = documentBuilderFactory.newDocumentBuilder();
			Document doc = documentBuilder.parse(settingsFile);
			doc.getDocumentElement().normalize();

			Element rootElement = doc.getDocumentElement();

			// minFileSize
			Element minSize = (Element) rootElement.getElementsByTagName("minFileSize").item(0);
			double minFileSize = Double.parseDouble(minSize.getAttribute("MB"));
			
			//  maxFileSize
			Element maxSize = (Element) rootElement.getElementsByTagName("maxFileSize").item(0);
			double maxFileSize = Double.parseDouble(maxSize.getAttribute("MB"));
						
			//  Liste der Dateierweiterungen als String
			Element fExt = (Element) rootElement.getElementsByTagName("FileExtentions").item(0);
			String fileExt = fExt.getAttribute("ext");

			// FileSize
			Element fSize = (Element) rootElement.getElementsByTagName("FileSize").item(0);
			boolean fileSize = Boolean.parseBoolean(fSize.getAttribute("On"));
			
			// FileName
			Element fName = (Element) rootElement.getElementsByTagName("FileName").item(0);
			boolean fileName = Boolean.parseBoolean(fName.getAttribute("On"));
			
			// SubFolder
			Element sFolder = (Element) rootElement.getElementsByTagName("SubFolder").item(0);
			boolean subFolder = Boolean.parseBoolean(sFolder.getAttribute("On"));
			
			// Dateierweiterungen als boolean
			Element fExtB = (Element) rootElement.getElementsByTagName("FileExtentionBoolean").item(0);
			boolean FileExtB = Boolean.parseBoolean(fExtB.getAttribute("On"));
			
			System.out.println("Einstellungen geladen von: " + settingsFile.getAbsolutePath());
			return new Object[] { minFileSize, maxFileSize, fileExt, fileSize, fileName, subFolder, FileExtB};

		} catch (Exception e) {
			e.printStackTrace();
			System.err.println("Fehler beim lesen der Datei!");
			return null;
		}
	}      
}
