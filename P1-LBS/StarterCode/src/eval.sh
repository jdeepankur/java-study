#!/bin/bash

# Script to evaluate the performance of the program as outlined in the report.

TSTAMP=$(date +%s)
touch eval-$TSTAMP.log
mkdir PUZZLES

for i in {1..40}
do
   touch PUZZLES/$i
   java LBSMain GEN $(date +%s) $i > PUZZLES/$i
   echo $i >> eval-$TSTAMP.log
   { timeout 6 time java LBSMain SOLVE PUZZLES/$i; } 2>> eval-$TSTAMP.log
done
