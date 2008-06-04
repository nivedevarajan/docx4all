/*
 *  Copyright 2007, Plutext Pty Ltd.
 *   
 *  This file is part of Docx4all.

    Docx4all is free software: you can redistribute it and/or modify
    it under the terms of version 3 of the GNU General Public License 
    as published by the Free Software Foundation.

    Docx4all is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License   
    along with Docx4all.  If not, see <http://www.gnu.org/licenses/>.
    
 */

package org.docx4all.util;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.text.BreakIterator;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;

import javax.swing.JEditorPane;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import javax.swing.text.EditorKit;
import javax.swing.text.Element;
import javax.swing.text.Position;
import javax.swing.text.Segment;
import javax.swing.text.DefaultStyledDocument.ElementSpec;

import org.apache.log4j.Logger;
import org.docx4all.swing.WordMLTextPane;
import org.docx4all.swing.text.BadSelectionException;
import org.docx4all.swing.text.DocumentElement;
import org.docx4all.swing.text.ElementMLIteratorCallback;
import org.docx4all.swing.text.SdtBlockInfo;
import org.docx4all.swing.text.TextSelector;
import org.docx4all.swing.text.WordMLDocument;
import org.docx4all.swing.text.WordMLStyleConstants;
import org.docx4all.ui.main.Constants;
import org.docx4all.xml.DocumentML;
import org.docx4all.xml.ElementML;
import org.docx4all.xml.ElementMLFactory;
import org.docx4all.xml.ElementMLIterator;
import org.docx4all.xml.ParagraphML;
import org.docx4all.xml.ParagraphPropertiesML;
import org.docx4all.xml.RunContentML;
import org.docx4all.xml.RunML;
import org.docx4all.xml.RunPropertiesML;
import org.docx4all.xml.SdtBlockML;
import org.docx4j.XmlUtils;
import org.docx4j.openpackaging.packages.WordprocessingMLPackage;

/**
 *	@author Jojada Tirtowidjojo - 27/11/2007
 */
public class DocUtil {
	private static Logger log = Logger.getLogger(DocUtil.class);

	private final static String TAB = "    ";
	
	public final static boolean isSharedDocument(WordMLDocument doc) {
		boolean isShared = false;
		
		String uri = 
			(String) doc.getProperty(WordMLDocument.FILE_PATH_PROPERTY);
		if (uri != null) {
	    	int idx = uri.indexOf("/alfresco/"); 
	    	if (idx > 0) {
	    		//temporary checking
	    		//TODO: Has to check whether fileUri's protocol is webdav
	    		//and its context is correct.
	    		DocumentElement elem = (DocumentElement) doc.getDefaultRootElement();
	    		DocumentML docML = (DocumentML) elem.getElementML();
				WordprocessingMLPackage wmlPackage = docML.getWordprocessingMLPackage();
				if (wmlPackage != null) {
					org.docx4j.docProps.custom.Properties.Property groupingProp =
						XmlUtil.getCustomProperty(
								wmlPackage, 
								Constants.PLUTEXT_GROUPING_PROPERTY_NAME);
					org.docx4j.docProps.custom.Properties.Property checkinProp =
						XmlUtil.getCustomProperty(
								wmlPackage, 
								Constants.PLUTEXT_CHECKIN_MESSAGE_ENABLED_PROPERTY_NAME);
					isShared = (groupingProp != null && checkinProp != null);
				}
	    	}
		}
		
		return isShared;
	}
	
	/**
	 * Makes the xml content of document become the main document part
	 * of WordprocessingMLPackage.
	 *  
	 * @param kit EditorKit instance
	 * @param doc Xml document
	 * @param outputPackage WordprocessingMLPackage that will store the resulting output.
	 *        If it is null a brand new WordprocessingMLPackage will be created. 
	 * @return The passed in outputPackage argument if it is NOT NULL.
	 *         A brand new WordprocessingMLPackage; otherwise.
	 */
	public final static WordprocessingMLPackage write(
		final EditorKit kit, 
		final Document doc, 
		WordprocessingMLPackage outputPackage) { 	
		
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		try {
			kit.write(out, doc, 0, doc.getLength());
		} catch (BadLocationException exc) {
			;// ignore
		} catch (IOException exc) {
			exc.printStackTrace();
		}
		ByteArrayInputStream in = new ByteArrayInputStream(out.toByteArray());
		outputPackage = XmlUtil.deserialize(outputPackage, in);
		return outputPackage;
	}
	
