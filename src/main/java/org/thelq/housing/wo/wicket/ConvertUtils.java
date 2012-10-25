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
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
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
		Spreadsheet spreadsheet = new Spreadsheet("dev_");
		log.info("Grabbing feed...");
		ListFeed listFeed = spreadsheet.ssService.getFeed(new URL(spreadsheet.getUrlRaw()), ListFeed.class);

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
			try {
				return format2.parse(dateSTring);
			} catch (ParseException ex) {
				//Stupid format due to Javascript bug
				return new SimpleDateFormat("MM/dd/yyyy kk:mm:ss").parse(dateSTring);
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
				curRow.getCustomElements().setValueLocal("id", "" + curIssueNum);
			}
	}

	public static void convertNotesToDate(List<ListEntry> rows) {
		System.out.println("Converting notes to notes dates");
		for (ListEntry row : rows) {
			CustomElementCollection rowData = row.getCustomElements();

			//Loop over notes
			Pattern dateRegex = Pattern.compile("[0-9]{1,2}");
			Calendar cal = Calendar.getInstance();
			for (int i = 1; i <= 10; i++) {
				//Get our date
				String note = StringUtils.defaultString(rowData.getValue("notes" + i));
				String[] notesParts = StringUtils.split(note, " ", 2);
				Date noteDate;
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

					//Java is stupididly complex when it comes to dates
					int year = cal.get(Calendar.YEAR);
					cal.clear();
					cal.set(year, month, day);

					//Yay
					noteDate = cal.getTime();
					note = notesParts[1].trim();
				} else {
					//No date, give default date in 1970
					cal.clear();
					noteDate = cal.getTime();
				}

				//Set date and value
				rowData.setValueLocal("notes" + i + "date", Spreadsheet.getNewDateFormat().format(noteDate));
				rowData.setValueLocal("notes" + i, note);
			}
		}
	}
	
	public static void convertToTrimmedStrings(List<ListEntry> rows) {
		for(ListEntry row : rows) {
			CustomElementCollection rowData = row.getCustomElements();
			for(String columnName : rowData.getTags()) {
				//Trim all values
				rowData.setValueLocal(columnName, rowData.getValue(columnName).trim());
			}
		}
	}
}
