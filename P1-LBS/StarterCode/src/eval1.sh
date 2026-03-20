\#!/bin/bash

# Script to evaluate the effect of number of cards in the deck on solvability.

TSTAMP=$(date +%s)
touch eval1-$TSTAMP.log
mkdir PUZZLES
mkdir PUZZLES/eval1

for i in {0..15}
do
   echo $i
   touch PUZZLES/eval1/$i
   echo $(( (i+13) * (i+4) )) >> eval1-$TSTAMP.log
   touch tmp-$TSTAMP.log
   for j in {0..10}
   do
     java LBSMain GEN $(( $(date +%s) + 2022)) 16 $((i+13)) $((i+4)) > PUZZLES/eval1/$i
     java LBSMain SOLVE PUZZLES/eval1/$i >> tmp-$TSTAMP.log
   done
   { ag -- -1 tmp-$TSTAMP.log; } >> eval1-$TSTAMP.log
   rm tmp-$TSTAMP.log
   echo ====== >> eval1-$TSTAMP.log
done

