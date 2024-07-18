# REST API Metabase driver

This driver allows easy integration with custom REST service.

# How to use

## Installation

Download .jar file from [releases page](https://github.com/nano3ti/metabase-rest-api-driver/releases) and put it to metabase/plugins directory. You can specify custom plugins directory using MB_PLUGINS_DIR environment variable.

## Configuration

Driver has the following parameters:

| Parameter | Description |
| -------- | ------- |
| Base URL  | Base URL for your REST API |
| Token | Optional. API access token. Will be send as Bearer auth header.     |
| Table Definitions | Optional. Table definitions |

## Raw query format

Can be used in SQL Editor or in table definition

```
{
    "path": "/tables/1", // Path to API endpoint
    "method": "post", // HTTP method. Default: "get"
    "body": {}, // Request json body. Optional
    "headers": {}, // Extra headers. Optional
}
```

## Required Api response format

```
{
    "info": {
        "name": "Table1",
        "columns": [
            {"name": "Column1"},
            {"name": "Column2"},
            {"name": "Column3"}
        ]
    },
    "rows": [
        ["2024-03-09", 68926, 1955],
        ["2024-03-10", 70004, 1803],
        ["2024-03-11", 74397, 1724]
    ]
}
```
