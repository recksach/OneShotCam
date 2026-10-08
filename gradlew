#!/usr/bin/env sh
set -e
if [ -x ./gradlew.bat ]; then
  ./gradlew.bat " $@\
else
 exec gradle \$@\
fi
