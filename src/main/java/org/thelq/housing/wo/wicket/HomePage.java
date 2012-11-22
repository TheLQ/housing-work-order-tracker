/**
 * Copyright (C) 2012 Leon Blakey <lord.quackstar at gmail.com>, RATS 2012
 *
 * This file is part of University of Louisville Housing WorkOrder System.
 */
package org.thelq.housing.wo.wicket;

import com.google.gdata.util.ServiceException;
import java.io.IOException;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.apache.wicket.markup.html.WebPage;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.markup.html.form.IChoiceRenderer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Main home page with form
 *
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
		Spreadsheet.UIData uidata = Spreadsheet.get().loadUI();
		add(new DropDownChoice("building", uidata.getBuildings(), new SameChoiceRenderer()));

		//Handle right side table
		add(new DropDownChoice("existBuilding", uidata.getBuildings(), new SameChoiceRenderer()) {
			@Override
			protected CharSequence getDefaultChoice(String selectedValue) {
				return "\n<option selected=\"selected\" value=\"all\">All</option>";
			}
		});

		//Generate issue drop down
		List<String> combinedIssues = new ArrayList();
		for (Map.Entry<String, List<String>> curEntry : uidata.getIssues().entrySet())
			for (String curIssue : curEntry.getValue())
				combinedIssues.add(curEntry.getKey() + " - " + curIssue);
		add(new DropDownChoice("issue", combinedIssues, new SameChoiceRenderer()));

		//Auto fix select
		add(new DropDownChoice("autoFix", combinedIssues, new SameChoiceRenderer()) {
			@Override
			protected CharSequence getDefaultChoice(String arg0) {
				return "";
			}
		}.setNullValid(true));
	}

	/**
	 * Have both the option value and the displayed value be the same
	 */
	public static class SameChoiceRenderer implements IChoiceRenderer<String> {
		@Override
		public String getIdValue(String object, int index) {
			return object;
		}

		@Override
		public Object getDisplayValue(String object) {
			return object;
		}
	}
}
