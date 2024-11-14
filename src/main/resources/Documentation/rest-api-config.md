@PLUGIN@ - /config/ REST API
============================

This page describes the REST endpoints that are added by the @PLUGIN@
plugin.

Please also take note of the general information on the
[REST API](../../../Documentation/rest-api.html).

<a id="config-endpoints"> Config Endpoints
------------------------------------------

### <a id="get-menus"> Get Menus
_GET /config/server/@PLUGIN@~message/_

Gets the message of the day.

#### Request

```
  GET /config/server/@PLUGIN@~message/ HTTP/1.0
```

As response a [MessageOfTheDayInfo](#message-of-the-day-info) entity
is returned that contains the message and associated metadata.

#### Response

```
  HTTP/1.1 200 OK
  Content-Disposition: attachment
  Content-Type: application/json;charset=UTF-8

  )]}'
  {
    "id": "hello",
    "starts_at": "Feb 4, 2020 5:53:00 PM",
    "expires_at": "Dec 30, 2020 6:00:00 PM",
    "html": "hello you!"
  }
```

### <a id="set-message"> Set Message
_POST /config/server/@PLUGIN@~message/_

Sets the message of the day. Only users with `Update Banner` capability on the
server can call this API. In the request body, the data must be provided as
a [MessageInput](#message-input) entity. `message` is
required. `expire_after` is optional and its value should use common unit
suffixes:

* s, sec, second, seconds
* m, min, minute, minutes
* h, hr, hour, hours
* d, day, days
* w, week, weeks (`1 week` is treated as `7 days`)
* mon, month, months (`1 month` is treated as `30 days`)
* y, year, years (`1 year` is treated as `365 days`)

#### Request

```
  POST /config/server/@PLUGIN@~message/ HTTP/1.0
  Content-Type: application/json; charset=UTF-8
  {
    "message": "sample announcement",
    "expire_after": "1 week"
  }
```

#### Response

```
  HTTP/1.1 200 OK
```

### <a id="message-of-the-day-info"> MessageOfTheDayInfo

The `MessageOfTheDayInfo` entity contains information about the message of the day.

* `id`: ID of the message generated based on the message content.
* `starts_at`: Date, when the message will be first displayed
* `expires_at`: Date, after which the message will not be displayed anymore
* `html`: String, containing the HTML-formatted message

### <a id="message-input"> MessageInput

The `MessageInput` entity contains information about setting the message.

| Field Name     | Description                                   |
|----------------|-----------------------------------------------|
| `message`      | The message to display in the banner.         |
| `expire_after` | The duration after which the message expires. |


[Back to @PLUGIN@ documentation index][index]

[index]: index.html
