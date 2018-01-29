## Testing Guidelines for hrls servlet

The suite can test following parts:
* hrls servlet

The suite of unit and integration test cases for all parts uses the standard testing framework, i.e. `unittest`, aka “PyUnit”, the Python version of JUnit.

* hrls Integrations tests require a valid credentials file (otherwise test suite will be skipped):
  * filename: `hrls_credentials`
  * location: under `resources` directory
An example of the credentials file is: 
```
{
    "handle_server_url": "https://fqdn:port",
    "prefix": "<prefix>",
    "reverselookup_username": "<reverselookup_username>",
    "reverselookup_password": "<reverselookup_passwordd>",
    "HTTPS_verify": "True"
}
```
The HTTPS_verify can be set to True or False. It can also use a CA to authenticate against. But that has not been tested.


### Requirements

A running handle server with the hrls servlet installed and a valid configurtion.
The running servlet needs to have 100010 specific handles in it.
They specifix handles can be generated with `create_100010_hrls_test_handles.sh`.
It is run as follows:
```
./create_100010_hrls_test_handles.sh <prefix>
```
The resulting output file has to be fed to hdl-generic-batch and the handles need to be created.

### Usage

    ./testHrlsCmd.py --help
    ./testHrlsCmd.py -test hrls

### Test coverage


### Known Issues

