#!/usr/bin/env sh

if [[ -z $USER_NAME ]]; then
    echo "Hello Docker!"
else
    echo "Hello Docker! Welcome $USER_NAME"
fi