	/**
	 * Reads the xml document part of WordprocessingMLPackage and puts it into
	 * Swing JEditorPane's document.
	 * 
	 * @param targetView
	 *            The destination view for the reading
	 * @param sourcePackage
	 *            WordprocessingMLPackage to read from
	 * @return targetView's document
	 */
	public final static Document read(
		final JEditorPane targetView, 
		final WordprocessingMLPackage sourcePackage) {

		EditorKit kit = targetView.getEditorKit();
    	Document theDoc = targetView.getDocument();
    	
    	ByteArrayOutputStream out = new ByteArrayOutputStream();
    	XmlUtil.serialize(sourcePackage, out);
    	
    	ByteArrayInputStream in = new ByteArrayInputStream(out.toByteArray());
    	out = null;
    	
		try {
			kit.read(in, theDoc, 0);
		} catch (BadLocationException exc) {
			;// ignore
		} catch (IOException exc) {
			exc.printStackTrace();
		}
		
		return theDoc;
	}
	
	public final static HashMap<BigInteger, SdtBlockInfo> createSdtBlockInfoMap(WordMLDocument doc) {
		HashMap<BigInteger, SdtBlockInfo> theMap = new HashMap<BigInteger, SdtBlockInfo>();
		
		try {
			doc.readLock();
			DocumentElement root = (DocumentElement) doc
					.getDefaultRootElement();
			for (int i = 0; i < root.getElementCount() - 1; i++) {
				DocumentElement elem = (DocumentElement) root.getElement(i);
				ElementML ml = elem.getElementML();
				if (ml instanceof SdtBlockML) {
					try {
						Position pos = doc.createPosition(elem.getStartOffset());
						org.docx4j.wml.SdtBlock sdt = (org.docx4j.wml.SdtBlock) ml
							.getDocxObject();
						SdtBlockInfo info = new SdtBlockInfo();
						info.setPosition(pos);
						info.setSdtBlock(sdt);
						theMap.put(sdt.getSdtPr().getId().getVal(), info);
					} catch (BadLocationException exc) {
						;//ignore
					}
				}
			}
		} finally {
			doc.readUnlock();
		}
		
		return theMap;
	}
	
	/**
	 * When a text is inserted into a document its attributes are determined by
	 * the text element at the insertion position. This text element is called
	 * input attribute element and has to be editable.
	 * 
	 * Given an insertion position there are two potential input attribute
	 * elements, namely the one on the left of the insertion position and on the
	 * right. By default this method returns the one on the left. In the case
	 * where this default input attribute element does not exist or the
	 * insertion position is at the beginning of a paragraph then the other
	 * input attribute element will be visited.
	 * 
	 * To override the default behaviour 'bias' parameter can be set to either
	 * Position.Bias.Forward (for selecting the right element) or
	 * Position.Bias.Backward (for the left one).
	 * 
	 * @param doc
	 *            the document where input attribute element is desired
	 * @param offset
	 *            Offset position
	 * @param bias
	 *            Null, default behaviour.
	 *            Position.Bias.Forward, choose the right element.
	 *            Position.Bias.Backward, choose the left element
	 * @return An editable WordMLDocument.TextElement if any; Null, otherwise.
	 */
	public final static WordMLDocument.TextElement getInputAttributeElement(
		WordMLDocument doc, 
		int offset,
		Position.Bias bias) {
		
		DocumentElement theElem = null;
		
		if (bias == null) {
			theElem = (DocumentElement) doc.getParagraphMLElement(offset, true);
			if (theElem.getStartOffset() == offset) {
				theElem = (DocumentElement) doc.getCharacterElement(offset);
			} else {
				theElem = 
					(DocumentElement) 
						doc.getCharacterElement(Math.max(offset - 1, 0));
				if (!theElem.isEditable()) {
					theElem = (DocumentElement) doc.getCharacterElement(offset);
				}
			}
		} else {
			offset = (bias == Position.Bias.Forward) ? offset : Math.max(offset - 1, 0);
			theElem = 
				(WordMLDocument.TextElement) doc.getCharacterElement(offset);
		}

		if (!theElem.isEditable()) {
			theElem = null;
		}
		
		return (WordMLDocument.TextElement) theElem;
	}
	
