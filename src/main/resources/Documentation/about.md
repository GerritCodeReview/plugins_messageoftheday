This plugin provides message of the day to be displayed in Gerrit UI.

The plugin makes use of Gerrit's MessageOfTheDay extension point.

The plugin will display the configured message at the top of the screen.
The user can dismiss the message banner. Upon dismissal, the banner will
be redisplayed on the web UI the next day at 00:00. The next day is
calculated with respect to the client. Note that, after dismissal, the
banner will be redisplayed (before the next day) if its message is updated.

The plugin also provides the ability to set the message via the UI. An
'announce' icon will be rendered in the top right of the header, which
will be visible only to users who have the 'updateBanner' capability.
