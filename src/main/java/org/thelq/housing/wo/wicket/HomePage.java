/**
 * Copyright (C) 2012 Leon Blakey <lord.quackstar at gmail.com>, RATS 2012
 *
 * This file is part of Housing WorkOrder System.
 */
package org.thelq.housing.wo.wicket;

import com.google.gdata.client.spreadsheet.SpreadsheetService;
import com.google.gdata.data.spreadsheet.CustomElementCollection;
import com.google.gdata.data.spreadsheet.ListEntry;
import com.google.gdata.data.spreadsheet.ListFeed;
import java.net.URL;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import org.apache.wicket.markup.html.WebPage;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.DropDownChoice;

/** Page is responsible of
 * @author rhansen@kindleit.net
 *
 */
public class HomePage extends WebPage {
	/**
	 * Spreadsheet feed key, NOT document key
	 */
	protected static final String url_raw = "https://spreadsheets.google.com/feeds/list/t280PyPU5dOYJ_ref3fX38Q/od6/private/full";
	protected static final String url_ui = "https://spreadsheets.google.com/feeds/list/t280PyPU5dOYJ_ref3fX38Q/od7/private/full";
	protected SpreadsheetService ssService;
	protected Map<String, List<String>> issueMap = new LinkedHashMap<String, List<String>>();
	protected List<String> buildings = new ArrayList();

	public HomePage() {
		loadSpreadsheet();
		add(new Label("hello", "Hello World"));
		add(new DropDownChoice("building", buildings));
	}

	public void loadSpreadsheet() {
		try {
			//Load the Spreadsheet service
			ssService = new SpreadsheetService("UofL-Workorder");
			Properties userProp = new Properties();
			userProp.load(this.getClass().getClassLoader().getResourceAsStream("creds.properties"));
			ssService.setUserCredentials(userProp.getProperty("user"), userProp.getProperty("pass"));

			//Start loading stuff from the UI
			ListFeed listFeed = ssService.getFeed(new URL(url_ui), ListFeed.class);
			for (ListEntry row : listFeed.getEntries()) {
				CustomElementCollection rowData = row.getCustomElements();

				//Parse each column in row
				for (String columnName : rowData.getTags())
					if (columnName.equalsIgnoreCase("Buildings"))
						buildings.add(rowData.getValue(columnName));
					else {
						//Must be issue ui data
						if (issueMap.get(columnName) == null)
							issueMap.put(columnName, new ArrayList());
						issueMap.get(columnName).add(rowData.getValue(columnName));
					}
			}
		} catch (Exception ex) {
			throw new RuntimeException("Can't load spreadsheet", ex);
		}
	}
}