	public final static void saveTextContentToElementML(WordMLDocument.TextElement elem) {
		if (elem == null) {
			return;
		}
		
		WordMLDocument doc = (WordMLDocument) elem.getDocument();
		try {
			doc.readLock();

			if (elem.getStartOffset() == elem.getEndOffset()) {
				return;
			}

			RunContentML rcml = (RunContentML) elem.getElementML();
			if (!rcml.isDummy() && !rcml.isImplied()) {
				int count = elem.getEndOffset() - elem.getStartOffset();
				String text = elem.getDocument().getText(elem.getStartOffset(),
						count);
				
				log.debug("saveTextContentToElementML(): text content=" + text);
				
				rcml.setTextContent(text);
			}

		} catch (BadLocationException exc) {
			;// ignore
			
		} finally {
			doc.readUnlock();
		}
	}
	
    public static final int getWordStart(WordMLTextPane editor, int offs)
			throws BadLocationException {
		WordMLDocument doc = (WordMLDocument) editor.getDocument();
		if (offs == doc.getLength()) {
			throw new BadLocationException("No word at " + offs, offs);
		}
		Element para = doc.getParagraphMLElement(offs, true);
		int paraStart = para.getStartOffset();
		int paraEnd = para.getEndOffset();

		Segment seg = new Segment();
		doc.getText(paraStart, paraEnd - paraStart, seg);
		if (seg.count > 0) {
			BreakIterator words = BreakIterator.getWordInstance(editor
					.getLocale());
			words.setText(seg);
			int wordPosition = seg.offset + offs - paraStart;
			if (wordPosition >= words.last()) {
				wordPosition = words.last() - 1;
			}
			words.following(wordPosition);
			offs = paraStart + words.previous() - seg.offset;
		}
		return offs;
	}
    
    public static final int getWordEnd(WordMLTextPane editor, int offs)
			throws BadLocationException {
		WordMLDocument doc = (WordMLDocument) editor.getDocument();
		if (offs == doc.getLength()) {
			throw new BadLocationException("No word at " + offs, offs);
		}
		Element para = doc.getParagraphMLElement(offs, true);
		int paraStart = para.getStartOffset();
		int paraEnd = para.getEndOffset();

		Segment seg = new Segment();
		doc.getText(paraStart, paraEnd - paraStart, seg);
		if (seg.count > 0) {
			BreakIterator words = BreakIterator.getWordInstance(editor
					.getLocale());
			words.setText(seg);
			int wordPosition = offs - paraStart + seg.offset;
			if (wordPosition >= words.last()) {
				wordPosition = words.last() - 1;
			}
			offs = paraStart + words.following(wordPosition) - seg.offset;
		}
		return offs;
	}

	public final static ElementML splitElementML(DocumentElement elem, int atIndex) {
    	ElementML elemML = elem.getElementML();		
		if (elem.getStartOffset() == elem.getEndOffset()
			|| elemML.isImplied()
			|| elem.getParentElement() == null
			|| !(elemML instanceof ParagraphML
				|| elemML instanceof RunML
				|| elemML instanceof RunContentML)) {
			throw new IllegalArgumentException("Invalid elem=" + elem);
		}
		
		WordMLDocument doc = (WordMLDocument) elem.getDocument();
		int offset = elem.getStartOffset() + atIndex;
		if (offset <= elem.getStartOffset()
			|| elem.getEndOffset() <= offset) {
			throw new IllegalArgumentException("Invalid atIndex=" + atIndex);
		}
		
		int length = elem.getEndOffset() - offset;
		TextSelector ts = null;
		try {
			ts = new TextSelector(doc, offset, length);
		} catch (BadSelectionException exc) {
			throw new IllegalArgumentException("Unable to split elem=" + elem);
		}
		
		List<ElementML> deletedElementMLs = null;
		
		List<DocumentElement> list = ts.getDocumentElements();
		//Check first element
    	DocumentElement tempE = list.get(0);
    	if (tempE.isLeaf() && tempE.getStartOffset() < offset) {
    		//Split into two RunContentMLs
    		RunContentML leftML = (RunContentML) tempE.getElementML();
    		RunContentML rightML = (RunContentML) leftML.clone();
    		
    		if (!leftML.isDummy()) {
				try {
					int start = tempE.getStartOffset();
					length = tempE.getEndOffset() - start;
					String text = doc.getText(start, length);
					String left = text.substring(0, offset - start);
					String right = text.substring(offset - start);

					leftML.setTextContent(left);
					rightML.setTextContent(right);
				} catch (BadLocationException exc) {
					;// ignore
				}
			}
			// Prevent leftML from being deleted
			list.remove(0);
			deletedElementMLs = DocUtil.deleteElementML(list);
			
			// Include rightML as a deleted ElementML
			deletedElementMLs.add(0, rightML);
		} else {
			deletedElementMLs = DocUtil.deleteElementML(list);
		}
    	list = null;
    	
    	ElementML newSibling = null;
    	
    	if (elemML instanceof ParagraphML) {
        	ParagraphML paraML = (ParagraphML) elemML;
        	ParagraphPropertiesML pPr =
        		(ParagraphPropertiesML) paraML.getParagraphProperties();
        	if (pPr != null) {
        		pPr = (ParagraphPropertiesML) pPr.clone();
        	}
        	
        	DocumentElement runE = 
        		(DocumentElement) doc.getRunMLElement(offset);
        	RunML runML = (RunML) runE.getElementML();
        	RunPropertiesML rPr = 
        		(RunPropertiesML) runML.getRunProperties();
        	if (rPr != null) {
        		rPr = (RunPropertiesML) rPr.clone();
        	}
        	
        	newSibling = ElementMLFactory.createParagraphML(deletedElementMLs, pPr, rPr);
        	
    	} else if (elemML instanceof RunML) {
    		RunML runML = (RunML) elemML;
        	RunPropertiesML rPr = 
        		(RunPropertiesML) runML.getRunProperties();
        	if (rPr != null) {
        		rPr = (RunPropertiesML) rPr.clone();
        	}
        	
        	newSibling = ElementMLFactory.createRunML(deletedElementMLs, rPr);
        	
    	} else {
    		//must be a RunContentML
    		newSibling = deletedElementMLs.get(0);
    	}
    	
    	elemML.addSibling(newSibling, true);
    	
    	return newSibling;
	}
	
