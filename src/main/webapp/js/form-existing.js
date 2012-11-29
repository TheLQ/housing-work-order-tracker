/*
 * Copyright (C) 2012 Leon Blakey <lord.quackstar at gmail.com>, RATS 2012
 *
 * This file is part of University of Louisville Housing WorkOrder System.
 */
$(document).ready(function(){
	/**
	 * Script to handle the existing issue part of the form
	 */
	existingContainer = $("#existingContainer")

	woUtils.injectExisting = function(data) {
		woUtils.resetExisting()

		//Hide the entire container if there's no data
		$(".existingBox", existingContainer).show();
		if ($.isEmptyObject(data)) {
			$(".existingBox", existingContainer).hide();
			return;
		}

		for(var location in data) {
			console.info("Starting to parse existing issues at " + location)
			lastExistingBox = $(".existingBox", existingContainer).last()

			//Create a new existingIssueBox if the last one has data in it
			if($(".existingSheetId", lastExistingBox).html().length != 0) {
				newExistingBox = lastExistingBox.clone()
				newExistingBox.insertAfter(lastExistingBox)
				woUtils.resetExistingIssue(newExistingBox)
				lastExistingBox = newExistingBox
			}

			//Show location
			$(".existingLocation", lastExistingBox).html(location);

			//Parse each issue that is under this location
			locationData = data[location]
			for(i in locationData) {
				console.debug("Parsing existing issue #" + i + " at location " + location)

				//Get the last existingIssueBox or create a new one if there's already data
				lastExistingIssueBox = $(".existingIssueBox", lastExistingBox).last();
				if($(".existingSheetId", lastExistingIssueBox).html().length != 0) {
					newExistingIssueBox = lastExistingIssueBox.clone();
					newExistingIssueBox.insertAfter(lastExistingIssueBox)
					lastExistingIssueBox = newExistingIssueBox;
				}

				$(".existingSheetId", lastExistingIssueBox).html(locationData[i]["sheetId"])
				$(".existingIssue", lastExistingIssueBox).html(locationData[i]["issue"])
				$(".existingOpened", lastExistingIssueBox).html(locationData[i]["opened"] + " (" + locationData[i]["openedAge"] + " days old)")
				$(".existingWaitingWrap", lastExistingIssueBox).hide()
				if(locationData[i]["waiting"].length != 0) {
					$(".existingWaiting", lastExistingIssueBox).html(locationData[i]["waiting"] + " (" + locationData[i]["waitingAge"] + " days old)")
					$(".existingWaitingWrap", lastExistingIssueBox).show()
				}

				//Set status
				$(".existingStatus", lastExistingIssueBox).html(locationData[i]["status"])
				$(".existingStatus", lastExistingIssueBox).removeClass("label-important label-warning label-success")
				if(locationData[i]["status"] == "Open")
					$(".existingStatus", lastExistingIssueBox).addClass("label-important")
				else if(locationData[i]["status"] == "Waiting")
					$(".existingStatus", lastExistingIssueBox ).addClass("label-warning")
				else
					alert("Unknown status " + locationData[i]["status"] + " contained at position " + location)

				//Note handling
				$(".existingLastNoteContainer", lastExistingIssueBox).hide()
				$(".exisitngNoteToggle", lastExistingIssueBox).hide()
				if(locationData[i]["notes"][0]["note"].length != 0) {
					//Show the last note
					lastNote = locationData[i]["notes"][locationData[i]["notes"].length - 1]
					$(".existingLastNote", lastExistingIssueBox).html(lastNote["note"])
					$(".existingLastNoteDays", lastExistingIssueBox).html(lastNote["noteAge"])
					$(".existingLastNoteContainer", lastExistingIssueBox).show()
					$(".exisitngNoteToggle", lastExistingIssueBox).show()

					//Configure the rest of the notes for long view
					notes = locationData[i]["notes"];
					for(j in notes) {
						lastNoteBox = $(".existingNoteBox", lastExistingIssueBox).last()
						$(".existingNoteDate", lastNoteBox).html(notes[j]["noteDate"])
						$(".existingNoteAge", lastNoteBox).html(notes[j]["noteAge"])
						$(".existingNote", lastNoteBox).html(notes[j]["note"])
						addExistingNote(lastExistingIssueBox)
					}
					//Remove last extra blank issue
					$(".existingNoteBox", lastExistingIssueBox).last().remove()
				}
			}
		}
	}

	$("#existingContainer").on("click", ".exisitngNoteToggle", function() {
		text = $(this).html();
		existingIssueBox = $(this).parent()
		if(text == "Show more") {
			$(".existingLastNoteContainer", existingIssueBox).hide()
			$(".existingNoteContainer", existingIssueBox).show()
			$(this).html("Show less")
		} else if(text == "Show less") {
			$(".existingLastNoteContainer", existingIssueBox).show()
			$(".existingNoteContainer", existingIssueBox).hide()
			$(this).html("Show more")
		}
	})

	//Note: Here we don't care about keeping the IDs up to date since its not being submitted
	function addExistingNote(existingIssueBox) {
		lastNoteBox = $(".existingNoteBox", existingIssueBox).last()

		newNoteBox = lastNoteBox.clone()
		newNoteBox.insertAfter(lastNoteBox)
		resetExistingNote(newNoteBox);
	}

	function resetExistingNote(existingNoteBox) {
		$(".existingNoteDate", existingNoteBox).html("")
		$(".existingNoteAge", existingNoteBox).html("")
		$(".existingNote", existingNoteBox).html("")
	}

	woUtils.resetExistingIssue = function(existingBox) {
		//Reset to default values
		$(".existingWaitingWrap", existingBox).hide();
		$(".existingLocation", existingBox).html("");
		$(".existingSheetId", existingBox).html("")
		$(".existingStatus", existingBox).html("")
		$(".existingIssue", existingBox).html("")
		$(".existingOpened", existingBox).html("")
		$(".existingWaiting", existingBox).html("")
		$(".existingLastNoteContainer", existingBox).hide();
		$(".existingLastNote", existingBox).html("")
		$(".existingLastNote", existingBox).html("")
		$(".existingLastNoteDays", existingBox).html("");
		$(".exisitngNoteToggle", existingBox).html("Show more")

		//Reset notes
		$(".existingNoteBox", existingBox).each(function(i) {
			if(i == 0)
				resetExistingNote($(this))
			else
				$(this).remove()
		})

		existingBox.children(".existingIssueBox").each(removeAllButFirst);
	}

	woUtils.resetExisting = function() {
		//Remove all existing existingBox's and existingIssueBox's except the first one
		existingContainer.children(".existingBox").each(removeAllButFirst);

		//Reset remaining issue
		woUtils.resetExistingIssue($(".existingBox", existingContainer))
	}

	//When the user presses the load button, load the issue
	existingContainer.on("click", ".existingLoad", function() {
		//Extract building and room from location and update values
		locationParts = $(".existingLocation", $(this).parent()).html().split(" ")
		woUtils.loadIssues(locationParts[0], locationParts[1])
	})

	function removeAllButFirst(i) {
		if(i == 0)
			return;
		$(this).remove()
	}
});