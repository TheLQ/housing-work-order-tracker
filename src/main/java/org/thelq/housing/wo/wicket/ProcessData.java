/**
 * Copyright (C) 2012 Leon Blakey <lord.quackstar at gmail.com>, RATS 2012
 *
 * This file is part of University of Louisville Housing WorkOrder System.
 */
package org.thelq.housing.wo.wicket;

import com.google.gdata.util.ServiceException;
import java.io.IOException;
import java.net.MalformedURLException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.thelq.housing.wo.wicket.Spreadsheet.RawDataEntry;

/**
 * Handle input. 
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
		
		//Get all the selected auto-fixed issues
		List<String> autoFix = new ArrayList();
		if (params.getParameterValues("autoFix") != null)
			for (StringValue curValue : params.getParameterValues("autoFix"))
				if (curValue != null)
					autoFix.add(curValue.toString());

		//Parse out POST data
		Date date = new Date();
		Map<Integer, Spreadsheet.RawDataEntry> entriesByNum = new HashMap();
		for (String curField : params.getParameterNames()) {
			if (!curField.startsWith("issueBox"))
				//Must be another field
				continue;
			String[] fieldParts = StringUtils.split(curField, "[]");
			int curIssueNum = Integer.parseInt(fieldParts[2]);
			
			//Make sure our entry exists
			Spreadsheet.RawDataEntry entry = entriesByNum.get(curIssueNum);
			if (entry == null) {
				entry = new Spreadsheet.RawDataEntry();
				entry.setBuilding(building);
				entry.setRoom(room);
				entry.setOpenedDate(date);
				entriesByNum.put(curIssueNum, entry);
			}

			String value = params.getParameterValue(curField).toString();
			if (fieldParts[3].equals("issue")) {
				//This is specifying the issue
				log.debug("Value: " + value);
				log.debug("Autofix contents: " + autoFix);
				String[] parts = value.split(" - ");
				entry.setType(parts[0]);
				entry.setIssue(parts[1]);
				log.debug("Contains: " + autoFix.contains(value));
				if (!params.getParameterValue("modeSelect").toString().equalsIgnoreCase("Normal") && autoFix.contains(value))
					//Issue is going to be autofixed
					entry.setStatus(Spreadsheet.Status.CLOSED);
				else
					//New issue
					entry.setStatus(Spreadsheet.Status.OPEN);
			} else
				//This is a note
				entry.getNotes().add(value);
		}

		//Check for notes sections that need to be updated
		List<RawDataEntry> rawEntries = Spreadsheet.get().loadRawRoom(building, room);
		for (RawDataEntry curRawEntry : rawEntries)
			//Find this issue in input
			for (Iterator<RawDataEntry> entriesItr = entriesByNum.values().iterator(); entriesItr.hasNext();) {
				RawDataEntry curNewEntry = entriesItr.next();
				if (curNewEntry.isSameIssue(curRawEntry)) {
					//See if anything needs to be updated
					boolean update = false;
					if (!curRawEntry.getNotes().equals(curNewEntry.getNotes())) {
						curRawEntry.setNotes(curNewEntry.getNotes());
						update = true;
					}
					if (!curRawEntry.getStatus().equals(curNewEntry.getStatus())) {
						//Status changed, handle seperately
						if (curNewEntry.getStatus().equals(Spreadsheet.Status.CLOSED))
							curRawEntry.setClosedDate(date); //TODO: Walkthrough
						else if (curNewEntry.getStatus().equals(Spreadsheet.Status.WAITING))
							curRawEntry.getNotes().add("Marked Waiting on " + Spreadsheet.getNewDateFormat().format(date));
						else if (curNewEntry.getStatus().equals(Spreadsheet.Status.OPEN))
							//Status changed back to Open, probably should note this
							curRawEntry.getNotes().add("Remarked Open on " + Spreadsheet.getNewDateFormat().format(date));
						curRawEntry.setStatus(curNewEntry.getStatus());
						update = true;
					}

					//Anything got updated, update the sheet
					if (update) {
						curRawEntry.getListEntry().update();
						//Remove from new entries so its not inserted as a new row
						entriesItr.remove();
					}
				}
			}

		response.put("submitStatus", "Added " + entriesByNum.size() + " issues for " + building + " " + room + " on "
				+ Spreadsheet.getNewDateFormat().format(date));

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

			//Generate response
			JSONObject curNewIssue = new JSONObject();
			curNewIssue.put("issue", generateIssueName(curEntry));
			curNewIssue.put("notesBox", new JSONArray());
			Iterator<String> notesItr = curEntry.getNotes().iterator();
			do {
				//Make sure this collection is never empty, should have at least an empty string in it
				String curNote = notesItr.hasNext() ? notesItr.next() : "";
				JSONObject note = new JSONObject();
				note.put("note", curNote);
				curNewIssue.accumulate("notesBox", note);
			} while(notesItr.hasNext());
			issues.add(curNewIssue);
		}

		response.put("data", issues);
		response.put("response", "Found " + issues.size() + " issues(s) for " + building + " " + room
				+ " on " + Spreadsheet.getNewDateFormat().format(new Date()));
		return response;
	}

	protected String generateIssueName(RawDataEntry entry) {
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
