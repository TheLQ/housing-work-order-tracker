/*
 * Copyright (C) 2012 Leon Blakey <lord.quackstar at gmail.com>, RATS 2012
 *
 * This file is part of University of Louisville Housing WorkOrder System.
 */
$(document).ready(function(){
	//Dynamic room lookup
	woUtils.loadIssues = function(building, room) {
		console.debug("Room: " + room.length + " | Building: " + $("#building")[0].selectedIndex)

		//Ignore rooms with less than 3 characters, user is probably typing in a room
		if(room.length < 3)
			return;

		//Ignore building that is unselected
		if($("#building")[0].selectedIndex == 0)
			return;

		//Passed validation, get room info from server
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
	$("#room, #building").on('input change', function() {
		woUtils.loadIssues($("#room").val(), $("#building option:selected").val())
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

			/* Form Checks */
			if($("#building").val().length == 0) {
				alert("You haven't selected a building!")
				return false;
			}
			else if($("#room").val().length == 0) {
				alert("You haven't selected a room!")
				return false;
			}

			allGood = true;
			$(".note").each(function() {
				//See if the length is 0 and the note isn't the first one in the issue
				if($(this).val().length == 0 && $(".note", $(this).parent().parent()).index($(this)) != 0) {
					alert("A note is empty. Please make sure all notes have text or are removed");
					allGood = false;
					return false;
				}
			});
			if(!allGood)
				return allGood;
			$(".issueSelect").each(function() {
				if($(this)[0].selectedIndex == 0) {
					alert("An issue isn't selected. Please select an issue")
					allGood = false;
					return false;
				}
			});
			return allGood
		},
		success: function(data) {
			if(typeof data.error != 'undefined')
				$("#submitStatus").html("Server Error! " + data.error);
			else {
				console.log("Finished submitting form")
				$("#submitStatus").html("Success! " + data.submitStatus);
				woUtils.resetForm()
				woUtils.resetRoom()
				woUtils.handleExisting($("#existBuilding").val())
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

	//Existing issue handling
	function handleExisting(building) {
		$("#existingStatus").html("Loading...")

		$.ajaxSetup ({
			cache: false
		});
		$.getJSON("processData?mode=existing", {
			building: building
		}, function(json) {
			if(typeof json.error != 'undefined')
				$("#existingStatus").html("Server Error! " + json.error);
			else {
				console.log("Finished querying exisitng issues")
				$("#existingStatus").html(json.response)
				woUtils.injectExisting(json.data);
			}
		}
		);
	}
	$("#existBuilding").on("change", function() {
		handleExisting($(this).val())
	})
	handleExisting($("#existBuilding").val())
});