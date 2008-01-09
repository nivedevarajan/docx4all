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

import javax.xml.bind.JAXBElement;

import org.apache.log4j.Logger;
import org.docx4all.util.XmlUtil;
import org.docx4j.XmlUtils;
import org.docx4j.jaxb.document.RPr;

/**
 *	@author Jojada Tirtowidjojo - 30/11/2007
 */
public class RunML extends ElementML {
	private static Logger log = Logger.getLogger(RunML.class);
	
	private PropertiesContainerML rPr;
	
	public RunML(Object docxObject) {
		this(docxObject, false);
	}
	
	public RunML(Object docxObject, boolean isDummy) {
		super(docxObject, isDummy);
	}
	
	/**
	 * Gets the run property element of this run element.
	 * 
	 * @return a RunPropertiesML, if any
	 *         null, otherwise 
	 */
	public PropertiesContainerML getRunProperties() {
		return this.rPr;
	}
	
	public Object clone() {
		Object obj = null;
		if (this.docxObject != null) {
			obj = XmlUtils.deepCopy(this.docxObject);
		}
		return new RunML(obj);
	}
	
	public boolean canAddChild(int idx, ElementML child) {
		boolean canAdd = true;
		
		if (!(child instanceof RunContentML)) {
			canAdd = false;
		} else if (this.children == null) {
			canAdd = (idx == 0);
		}
		
		return canAdd;
	}
	
	public void addChild(int idx, ElementML child) {
		if (!(child instanceof RunContentML)) {
			throw new IllegalArgumentException("NOT a RunContentML");
		}
		super.addChild(idx, child);
	}
		
	public void setParent(ElementML parent) {
		if (!(parent instanceof ParagraphML)) {
			throw new IllegalArgumentException("NOT a ParagraphML.");
		}
		this.parent = parent;
	}
	
	public void setDocxParent(Object docxParent) {
		if (this.docxObject == null) {
			;//do nothing
		} else if (this.docxObject instanceof org.docx4j.jaxb.document.R) {
			org.docx4j.jaxb.document.R run = 
				(org.docx4j.jaxb.document.R) this.docxObject;
			run.setParent(docxParent);
			
		} else if (this.docxObject instanceof JAXBElement) {
			JAXBElement<?> jaxbElem = (JAXBElement<?>) this.docxObject;
			String typeName = jaxbElem.getDeclaredType().getName();

			if ("org.docx4j.jaxb.document.RunTrackChange".equals(typeName)) {
				org.docx4j.jaxb.document.RunTrackChange rtc =
					(org.docx4j.jaxb.document.RunTrackChange) jaxbElem.getValue();
				rtc.setParent(docxParent);
			} else {
				throw new IllegalArgumentException(
						"Unsupported Docx Object Type = " + typeName);
			}
		} else {
			;//should not come here. See init().
		}
	}// setDocxParent()
	
	protected List<Object> getDocxChildren() {
		List<Object> theChildren = null;
		
		if (this.docxObject == null) {
			;//do nothing
		} else if (this.docxObject instanceof org.docx4j.jaxb.document.R) {
			org.docx4j.jaxb.document.R run = 
				(org.docx4j.jaxb.document.R) this.docxObject;
			theChildren = run.getRunContent();
			
		} else if (this.docxObject instanceof JAXBElement) {
			JAXBElement<?> jaxbElem = (JAXBElement<?>) this.docxObject;
			String typeName = jaxbElem.getDeclaredType().getName();

			if ("org.docx4j.jaxb.document.RunTrackChange".equals(typeName)) {
				org.docx4j.jaxb.document.RunTrackChange rtc =
					(org.docx4j.jaxb.document.RunTrackChange) jaxbElem.getValue();
				theChildren = rtc.getEGContentRunContent();
			} else {
				throw new IllegalArgumentException(
						"Unsupported Docx Object Type = " + typeName);
			}
		} else {
			;//should not come here. See init().
		}

		return theChildren;
	}
	
	protected void init(Object docxObject) {
		org.docx4j.jaxb.document.R run = null;
		
		if (docxObject == null) {
			;//implied RunML
			
		} else if (docxObject instanceof org.docx4j.jaxb.document.R) {
			run = (org.docx4j.jaxb.document.R) docxObject;
			this.isDummy = false;
			
		} else if (docxObject instanceof JAXBElement<?>) {
			JAXBElement<?> jaxbElem = (JAXBElement<?>) docxObject;
			//Create a dummy RunML for this unsupported element
			// TODO: A more informative text content in dummy RunML
			String renderedText = 
				XmlUtil.getEnclosingTagPair(jaxbElem.getName());
			run = ObjectFactory.createR(renderedText);
			this.isDummy = true;
			
		} else {
			throw new IllegalArgumentException("Unsupported Docx Object = " + docxObject);			
		}
		
		initRunProperties(run);
		initChildren(run);
	}
	
	private void initRunProperties(org.docx4j.jaxb.document.R run) {
		this.rPr = null;
		if (run != null) {
			RPr rPr = run.getRPr();
			if (rPr != null) {
				this.rPr = new RunPropertiesML(rPr);
			}
		}
	}
	
	private void initChildren(org.docx4j.jaxb.document.R run) {
		this.children = null;

		if (run == null) {
			return;
		}

		List<Object> rKids = run.getRunContent();
		if (!rKids.isEmpty()) {
			this.children = new ArrayList<ElementML>(rKids.size());
			for (Object o : rKids) {
				RunContentML child = new RunContentML(o, this.isDummy);
				child.setParent(RunML.this);
				this.children.add(child);
			}
		}
	}// initChildren()
	
}// RunML class





















