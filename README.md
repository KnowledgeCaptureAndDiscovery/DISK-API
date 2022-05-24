
# DISK

The DISK system automates the execution of scientific workflows triggered 
on data changes. To do this DISK collects data from different data repositories
and defines methods on different workflows systems. User defined goals are 
periodically check for new data/methods available. When a method detects new data,
a new workflow execution will be send. Each experiment execution is stored with its
metadata and outputs for posterior analysis.

## Installation

You can install DISK using Docker or [building from sourcfe](./building.md)
We recommend to use `docker` to install DISK. 


### Docker

Install DISK with docker

```bash
docker-compose up -d
```


## Configuration

1. Customize the client by changing the [config.js file](./config.js)
2. Open http://localhost:8080/disk-client/index.html to access the Disk UI that connects with the local repository

## Usage

### Check the server

Open http://localhost:8080/disk-server/vocabulary to check that the local repository server is working fine. It might take a little while to open it for the first time as it downloads vocabularies from the internet.

### Check the client

Open http://localhost:8080/disk-client/index.html to access the Disk UI that connects with the local repository

## Using DISK

We have prepared some examples to show how to use DISK.

- [Example 1](./docs/example1.md)


## Documentation

Full documentation is available at [https://disk.readthedocs.io/en/latest/](https://disk.readthedocs.io/en/latest/)
