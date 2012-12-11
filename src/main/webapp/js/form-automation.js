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
		console.log("Toggling autofix visibility based on current mode " + $("#modeSelect").val())
		if($("#modeSelect").val() == "Normal") {
			$("#autoFixContainer").hide();
			if(!$("#modeContainer").hasClass("offset3"))
				$("#modeContainer").addClass("offset3")
			//JQuery can't do this good apparently, so old-school time
			document.getElementById("autoFix").selectedIndex = -1;
		} else {
			$("#autoFixContainer").show();
			$("#modeContainer").removeClass("offset3")
		}
	}
	//Show or hide based on preselected value
	toggleAutoFix();
	$('#modeSelect').change(function () {
		toggleAutoFix();
	});

	//AutoFixIt autoclosing issues
	woUtils.autoCloseIssues = function() {
		selectedIssues = $("#autoFix").val();
		console.log("AutoFixing all issues with " + selectedIssues)
		$(".issueBox").each(function() {
			//Find any issues that should be autofixed
			if($.inArray($(this).find(".issueSelect").val(), selectedIssues) != -1) {
				woUtils.setStatus($(this), "Closed");
				$(this).find(".issueInfo").html("(autofixed)")
			}
			//Find any issues that should no longer be autofixed
			else if($(this).find(".issueInfo").html() == "(autofixed)") {
				woUtils.setStatus($(this), "Open")
				$(this).find(".issueInfo").html("")
			}
		})
	}
	$('#autoFix').change(woUtils.autoCloseIssues);
	$(".issueSelect").change(woUtils.autoCloseIssues);

	//Status Message
	woUtils.setStatus = function(issueBox, val) {
		console.log("Updating status for " + issueBox.attr("id"))
		select = $(".issueStatus", issueBox);
		if(val != undefined)
			select.val(val)

		//Set color based on value (classes don't work on Chrome, manually set color)
		if(val == "Open")
			select.css("background-color", "#DA4F49")
		else if(val == "Waiting")
			select.css("background-color", "#FAA723")
		else if(val == "Closed")
			select.css("background-color", "#5BB75B")
	}
	$("#mainForm").on("change", ".issueStatus", function() {
		woUtils.setStatus($(this).closest(".issueBox"), $(this).val())
	});

	//Disable issue options that have already been used
	woUtils.autoDisableIssues = function() {
		console.warn("Auto disabling issues")

		//Undisable all the options
		$(".issueSelect option").removeAttr("disabled")

		//Gather all selected options that aren't the first "Choose One" option
		$(".issueSelect option").filter(":selected").not('[value=""]').each(function() {
			//Disable this option in all other issueSelect's
			console.debug("Current option: " + $(this).val())
			$(".issueSelect option[value='" + $(this).val() + "']").not($(this)).attr("disabled", "disabled")
		});
	}
	$("#mainForm").on("change", ".issueSelect", function() {
		woUtils.autoDisableIssues()
	});
});