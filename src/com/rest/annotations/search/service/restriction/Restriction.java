package com.rest.annotations.search.service.restriction;

import java.lang.annotation.Annotation;

public class Restriction {
	
	private Class<? extends Annotation> annotation;
	private String property;
	private String checkValue;
	private CheckType checkType;
	
	public Restriction( final Class<? extends Annotation> annotation, final String property, final String checkValue, final CheckType checkType ) {
		this.annotation = annotation;
		this.property = property;
		this.checkValue = checkValue;
		this.checkType = checkType;
	}
	
	public Restriction( final Class<? extends Annotation> annotation, final String property, final String checkValue ) {
		this( annotation, property, checkValue, CheckType.EQUAL );
	}
	
	public Class<? extends Annotation> getAnnotation() {
		return annotation;
	}

	public String getProperty() {
		return property;
	}

	public String getCheckValue() {
		return checkValue;
	}

	public CheckType getCheckType() {
		return checkType;
	}
	
	public enum CheckType {
		EQUAL("=="), NOT_EQUAL("!=");
		
		private String text;
		
		private CheckType( final String text ) {
			this.text = text;
		}
		
		@Override
		public String toString() {
			return text;
		}
	}
}
