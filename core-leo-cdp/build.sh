#!/bin/sh

gradle minifyJsAdminResource
echo "\n !!!!! minifyJsAdminResource DONE !!!!! \n"

gradle AllBuildJarLeoCDP
echo "\n !!!!! AllBuildJarLeoCDP DONE !!!!! \n"

gradle CopyResourcesFolderToBUILD
echo "\n !!!!! CopyResourcesFolderToBUILD DONE !!!!! \n"

gradle CopyDatabaseQueryTemplateToBuild
echo "\n !!!!! CopyDatabaseQueryTemplateToBuild DONE !!!!! \n"

gradle CopyRuntimeLibsFolderToBUILD
echo "\n !!!!! CopyRuntimeLibsFolderToBUILD DONE !!!!! \n"

gradle CopyPublicFolderToBUILD
echo "\n !!!!! CopyPublicFolderToBUILD DONE !!!!! \n"

gradle CopyPublicFolderToSTATIC
echo "\n !!!!! CopyPublicFolderToSTATIC DONE !!!!! \n"

gradle CopyTechDocuments
echo "\n !!!!! CopyTechDocuments DONE !!!!! \n"

echo "\n !!!!!!!!!!!!!!!!!!!! ALL BUILD DONE !!!!!!!!!!!!!!!!!!!!!!!!! \n"