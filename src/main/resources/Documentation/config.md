# Configuration

Configuration of the @PLUGIN@ plugin is done in the `messageoftheday.config`
file in the site's `etc` directory by default. Its location can be configured
in `gerrit.config`.

NOTE: when the `messageoftheday.config` file changes, the plugin needs to
be reloaded for the configuration change to take effect.

## `messageoftheday.config` file format

```
  [message]
    id = hello
    startsAt = 20170803:1420
    expiresAt = 20170810:1730
```

message.id
:	The ID of the current message of the day.

message.startsAt
:	Start date:time for the current message of the day. The message will be
	displayed starting from and including that date:time. The message will not
	be displayed before that date:time. This field is optional.
	The format of this field is `yyyyMMdd:HHmm`.

message.expiresAt
:	Expiration date:time for the current message of the day. The message will be
	displayed up to and including that date:time. The message will not be displayed
	any more after that date:time. This field is required. If not set, the message
	will not be displayed.
	The format of this field is `yyyyMMdd:HHmm`.

## `gerrit.config`
For file based configuration:
```
  [plugin "@PLUGIN@"]
    configDir = /full/path/to/configDir
    dataDir = /full/path/to/htmlContentDir
```

For Git based configuration:
```
  [plugin "@PLUGIN@"]
    gitRepository = motd
```

plugin.@PLUGIN@.configDir
:	The path where `@PLUGIN@.config` is located. Defaults to site's `etc/` directory.

plugin.@PLUGIN@.dataDir
:	The path where `<message.id>.html` is located. Defaults to site's `data/@PLUGIN@/` directory.

plugin.@PLUGIN@.gitRepository
: The name of the Git repository where `@PLUGIN.config` and `<message.id>.html` are located.
  Both `@PLUGIN.config` and `<message.id>.html` must be placed in the `master`
  branch of this repository, in the root directory.

## Message content and location

A message is an HTML file, named like `<message.id>.html` and stored by default in
the site's `data/@PLUGIN@/` directory. For a config like below, the message.id is
`hello` which means that the file containing the message content is stored under
`$GERRIT_SITE/data/@PLUGIN@/hello.html`

```
  [message]
    id = hello
    startsAt = 20170803:1420
    expiresAt = 20170810:1730
```

The message content is an HTML snippet. For example:

```
  Hello, this is a <b>message of the day</b>
```
