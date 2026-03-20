#!/bin/bash

dataDir="$TESTDIR/../Part2/Sol"

read filename < /dev/stdin

java LBSMain CHECK $dataDir/$filename

