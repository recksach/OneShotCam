#!/bin/sh
exec gradle --no-build-cache assembleDebug "$@"
