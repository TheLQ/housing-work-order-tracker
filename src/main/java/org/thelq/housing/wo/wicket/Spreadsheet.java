/**
 * Copyright (C) 2012 Leon Blakey <lord.quackstar at gmail.com>, RATS 2012
 *
 * This file is part of University of Louisville Housing WorkOrder System.
 */
package org.thelq.housing.wo.wicket;

import com.google.gdata.client.spreadsheet.SpreadsheetService;
import com.google.gdata.data.spreadsheet.CustomElementCollection;
import com.google.gdata.data.spreadsheet.ListEntry;
import com.google.gdata.data.spreadsheet.ListFeed;
import com.google.gdata.util.AuthenticationException;
import com.google.gdata.util.ServiceException;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Provides utility methods to extract information from the Spreadsheet.
 * @author Leon Blakey <lord.quackstar at gmail.com>
 */
public class Spreadsheet {
	private static final Logger log = LoggerFactory.getLogger(Spreadsheet.class);
	/**
	 * Spreadsheet feed key, NOT document key
	 */
	@Getter
	protected final String urlRaw;
	@Getter
	protected final String urlUi;
	protected SpreadsheetService ssService;
	protected final String user;
	protected final String pass;

	public Spreadsheet(String prefix) throws IOException, AuthenticationException {
		//Load username and password and login to Spreadsheet API
		Properties userProp = new Properties();
		if(this.getClass().getClassLoader().getResource("creds.properties") == null)
			throw new RuntimeException("Cannot load creds.properties, does it exist?");
		userProp.load(this.getClass().getClassLoader().getResourceAsStream("creds.properties"));
		user = userProp.getProperty("user");
		pass = userProp.getProperty("pass");
		urlRaw = userProp.getProperty(prefix + "url_raw");
		urlUi = userProp.getProperty(prefix + "url_ui");
		//Load the Spreadsheet service
		ssService = new SpreadsheetService("UofL-Workorder");
		ssService.setUserCredentials(user, pass);
	}

