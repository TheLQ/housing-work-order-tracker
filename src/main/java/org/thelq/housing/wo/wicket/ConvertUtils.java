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
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.commons.lang3.StringUtils;
import org.joda.time.DateMidnight;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.joda.time.format.DateTimeFormatterBuilder;
import org.joda.time.format.DateTimeParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Utilities for converting the sheet from the old format to the new format
 * <p/>
 * @author Leon Blakey <lord.quackstar at gmail.com>
 */
public class ConvertUtils {
	private static Logger log = LoggerFactory.getLogger(ConvertUtils.class);
	protected static DateTimeFormatter formatter;

	public static void main(String[] args) throws MalformedURLException, ParseException, IOException, ServiceException {
		//Init
		formatter = new DateTimeFormatterBuilder().append(null, new DateTimeParser[]{
					Spreadsheet.getNewDateFormat().getParser(),
					Spreadsheet.getOldDateFormat().getParser(),
					DateTimeFormat.forPattern("MM/dd/yyyy kk:mm:ss").getParser(),
					DateTimeFormat.forPattern("MM/dd/yyyy").getParser()
				}).toFormatter();


		log.info("Logging in...");
		Spreadsheet spreadsheet = new Spreadsheet("prod_");
		log.info("Grabbing feed...");
		ListFeed listFeed = spreadsheet.ssService.getFeed(new URL(spreadsheet.genRawAddress()), ListFeed.class);

		//Pass to different methods to work on data
		List<ListEntry> enteries = new ArrayList(listFeed.getEntries());
		convertDateToNew(enteries);
		convertDateToCounter(enteries);
		convertNotesToDate(enteries);
		convertToTrimmedStrings(enteries);

		//Update
		int counter = 0;
		for (ListEntry curEntry : enteries) {
			System.out.println("Updating row " + (++counter) + " of " + enteries.size());
			curEntry.update();
		}
	}

	public static void convertDateToNew(List<ListEntry> enteries) throws MalformedURLException, ParseException, IOException, ServiceException {
		System.out.println("Converting dates to new format...");
		for (ListEntry row : enteries) {
			CustomElementCollection rowData = row.getCustomElements();

			//Convert dates
			DateTime oldOpenDate = formatter.parseDateTime(rowData.getValue("opened"));
			rowData.setValueLocal("opened", Spreadsheet.getNewDateFormat().print(oldOpenDate));
			String oldClosedString = rowData.getValue("closed");
			if (StringUtils.isNotEmpty(oldClosedString)) {
				DateTime oldClosedDate = formatter.parseDateTime(rowData.getValue("closed"));
				rowData.setValueLocal("closed", Spreadsheet.getNewDateFormat().print(oldClosedDate));
			}
		}
	}

	public static void convertDateToCounter(List<ListEntry> rows) throws IOException, AuthenticationException, ServiceException, ParseException {
		System.out.println("Converting dates to counter");
		//Load rows into map
		Map<Long, List<ListEntry>> counterMap = new TreeMap();
		for (ListEntry row : rows) {
			CustomElementCollection rowData = row.getCustomElements();

			//Convert dates
			DateTime openDate = formatter.parseDateTime(rowData.getValue("opened"));

			//Insert into map with timestamp for sorting
			long time = openDate.getMillis();
			if (!counterMap.containsKey(time))
				counterMap.put(time, new ArrayList());
			counterMap.get(time).add(row);
		}

		//Now that values are in the sorted TreeMap, add a counter value and update
		int counter = 0;
		for (Map.Entry<Long, List<ListEntry>> curEntry : counterMap.entrySet())
			for (ListEntry curRow : curEntry.getValue()) {
				int curIssueNum = ++counter;
				curRow.getCustomElements().setValueLocal("id", "" + curIssueNum);
			}
	}

	public static void convertNotesToDate(List<ListEntry> rows) {
		System.out.println("Converting notes to notes dates");
		Pattern dateRegex = Pattern.compile("[0-9]{1,2}");
		for (ListEntry row : rows) {
			CustomElementCollection rowData = row.getCustomElements();

			//Loop over notes
			for (int i = 1; i <= 10; i++) {
				//Get our date
				String note = StringUtils.defaultString(rowData.getValue("notes" + i));
				String[] notesParts = StringUtils.split(note, " ", 2);
				DateMidnight noteDate = new DateMidnight();
				if (StringUtils.isBlank(note))
					//Ignore as there's no note
					continue;
				else if (notesParts[0].matches(".*[0-9].*")) {
					//Extract first 2 numbers, assum first is month and second is day
					Matcher matcher = dateRegex.matcher(notesParts[0]);
					matcher.find();
					int month = Integer.parseInt(matcher.group());
					matcher.find();
					int day = Integer.parseInt(matcher.group());

					//And with the magic of Joda time
					noteDate = new DateMidnight(2012, month, day);
					note = notesParts[1].trim();
				}

				//Set date and value
				rowData.setValueLocal("notes" + i + "date", Spreadsheet.getNewDateFormat().print(noteDate));
				rowData.setValueLocal("notes" + i, note);
			}
		}
	}

	public static void convertToTrimmedStrings(List<ListEntry> rows) {
		for (ListEntry row : rows) {
			CustomElementCollection rowData = row.getCustomElements();
			for (String columnName : rowData.getTags())
				//Trim all values (use StringUtils since it handles null values)
				rowData.setValueLocal(columnName, StringUtils.trimToEmpty(rowData.getValue(columnName)));
		}
	}
}
