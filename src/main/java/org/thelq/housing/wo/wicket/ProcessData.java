/**
 * Copyright (C) 2012 Leon Blakey <lord.quackstar at gmail.com>, RATS 2012
 *
 * This file is part of University of Louisville Housing WorkOrder System.
 */
package org.thelq.housing.wo.wicket;

import com.google.gdata.util.ServiceException;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.regex.Pattern;
import javax.annotation.RegEx;
import lombok.Data;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import org.apache.commons.lang3.StringEscapeUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.wicket.request.IRequestParameters;
import org.apache.wicket.request.cycle.RequestCycle;
import org.apache.wicket.request.resource.AbstractResource;
import org.apache.wicket.request.resource.AbstractResource.ResourceResponse;
import org.apache.wicket.request.resource.AbstractResource.WriteCallback;
import org.apache.wicket.request.resource.IResource.Attributes;
import org.apache.wicket.util.string.StringValue;
import org.joda.time.DateTime;
import org.joda.time.Days;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.thelq.housing.wo.wicket.Spreadsheet.NoteEntry;
import org.thelq.housing.wo.wicket.Spreadsheet.RawDataEntry;

/**
 * Handle input for room issues and form submission.
 * <p>
 * NOTE TO FUTURE MAINTAINERS
 * <p>
 * This is not how your supposed to use Wicket. Wicket says that everything should be a
 * listener to the many different components that your supposed to have wicket manage.
 * However this form is not your typical wicket form: There is a set of fields that
 * gets duplicated and another set of fields inside of that box that gets duplicated
 * as well. In Wicket your supposed to use a complex AJAX handler that would add
 * the fields in a request. This plus the required complex submission logic would
 * make this program an unmaintainable mess that would confuse anybody that thought
 * about looking at it
 * <p>
 * To prevent this, I've abused Request cycle and a Wicket resource as a form processor.
 * This gives easily maintainable code that makes sense by parsing the raw POST
 * data. Might look a little ugly, but it allows me to write 4x the amount of functionality
 * in half to a quarter of the time. It works for me, and thats whats important
 *
 * @author Leon Blakey <lord.quackstar at gmail.com>
 */
public class ProcessData extends AbstractResource {
	private static final Logger log = LoggerFactory.getLogger(ProcessData.class);

	@Override
	protected ResourceResponse newResourceResponse(Attributes a) {
		ResourceResponse r = new ResourceResponse();
		r.setContentType("application/json");
		r.setWriteCallback(new WriteCallback() {
			@Override
			public void writeData(Attributes a) {
				String responseString;
				try {
					//Figure out what method to use
					IRequestParameters params = RequestCycle.get().getRequest().getRequestParameters();

					String mode = params.getParameterValue("mode").toString();
					if (mode.equalsIgnoreCase("form"))
						responseString = handleFormSubmit().toString();
					else if (mode.equalsIgnoreCase("room"))
						responseString = handleRoomSubmit().toString();
					else if (mode.equalsIgnoreCase("existing"))
						responseString = handleExistingSubmit().toString();
					else
						responseString = new JSONObject().put("error", "Unknown mode " + mode).toString();
				} catch (Exception ex) {
					String error = StringEscapeUtils.escapeEcmaScript(ExceptionUtils.getStackTrace(ex));
					responseString = "{\"error\": \"" + error + "\"}";
				}
				a.getResponse().write(responseString);
			}
		});
		return r;
	}

