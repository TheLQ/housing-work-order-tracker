/**
 * Copyright (C) 2012 Leon Blakey <lord.quackstar at gmail.com>, RATS 2012
 *
 * This file is part of University of Louisville Housing WorkOrder System.
 */
package org.thelq.housing.wo.wicket;

import com.google.gdata.util.ServiceException;
import java.io.IOException;
import java.io.Serializable;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import lombok.Data;
import org.apache.wicket.Component;
import org.apache.wicket.MarkupContainer;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.form.AjaxSubmitLink;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.WebPage;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
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
		Spreadsheet.UIData uidata = ((GaeWicketApplication) GaeWicketApplication.get()).getSpreadsheet().loadUI();
		Form form = new Form("mainForm") {
			@Override
			protected void onSubmit() {
				//HomePage.this.onSubmit();
			}
		};
		add(new Label("hello", "Hello World"));
		form.add(new TextField<String>("room"));
		form.add(new DropDownChoice("building", uidata.getBuildings()));

		//Generate issue drop down
		final List<String> combinedIssues = new ArrayList();
		for (Map.Entry<String, List<String>> curEntry : uidata.getIssues().entrySet())
			for (String curIssue : curEntry.getValue())
				combinedIssues.add(curEntry.getKey() + " - " + curIssue);

		//Issue box magic
		final MarkupContainer rowPanel = new WebMarkupContainer("issuesPanel");
		rowPanel.setOutputMarkupId(true);
		form.add(rowPanel);
		ArrayList numIssues = new ArrayList();
		numIssues.add(new Object());
		numIssues.add(new Object());
		numIssues.add(new Object());
		final ListView lv = new ListView("issuesBox", numIssues) {
			@Override
			protected void populateItem(ListItem item) {
				int index = item.getIndex() + 1;

				item.add(new DropDownChoice("issues", combinedIssues));
				item.add(new TextField<String>("note"));
			}
		};
		lv.setReuseItems(true);
		rowPanel.add(lv);
		form.add(new AjaxSubmitLink("addIssue", form) {
			@Override
			public void onSubmit(AjaxRequestTarget target, Form form) {
				lv.getModelObject().add(new Object());
				if (target != null)
					target.add(rowPanel);
			}
		}.setDefaultFormProcessing(false));
		form.add(new AjaxSubmitLink("removeIssue", form) {
			@Override
			public void onSubmit(AjaxRequestTarget target, Form form) {
				//Wicket gets very angry when you remove components, so just hide it (recommended way)
				if (target != null) {
					Component lastObject = target.getComponents().toArray(new Component[0])[target.getComponents().size() - 1];
					lastObject.setVisible(false);
				}

			}
		}.setDefaultFormProcessing(false));

		add(form);
	}
	
	@Data
	public static class FormData implements Serializable {
		protected String building;
		protected String room;
		protected List<FormDataIssueEntry> issues = new ArrayList();
	}
	
	@Data
	public static class FormDataIssueEntry implements Serializable {
		protected boolean exists;
		protected String issue;
		protected String notes;
	}
}
