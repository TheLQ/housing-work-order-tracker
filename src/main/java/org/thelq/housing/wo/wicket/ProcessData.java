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
import java.util.Collection;
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
		List<RawDataEntry> entriesRaw = Spreadsheet.get().loadRawRoom(building, room);
		List<RawDataEntry> entriesNew = new ArrayList();
		Map<Integer, Spreadsheet.RawDataEntry> entriesByNum = new HashMap();
		for (String curField : params.getParameterNames()) {
			if (!curField.startsWith("issueBox"))
				//Must be another field
				continue;
			String[] fieldParts = StringUtils.split(curField, "[]");
			int curIssueNum = Integer.parseInt(fieldParts[2]);
			String prefix = "issueBox[issueBox][" + curIssueNum + "]";

			//Skip parameter if we've already dealt with the entry
			if (entriesByNum.containsKey(curIssueNum))
				continue;

			//Get the correct RawDataEntry
			int sheetId = params.getParameterValue(prefix + "[sheetId]").toInt();
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

				String[] issueParts = params.getParameterValue(prefix + "[issue]").toString().split(" - ");
				entry.setType(issueParts[0]);
				entry.setIssue(issueParts[1]);
			}
			
			//Insert all entries into entriesByNum so they can be referenced later
			entriesByNum.put(curIssueNum, entry);

			//Handle status appropriately
			String status = params.getParameterValue(prefix + "[statusSelect]").toString();
			if (status.equalsIgnoreCase("open"))
				entry.setStatus(Spreadsheet.Status.OPEN);
			else if (status.equalsIgnoreCase("closed")) {
				entry.setStatus(Spreadsheet.Status.CLOSED);
				entry.setClosedDate(date);
				entry.setClosedWalkthrough(!params.getParameterValue("modeSelect").toString().equalsIgnoreCase("Normal"));
			} else if (status.equalsIgnoreCase("waiting"))
				entry.setStatus(Spreadsheet.Status.WAITING);

			//Load notes
			int curNoteId = -1;
			String value;
			while ((value = params.getParameterValue(prefix + "[notesBox][" + (++curNoteId) + "][note]").toString()) != null)
				if (!entry.getNotes().contains(value)) {
					entry.getNotes().add(value);
					log.info("Added note " + value);
				}
		}
		
		//Insert and update our data
		Spreadsheet.get().updateData(entriesRaw);
		Spreadsheet.get().insertData(entriesNew);

		response.put("submitStatus", "Added " + entriesNew.size() + " issues for " + building + " " + room + " on "
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
			curNewIssue.put("sheetId", curEntry.getSheetId());
			curNewIssue.put("issue", generateIssueName(curEntry));
			curNewIssue.put("notesBox", new JSONArray());
			Iterator<String> notesItr = curEntry.getNotes().iterator();
			do {
				//Make sure this collection is never empty, should have at least an empty string in it
				String curNote = notesItr.hasNext() ? notesItr.next() : "";
				JSONObject note = new JSONObject();
				note.put("note", curNote);
				curNewIssue.accumulate("notesBox", note);
			} while (notesItr.hasNext());
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
