/**
 * Copyright (C) 2012 Leon Blakey <lord.quackstar at gmail.com>, RATS 2012
 *
 * This file is part of Housing WorkOrder System.
 */
package org.thelq.housing.wo.wicket;

import org.apache.wicket.markup.html.WebPage;
import org.apache.wicket.markup.html.basic.Label;

/**
 * Handle input
 * @author Leon Blakey <lord.quackstar at gmail.com>
 */
public class ProcessData extends WebPage {
	public ProcessData() {
		add(new Label("test", "Hello World!"));
	}
}
