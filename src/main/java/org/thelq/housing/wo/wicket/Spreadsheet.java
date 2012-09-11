/**
 * Copyright (C) 2012 Leon Blakey <lord.quackstar at gmail.com>, RATS 2012
 *
 * This file is part of University of Louisville Housing WorkOrder System.
 */
/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.thelq.housing.wo.wicket;

import com.google.gdata.client.spreadsheet.SpreadsheetService;
import com.google.gdata.data.spreadsheet.CustomElementCollection;
import com.google.gdata.data.spreadsheet.ListEntry;
import com.google.gdata.data.spreadsheet.ListFeed;
import com.google.gdata.util.AuthenticationException;
import com.google.gdata.util.ServiceException;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import lombok.Data;
import lombok.Getter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author Leon Blakey <lord.quackstar at gmail.com>
 */
public class Spreadsheet {
	private static final Logger log = LoggerFactory.getLogger(Spreadsheet.class);
	/**
	 * Spreadsheet feed key, NOT document key
	 */
	protected static final String url_raw = "https://spreadsheets.google.com/feeds/list/t280PyPU5dOYJ_ref3fX38Q/od6/private/full";
	protected static final String url_ui = "https://spreadsheets.google.com/feeds/list/t280PyPU5dOYJ_ref3fX38Q/od7/private/full";
	protected SpreadsheetService ssService;
	protected final String user;
	protected final String pass;

	public Spreadsheet() throws IOException, AuthenticationException {
		Properties userProp = new Properties();
		userProp.load(this.getClass().getClassLoader().getResourceAsStream("creds.properties"));
		user = userProp.getProperty("user");
		pass = userProp.getProperty("pass");
		//Load the Spreadsheet service
		ssService = new SpreadsheetService("UofL-Workorder");
		ssService.setUserCredentials(user, pass);
	}

	public UIData loadUI() throws MalformedURLException, ServiceException, IOException {
		UIData data = new UIData();
		//Start loading stuff from the UI
		ListFeed listFeed = ssService.getFeed(new URL(url_ui), ListFeed.class);
		for (ListEntry row : listFeed.getEntries()) {
			CustomElementCollection rowData = row.getCustomElements();

			//Parse each column in row
			for (String columnName : rowData.getTags())
				if (rowData.getValue(columnName) == null)
					continue;
				else if (columnName.equalsIgnoreCase("Buildings"))
					data.getBuildings().add(rowData.getValue(columnName));
				else {
					//Must be issue ui data
					if (data.getIssues().get(columnName) == null)
						data.getIssues().put(columnName, new ArrayList());
					data.getIssues().get(columnName).add(rowData.getValue(columnName));
				}
		}
		log.debug("Loaded " + data.getBuildings().size() + " buildings and " + data.getIssues().size() + " issue types ");
		return data;
	}

	@Data
	public static class UIData {
		protected Map<String, List<String>> issues = new LinkedHashMap<String, List<String>>();
		protected List<String> buildings = new ArrayList();
	}
}