	public final static List<ElementML> deleteElementML(List<DocumentElement> list) {
		List<ElementML> deletedElementMLs = new ArrayList<ElementML>(list.size());
		
    	for (int i=0; i < list.size(); i++) {
    		DocumentElement tempE = (DocumentElement) list.get(i);
    		if (log.isDebugEnabled()) {
    			log.debug("deleteElementML(): elem[" + i + "]=" + tempE);
    		}
    		ElementML ml = tempE.getElementML();
    		ml.delete();
    		deletedElementMLs.add(ml);
    	}
    	
    	return deletedElementMLs;
	}

	public final static List<ElementSpec> getElementSpecs(ElementML elem) {
		ElementMLIterator parser = new ElementMLIterator(elem);
		ElementMLIteratorCallback result = new ElementMLIteratorCallback();
		parser.cruise(result);
		return result.getElementSpecs();
	}

	public final static List<String> getElementNamePath(DocumentElement elem, int pos) {
		List<String> thePath = null;
		if (elem.getStartOffset() <= pos && pos < elem.getEndOffset()) {
			thePath = new ArrayList<String>();
			String name = elem.getElementML().getClass().getSimpleName();
			thePath.add(name);
			while (!elem.isLeaf()) {
				int idx = elem.getElementIndex(pos);
				elem = (DocumentElement) elem.getElement(idx);
				name = elem.getElementML().getClass().getSimpleName();
				thePath.add(name);
			}
		}
		return thePath;
	}
	
	public final static DocumentElement getCommonParentElement(
		DocumentElement elem1, 
		DocumentElement elem2) {

		if (elem1.getDocument() != elem2.getDocument()) {
			throw new IllegalArgumentException("Elements belong to two different documents");
		}
		
		List<Element> path = new ArrayList<Element>();
		Element temp = elem1;
		while (temp != null) {
			path.add(temp);
			temp = temp.getParentElement();
		}
		
		temp = elem2;
		while (temp != null && path.indexOf(temp) == -1) {
			temp = temp.getParentElement();
		}
		
		return (DocumentElement) temp;
	}
	
	public final static void displayXml(Document doc) {
		org.docx4j.wml.Document jaxbDoc = null;
	
		if (doc instanceof WordMLDocument) {
			DocumentElement root = (DocumentElement) doc
					.getDefaultRootElement();

			jaxbDoc = (org.docx4j.wml.Document) root.getElementML()
					.getDocxObject();
		} else {
			WordprocessingMLPackage wmlPackage =
				(WordprocessingMLPackage) doc.getProperty(
						WordMLDocument.WML_PACKAGE_PROPERTY);
			jaxbDoc = (org.docx4j.wml.Document) 
				wmlPackage.getMainDocumentPart().getJaxbElement();
		}
		
		List<Object> list = jaxbDoc.getBody().getEGBlockLevelElts();
		int i = 0;
		for (Object obj : list) {
			String s = XmlUtils.marshaltoString(obj, true);
			log.debug("displayXml(): BodyChild[" + i + "]=" + s);
			i++;
		}
	}
	
    public final static void displayStructure(Document doc) {
          Element e = doc.getDefaultRootElement();
          displayStructure(doc, e, 0);
    }

