/**
 * Copyright (C) 2012 Leon Blakey <lord.quackstar at gmail.com>, RATS 2012
 *
 * This file is part of University of Louisville Housing WorkOrder System.
 */
package org.thelq.housing.wo.wicket;

import com.google.gdata.data.spreadsheet.CustomElementCollection;
import com.google.gdata.data.spreadsheet.ListEntry;
import com.google.gdata.data.spreadsheet.ListFeed;
import com.google.gdata.util.AuthenticationException;
import com.google.gdata.util.ServiceException;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Utilities for converting the sheet from the old format to the new format
 * @author Leon Blakey <lord.quackstar at gmail.com>
 */
public class ConvertUtils {
	private static Logger log = LoggerFactory.getLogger(ConvertUtils.class);

	public static void main(String[] args) throws MalformedURLException, ParseException, IOException, ServiceException {
		convertDateToNew();
		convertDateToNumber();
	}

	public static void convertDateToNew() throws MalformedURLException, ParseException, IOException, ServiceException {
		log.info("Logging in...");
		Spreadsheet spreadsheet = new Spreadsheet();
		log.info("Grabbing feed...");
		ListFeed listFeed = spreadsheet.ssService.getFeed(new URL(Spreadsheet.url_raw), ListFeed.class);
		int counter = 0;
		for (ListEntry row : listFeed.getEntries()) {
			CustomElementCollection rowData = row.getCustomElements();
			log.info("Current row: " + ++counter);

			//Convert dates
			SimpleDateFormat formatterOld = new SimpleDateFormat("MMMMMMMMMM dd, yyyy hh:mm:ss aa zzz");
			SimpleDateFormat formatterNew = new SimpleDateFormat("MMM dd yyyy, hh:mm aa");
			Date oldOpenDate = getDateFromFormat(formatterNew, formatterOld, rowData.getValue("opened"));
			rowData.setValueLocal("opened", formatterNew.format(oldOpenDate));
			String oldClosedString = rowData.getValue("closed");
			if (StringUtils.isNotEmpty(oldClosedString)) {
				Date oldClosedDate = getDateFromFormat(formatterNew, formatterOld, rowData.getValue("closed"));
				rowData.setValueLocal("closed", formatterNew.format(oldClosedDate));
			}

			//Update
			row.update();
		}
	}

	protected static Date getDateFromFormat(SimpleDateFormat format1, SimpleDateFormat format2, String dateSTring) throws ParseException {
		try {
			return format1.parse(dateSTring);
		} catch (ParseException e) {
			return format2.parse(dateSTring);
		}
	}

	public static void convertDateToNumber() throws IOException, AuthenticationException, ServiceException, ParseException {
		log.info("Logging in...");
		Spreadsheet spreadsheet = new Spreadsheet();
		log.info("Grabbing feed...");
		ListFeed listFeed = spreadsheet.ssService.getFeed(new URL(Spreadsheet.url_raw), ListFeed.class);

		//Load rows into map
		int counter = 0;
		Map<Long, List<ListEntry>> rows = new TreeMap();
		for (ListEntry row : listFeed.getEntries()) {
			CustomElementCollection rowData = row.getCustomElements();
			log.info("Current initial parse row: " + ++counter);

			//Convert dates
			SimpleDateFormat formatterOld = new SimpleDateFormat("MMMMMMMMMM dd, yyyy hh:mm:ss aa zzz");
			SimpleDateFormat formatterNew = new SimpleDateFormat("MMM dd yyyy, hh:mm aa");
			Date openDate = null;
			try {
				openDate = formatterOld.parse(rowData.getValue("opened"));
			} catch (ParseException e) {
				openDate = formatterNew.parse(rowData.getValue("opened"));
			}

			//Insert into map with timestamp for sorting
			long time = openDate.getTime();
			if (!rows.containsKey(time))
				rows.put(time, new ArrayList());
			rows.get(time).add(row);
		}

		//Now that values are in the sorted TreeMap, add a counter value and update
		counter = 0;
		for (Map.Entry<Long, List<ListEntry>> curEntry : rows.entrySet())
			for (ListEntry curRow : curEntry.getValue()) {
				int curIssueNum = ++counter;
				log.info("Current update issue: " + curIssueNum);
				curRow.getCustomElements().setValueLocal("_df9om", "" + curIssueNum);
				curRow.update();
			}
	}
}
