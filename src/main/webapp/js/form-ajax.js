/*
 * Copyright (C) 2012 Leon Blakey <lord.quackstar at gmail.com>, RATS 2012
 *
 * This file is part of University of Louisville Housing WorkOrder System.
 */
$(document).ready(function(){
	//Dynamic room lookup
	woUtils.loadIssues = function(building, room) {
		//Ignore rooms with less than 3 characters, user is probably typing in a room
		if(room.length < 3)
			return;

		//Make sure building isn't the default choice
		if(building == $("#building option").first().val())
			return;

		$("#building").val(building)
		$("#room").val(room)

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
		woUtils.loadIssues($("#building option:selected").val(), $("#room").val())
	});

	//Automagical AJAX submit
	$("#mainForm").ajaxForm({
		//target:        '#submitStatus',   // target element(s) to be updated with server response
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

				//Autoloading of next room
				mode = $("#modeSelect").val();
				if(mode != "Normal" && $(".existingLocation").length > 1) {
					//Determine exactly what is the "next room"
					building = "";
					room = "";
					if(mode == "Walkthrough") {
						building = $("#building").val()
						room = parseInt($("#room").val(), 10) + 1
					} else {
						//Autofix mode, try to find the next issue
						found = false;
						$(".existingLocation").each(function() {
							if(found) {
								//Previous iteration found the location, so return this one
								locations = $(this).html().split(" ")
								building = locations[0]
								room = locations[1]
								return false;
							}
							if($(this).html() == $("#building").val() + " " + $("#room").val())
								found = true;
						})
						console.debug("Autoloating next issue Building: " + building + " | Next room: " + room)
					}
					//Make sure we have enough data
					if(building.length != 0 && room.length != 0)
						woUtils.loadIssues(building, room)
				} else {
					woUtils.resetRoom()
				}
				handleExisting()
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
	function handleExisting() {
		$("#existingStatus").html("Loading...")

		$.ajaxSetup ({
			cache: false
		});
		$.getJSON("processData?mode=existing", {
			building: $("#existBuilding").val(),
			sort: $("#existingSort").val()
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
	$("#existBuilding, #existingSort").on("change", function() {
		handleExisting()
	})
	handleExisting()
});