package org.iplass.mtp.webhook.template.definition;

import java.io.Serializable;

public class WebHookContent implements Serializable {

	private static final long serialVersionUID = -344879145295664096L;

	//主にJSONが流行らしい、できれば要求された以外、JSONをおすすめします
	public enum webHookContentType{
			JSON,
			XML,
			FORM,
			TEXTPLAIN,
			MULTIPART
	}
	
	/** webHook 内容のタイプ */
	private webHookContentType contentType;
	private String charset;
	
	/** String content container */
	private String content;
	
	public WebHookContent() {
		this.contentType = webHookContentType.TEXTPLAIN;
		this.content = "";
	}
	
	public WebHookContent(String content, String type, String charset) {
		setContent(content);
		setContentTypeString(type);
		setCharset(charset);
	}
	
	public WebHookContent(String content, webHookContentType type, String charset) {
		setContent(content);
		setContentType(type);
		setCharset(charset);
	}
	
	public void setContent (String content) {
		this.content = content;
	}
	
	public void setContentTypeString (String type) {
		type = type.toLowerCase();
		if (type.equals("json")) {
			this.contentType = webHookContentType.JSON;
		}
		
		if (type.equals("xml")) {
			this.contentType = webHookContentType.XML;
		}
		
		if (type.equals("plain")) {
			this.contentType = webHookContentType.TEXTPLAIN;
		}
		
		if (type.equals("form")) {
			this.contentType = webHookContentType.FORM;
		}
		
		if (type.equals("multipart")) {
			this.contentType = webHookContentType.MULTIPART;
		}
	}
	
	public String getContent() {
		return this.content;
	}
	
	public String getContentTypeString() {
		switch(this.contentType) {
		case JSON :
			return "application/json";
		case XML :
			return "application/xml";
		case TEXTPLAIN :
			return "text/plain";
		case FORM :
			return "application/x-www-form-urlencoded";
		case MULTIPART :
			return "multipart/form-data";
		default : 
			return "text/plain";
		
		
		}
			
	}
	
	public webHookContentType getContentType() {
		return contentType;
	}

	public String getCharset() {
		return charset;
	}

	public void setCharset(String charset) {
		this.charset = charset;
	}

	public void setContentType(webHookContentType contentType) {
		this.contentType = contentType;
	}

}
