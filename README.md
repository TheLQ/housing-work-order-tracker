This was an tablet-friendly web app to track work orders from student dorms on campus. Requests would be entered into the form by the tech specifying name, building, room, category, issue details, etc. Afterwards dated notes could be added for when issues took multiple visits. There was also a bulk-fix issues mode for quickly updating all issues on the floors of different buildings.

It was designed to store all of its data into a large Google Docs Spreadsheet as future techs are college students who might not be Java developers. If the app stops working for any reason the spreadsheet containing all the data is still easily available instead of being in a difficult to access MySQL database. Google Docs is also free for the foreseeable future.

The app itself is a single page JQuery Javascript powered web app backed by Java servlet designed to deploy to Google App Engine. It uses JQuery, the Google Docs Spreadsheet API, Maven, Apache Wicket (templates), Jetty (local server), and other libraries.

This was written while I was a student at the University of Louisville. **This code is public domain, do whatever you want with it.**
