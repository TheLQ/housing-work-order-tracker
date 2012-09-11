/**
 * Copyright (C) 2012 Leon Blakey <lord.quackstar at gmail.com>, RATS 2012
 *
 * This file is part of University of Louisville Housing WorkOrder System.
 */
package org.thelq.housing.wo.wicket;

import com.google.gdata.client.spreadsheet.SpreadsheetService;
import com.google.gdata.data.spreadsheet.CustomElementCollection;
import com.google.gdata.data.spreadsheet.ListEntry;
import com.google.gdata.data.spreadsheet.ListFeed;
import com.google.gdata.util.ServiceException;
import java.io.IOException;
import java.net.MalformedURLException;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Main home page with form
 * @author Leon Blakey <lord.quackstar at gmail.com>
 */
public class HomePage extends WebPage {
	private static final Logger log = LoggerFactory.getLogger(HomePage.class);

	public HomePage() throws Exception {
		log.debug("Started loading home page");
		buildForm();
		log.debug("Finished loading page!");
	}
	
	public void buildForm() throws MalformedURLException, ServiceException, IOException {
		Spreadsheet.UIData uidata = ((GaeWicketApplication)GaeWicketApplication.get()).getSpreadsheet().loadUI();
		add(new Label("hello", "Hello World"));
		add(new DropDownChoice("building", uidata.getBuildings()));

		//Generate issue drop down
		List<String> combinedIssues = new ArrayList();
		for (Map.Entry<String, List<String>> curEntry : uidata.getIssues().entrySet())
			for (String curIssue : curEntry.getValue())
				combinedIssues.add(curEntry.getKey() + " - " + curIssue);
		add(new DropDownChoice("issues", combinedIssues));
	}

}
