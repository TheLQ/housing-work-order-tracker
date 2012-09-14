/**
 * Copyright (C) 2012 Leon Blakey <lord.quackstar at gmail.com>, RATS 2012
 *
 * This file is part of University of Louisville Housing WorkOrder System.
 */
/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.thelq.housing.wo.wicket;

import com.google.gdata.client.spreadsheet.SpreadsheetService;
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
import java.util.Collection;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import lombok.Data;
import lombok.Getter;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author Leon Blakey <lord.quackstar at gmail.com>
 */
public class Spreadsheet {
	private static final Logger log = LoggerFactory.getLogger(Spreadsheet.class);
	/**
	 * Spreadsheet feed key, NOT document key
	 */
	protected static final String url_raw = "https://spreadsheets.google.com/feeds/list/t280PyPU5dOYJ_ref3fX38Q/od6/private/full";
	protected static final String url_ui = "https://spreadsheets.google.com/feeds/list/t280PyPU5dOYJ_ref3fX38Q/od7/private/full";
	protected SpreadsheetService ssService;
	protected final String user;
	protected final String pass;
	@Getter
	protected static SimpleDateFormat oldDateFormat = new SimpleDateFormat("MMMMMMMMMM FF, yyyy hh:mm:ss aa zzz");

	public Spreadsheet() throws IOException, AuthenticationException {
		Properties userProp = new Properties();
		userProp.load(this.getClass().getClassLoader().getResourceAsStream("creds.properties"));
		user = userProp.getProperty("user");
		pass = userProp.getProperty("pass");
		//Load the Spreadsheet service
		ssService = new SpreadsheetService("UofL-Workorder");
		ssService.setUserCredentials(user, pass);
	}

	public UIData loadUI() throws MalformedURLException, ServiceException, IOException {
		UIData data = new UIData();
		//Start loading stuff from the UI
		ListFeed listFeed = ssService.getFeed(new URL(url_ui), ListFeed.class);
		for (ListEntry row : listFeed.getEntries()) {
			CustomElementCollection rowData = row.getCustomElements();

			//Parse each column in row
			for (String columnName : rowData.getTags())
				if (rowData.getValue(columnName) == null)
					continue;
				else if (columnName.equalsIgnoreCase("Buildings"))
					data.getBuildings().add(rowData.getValue(columnName));
				else {
					//Must be issue ui data
					if (data.getIssues().get(columnName) == null)
						data.getIssues().put(columnName, new ArrayList());
					data.getIssues().get(columnName).add(rowData.getValue(columnName));
				}
		}
		log.debug("Loaded " + data.getBuildings().size() + " buildings and " + data.getIssues().size() + " issue types ");
		return data;
	}

	public List<RawDataEntry> loadRaw() throws MalformedURLException, ServiceException, IOException, ParseException {
		List<RawDataEntry> enteries = new ArrayList();

		//Load entire sheet into list
		ListFeed listFeed = ssService.getFeed(new URL(url_raw), ListFeed.class);
		for (ListEntry row : listFeed.getEntries()) {
			CustomElementCollection rowData = row.getCustomElements();
			RawDataEntry curEntry = new RawDataEntry();

			//Parse each column in row
			for (String columnName : rowData.getTags()) {
				String value = rowData.getValue(columnName);
				if (rowData.getValue(columnName) == null)
					continue;
				else if (columnName.equalsIgnoreCase("Opened"))
					curEntry.setOpenedDate(oldDateFormat.parse(value));
				else if (columnName.equalsIgnoreCase("WT"))
					curEntry.setOpenedWalkthrough(value.equals("Y"));
				else if (columnName.equalsIgnoreCase("Building"))
					curEntry.setBuilding(value);
				else if (columnName.equalsIgnoreCase("Room"))
					curEntry.setRoom(value);
				else if (columnName.equalsIgnoreCase("Type"))
					curEntry.setType(value);
				else if (columnName.equalsIgnoreCase("Issue"))
					curEntry.setIssue(value);
				else if (columnName.equalsIgnoreCase("Status"))
					curEntry.setStatus(Status.valueOf(value.toUpperCase()));
				else if (columnName.equalsIgnoreCase("Closed"))
					curEntry.setClosedDate(oldDateFormat.parse(value));
				else if (columnName.equalsIgnoreCase("CWT"))
					curEntry.setClosedWalkthrough(value.equals("Y"));
				else if (StringUtils.startsWithIgnoreCase(columnName, "Notes"))
					curEntry.getNotes().add(value);
				else
					throw new RuntimeException("Unknown column " + columnName);
			}
			enteries.add(curEntry);
		}

		return enteries;
	}

	public void insertData(Collection<RawDataEntry> enteries) throws IOException, ServiceException {
		for (RawDataEntry curEntry : enteries) {
			ListEntry row = new ListEntry();
			row.getCustomElements().setValueLocal("opened", oldDateFormat.format(curEntry.getOpenedDate()));
			row.getCustomElements().setValueLocal("wt", curEntry.isOpenedWalkthrough() ? "Y" : "N");
			row.getCustomElements().setValueLocal("building", curEntry.getBuilding());
			row.getCustomElements().setValueLocal("room", curEntry.getRoom());
			row.getCustomElements().setValueLocal("type", curEntry.getType());
			row.getCustomElements().setValueLocal("issue", StringUtils.capitalize(curEntry.getIssue()));
			row.getCustomElements().setValueLocal("status", curEntry.getStatus().getHumanName());
			String date = (curEntry.getClosedDate() != null ) ? oldDateFormat.format(curEntry.getClosedDate()) : "";
			row.getCustomElements().setValueLocal("closed", date);
			row.getCustomElements().setValueLocal("cwt", curEntry.isOpenedWalkthrough() ? "Y" : "N");
			int counter = 0;
			for (String curNote : curEntry.getNotes())
				row.getCustomElements().setValueLocal("notes" + (++counter), curNote);
			ssService.insert(new URL(url_raw), row);
		}
	}

	public static Spreadsheet get() {
		return ((GaeWicketApplication) GaeWicketApplication.get()).getSpreadsheet();
	}

	@Data
	public static class RawDataEntry {
		protected Date openedDate;
		protected boolean openedWalkthrough;
		protected String building;
		protected String room;
		protected String type;
		protected String issue;
		protected Status status;
		protected Date closedDate;
		protected boolean closedWalkthrough;
		protected List<String> notes = new ArrayList();
	}

	public static enum Status {
		OPEN,
		CLOSED,
		WAITING;
		
		public String getHumanName() {
			//Make it look like a normal word
			return StringUtils.capitalize(name().toLowerCase());
		}
	}

	@Data
	public static class UIData {
		protected Map<String, List<String>> issues = new LinkedHashMap<String, List<String>>();
		protected List<String> buildings = new ArrayList();
	}
}
