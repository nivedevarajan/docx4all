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

package org.docx4all.xml;

import java.util.ArrayList;
import java.util.List;

import javax.swing.text.AttributeSet;
import javax.xml.bind.JAXBIntrospector;
import javax.xml.namespace.QName;

import org.apache.log4j.Logger;
import org.docx4all.util.XmlUtil;
import org.docx4j.XmlUtils;
import org.docx4j.jaxb.Context;
import org.docx4j.wml.PPr;

/**
 *	@author Jojada Tirtowidjojo - 30/11/2007
 */
public class ParagraphML extends ElementML {
	private static Logger log = Logger.getLogger(ParagraphML.class);

	private ParagraphPropertiesML pPr;
	
	public ParagraphML(Object docxObject) {
		this(docxObject, false);
	}
	
	public ParagraphML(Object docxObject, boolean isDummy) {
		super(docxObject, isDummy);
	}
	
    public void addAttributes(AttributeSet attrs, boolean replace) {
		if (this.pPr == null) {
			if (attrs.getAttributeCount() > 0) {
				ParagraphPropertiesML ml = ElementMLFactory.createParagraphPropertiesML(attrs);
				setParagraphProperties(ml);
			}
		} else {
			if (replace) {
				this.pPr.removeAttributes(attrs);							
			}
			this.pPr.addAttributes(attrs);
			this.pPr.save();
		}
    }

	/**
	 * Gets the paragraph property element of this paragraph.
	 * 
	 * @return a ParagraphPropertiesML, if any
	 *         null, otherwise 
	 */
	public PropertiesContainerML getParagraphProperties() {
		return this.pPr;
	}
	
	public void setParagraphProperties(ParagraphPropertiesML pPr) {
		if (pPr != null && pPr.getParent() != null) {
			throw new IllegalArgumentException("Not an orphan.");
		}
		
		this.pPr = pPr;
		
		if (this.docxObject instanceof org.docx4j.wml.P) {
			org.docx4j.wml.PPr newDocxPPr = null;
			if (pPr != null) {
				pPr.setParent(ParagraphML.this);
				newDocxPPr = 
					(org.docx4j.wml.PPr) pPr.getDocxObject();
			}
			((org.docx4j.wml.P)this.docxObject).setPPr(newDocxPPr);
			
			if (newDocxPPr != null) {
				newDocxPPr.setParent(docxObject);
			}			
		}
	}
	
	public Object clone() {
		Object obj = null;
		if (this.docxObject != null) {
			obj = XmlUtils.deepCopy(this.docxObject);
		}
		
		return new ParagraphML(obj, this.isDummy);
	}
	
	public boolean canAddChild(int idx, ElementML child) {
		boolean canAdd = true;
		
		if (!(child instanceof RunML)) {
			canAdd = false;
		} else {
			canAdd = super.canAddChild(idx, child);
		}
		
		return canAdd;
	}
	
	public void addChild(int idx, ElementML child, boolean adopt) {
		if (!(child instanceof RunML)) {
			throw new IllegalArgumentException("NOT a RunML");
		}
		if (child.getParent() != null) {
			throw new IllegalArgumentException("Not an orphan.");
		}
		super.addChild(idx, child, adopt);
	}
		
	public void setParent(ElementML parent) {
		if (parent != null && !(parent instanceof BodyML)) {
			throw new IllegalArgumentException("NOT a BodyML.");
		}
		this.parent = parent;
	}
	
	protected List<Object> getDocxChildren() {
		List<Object> theChildren = null;
		
		JAXBIntrospector inspector = Context.jc.createJAXBIntrospector();
		
		if (this.docxObject == null) {
			;//do nothing
			
		} else if (inspector.isElement(docxObject)) {
			Object value = JAXBIntrospector.getValue(docxObject);
			if (value instanceof org.docx4j.wml.P) {
				theChildren = ((org.docx4j.wml.P)value).getParagraphContent();
			
			/*
			} else if (value instanceof org.docx4j.wml.Tbl) {
				org.docx4j.wml.Tbl tbl = (org.docx4j.wml.Tbl) value;
				theChildren = new ArrayList<Object>();
				theChildren.addAll(tbl.getEGRangeMarkupElements());
			
			} else if (value instanceof org.docx4j.wml.SdtBlock) {
				org.docx4j.wml.SdtBlock sdt = 
					(org.docx4j.wml.SdtBlock) value;
				org.docx4j.wml.SdtContentBlock content = sdt.getSdtContent();
				if (content != null) {
					theChildren = new ArrayList<Object>();
					theChildren.add(content);
				}
			*/
			}
		} else {
			;//should not come here. See init().
		}
		
		return theChildren;
	}
	
	protected void init(Object docxObject) {
		
		System.out.print("ParagraphML - init");
		
		org.docx4j.wml.P para = null;
		
		JAXBIntrospector inspector = Context.jc.createJAXBIntrospector();
		
		if (docxObject == null) {
			;//implied ParagraphML
			
		} else if (inspector.isElement(docxObject)) {
			Object value = JAXBIntrospector.getValue(docxObject);
			if (value instanceof org.docx4j.wml.P) {
				para = (org.docx4j.wml.P) value;
				this.isDummy = false;
				
			//} else if (value instanceof org.docx4j.wml.Tbl) {
				//unsupported yet
				
			//} else if (value instanceof org.docx4j.wml.SdtBlock) {
				//unsupported yet
			} else {
				//Create a dummy ParagraphML for this unsupported element
				//TODO: A more informative text content in dummy ParagraphML
				QName name = inspector.getElementName(docxObject);
				String renderedText = XmlUtil.getEnclosingTagPair(name);
				para = ObjectFactory.createP(renderedText);
				this.isDummy = true;
			}
	
		} else {
			throw new IllegalArgumentException("Unsupported Docx Object = " + docxObject);			
		}
		
		initParagraphProperties(para);
		initChildren(para);
	}
	
	private void initParagraphProperties(org.docx4j.wml.P para) {
		this.pPr = null;
		if (para != null) {
			//if not an implied ParagraphML
			PPr pProp = para.getPPr();
			if (pProp != null) {
				this.pPr = new ParagraphPropertiesML(pProp);
				this.pPr.setParent(ParagraphML.this);
			}
		}
	}
	
	private void initChildren(org.docx4j.wml.P para) {
		this.children = null;
		
		if (para == null) {
			return;
		}
		
		List<Object> pKids = para.getParagraphContent();
		if (!pKids.isEmpty()) {
			this.children = new ArrayList<ElementML>(pKids.size());
			for (Object o : pKids) {
				RunML run = new RunML(o, this.isDummy);
				run.setParent(ParagraphML.this);
				this.children.add(run);				
			}
		}
	}// initChildren()
	
}// ParagraphML class





















