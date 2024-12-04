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
    "starts_at": "Feb 4, 2020 5:53:00 PM",
    "expires_at": "Dec 30, 2020 6:00:00 PM",
    "html": "hello you!"
  }
```

## JSON Entities

### MessageOfTheDayInfo

The `MessageOfTheDayInfo` entity contains information about the message of the day.

* `id`: ID of the message generated based on the message content.
* `starts_at`: Date, when the message will be first displayed
* `expires_at`: Date, after which the message will not be displayed anymore
* `html`: String, containing the HTML-formatted message


[Back to @PLUGIN@ documentation index][index]

[index]: index.html
