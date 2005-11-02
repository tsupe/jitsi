/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.history;

import java.io.File;
import java.io.IOException;
import java.text.ParseException;
import java.util.ArrayList;

import javax.xml.parsers.DocumentBuilder;

import net.java.sip.communicator.service.history.History;
import net.java.sip.communicator.service.history.HistoryID;
import net.java.sip.communicator.service.history.records.HistoryRecordStructure;
import net.java.sip.communicator.service.history.records.TextType;
import net.java.sip.communicator.util.EnumerationBase;
import net.java.sip.communicator.util.xml.XMLUtils;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

/**
 * 
 * @author Alexander Pelov
 */
public class DBStructSerializer {
	
	private HistoryServiceImpl historyService;

	public DBStructSerializer(HistoryServiceImpl historyService) {
		this.historyService = historyService;
	}
	
	public void writeHistory(File dbDatFile, History history) 
		throws IOException
	{	
		DocumentBuilder builder = this.historyService.getDocumentBuilder();
		Document doc = builder.newDocument();
		
		Element root = doc.createElement("dbstruct");
		root.setAttribute("version", "1.0");
		
		Element structure = this.createStructureTag(doc, 
				history.getHistoryRecordsStructure());
		Element id = this.createIDTag(doc, history.getID());
		
		root.appendChild(structure);
		root.appendChild(id);
		
		doc.appendChild(root);
		
		XMLUtils.writeXML(doc, dbDatFile);
	}
	
	private Element createIDTag(Document doc, HistoryID historyID) {
		Element idroot = doc.createElement("id");
		Element current = idroot;
		
		String[] idelements = historyID.getID();
		for(int i = 0; i < idelements.length; i++) {
			Element idnode = doc.createElement("component");
			idnode.setAttribute("value", idelements[i]);
			
			current.appendChild(idnode);
			current = idnode;
		}
		
		return idroot;
	}

	private Element createStructureTag(Document doc, 
			HistoryRecordStructure recordStructure)
	{
		Element structure = doc.createElement("structure");
		String[] propertyNames = recordStructure.getPropertyNames();
		TextType[] propertyTypes = recordStructure.getValueTypes();
		int count = recordStructure.getPropertyCount();
		for(int i = 0; i < count; i++) {
			Element property = doc.createElement("property");
			property.setAttribute("name", propertyNames[i]);
			property.setAttribute("type", propertyTypes[i].toString());
			
			structure.appendChild(property);
		}
		
		return structure;
	}

	/**
	 * This method parses an XML file, and returns a History object created
	 * with the information from it. The parsing is non-validating, so if a 
	 * malformed XML is passed the results are undefined. 
	 * The file should be with the following structure:
	 * 
	 * <dbstruct version="1.0">
	 * 	<id value="idcomponent1">
	 * 		<id value="idcomponent2">
	 * 			<id value="idcomponent3"/>
	 * 		</id>
	 *  </id>
	 *  
	 * 	<structure>
	 * 		<property name="propertyName" type="textType" />
	 * 		<property name="propertyName" type="textType" />
	 * 		<property name="propertyName" type="textType" />
	 * 	</structure>
	 * </dbstruct>
	 * 
	 * @param dbDatFile The file to be parsed.
	 * @return A History object corresponding to this dbstruct file.
	 * @throws SAXException Thrown if an error occurs during XML parsing.
	 * @throws IOException Thrown if an IO error occurs.
	 * @throws ParseException Thrown if there is error in the XML data format.  
	 */
	public History loadHistory(File dbDatFile) 
		throws SAXException, IOException, ParseException
	{	
		DocumentBuilder builder = historyService.getDocumentBuilder();
		Document doc = builder.parse(dbDatFile);
		
		Node root = doc.getFirstChild();
		HistoryID id = loadID(root);
		HistoryRecordStructure structure = loadStructure(root);
		
		return new HistoryImpl(id, dbDatFile.getParentFile(), 
				structure, historyService);
	}
	
	/**
	 * This method parses a "structure" tag and returns the corresponging
	 * HistoryRecordStructure.
	 *  
	 * @throws ParseException Thrown if there is no structure tag. 
	 */
	private HistoryRecordStructure loadStructure(Node root) 
		throws ParseException
	{
		Element structNode = findElement(root, "structure");
		if(structNode == null) {
			throw new ParseException("There is not structure tag defined!", 0);
		}
		
		NodeList nodes = structNode.getChildNodes();
		int count = nodes.getLength();
		
		ArrayList propertyNames = new ArrayList(count);
		ArrayList propertyTypes = new ArrayList(count);
		
		for(int i = 0; i < count; i++) {
			Node node = nodes.item(i);
			if(node.getNodeType() == Node.ELEMENT_NODE &&
					"property".equals(node.getNodeName()))
			{
				Element parameter = (Element) node;
				String paramName = parameter.getAttribute("name");
				String paramType = parameter.getAttribute("type");
				
				if(paramName == null) {
					continue;
				}
				
				TextType type = (TextType)EnumerationBase.fromString(
						TextType.class,	paramType);
				if(type == null) {
					type = HistoryRecordStructure.DEFAULT_TEXT_TYPE;
				}
				
				propertyNames.add(paramName);
				propertyTypes.add(type);
			}
		}
		
		String[] names = new String[propertyNames.size()];
		TextType[] types = new TextType[propertyTypes.size()];
		
		propertyNames.toArray(names);
		propertyTypes.toArray(types);
		 
		return new HistoryRecordStructure(names, types);
	}

	private HistoryID loadID(Node parent) throws ParseException {
		Element idnode = findElement(parent, "id");
		ArrayList al = loadID(new ArrayList(), idnode);
		
		String[] id = new String[al.size()];
		al.toArray(id);
		
		return HistoryID.createFromID(id);
	}
	
	private ArrayList loadID(ArrayList loadedIDs, Node parent) 
		throws ParseException
	{
		Element node = findElement(parent, "component");
		if(node != null) {
			String idValue = node.getAttribute("value");
			if(idValue != null) {
				loadedIDs.add(idValue);
			} else {
				throw new ParseException(
						"There is an ID object without value.", 0);
			}
		}
		
		return loadID(loadedIDs, node);
	}
	
	/**
	 * This method seraches through all children of a given node
	 * and returns the first with the name matching the given one.
	 * If no node is found, null is returned.
	 */
	private Element findElement(Node parent, String name) {
		Element retVal = null;
		NodeList nodes = parent.getChildNodes();
		int count = nodes.getLength();

		for(int i = 0; i < count; i++) {
			Node node = nodes.item(i);
			if(node.getNodeType() == Node.ELEMENT_NODE && 
					name.equals(node.getNodeName()))
			{
				retVal = (Element)node;
				break;
			}
		}
		return retVal;
	}
}
