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
		log.info("Logging in...");
		Spreadsheet spreadsheet = new Spreadsheet();
		log.info("Grabbing feed...");
		ListFeed listFeed = spreadsheet.ssService.getFeed(new URL(Spreadsheet.url_raw), ListFeed.class);

		//Pass to different methods to work on data
		List<ListEntry> enteries = new ArrayList(listFeed.getEntries());
		convertDateToNew(enteries);
		convertDateToCounter(enteries);
		
		//Update
		int counter = 0;
		for(ListEntry curEntry : enteries) {
			System.out.println("Updating row " + (++counter) + " of " + enteries.size());
			curEntry.update();
		}
	}

	public static void convertDateToNew(List<ListEntry> enteries) throws MalformedURLException, ParseException, IOException, ServiceException {
		System.out.println("Converting dates to new format...");
		for (ListEntry row : enteries) {
			CustomElementCollection rowData = row.getCustomElements();

			//Convert dates
			SimpleDateFormat formatterOld = Spreadsheet.getOldDateFormat(); //new SimpleDateFormat("MMMMMMMMMM dd, yyyy hh:mm:ss aa zzz");
			SimpleDateFormat formatterNew = Spreadsheet.getNewDateFormat(); //new SimpleDateFormat("MMM dd yyyy, hh:mm aa");
			Date oldOpenDate = getDateFromFormat(formatterNew, formatterOld, rowData.getValue("opened"));
			rowData.setValueLocal("opened", formatterNew.format(oldOpenDate));
			String oldClosedString = rowData.getValue("closed");
			if (StringUtils.isNotEmpty(oldClosedString)) {
				Date oldClosedDate = getDateFromFormat(formatterNew, formatterOld, rowData.getValue("closed"));
				rowData.setValueLocal("closed", formatterNew.format(oldClosedDate));
			}
		}
	}

	protected static Date getDateFromFormat(SimpleDateFormat format1, SimpleDateFormat format2, String dateSTring) throws ParseException {
		try {
			return format1.parse(dateSTring);
		} catch (ParseException e) {
			return format2.parse(dateSTring);
		}
	}

	public static void convertDateToCounter(List<ListEntry> rows) throws IOException, AuthenticationException, ServiceException, ParseException {
		System.out.println("Converting dates to counter");
		//Load rows into map
		Map<Long, List<ListEntry>> counterMap = new TreeMap();
		for (ListEntry row : rows) {
			CustomElementCollection rowData = row.getCustomElements();

			//Convert dates
			SimpleDateFormat formatterOld = Spreadsheet.getOldDateFormat();
			SimpleDateFormat formatterNew = Spreadsheet.getNewDateFormat();
			Date openDate = getDateFromFormat(formatterNew, formatterOld, rowData.getValue("opened"));

			//Insert into map with timestamp for sorting
			long time = openDate.getTime();
			if (!counterMap.containsKey(time))
				counterMap.put(time, new ArrayList());
			counterMap.get(time).add(row);
		}

		//Now that values are in the sorted TreeMap, add a counter value and update
		int counter = 0;
		for (Map.Entry<Long, List<ListEntry>> curEntry : counterMap.entrySet())
			for (ListEntry curRow : curEntry.getValue()) {
				int curIssueNum = ++counter;
				curRow.getCustomElements().setValueLocal("_df9om", "" + curIssueNum);
			}
	}
}
