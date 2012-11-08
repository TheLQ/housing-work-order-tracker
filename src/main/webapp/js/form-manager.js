/*
 * Copyright (C) 2012 Leon Blakey <lord.quackstar at gmail.com>, RATS 2012
 *
 * This file is part of University of Louisville Housing WorkOrder System.
 */
(function($){
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
	 * So here I present a wall of (probably ugly) javascript. I'm sorry.
	 * -Leon, 11/1/12
	 */
	
	var mainForm = $("#mainForm");
	
	//Rename all the fields to a standard parsable format
	$("input, select, textarea").each(function() {
		prefix = "issues[0]";
		if($(this).parent().attr("id") == "notesBox")
			prefix = prefix + "[notes][0]";
		$(this).attr("name", prefix + $(this).attr("name"));
	});
	
	//Handlers for add/remove issue buttons
	mainForm.children("#addIssue").on("click", function(event) {
		event.preventDefault();
		lastIssueBox = mainForm.children(".issueBox").last();
		issueId = lastIssueBox.attr("id").replace( /^\D+/g, '');
		clonedIssueBox = lastIssueBox.clone();
		
		//Reset the cloned issue since it might have data in it
		resetIssue(clonedIssueBox);
		
		//Finished, add to the end
		clonedIssueBox.insertAfter(lastIssueBox)
	});
	mainForm.children("#removeIssue").on("click", function(event) {
		event.preventDefault();
		lastIssueBox = mainForm.children(".issueBox").last();
		
		//Check if there is data
		isData = false;
		if($(".statusSelect", lastIssueBox).get().selectedIndex < 1)
			isData = true;
		$(".note, .noteDate", lastIssueBox).each(function(){
			if($(this).val().length != 0)
				isData = true;
		});
	
		//If there is data, prompt the user for confirmation
		if(isData)
			if(!confirm("The last issue has data in it. Are you sure you wish to remove it?"))
				return;
		
		//All good, remove it
		lastIssueBox.remove();
	});
	
	//Handlers for add/remove note buttons
	mainForm.on("click", ".addNote", function(event){
		event.preventDefault();
		notesContainer = $(this).parent();
		allBoxes = notesContainer.children(".notesBox");
		lastNotesBox = allBoxes.last();
		clonedNotesBox = lastNotesBox.clone();
		
		//Start setting info
		noteId = allBoxes.length + 1
		issueId = lastNotesBox.attr("id").replace( /^\D+/g, '');
		clonedNotesBox.attr("id", "notesBox" + noteId);
		clonedNotesBox.children(".note").attr("name", genName(issueId, null, noteId, "note"));
		clonedNotesBox.children(".noteDate").attr("name", genName(issueId, null, noteId, "noteDate"));
		
		//Add
		clonedNotesBox.insertAfter(lastNotesBox);
		
		autoDisableNoteRemove(notesContainer);
		return false;
	});
	mainForm.on("click", ".removeNote", function(event){
		event.preventDefault();
		notesContainer = $(this).parent();
		lastNotesBox = notesContainer.children(".notesBox").last();
		
		//Make sure the text and date fields are empty
		if(lastNotesBox.children(".noteDate").val().length != 0 || lastNotesBox.children(".note").val().length != 0)
			console.log("Ignoring remove, notes are not empty")
		else
			lastNotesBox.remove();
		
		autoDisableNoteRemove(notesContainer);
	});
	
	//Automatically disable remove note button if text is entered into the last note
	function autoDisableNoteRemove(notesContainer) {
		removeButton = notesContainer.children(".removeNote")
		allNotes = notesContainer.children(".notesBox");
		if(allNotes.length == 1) {
			removeButton.hide();
			return;
		}
		notesBox = allNotes.last();
		if(notesBox.children(".note").val().length != 0 || notesBox.children(".noteDate").val().length != 0)
			removeButton.hide();
		else
			removeButton.show();
	}
	mainForm.on("keyup", ".note, .noteDate", function(){
		autoDisableNoteRemove($(this).parent().parent());
	});
	
	function resetIssue(issueBox) {
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
	
	function genName(issueId, issueField, noteId, noteField) {
		name = "issues[" + issueId + "]";
		if(noteId != undefined)
			name = name + "[note][" + noteId + "]" + noteField;
		else
			name = name + issueField
		return name;
	}
})(jQuery);


