/*
 * Copyright (C) 2018 INFORMATION SERVICES INTERNATIONAL - DENTSU, LTD. All Rights Reserved.
 * 
 * Unless you have purchased a commercial license,
 * the following license terms apply:
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 * 
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 */
package org.iplass.mtp.auth.login.token;

import org.iplass.mtp.auth.token.AuthTokenInfo;

public class SimpleAuthTokenInfo implements AuthTokenInfo {
	
	private String type;
	private String key;
	private String application;
	
	public SimpleAuthTokenInfo() {
	}

	public SimpleAuthTokenInfo(String type, String application) {
		this.type = type;
		this.application = application;
	}
	
	@Override
	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}
	
	@Override
	public String getKey() {
		return key;
	}
	
	public void setKey(String key) {
		this.key = key;
	}

	public String getApplication() {
		return application;
	}

	public void setApplication(String application) {
		this.application = application;
	}
	
	@Override
	public String getDescription() {
		//TODO 多言語化
		StringBuilder sb = new StringBuilder();
		sb.append("Simple Persistant Auth Token for ");
		sb.append(application);
		return sb.toString();
	}

}
