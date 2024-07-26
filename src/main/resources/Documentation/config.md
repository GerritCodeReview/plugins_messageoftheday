# Configuration

Configuration of the @PLUGIN@ plugin is done in the `messageoftheday.config`
file. By default, it is read from site's `etc` directory. Its location can
be overridden from `gerrit.config`.

NOTE: when the `messageoftheday.config` file in site's `etc` directory changes,
the plugin needs to be reloaded for the configuration change to take effect.

## `messageoftheday.config`

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
```
  [messageoftheday "project"]
    name = motd
    branch = refs/heads/master
    configDir = configurations
    dataDir = html_content
```
project.name
:	The project name containing `messageoftheday.config` and the html file with
the message content.

project.branch
:	The project branch containing `messageoftheday.config` and the html file
with the message content. Defaults to `master` branch.

project.configDir
:	The path in the project where `messageoftheday.config` is located. If this
is not set, it is expected to be in the root of the repository.

project.dataDir
:	The path in the project where `<message.id>.html` is located. If this
is not set, it is expected to be in the root of the repository.

## Message content and location

A message is an HTML file, named like `<message.id>.html` and expected in the
site's `data/@PLUGIN@/` directory, by default. The message content is an HTML
snippet. For example:

```
  Hello, this is a <b>message of the day</b>
```

For a configuration like below, the message.id is `hello` and the file containing
the message content is expected at `$GERRIT_SITE/data/@PLUGIN@/hello.html`.

`messageoftheday.config:`
```
  [message]
    id = hello
    startsAt = 20170803:1420
    expiresAt = 20170810:1730
```
### Override default message location

The location of `<message.id>.html` can be overridden using `gerrit.config`. For a
configuration like below, the message.id is `hello` and the file containing the
message content is expected at `html_content/hello.html` in the repository
`mtod.git:refs/heads/master`.

```
  gerrit.config:
    [messageoftheday "project"]
      name = motd
      branch = refs/heads/master
      configDir = configurations
      dataDir = html_content
  messageoftheday.config:
    [message]
      id = hello
      expiresAt = 20240810:1730
```
