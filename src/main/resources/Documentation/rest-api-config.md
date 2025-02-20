# @PLUGIN@ - /config/ REST API

This page describes the REST endpoints that are added by the @PLUGIN@
plugin.

Please also take note of the general information on the
[REST API](../../../Documentation/rest-api.html).

## Config Endpoints

### Get Menus
_GET /config/server/@PLUGIN@~message/_

Gets the message of the day.

#### Request

```
  GET /config/server/@PLUGIN@~message/ HTTP/1.0
```

As response a [MessageOfTheDayInfo](#messageofthedayinfo) entity
is returned that contains the message and associated metadata.

#### Response

```
  HTTP/1.1 200 OK
  Content-Disposition: attachment
  Content-Type: application/json;charset=UTF-8

  )]}'
  {
    "id": "hello",
    "content_id": "768630922",
    "starts_at": "Feb 4, 2020 5:53:00 PM",
    "expires_at": "Dec 30, 2020 6:00:00 PM",
    "html": "hello you!"
  }
```

### Set Message
_POST /config/server/@PLUGIN@~message/_

Sets the message of the day. Only users with `Update Banner` capability on the
server can call this API. In the request body, the data must be provided as
a [MessageInput](#messageinput) entity.

#### Request

```
  POST /config/server/@PLUGIN@~message/ HTTP/1.0
  Content-Type: application/json; charset=UTF-8
  {
    "message": "sample announcement",
    "expires_at": "11/11/2024, 11:11 PM PST"
  }
```

#### Response

```
  HTTP/1.1 200 OK
```

## JSON Entities

### MessageOfTheDayInfo

The `MessageOfTheDayInfo` entity contains information about the message of the day.

* `id`: ID of the message
* `content_id`: ID generated based on the message content
* `starts_at`: Date, when the message will be first displayed
* `expires_at`: Date, after which the message will not be displayed anymore
* `html`: String, containing the HTML-formatted message

### MessageInput

The `MessageInput` entity contains information about setting the message.

| Field Name   |          | Description                                                                                                      |
|--------------|----------|------------------------------------------------------------------------------------------------------------------|
| `message`    |          | The message to display in the banner.                                                                            |
| `expires_at` | optional | Date, after which the message will not be displayed anymore. Must be specified in `MM/dd/yyyy, hh:mm a z` format |

[Back to @PLUGIN@ documentation index][index]

[index]: index.html
