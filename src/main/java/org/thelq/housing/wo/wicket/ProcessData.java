/**
 * Copyright (C) 2012 Leon Blakey <lord.quackstar at gmail.com>, RATS 2012
 *
 * This file is part of University of Louisville Housing WorkOrder System.
 */
package org.thelq.housing.wo.wicket;

import com.google.gdata.util.ServiceException;
import java.io.IOException;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.Data;
import org.apache.commons.lang.StringEscapeUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.exception.ExceptionUtils;
import org.apache.wicket.ajax.json.JSONException;
import org.apache.wicket.ajax.json.JSONObject;
import org.apache.wicket.request.IRequestParameters;
import org.apache.wicket.request.cycle.RequestCycle;
import org.apache.wicket.request.resource.AbstractResource;
import org.apache.wicket.request.resource.AbstractResource.ResourceResponse;
import org.apache.wicket.request.resource.AbstractResource.WriteCallback;
import org.apache.wicket.request.resource.IResource.Attributes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Handle input
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
					else
						responseString = new JSONObject().append("error", "Unknown mode " + mode).toString();
				} catch (Exception ex) {
					String error = StringEscapeUtils.escapeJavaScript(ExceptionUtils.getFullStackTrace(ex));
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
				log.info("Value: " + value);
				String[] parts = value.split(" - ");
				entry.setType(parts[0]);
				entry.setIssue(parts[1]);
				entry.setStatus(Spreadsheet.Status.OPEN);
			} else
				//This is a note
				entry.getNotes().add(value);
		}
		
		Spreadsheet.get().insertData(enteries.values());
		
		response.put("submitStatus", "Added " + enteries.size() + " issues for " + building + " " + room + " on " 
				+ Spreadsheet.getOldDateFormat().format(date));

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
