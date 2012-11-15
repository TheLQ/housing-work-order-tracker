/*
 * Copyright (C) 2012 Leon Blakey <lord.quackstar at gmail.com>, RATS 2012
 *
 * This file is part of University of Louisville Housing WorkOrder System.
 */
$(document).ready(function(){
	/**
	 * Manages the backend form handling
	 * 
	 * Question: So why is this nessesary? Why can't we use the automagical Jquery Dynamic Form plugin?
	 * Answer: The Jquery Dynamic Form plugin is the reason this project has taken 
	 * 2+ months to write. Yes it does 99% of what were doing here, but the messy
	 * persistant internal state doesn't mesh well with our super dynamic form.
	 * 
	 * Want to reset the form to a clean state? Sorry, you can't. Want to remove
	 * injected values? Sorry, you can't. Want to use the form more than twice
	 * before it becomes horribly broken? Keep dreaming. 
	 * 
	 * I learned something in this project: Do not always resort to libraries. This 
	 * library is the reason this project took 2 extra months to write.
	 * 
	 * So here I present a wall of (probably ugly) javascript. I'm sorry.
	 * -Leon, 11/1/12
	 */
	
	var mainForm = $("#mainForm");
	
	//Rename all the fields to a standard parsable format
	function updateName() {
		console.log("Updating all names in form")
		allBoxes = $(".issueBox", mainForm)
		allBoxes.each(function(issueId) {
			curIssueBox = $(this)
			allNotes = $(".notesBox", curIssueBox );
			//Update all notesBoxes 
			$("input, select, textarea", curIssueBox).each(function() {
				prefix = "issues[" + issueId + "]";
				parent = $(this).parent()
				if(parent.hasClass("notesBox")) {
					//Figure out the position of the note
					noteId = allNotes.index(parent)
					prefix = prefix + "[notes][" + noteId + "]";
					
					//Update notesBox id
					parent.attr("id", "notesBox" + noteId)
				}
				$(this).attr("name", prefix + $(this).attr("class"));
				
				//Set issuebox id
				curIssueBox.attr("id", "issueBox" + issueId)
			});
		});
		
	}
	updateName();
	
	/**
	 * Handling add/remove issue buttons
	 */
	 woUtils.addIssue = function() {
		lastIssueBox = mainForm.children(".issueBox").last();
		clonedIssueBox = lastIssueBox.clone();
		
		//Reset the cloned issue since it might have data in it
		resetIssue(clonedIssueBox);
		
		//Finished, add to the end
		clonedIssueBox.insertAfter(lastIssueBox)
		
		//Change the status to open with correct color
		woUtils.setStatus(clonedIssueBox, "Open")
		
		updateName();
		autoDisableIssueRemove();
	}
	mainForm.children("#addIssue").on("click", function(event) {
		event.preventDefault()
		woUtils.addIssue()
	});
	mainForm.children("#removeIssue").on("click", function(event) {
		event.preventDefault();
		lastIssueBox = mainForm.children(".issueBox").last();
	
		//If there is data, prompt the user for confirmation
		if(issueHasData(lastIssueBox))
			if(!confirm("The last issue has data in it. Are you sure you wish to remove it?"))
				return;
		
		//All good, remove it
		lastIssueBox.remove();
		
		autoDisableIssueRemove();
	});
	function autoDisableIssueRemove(){
		allBoxes = mainForm.children(".issueBox");
		if(allBoxes.length == 1) {
			$("#removeIssue").attr("disabled", "disabled");
			return;
		}
		lastNotesBox = allBoxes.last();
		if($(".sheetId", mainForm).val() > 0)
			$("#removeIssue").attr("disabled", "disabled");
		else
			$("#removeIssue").removeAttr("disabled")
	}
	mainForm.on("change", ".issueSelect", autoDisableIssueRemove);
	mainForm.on("keyup", ".noteDate, .note", autoDisableIssueRemove);
	
	/**
	 * Handling of add/remove note buttons
	 */
	woUtils.addNote = function(issueBox){
		console.log("Adding a note")
		notesContainer = $(".notesContainer", issueBox);
		allBoxes = notesContainer.children(".notesBox");
		lastNotesBox = allBoxes.last();
		clonedNotesBox = lastNotesBox.clone();
		
		//Add
		clonedNotesBox.insertAfter(lastNotesBox);
		
		autoDisableNoteRemove(notesContainer);
		updateName();
		return false;
	}
	mainForm.on("click", ".addNote", function(event) {
		event.preventDefault();
		woUtils.addNote($(this).closest(".issueBox"))
	});
	mainForm.on("click", ".removeNote", function(event){
		event.preventDefault();
		console.log("Clicked remove note")
		notesContainer = $(this).parent();
		lastNotesBox = notesContainer.children(".notesBox").last();
		
		//Make sure the text and date fields are empty
		if(lastNotesBox.children(".noteDate").val().length != 0)
			console.log("Ignoring remove, note has date which means its in the spreadsheet")
		else if(lastNotesBox.children(".note").val().length != 0)
			if(!confirm("The last note has data in it. Are you sure you wish to remove it?"))
				return;
		else
			lastNotesBox.remove();
		else
			lastNotesBox.remove();
		
		autoDisableNoteRemove(notesContainer);
	});
	
	//Automatically disable remove note button if text is entered into the last note
	function autoDisableNoteRemove(notesContainer) {
		console.log("Enabling/disabling Note remove button on " + notesContainer.parent().attr("id"))
		
		removeButton = notesContainer.children(".removeNote")
		allNotes = notesContainer.children(".notesBox");
		if(allNotes.length == 1) {
			removeButton.attr("disabled", "disabled");
			return;
		}
		notesBox = allNotes.last();
		console.log("Notes date length: " + notesBox.children(".noteDate").val().length)
		if(notesBox.children(".noteDate").val().length != 0)
			removeButton.attr("disabled", "disabled");
		else
			removeButton.removeAttr("disabled")
	}
	mainForm.on("keyup", ".note, .noteDate", function(){
		autoDisableNoteRemove($(this).parent().parent());
	});
	
	/**
	 * Utilities
	 */
	function issueHasData(issueBox) {
		isData = false;
		if($(".statusSelect", issueBox).get().selectedIndex < 1)
			isData = true;
		$(".note, .noteDate", issueBox).each(function(){
			if($(this).val().length != 0)
				isData = true;
		});
		
		return isData;
	}
	
	function resetIssue(issueBox) {
		console.log("Resetting issue " + issueBox.attr("id"))
		
		//Remove all noteBoxes except 1
		notesContainer = issueBox.children(".notesContainer");
		notesContainer.children(".notesBox").each(function(i) {
			if(i == 0)
				return;
			$(this).remove()
		});
		
		//Reset the remaining noteBox
		lastNotesBox = notesContainer.children(".notesBox")
		lastNotesBox.children(".note").val("")
		lastNotesBox.children(".noteDate").val("")
	
		//Reset status
		//TODO
		
		//Reset issue select
		$(".issueSelect", issueBox).get().selectedIndex = -1;
	}
	
	woUtils.resetForm = function() {
		console.log("Resetting form...")
		mainForm.children(".issueBox").each(function(i) {
			if(i == 0)
				resetIssue($(this))
			else
				$(this).remove();
		});
	}
	
	function genName(issueId, issueField, noteId, noteField) {
		name = "issues[" + issueId + "]";
		if(noteId != undefined)
			name = name + "[note][" + noteId + "]" + noteField;
		else
			name = name + issueField
		return name;
	}
});