	public JSONObject handleFormSubmit() throws IOException, ServiceException, MalformedURLException, ParseException {
		JSONObject response = new JSONObject();
		IRequestParameters params = RequestCycle.get().getRequest().getRequestParameters();

		//Get our basic values
		String building = params.getParameterValue("building").toString();
		//Note: room is a string since some buildings (IE Louisville) have suites with letters
		String room = params.getParameterValue("room").toString();

		log.info("Building: " + building + " | room: " + room);

		//Parse out POST data
		DateTime date = new DateTime();
		List<RawDataEntry> entriesRaw = Spreadsheet.get().loadRawRoom(building, room);
		List<RawDataEntry> entriesNew = new ArrayList();
		final int totalRawRows = Spreadsheet.get().loadTotalRawRows();
		int lastSheetId = totalRawRows - 1;
		int curIssueNum = -1;
		while (true) {
			curIssueNum++;
			String prefix = "issues[" + curIssueNum + "]";

			//Make sure something exists
			StringValue sheetIdParam = params.getParameterValue(prefix + "sheetId");
			if (sheetIdParam.isEmpty())
				//Doesn't exist
				break;

			//Get the correct RawDataEntry
			int sheetId = sheetIdParam.toInt();
			Spreadsheet.RawDataEntry entry = null;
			if (sheetId > 0) {
				//Has a sheet id, this is updating an existing issue
				for (RawDataEntry curEntry : entriesRaw)
					if (curEntry.getSheetId() == sheetId) {
						entry = curEntry;
						break;
					}

				//Make sure we got an issue out of it
				if (entry == null)
					throw new RuntimeException("Given sheet id " + sheetId + " but can't find a matching issue!");
			} else {
				entry = new Spreadsheet.RawDataEntry();
				entriesNew.add(entry);

				//Start populating fields that only a new issue needs
				entry.setBuilding(building);
				entry.setRoom(room);
				entry.setOpenedDate(date);
				entry.setSheetId(++lastSheetId);
				log.info("Created new issue with sheet id " + entry.getSheetId());

				String[] issueParts = params.getParameterValue(prefix + "issueSelect").toString().split(" - ");
				entry.setType(issueParts[0]);
				entry.setIssue(issueParts[1]);
			}

			//Handle status appropriately
			String status = params.getParameterValue(prefix + "issueStatus").toString();
			log.info("Setting issue with prefix " + prefix + " to status " + status);
			if (status.equalsIgnoreCase("open"))
				entry.setStatus(Spreadsheet.Status.OPEN);
			else if (status.equalsIgnoreCase("closed")) {
				entry.setStatus(Spreadsheet.Status.CLOSED);
				entry.setClosedDate(date);
				entry.setClosedWalkthrough(!params.getParameterValue("modeSelect").toString().equalsIgnoreCase("Normal"));
			} else if (status.equalsIgnoreCase("waiting")) {
				entry.setStatus(Spreadsheet.Status.WAITING);
				entry.setWaitingDate(date);
				entry.setWaitingWalkthrough(!params.getParameterValue("modeSelect").toString().equalsIgnoreCase("Normal"));
			}

			//Load notes (disabled fields aren't submitted, so loop through all of them)
			for (int i = 0; i <= 10; i++) {
				//Attempt to get note
				StringValue noteTextParam = params.getParameterValue(prefix + "[notes][" + i + "]note");
				if (noteTextParam.isEmpty())
					continue;

				String noteText = noteTextParam.toString();
				log.info("For prefix " + prefix + " created note #" + i + " with text " + noteText);
				entry.getNotes().add(new NoteEntry(noteText, date));
			}
		}

		//Insert and update our data
		Spreadsheet.get().updateData(entriesRaw);
		Spreadsheet.get().insertData(entriesNew);

		response.put("submitStatus", "Added " + entriesNew.size() + " issues, "
				+ "updated " + entriesRaw.size() + " issues "
				+ "for " + building + " " + room + " on "
				+ Spreadsheet.getNewDateFormat().print(date));

		return response;
	}

	public JSONObject handleRoomSubmit() throws MalformedURLException, ServiceException, IOException, ParseException {
		JSONObject response = new JSONObject();
		List<JSONObject> issues = new ArrayList();
		IRequestParameters params = RequestCycle.get().getRequest().getRequestParameters();

		//Parse out
		String building = params.getParameterValue("building").toString();
		String room = params.getParameterValue("room").toString();
		for (Spreadsheet.RawDataEntry curEntry : Spreadsheet.get().loadRawRoom(building, room)) {
			//Ignore anything that isn't this room
			if (!curEntry.getRoom().equalsIgnoreCase(room))
				continue;
			issues.add(generateJsonFromEntry(curEntry));
		}

		response.put("data", issues);
		response.put("response", "Found " + issues.size() + " issues(s) for " + building + " " + room
				+ " on " + Spreadsheet.getNewDateFormat().print(new DateTime()));
		return response;
	}

