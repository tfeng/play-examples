#!/bin/sh

for dir in *-example; do
  (cd $dir; activator $@) || exit 1
done
