#!/usr/bin/env groovy

def call() {
   Util.getTimeSpanString(System.currentTimeMillis() - currentBuild.startTimeInMillis)
}
