#!/bin/bash

dataDir="$TESTDIR/../../Part3/Sol"

read filename < /dev/stdin

java LBSMain CHECK $dataDir/$filename




