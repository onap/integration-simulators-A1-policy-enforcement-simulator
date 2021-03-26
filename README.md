# A1 Policy Enforcement Simulator (A1 PE Simulator)

This simulator supports standard A1-P OSC\_2.1.0 interface. In addition, internal APIs are provided to manage the RAN elements (Cells, UEs) and to customize and send VES Events.

## How to use the A1 PE Simulator?

### RAN

A1 PE Simulator needs two main files to define the topology (cells) and user equipments that should be managed (those cells/UEs are then used in A1 Policy Enforcement loop).

- doc/resource/cells.json

```json
{
  "cellList": [
    {
      "Cell": {
        "networkId": "RAN001",
        "nodeId": "Cell1",
        "physicalCellId": 0,
        "pnfName": "ncserver1",
        "sectorNumber": 0,
        "latitude": "50.11",
        "longitude": "19.98"
      },
      "neighbor": [
        {
          "nodeId": "Cell3",
          "blacklisted": "false"
        },
        {
          "nodeId": "Cell4",
          "blacklisted": "false"
        },
        {
          "nodeId": "Cell2",
          "blacklisted": "false"
        }
      ]
    }
}
```

- doc/resource/ue.json

```json
[
    {
        "id": "emergency_samsung_s10_01",
        "latitude": "50.09",
        "longitude": "19.94",
        "cellId": "Cell1"
    },
    {
        "id": "emergency_police_01",
        "latitude": "50.035",
        "longitude": "19.97",
        "cellId": "Cell3"
    }
]
```

The locations of these files are defined in *src/main/resources/application.properties*.

