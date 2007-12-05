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

import java.util.Enumeration;
import java.util.List;

import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import javax.swing.text.Element;
import javax.swing.text.DefaultStyledDocument.ElementSpec;

import org.apache.log4j.Logger;
import org.docx4all.swing.text.WordMLStyleConstants;
import org.docx4all.xml.ElementML;
import org.docx4all.xml.RunContentML;

/**
 *	@author Jojada Tirtowidjojo - 27/11/2007
 */
public class DocUtil {
	private static Logger log = Logger.getLogger(DocUtil.class);

	private final static String TAB = "    ";
	
    public static void displayStructure(Document doc) {
          Element e = doc.getDefaultRootElement();
          displayStructure(doc, e, 0);
    }

    public static void displayStructure(Document doc, Element elem, int numberOfTabs) {
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
				int lf = text.indexOf('\n');
				if (lf >= 0) {
					sb.append(text.substring(0, lf));
					sb.append("<<LF>>");
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

    public static void displayStructure(List<ElementSpec> list) {
		int depth = -1;

		for (int i = 0; i < list.size(); i++) {
			ElementSpec es = list.get(i);
			StringBuffer info = new StringBuffer();
			
			ElementML elemML = WordMLStyleConstants.getElementML(es.getAttributes());
			if (es.getType() == ElementSpec.StartTagType) {
				info.append(getTabSpace(++depth));
				info.append("OPEN <");
				info.append(elemML.getTag());
				info.append("> - ");
				info.append(elemML.getClass().getSimpleName());
				
			} else if (es.getType() == ElementSpec.ContentType) {
				String text = ((RunContentML) elemML).getText();
				if (text != null && text.length() > 25) {
					text = text.substring(0, 25);
					
					StringBuffer sb = new StringBuffer();
					int lf = text.indexOf('\n');
					if (lf >= 0) {
						sb.append(text.substring(0, lf));
						sb.append("<<LF>>");
						sb.append(text.substring(lf + 1));
					} else {
						sb.append(text);
					}
					text = sb.toString();
				}
				info.append(getTabSpace(depth + 1));
				info.append("TEXT - RunContentML[");
				info.append(text);
				info.append("]");
				
			} else {
				info.append(getTabSpace(depth--));
				info.append("CLOSE <");
				info.append(elemML.getTag());
				info.append("> - ");
				info.append(elemML.getClass().getSimpleName());
			}
			log.debug(info.toString());
		}
	}

    private static String getTabSpace(int numberOfTabs) {
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

