	public JSONObject handleExistingSubmit() throws UnsupportedEncodingException, UnsupportedEncodingException, MalformedURLException, ServiceException, IOException, ParseException {
		JSONObject response = new JSONObject();
		IRequestParameters params = RequestCycle.get().getRequest().getRequestParameters();
		//Use TreeMap since it can sort keys
		Map<String, List<JSONObject>> issueMap = new TreeMap();

		//Process and sort incoming list entries into issueMap
		String building = params.getParameterValue("building").toString();
		List<RawDataEntry> rawEntries = Spreadsheet.get().loadRawBuilding(building);
		for (RawDataEntry curEntry : rawEntries) {
			//Generate location string to be used as a key and displayed in form
			String location = curEntry.getBuilding() + " " + curEntry.getRoom();

			//Insert
			if (!issueMap.containsKey(location))
				issueMap.put(location, new ArrayList());
			issueMap.get(location).add(generateJsonFromEntry(curEntry));
		}

		response.put("data", issueMap);
		response.put("response", "Loaded " + rawEntries.size() + " issues into " + issueMap.size() + " rooms "
				+ " on " + Spreadsheet.getNewDateFormat().print(new DateTime()));
		return response;
	}

	protected static JSONObject generateJsonFromEntry(Spreadsheet.RawDataEntry entry) {
		//Generate response
		JSONObject curNewIssue = new JSONObject();
		curNewIssue.put("sheetId", entry.getSheetId());
		curNewIssue.put("issue", generateIssueName(entry));
		curNewIssue.put("status", StringUtils.capitalize(entry.getStatus().toString().toLowerCase()));
		if (entry.getOpenedDate() != null) {
			curNewIssue.put("opened", Spreadsheet.getNewDateFormat().print(entry.getOpenedDate()));
			curNewIssue.put("openedAge", Days.daysBetween(entry.getOpenedDate(), new DateTime()).getDays());
		} else {
			curNewIssue.put("opened", "");
			curNewIssue.put("openedAge", "");
		}
		if (entry.getWaitingDate() != null) {
			curNewIssue.put("waiting", Spreadsheet.getNewDateFormat().print(entry.getWaitingDate()));
			curNewIssue.put("waitingAge", Days.daysBetween(entry.getWaitingDate(), new DateTime()).getDays());
		} else {
			curNewIssue.put("waiting", "");
			curNewIssue.put("waitingAge", "");
		}
		curNewIssue.put("notes", new JSONArray());
		Iterator<Spreadsheet.NoteEntry> notesItr = entry.getNotes().iterator();
		do {
			//Make sure this collection is never empty, should have at least an empty string in it
			Spreadsheet.NoteEntry curNoteEntry = notesItr.hasNext() ? notesItr.next() : null;

			JSONObject note = new JSONObject();
			if (curNoteEntry != null) {
				log.info("NoteEntry date: " + curNoteEntry.getDate() + " | Note: " + curNoteEntry.getNote());
				note.put("noteDate", Spreadsheet.getNewDateFormat().print(curNoteEntry.getDate()));
				note.put("note", StringUtils.defaultString(curNoteEntry.getNote()));
			} else {
				note.put("noteDate", "");
				note.put("note", "");
			}
			curNewIssue.accumulate("notes", note);
		} while (notesItr.hasNext());
		return curNewIssue;
	}

	protected static String generateIssueName(RawDataEntry entry) {
		return entry.getType().toLowerCase() + " - " + entry.getIssue();
	}

	@Data
	protected static class FormData {
		protected String building;
		protected String room;
		/**
		 * Both issueMap and notesMap help track what are the issues and what
		 * notes go where. They are split into 2 maps since they need to be
		 * looked up by id as JQuery Forms duplicates fields by incrementing the
		 * issue number.
		 */
		protected Map<Integer, String> issueMap = new HashMap();
		protected Map<Integer, List<String>> notesMap = new HashMap();
	}
}
