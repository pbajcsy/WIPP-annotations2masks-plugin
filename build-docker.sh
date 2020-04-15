#!/bin/bash
version=$(<VERSION)
docker build . -t wipp/wipp-annotations2masks-plugin:${version}