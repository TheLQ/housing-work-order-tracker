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
import java.util.List;
import java.util.Map;
import lombok.Data;
import org.apache.commons.lang3.StringEscapeUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.wicket.ajax.json.JSONException;
import org.apache.wicket.ajax.json.JSONObject;
import org.apache.wicket.request.IRequestParameters;
import org.apache.wicket.request.cycle.RequestCycle;
import org.apache.wicket.request.resource.AbstractResource;
import org.apache.wicket.request.resource.AbstractResource.ResourceResponse;
import org.apache.wicket.request.resource.AbstractResource.WriteCallback;
import org.apache.wicket.request.resource.IResource.Attributes;
import org.apache.wicket.util.string.StringValue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
			public void writeData(Attributes a) {
				String responseString = "EMPTY!!";
				try {
					//Figure out what method to use
					IRequestParameters params = RequestCycle.get().getRequest().getRequestParameters();

					String mode = params.getParameterValue("mode").toString();
					if (mode.equalsIgnoreCase("form"))
						responseString = handleFormSubmit().toString();
					else if (mode.equalsIgnoreCase("room"))
						responseString = handleRoomSubmit().toString();
					else
						responseString = new JSONObject().append("error", "Unknown mode " + mode).toString();
				} catch (Exception ex) {
					String error = StringEscapeUtils.escapeEcmaScript(ExceptionUtils.getStackTrace(ex));
					responseString = "{\"error\": \"" + error + "\"}";
				}
				a.getResponse().write(responseString);
			}
		});
		return r;
	}

	public JSONObject handleFormSubmit() throws JSONException, IOException, ServiceException {
		JSONObject response = new JSONObject();
		IRequestParameters params = RequestCycle.get().getRequest().getRequestParameters();

		//Get our basic values
		String building = params.getParameterValue("building").toString();
		//Note: room is a string since some buildings (IE Louisville) have suites with letters
		String room = params.getParameterValue("room").toString();

		log.info("Building: " + building + " | room: " + room);

		//Get all the selected auto-fixed issues
		List<String> autoFix = new ArrayList();
		for (StringValue curValue : params.getParameterValues("autoFix"))
			if (curValue != null)
				autoFix.add(curValue.toString());

		//Parse out POST data
		Date date = new Date();
		Map<Integer, Spreadsheet.RawDataEntry> enteries = new HashMap();
		for (String curField : params.getParameterNames()) {
			if (!curField.startsWith("issueBox"))
				//Must be another field
				continue;
			String[] fieldParts = StringUtils.split(curField, "[]");
			int curIssueNum = Integer.parseInt(fieldParts[2]);

			//Make sure our entry exists
			Spreadsheet.RawDataEntry entry = enteries.get(curIssueNum);
			if (entry == null) {
				entry = new Spreadsheet.RawDataEntry();
				entry.setBuilding(building);
				entry.setRoom(room);
				entry.setOpenedDate(date);
				enteries.put(curIssueNum, entry);
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

		Spreadsheet.get().insertData(enteries.values());

		response.put("submitStatus", "Added " + enteries.size() + " issues for " + building + " " + room + " on "
				+ Spreadsheet.getNewDateFormat().format(date));

		return response;
	}

	public JSONObject handleRoomSubmit() throws MalformedURLException, ServiceException, IOException, ParseException, JSONException {
		JSONObject response = new JSONObject();
		List<JSONObject> issues = new ArrayList();
		IRequestParameters params = RequestCycle.get().getRequest().getRequestParameters();

		//Parse out
		String room = params.getParameterValue("room").toString();
		for (Spreadsheet.RawDataEntry curEntry : Spreadsheet.get().loadRawAll()) {
			//Ignore anything that isn't this room
			if (!curEntry.getRoom().equalsIgnoreCase(room))
				continue;

			//Generate response
			JSONObject curNewIssue = new JSONObject();
			curNewIssue.put("issue", curEntry.getType().toLowerCase() + " - " + curEntry.getIssue());
			for (String curNote : curEntry.getNotes())
				curNewIssue.append("notesBox", new JSONObject().put("note", curNote));
			issues.add(curNewIssue);
		}

		response.put("data", issues);
		response.put("response", "Found " + issues.size() + " issues(s) on " + Spreadsheet.getNewDateFormat().format(new Date()));
		return response;
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
