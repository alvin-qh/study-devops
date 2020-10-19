#! /bin/bash

scp sources.list vsrv1:/tmp
scp sources.list vsrv2:/tmp
scp sources.list vsrv3:/tmp

ssh -t vsrv1 "sudo cp /tmp/sources.list /etc/apt/"
ssh -t vsrv2 "sudo cp /tmp/sources.list /etc/apt/"
ssh -t vsrv3 "sudo cp /tmp/sources.list /etc/apt/"
