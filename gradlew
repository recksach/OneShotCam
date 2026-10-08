#!/usr/bin/env sh
exec ./gradlew.bat " $@\ 2>/dev/null
exec gradle \$@\ 2>/dev/null
exec java -jar gradle/wrapper/gradle-wrapper.jar \$@\