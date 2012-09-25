/**
 * Copyright (C) 2012 Leon Blakey <lord.quackstar at gmail.com>, RATS 2012
 *
 * This file is part of University of Louisville Housing WorkOrder System.
 */
package org.thelq.housing.wo.wicket;

import lombok.Getter;
import org.apache.wicket.Application;
import org.apache.wicket.Page;
import org.apache.wicket.RuntimeConfigurationType;
import org.apache.wicket.protocol.http.WebApplication;
import org.apache.wicket.request.resource.IResource;
import org.apache.wicket.request.resource.ResourceReference;
import org.apache.wicket.settings.IExceptionSettings;
import org.apache.wicket.util.time.Duration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Wicket init class. Sets some settings
 *
 * @author Leon Blakey <lord.quackstar at gmail.com>
 */
public class GaeWicketApplication extends WebApplication {
	private static final Logger log = LoggerFactory.getLogger(GaeWicketApplication.class);
	@Getter
	protected Spreadsheet spreadsheet;

	@Override
	public Class<? extends Page> getHomePage() {
		return HomePage.class;
	}

	@Override
	protected void init() {
		super.init();

		if (Application.get().getConfigurationType().equals(RuntimeConfigurationType.DEVELOPMENT)) {
			log.info("You are in DEVELOPMENT mode");
			getResourceSettings().setResourcePollFrequency(Duration.ONE_SECOND);
			getDebugSettings().setComponentUseCheck(true);
			getMarkupSettings().setStripWicketTags(true);
			getExceptionSettings().setUnexpectedExceptionDisplay(IExceptionSettings.SHOW_EXCEPTION_PAGE);
		} else if (Application.get().getConfigurationType().equals(RuntimeConfigurationType.DEPLOYMENT)) {
			log.info("Production mode enabled");
			getResourceSettings().setResourcePollFrequency(null);
			getDebugSettings().setComponentUseCheck(false);
			getMarkupSettings().setStripWicketTags(true);
			getExceptionSettings().setUnexpectedExceptionDisplay(IExceptionSettings.SHOW_EXCEPTION_PAGE);
		} else
			throw new RuntimeException("Unknown application config " + Application.get().getConfigurationType());

		//Explicity disable resource poll frequency regardless of mode if on google app engine
		if (System.getProperty("com.google.appengine.runtime.environment") != null) {
			getResourceSettings().setResourcePollFrequency(null);
			log.info("Disabled poll frequency due to being on google app engine");
		}

		//Handle URL mapping
		mountResource("/processData", new ResourceReference(ProcessData.class, "processData") {
			@Override
			public IResource getResource() {
				return new ProcessData();
			}
		});

		try {
			//Global Spreadsheet instance
			spreadsheet = new Spreadsheet();
		} catch (Exception ex) {
			throw new RuntimeException("Can't load spreadsheet", ex);
		}
	}
}
