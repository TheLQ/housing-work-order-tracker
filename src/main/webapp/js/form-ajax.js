/*
 * Copyright (C) 2012 Leon Blakey <lord.quackstar at gmail.com>, RATS 2012
 *
 * This file is part of University of Louisville Housing WorkOrder System.
 */
$(document).ready(function(){
	//Dynamic room lookup
	$("#room").live('input', function() {
		room = $(this).val();
					
		//Ignore rooms with less than 3 characters, user is probably typing in a room
		if(room.length < 3)
			return;
					
		building = $("#building option:selected").val();
		if(building == "")
			$("#roomStatus").html("No building is selected!");
		else {
			//Need to get room info from server
			$("#roomStatus").html("Fetching info for " + building + " " + room + "...");
			$.ajaxSetup ({
				cache: false
			}); 
			$.getJSON("processData?mode=room", {
				building: building, 
				room: room
			}, function(json) {  
				if(typeof json.error != 'undefined')
					$("#roomStatus").html("Server Error! " + json.error);
				else {
					console.log("Finished looking up room")
					$("#roomStatus").html(json.response)
					woUtils.inject(json.data);
				}
			}  
			);  
		}
	});
				
	//Automagical AJAX submit
	$("#mainForm").ajaxForm({
		//target:        '#submitStatus',   // target element(s) to be updated with server response 
		beforeSerialize: function() {
			$(".issueBox").each(function() {
				curStatus = $(".statusName", $(this)).html();
				$(".statusSelect", $(this)).val(curStatus);
			});
		},
		beforeSubmit: function() {
			$("#submitStatus").html("Submitting...");
		},
		success: function(data) {
			if(typeof data.error != 'undefined')
				$("#submitStatus").html("Server Error! " + data.error);
			else {
				console.log("Finished submitting form")
				$("#submitStatus").html("Success! " + data.submitStatus);
				woUtils.resetForm()
				woUtils.resetRoom()
			}
		},
		error: function(data) {
			$("#submitStatus").html("Javascript Error! " + data.responseText);
		},
 
		// other available options: 
		//url:       "processData?mode=form",         // override for form's 'action' attribute 
		//type:      "POST",        // 'get' or 'post', override for form's 'method' attribute 
		dataType:  'json'        // 'xml', 'script', or 'json' (expected server response type) 
		//clearForm: true,        // clear all form fields after successful submit 
		//resetForm: true        // reset the form after successful submit 
	});
});