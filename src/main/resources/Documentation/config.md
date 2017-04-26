# Configuration

Configuration of the @PLUGIN@ plugin is done in the `messageoftheday.config`
file in the site's `etc` directory.

NOTE: when the `messageoftheday.config` file changes, the plugin needs to
be reloaded for the configuration change to take effect.

## `messageoftheday.config` file format

```
  [message]
    id = hello
    expiresAt = 20161209:1730
```

message.id
:	The ID of the current message of the day.

message.expiresAt
:	Expiration date:time for the current message of the day. The message will be
	displayed up to and including that date:time. The message will not be displayed
	any more after that date:time. This field is required. If not set, the message
	will not be displayed.
	The format of this field is `YYYYMMdd:HHmm`.

## Message content and location

A message is an HTML file, named like `<message.id>.html` and stored in the
site's `data/@PLUGIN@/` directory. For the above example, the message.id is
`hello` which means that the file containing the message content is stored under:

```
  $GERRIT_SITE/data/@PLUGIN@/hello.html
```

The message content is an HTML snippet. For example:

```
  Hello, this is a <b>message of the day</b>
```
