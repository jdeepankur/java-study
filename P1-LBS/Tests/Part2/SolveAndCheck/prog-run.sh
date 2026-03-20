#!/bin/bash

dataDir="$TESTDIR/../Sol"

read filename < /dev/stdin

# just look at every field except first of first line to get solution then pass it in 
java LBSMain SOLVE $dataDir/$filename | head -1 | java LBSMain CHECK $dataDir/$filename




