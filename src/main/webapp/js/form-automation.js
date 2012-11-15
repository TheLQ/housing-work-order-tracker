/*
 * Copyright (C) 2012 Leon Blakey <lord.quackstar at gmail.com>, RATS 2012
 *
 * This file is part of University of Louisville Housing WorkOrder System.
 */
$(document).ready(function(){
	/**
	 * This (somewhat poorly named) script handles general automation of the form,
	 * or essentially stuff that doesn't fit elsewhere 
	 */
	
	//AutoFixIt display toggle
	function toggleAutoFix() {
		if($("#modeSelect").val() == "Normal") {
			$("#autoFix").hide();
			//JQuery can't do this good apparently, so old-school time
			document.getElementById("autoFix").selectedIndex = -1;
		} else
			$("#autoFix").show();
	}
	//Show or hide based on preselected value
	toggleAutoFix();
	$('#modeSelect').change(function (e) {
		toggleAutoFix();
	});
				
	//AutoFixIt autoclosing issues
	function autoCloseIssues() {
		selectedIssues = $("#autoFix").val();
		$(".issueBox").each(function() {
			//Find any issues that should be autofixed
			if($.inArray($(this).find(".issueSelect").val(), selectedIssues) != -1) {
				setStatus($(this), "Closed");
				$(this).find(".issueInfo").html("(autofixed)")
			}
			//Find any issues that should no longer be autofixed
			else if($(this).find(".issueInfo").html() == "(autofixed)") {
				setStatus($(this), "Open")
				$(this).find(".issueInfo").html("")
			}
		})
	}
	$('#autoFix').change(autoCloseIssues);
	$(".issueSelect").change(autoCloseIssues);
				
	//Status Message
	woUtils.setStatus = function(issueBox, val) {
		console.log("Updating status for " + issueBox.attr("id"))
		select = $(".issueStatus", issueBox);
		select.val(val)
					
		//Set color based on value
		if(val == "Open")
			select.removeClass("btn-danger btn-warning btn-success").addClass("btn-danger");
		else if(val == "Waiting")
			select.removeClass("btn-danger btn-warning btn-success").addClass("btn-warning");
		else if(val == "Closed")
			select.removeClass("btn-danger btn-warning btn-success").addClass("btn-success")
	}
	$("#mainForm").on("change", ".issueStatus", function(event) {
		//event.preventDefault()
		woUtils.setStatus($(this).closest(".issueBox"), $(this).val())
	});
});