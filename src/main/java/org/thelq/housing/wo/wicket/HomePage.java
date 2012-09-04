/**
 * Copyright (C) 2012 Leon Blakey <lord.quackstar at gmail.com>, RATS 2012
 *
 * This file is part of Housing WorkOrder System.
 */
package org.thelq.housing.wo.wicket;

import com.google.gdata.client.spreadsheet.SpreadsheetService;
import com.google.gdata.data.spreadsheet.ListEntry;
import com.google.gdata.data.spreadsheet.ListFeed;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import org.apache.wicket.markup.html.WebPage;
import org.apache.wicket.markup.html.basic.Label;

/** Page is responsible of
 * @author rhansen@kindleit.net
 *
 */
public class HomePage extends WebPage {
	/**
	 * Spreadsheet feed key, NOT document key
	 */
	protected static String sheet_key = "t280PyPU5dOYJ_ref3fX38Q";
	protected static String url_raw = "https://spreadsheets.google.com/feeds/list/t280PyPU5dOYJ_ref3fX38Q/od6/private/full";
	protected static String url_ui = "https://spreadsheets.google.com/feeds/list/t280PyPU5dOYJ_ref3fX38Q/od7/private/full";


	public HomePage() {
		add(new Label("hello", "Hello World" + loadSpreadsheet()));
	}

	public String loadSpreadsheet() {
		try {
			//Load the Spreadsheet service
			SpreadsheetService ssService = new SpreadsheetService("UofL-Workorder");
			Properties userProp = new Properties();
			userProp.load(this.getClass().getClassLoader().getResourceAsStream("creds.properties"));
			ssService.setUserCredentials(userProp.getProperty("user"), userProp.getProperty("pass"));

			//Start loading stuff from the UI
			ListFeed listFeed = ssService.getFeed(new URL(url_ui), ListFeed.class);
			StringBuilder builder = new StringBuilder();
			for (ListEntry row : listFeed.getEntries()) {
				// Print the first column's cell value
				builder.append(row.getTitle().getPlainText()).append("dd\t");
				// Iterate over the remaining columns, and print each cell value
				for (String tag : row.getCustomElements().getTags())
					builder.append(row.getCustomElements().getValue(tag)).append("ff\t");
				builder.append("\n");
			}
			return builder.toString();
		} catch (Exception ex) {
			throw new RuntimeException("Can't load spreadsheet", ex);
		}
	}
}
