This plugin provides message of the day to be displayed in Gerrit UI.

The plugin makes use of Gerrit's MessageOfTheDay extension point.

The plugin will display the configured message at the top of the screen.
The user can dismiss the message banner. Upon dismissal, the banner will be
redisplayed on the web UI the next day at 00:00. (The next day is calculated
w.r.t the client)
