/**
 * Copyright (C) 2012 Leon Blakey <lord.quackstar at gmail.com>, RATS 2012
 *
 * This file is part of Housing WorkOrder System.
 */
package org.thelq.housing.wo.wicket;

import org.apache.wicket.markup.html.WebPage;
import org.apache.wicket.markup.html.basic.Label;

/** Page is responsible of
 * @author rhansen@kindleit.net
 *
 */
public class HomePage extends WebPage {
	public HomePage() {
		add(new Label("hello", "Hello World"));
	}
}
