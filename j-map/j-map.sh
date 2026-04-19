#!/bin/bash
set -e
mvn clean install
mvn clean javafx:run