    public final static void displayStructure(Document doc, Element elem, int numberOfTabs) {
    	String leftMargin = getTabSpace(numberOfTabs);
    	
    	//====== Display Element class name ======
		StringBuffer sb = new StringBuffer(leftMargin);
		sb.append("===== Element Class: ");
		sb.append(elem.getClass().getSimpleName());
		log.debug(sb);

		//====== Display the Element offset position ======
		int startOffset = elem.getStartOffset();
		int endOffset = elem.getEndOffset();
		sb = new StringBuffer(leftMargin);
		sb.append("Offsets [");
		sb.append(startOffset);
		sb.append(", ");
		sb.append(endOffset);
		sb.append("]");
		log.debug(sb);

		//====== Display the Element Attributes ======
		AttributeSet attr = elem.getAttributes();
		Enumeration<?> nameEnum = attr.getAttributeNames();

		sb = new StringBuffer(leftMargin);
		sb.append("ATTRIBUTES:");
		log.debug(sb);

		while (nameEnum.hasMoreElements()) {
			sb = new StringBuffer(leftMargin);
			Object attrName = nameEnum.nextElement();
			sb.append(" (" + attrName + ", " + attr.getAttribute(attrName) + ")");
			log.debug(sb);
		}

		//====== Display text content for a leaf element ======
		if (elem.isLeaf()) {
			sb = new StringBuffer(leftMargin);
			try {
				String text = doc.getText(startOffset, endOffset - startOffset);
				if (text.length() > 25) {
					text = text.substring(0, 25);
				}
				sb.append("[");
				int lf = text.indexOf(Constants.NEWLINE);
				if (lf >= 0) {
					sb.append(text.substring(0, lf));
					sb.append("<<NEWLINE>>");
					sb.append(text.substring(lf + 1));
				} else {
					sb.append(text);
				}
				sb.append("]");
				log.debug(sb);
			} catch (BadLocationException ex) {
			}
		}

		//====== Display child elements ======
		int count = elem.getElementCount();
		for (int i = 0; i < count; i++) {
			displayStructure(doc, elem.getElement(i), numberOfTabs + 1);
		}
	}

    public final static void displayStructure(List<ElementSpec> list) {
		int depth = -1;

		for (int i = 0; i < list.size(); i++) {
			ElementSpec es = list.get(i);
			StringBuffer info = new StringBuffer();
			
			ElementML elemML = 
				(es.getAttributes() != null)
					? WordMLStyleConstants.getElementML(es.getAttributes())
					: null;
			if (es.getType() == ElementSpec.StartTagType) {
				if (elemML == null) {
					info.append(getTabSpace(++depth));
					info.append("OPEN <NULL> - ...");
				} else {
					info.append(getTabSpace(++depth));
					info.append("OPEN <");
					info.append(elemML.getTag());
					info.append("> - ");
					info.append(elemML.toString());
				}
			} else if (es.getType() == ElementSpec.ContentType) {
				if (elemML == null) {
					info.append(getTabSpace(depth + 1));
					info.append("TEXT - RunContentML=NULL [...]");
				} else {
					String text = ((RunContentML) elemML).getTextContent();
					if (text.length() > 25) {
						text = text.substring(0, 25);
					}
					
					StringBuffer sb = new StringBuffer();
					int lf = text.indexOf(Constants.NEWLINE);
					if (lf >= 0) {
						sb.append(text.substring(0, lf));
						sb.append("<<NEWLINE>>");
						sb.append(text.substring(lf + 1));
					} else {
						sb.append(text);
					}
					
					info.append(getTabSpace(depth + 1));
					info.append("TEXT - ");
					info.append(elemML.toString());
					info.append("[");
					info.append(sb.toString());
					info.append("]");
				}				
			} else {
				if (elemML == null) {
					info.append(getTabSpace(depth--));
					info.append("CLOSE <NULL> - ...");
				} else {
					info.append(getTabSpace(depth--));
					info.append("CLOSE <");
					info.append(elemML.getTag());
					info.append("> - ");
					info.append(elemML.toString());
				}
				depth = Math.max(depth, -1);
			}
			log.debug(info.toString());
		}
	}

    private final static String getTabSpace(int numberOfTabs) {
		StringBuffer theSpace = new StringBuffer();
		for (int i = 0; i < numberOfTabs; i++) {
			theSpace.append(TAB);
		}
		return theSpace.toString();
    }

	private DocUtil() {
		;//uninstantiable
	}
	
}// DocUtil class
























