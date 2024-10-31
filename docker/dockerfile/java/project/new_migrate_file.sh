#!/usr/bin/env bash

work_dir=$(cd $(dirname $0); pwd)

if [[ -n $1 ]];then
	_cmd="${work_dir}/web/src/main/resources/migrations/V`date +%Y%m%d_%H%M`__${1// /_}.sql"
	touch $_cmd
	echo "File $_cmd is created"
else
	echo 'Please press migration comments.'
fi
