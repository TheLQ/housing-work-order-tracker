/*
 * Copyright (C) 2012 Leon Blakey <lord.quackstar at gmail.com>, RATS 2012
 *
 * This file is part of University of Louisville Housing WorkOrder System.
 */
/* 
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
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
	
	//Handlers for all add/remove buttons
	$("#notesContainer", mainForm).on("click", "#addNote", function(event){
		event.preventDefault();
		allBoxes = $(".notesBox", $(this).parent());
		lastNotesBox = allBoxes.last();
		clonedNotesBox = lastNotesBox.clone();
		
		//Start setting info
		noteId = allBoxes.length + 1
		console.log($(this).parent().parent().attr("id"));
		issueId = $(this).parent().parent().attr("id").replace( /^\D+/g, '');
		if(issueId.length == 0)
			issueId = 0;
		console.log("issueId: " + issueId)
		console.log("genName: " + genName(issueId, null, noteId, "Test"))
		clonedNotesBox.attr("id", "notesBox" + noteId);
		$(".note", clonedNotesBox).attr("name", genName(issueId, null, noteId, "note"));
		$(".noteDate", clonedNotesBox).attr("name", genName(issueId, null, noteId, "noteDate"));
		
		//Add
		clonedNotesBox.insertAfter(lastNotesBox);
		
		return false;
	});
	$("#notesContainer", mainForm).on("click", "#removeNote", function(event){
		event.preventDefault();
		lastNotesBox = $(".notesBox", $(this).parent()).last();
		
		//Make sure the text and date fields are empty
		if($(".noteDate", lastNotesBox).val().length != 0 || $(".note", lastNotesBox).val().length != 0)
			console.log("Ignoring remove, notes are not empty")
		else
			lastNotesBox.remove();
	});
	
	
	
	
	
	
	function genName(issueId, issueField, noteId, noteField) {
		name = "issues[" + issueId + "]";
		if(noteId != undefined)
			name = name + "[note][" + noteId + "]" + noteField;
		else
			name = name + issueField
		return name;
	}
})(jQuery);


