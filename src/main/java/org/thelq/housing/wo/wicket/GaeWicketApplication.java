/**
 * Copyright (C) 2012 Leon Blakey <lord.quackstar at gmail.com>, RATS 2012
 *
 * This file is part of Housing WorkOrder System.
 */
package org.thelq.housing.wo.wicket;

import org.apache.wicket.Page;
import org.apache.wicket.protocol.http.WebApplication;
import org.apache.wicket.settings.IExceptionSettings;
import org.apache.wicket.util.time.Duration;

public class GaeWicketApplication extends WebApplication {
	@Override
	public Class<? extends Page> getHomePage() {
		return HomePage.class;
	}

	@Override
	protected void init() {
		super.init();
		getResourceSettings().setResourcePollFrequency(Duration.ONE_SECOND);
		getMarkupSettings().setStripWicketTags(false);
		getDebugSettings().setComponentUseCheck(true);
		getExceptionSettings().setUnexpectedExceptionDisplay(IExceptionSettings.SHOW_EXCEPTION_PAGE);
		getMarkupSettings().setStripWicketTags(false);
	}
}