	public UIData loadUI() throws MalformedURLException, ServiceException, IOException {
		UIData data = new UIData();
		//Start loading stuff from the UI
		ListFeed listFeed = ssService.getFeed(new URL(urlUi), ListFeed.class);
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

	public List<RawDataEntry> loadRawRoom(String building, String room) throws MalformedURLException, IOException, ServiceException, ParseException {
		if (StringUtils.isBlank(building))
			throw new NullPointerException("Attempting to load raw room with null building");
		if (StringUtils.isBlank(room))
			throw new NullPointerException("Attempting to load raw room with null room");
		String query = URLEncoder.encode("building = " + building + " and room = \"" + room + "\" and status != Closed", "UTF-8");
		log.info("Querying sheet with: " + query);
		ListFeed listFeed = ssService.getFeed(new URL(urlRaw + "?sq=" + query), ListFeed.class);
		return loadRaw(listFeed);
	}

	public List<RawDataEntry> loadRawAll() throws MalformedURLException, ServiceException, IOException, ParseException {
		//Load entire sheet into list
		ListFeed listFeed = ssService.getFeed(new URL(urlRaw), ListFeed.class);
		return loadRaw(listFeed);
	}

	protected List<RawDataEntry> loadRaw(ListFeed listFeed) throws ParseException {
		List<RawDataEntry> entries = new ArrayList();
		for (ListEntry row : listFeed.getEntries()) {
			CustomElementCollection rowData = row.getCustomElements();
			RawDataEntry curEntry = new RawDataEntry();

			//Parse each column in row
			for (String columnName : rowData.getTags()) {
				String value = rowData.getValue(columnName);
				if (rowData.getValue(columnName) == null)
					continue;
				else if (columnName.equalsIgnoreCase("id"))
					curEntry.setSheetId(Integer.valueOf(value));
				else if (columnName.equalsIgnoreCase("Opened"))
					curEntry.setOpenedDate(getNewDateFormat().parse(value));
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
					curEntry.setClosedDate(getNewDateFormat().parse(value));
				else if (columnName.equalsIgnoreCase("CWT"))
					curEntry.setClosedWalkthrough(value.equals("Y"));
				else if (columnName.equalsIgnoreCase("Waiting"))
					curEntry.setWaitingDate(getNewDateFormat().parse(value));
				else if (columnName.equalsIgnoreCase("wwt"))
					curEntry.setWaitingWalkthrough(value.equals("Y"));
				else if (StringUtils.startsWithIgnoreCase(columnName, "Notes")) {
					//Get the number of this note by removing all letters and converting to int
					int num = Integer.parseInt(columnName.replaceAll("[^-?\\d+]", "")) - 1;
					List<NoteEntry> notes = curEntry.getNotes();

					if(notes.size() < num + 1)
						//List isn't even big enough to hold our num, add to end
						notes.add(new NoteEntry());

					//Set the appropiate value based on if this is the date or not
					if (StringUtils.endsWithIgnoreCase(columnName, "date"))
						notes.get(num).setDate(Spreadsheet.getNewDateFormat().parse(value));
					else
						notes.get(num).setNote(value);
				} else
					throw new RuntimeException("Unknown column " + columnName);
			}
			curEntry.setListEntry(row);
			entries.add(curEntry);
		}

		return entries;
	}

	public void insertData(Collection<RawDataEntry> entries) throws IOException, ServiceException {
		for (ListEntry curRawEntry : convertData(entries))
			ssService.insert(new URL(urlRaw), curRawEntry);
	}

	public void updateData(Collection<RawDataEntry> entries) throws IOException, ServiceException {
		//Make sure everything has a ListEntry
		for (RawDataEntry curEntry : entries)
			if (curEntry.getListEntry() == null)
				throw new NullPointerException("No ListEntry defined for " + curEntry);

		//Update all ListEntries
		for (ListEntry curEntry : convertData(entries))
			curEntry.update();
	}

	protected List<ListEntry> convertData(Collection<RawDataEntry> rawEntries) {
		List<ListEntry> listEntries = new ArrayList();
		for (RawDataEntry curEntry : rawEntries) {
			ListEntry row = (curEntry.getListEntry() != null) ? curEntry.getListEntry() : new ListEntry();
			row.getCustomElements().setValueLocal("id", "" + curEntry.getSheetId());
			row.getCustomElements().setValueLocal("opened", getNewDateFormat().format(curEntry.getOpenedDate()));
			row.getCustomElements().setValueLocal("wt", curEntry.isOpenedWalkthrough() ? "Y" : "N");
			row.getCustomElements().setValueLocal("building", curEntry.getBuilding());
			row.getCustomElements().setValueLocal("room", curEntry.getRoom());
			row.getCustomElements().setValueLocal("type", curEntry.getType());
			row.getCustomElements().setValueLocal("issue", StringUtils.capitalize(curEntry.getIssue()));
			row.getCustomElements().setValueLocal("status", curEntry.getStatus().getHumanName());
			String closedDate = (curEntry.getClosedDate() != null) ? getNewDateFormat().format(curEntry.getClosedDate()) : "";
			row.getCustomElements().setValueLocal("closed", closedDate);
			row.getCustomElements().setValueLocal("cwt", curEntry.isClosedWalkthrough() ? "Y" : "N");
			String waitingDate = (curEntry.getWaitingDate() != null) ? getNewDateFormat().format(curEntry.getWaitingDate()) : "";
			row.getCustomElements().setValueLocal("waiting", waitingDate);
			row.getCustomElements().setValueLocal("wwt", curEntry.isWaitingWalkthrough() ? "Y" : "N");

			//Add notes
			int counter = 0;
			for (NoteEntry noteEntry : curEntry.getNotes()) {
				counter++;
				if (StringUtils.isBlank(row.getCustomElements().getValue("notes" + counter + "date")))
					row.getCustomElements().setValueLocal("notes" + counter + "date", getNewDateFormat().format(noteEntry.getDate()));
				row.getCustomElements().setValueLocal("notes" + counter, noteEntry.getNote());
			}
			listEntries.add(row);
		}
		return listEntries;
	}

	public static Spreadsheet get() {
		return ((GaeWicketApplication) GaeWicketApplication.get()).getSpreadsheet();
	}

	@Deprecated
	public static SimpleDateFormat getOldDateFormat() {
		SimpleDateFormat date = new SimpleDateFormat("MMMMMMMMMM dd, yyyy hh:mm:ss aa zzz");
		//date.setTimeZone(TimeZone.getTimeZone("EDT"));
		return date;
	}

	public static SimpleDateFormat getNewDateFormat() {
		SimpleDateFormat date = new SimpleDateFormat("MMM dd yyyy, hh:mm aa");
		//date.setTimeZone(TimeZone.getTimeZone("EDT"));
		return date;
	}

	@Data
	public static class RawDataEntry {
		protected int sheetId;
		protected Date openedDate;
		protected boolean openedWalkthrough;
		protected String building;
		protected String room;
		protected String type;
		protected String issue;
		protected Status status;
		protected Date closedDate;
		protected boolean closedWalkthrough;
		protected Date waitingDate;
		protected boolean waitingWalkthrough;
		protected List<NoteEntry> notes = new ArrayList();
		protected ListEntry listEntry;

		public boolean isSameIssue(RawDataEntry otherEntry) {
			return getBuilding().equals(otherEntry.getBuilding())
					&& getRoom().equals(otherEntry.getRoom())
					&& getType().equals(otherEntry.getType())
					&& getIssue().equals(otherEntry.getIssue());
		}
	}

	@Data
	@AllArgsConstructor
	@NoArgsConstructor
	public static class NoteEntry {
		protected String note;
		protected Date date;
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
