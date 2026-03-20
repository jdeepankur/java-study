#!/bin/bash


# just look at first field of first line 
java LBSMain SOLVEGRACE | head -1 | sed "s/^ *//" | cut -f 1 -d" "



