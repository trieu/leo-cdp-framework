#!/bin/sh

buildOutputFolderPath="/home/thomas/0-github/leo-cdp-free-edition/"
staticOutputFolderPath="/home/thomas/0-github/leo-cdp-static-files"

gradle minifyJsAdminResource \
  -PstaticOutputFolderPath="$staticOutputFolderPath"

echo "\n !!!!! minifyJsAdminResource DONE !!!!! \n"

gradle buildAllTasksForDeployment \
  -PbuildOutputFolderPath="$buildOutputFolderPath" \
  -PstaticOutputFolderPath="$staticOutputFolderPath"

echo "\n !!!!! build All Tasks For Deployment DONE !!!!! \n"
echo "\n !!!!!!!!!!!!!!!!!!!! ALL BUILD DONE !!!!!!!!!!!!!!!!!!!!!!!!! \n"