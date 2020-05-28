#!/bin/bash

SCRIPT_DIR=$(cd $(dirname $0); pwd)

export GEM_HOME=$SCRIPT_DIR/build/.gems
export GEM_PATH=$SCRIPT_DIR/build/.gems

java -jar "${SCRIPT_DIR}/vendor/jruby-complete-9.2.11.1.jar" "${@}"