Important: The vnf.config, cells.json and ue.json files should be under */var/a1pesim/*  (default folder location, can be changed).
Copy the content of **doc/resources/** to this location on the host that will be running the simulator.

To change the default locations, see:

- **Run A1 PE Simulator with a new configuration**

To refresh the content of those files on runtime, see:

- **Refresh the configuration files on runtime**

### VES

A1 PE Simulator provides REST endpoints that can be used to trigger sending of VES events to DMaaP topic via VES-COLLECTOR (DCAE MS).

The file **vnf.config** provides the connectivity configuration as well as some commonEventHeader default values (like sourceId and sourceName):

```
vesHost=vesconsumer
vesPort=30417
vesUser=sample1
vesPassword=sample1
vnfId=de305d54-75b4-431b-adb2-eb6b9e546014
vnfName=ibcx0001vm002ssc001
```

- vesHost defines the hostname of the VES consumer
- vesPort defines the port on which consumer expects events
- vesUser and vesPassword are used to create the BasicAuth header in the request
- vnfId, vnfName map to the VES event -> commonEventHeader content:

```json
{
    "event":{
        "commonEventHeader": {
            "sourceId": "de305d54-75b4-431b-adb2-eb6b9e546014",
            "sourceName": "ibcx0001vm002ssc001",
            ...
        },
        ...
}
```

Cells can send two types of VES events: **normal** and **failure**.
In both cases the VES event values are mostly the same;
the only difference is that *measurementFields.additionalMeasurements.latency/throughput* values are generated by using different algorithms.

- for normal VES Events

```json
{
  ...
  "measurementFields" : {
              "additionalMeasurements" : [
                  {
                      "name": "latency",
                      "hashMap": {
                          "value": "[[10-150]]"
                      }
                  },
                  {
                      "name": "throughput",
                      "hashMap": {
                          "value": "[[10-100]]"
                      }
                  }
              ],
              ...
  }
}
```

**10-150** means that the generated values will oscillate between 10 and 150

- for failure VES Events

```json
{
...
"measurementFields" : {
            "additionalMeasurements" : [
                {
                    "name": "latency",
                    "hashMap": {
                        "value": "[[200->500]]"
                    }
                },
                {
                    "name": "throughput",
                    "hashMap": {
                        "value": "[[10->1]]"
                    }
                }
            ],
            ...
        }
}
```

**200->500** means that the value will be generated from 200 to 500 (by using the exponential function)

### A1-P Mediator API

The A1 Mediator listens to the northbound interface of the RIC for policy guidance.
The caller (e.g., non-RT RIC, SMO, etc.) creates policy types and policy instances through A1, and subsequently A1 exchanges messages with xApps via RMR.

#### Policy Type

Example schema (used in Policy Enforcement PoC):

```json
{
  "name": "samsung_policy_type",
  "description": "samsung policy type; standard model of a policy with unconstrained scope id combinations",
  "policy_type_id": 1000,
  "create_schema": {
    "$schema": "http://json-schema.org/draft-07/schema#",
    "title": "Samsung_demo",
    "description": "Samsung demo policy type",
    "type": "object",
    "properties": {
      "scope": {
        "type": "object",
        "properties": {
          "ueId": {
            "type": "string"
          },
          "groupId": {
            "type": "string"
          }
        },
        "additionalProperties": false,
        "required": [
          "ueId"
        ]
      },
      "resources": {
        "type": "array",
        "items": {
          "type": "object",
          "properties": {
            "cellIdList": {
              "type": "array",
              "minItems": 1,
              "uniqueItems": true,
              "items": {
                "type": "string"
              }
            },
            "preference": {
              "type": "string",
              "enum": [
                "SHALL",
                "PREFER",
                "AVOID",
                "FORBID"
              ]
            }
          },
          "additionalProperties": false,
          "required": [
            "cellIdList",
            "preference"
          ]
        }
      }
    },
    "additionalProperties": false,
    "required": [
      "scope",
      "resources"
    ]
  }
}
```

Required policy type can be created as follows:

```
curl -X PUT -v -H "accept: application/json" -H "Content-Type: application/json" --data-binary @/tmp/policy_type.json localhost:9998/v1/a1-p/policytypes/1000
```

where:

- @/tmp/policy_type.json file with policy schema
- localhost:9998/v1/a1-p/policytypes/${policy_type_id} - policy type ID (defined by client) used to create the policy type

#### Create/Delete Policy Instance

Create/update example policy instance request for given policy type:

```
curl --location --request PUT 'http://localhost:9998/a1-p/policytypes/1000/policies/1' \
--header 'Content-Type: application/json' \
--data-raw '{
  "scope" : {
    "ueId" : "emergency_samsung_s10_01"
  },
  "resources" : [
    {
      "cellIdList" : [ "Cell1" ],
      "preference" : "AVOID"
    }
  ]
}'
```

where:

- localhost:9998/a1-p/policytypes/${policy_type_id}/policies/${policy_instance_id} - policy instance ID (defined by client) used to create the policy instance

Delete the policy instance request of a given policy type:

```
curl --location --request DELETE 'http://localhost:9998/a1-p/policytypes/1000/policies/1'
```

### Run A1 PE Simulator with a new configuration

A1 PE Simulator uses the properties to define the following:

- File locations of vnf.config, cells.json, ue.json files
- VES consumer's supported protocol and endpoint (e.g. for VES-Collector)
- Cell range
- Default VES event sending interval
- Version of the A1 PE Simulator API

See (src/main/resources/application.properties) for default values.

The default values can be overridden in multiple ways:

#### 1. By defining the OS env variables

- VNF_CONFIG_FILE=
- TOPOLOGY_CELL_CONFIG_FILE=
- TOPOLOGY_UE_CONFIG_FILE=
- VES_COLLECTOR_PROTOCOL=
- VES_COLLECTOR_ENDPOINT=
- VES_DEFAULTINTERVAL=
- RESTAPI_VERSION=

#### 2. By adding the process arguments

Add -D flag to the execution command:

- "-Dvnf.config.file="
- "-Dtopology.cell.config.file="
- "-Dtopology.ue_config.file="
- "-Dves.collector.protocol="
- "-Dves.collector.endpoint="
- "-Dves.defaultinterval="
- "-Drestapi.version="

When running with -Dspring.profiles.active=dev default values for **vnf.config.file**, **topology.cell.config.file** and **topology.ue.config.file** are set to use the example files from *src/test/resources/*

### Refresh the configuration files on runtime

If the contents of cells.json or ue.json are changed, the user should send a request to A1 PE Simulator to reload those files:

```
curl --location --request GET 'http://localhost:9998/v1/ran/refresh'
```

Also, A1 PE Simulator automatically refreshes the topology/ues information from those files in defined time interval:

**refresher.fixed.rate.ms=60000**

# API

The API is documented by the Swagger tool.

## Swagger

The generated swagger html file can be found in *doc/swagger/html* directory.
JSON file that can be imported to Swagger GUI can be found in *doc/swagger*.
Those files are regenerated in each maven build, so to generate this file please see **Build the A1 PE Simulator** chapter.

# Developer Guide

## Build the A1 PE Simulator

Following mvn command (in the project directory) will build A1 Policy Enforcement Simulator:

```bash
mvn clean install
```

## Run the A1 PE Simulator

Following command will run the A1 Policy Enforcement Simulator:

```bash
java -jar a1-pe-simulator-1.0-SNAPSHOT.jar org.onap.a1pesimulator.A1PolicyEnforcementSimulatorApplication
```

The application should start on 9998 port.

## Logging

The logs file will be created in the ${user.home}/log path.
To define the **user.home** value, use the process arguments e.g "-Duser.home=/path_to_dir".

After the A1 PE Simulator starts successfully the */path_to_dir.log* should start to contain the logs:

```
.
└── a1-pe-simulator
    ├── application
    │        ├── debug-2021-03-15.0.log
    │        ├── error-2021-03-15.0.log
    │        └── metrics-2021-03-15.0.log
    └── debug-2021-03-15.0.log
```

