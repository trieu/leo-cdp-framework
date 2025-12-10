#!/bin/bash

PROPS_FILE="leocdp-build.properties"
DEFAULT_VERSION="1.0.0"

# --- Function: Prompt user for valid path ---
get_valid_path() {
    local prompt_msg="$1"
    local valid=0
    local input_path=""

    while [ $valid -eq 0 ]; do
        printf "%s" "$prompt_msg"
        read -r input_path

        if [ -z "$input_path" ]; then
            echo "Error: Path cannot be empty."
        elif [ -d "$input_path" ]; then
            valid=1
        else
            echo "Error: Path '$input_path' does not exist. Please enter a valid path."
        fi
    done
    echo "$input_path"
}

# --- Main Logic ---

# Initialize variables
FINAL_BUILD_PATH=""
FINAL_STATIC_PATH=""
FINAL_VERSION=""

# 1. Check System Environment Variables first (CI/CD Friendly)
# We check if the variables are set AND if the directories actually exist
if [[ -d "$buildOutputFolderPath" && -d "$staticOutputFolderPath" ]]; then
    echo ">> System Environment Variables detected."
    FINAL_BUILD_PATH="$buildOutputFolderPath"
    FINAL_STATIC_PATH="$staticOutputFolderPath"
    FINAL_VERSION="${buildVersion:-$DEFAULT_VERSION}" # Use Env or Default
else
    # 2. If Env Vars are missing/invalid, check existing file
    if [ -f "$PROPS_FILE" ]; then
        echo ">> Reading existing $PROPS_FILE..."
        source "$PROPS_FILE"
        
        # Validate the paths loaded from file
        if [[ -d "$buildOutputFolderPath" && -d "$staticOutputFolderPath" ]]; then
            FINAL_BUILD_PATH="$buildOutputFolderPath"
            FINAL_STATIC_PATH="$staticOutputFolderPath"
            FINAL_VERSION="${buildVersion:-$DEFAULT_VERSION}"
        else
            echo ">> Error: Paths in $PROPS_FILE are invalid or missing."
        fi
    fi
fi

# 3. If neither Env Vars nor File provided valid paths, Prompt User
if [ -z "$FINAL_BUILD_PATH" ]; then
    echo "------------------------------------------------------------"
    echo " Setup Required: Please configure build paths"
    echo "------------------------------------------------------------"

    FINAL_BUILD_PATH=$(get_valid_path "Enter buildOutputFolderPath: ")
    FINAL_STATIC_PATH=$(get_valid_path "Enter staticOutputFolderPath: ")
    
    printf "Enter buildVersion (default: %s): " "$DEFAULT_VERSION"
    read -r input_ver
    FINAL_VERSION="${input_ver:-$DEFAULT_VERSION}"
fi

# 4. Save/Update the properties file
# We rewrite the file to ensure it matches the current valid configuration
echo "buildOutputFolderPath=$FINAL_BUILD_PATH" > "$PROPS_FILE"
echo "staticOutputFolderPath=$FINAL_STATIC_PATH" >> "$PROPS_FILE"
echo "buildVersion=$FINAL_VERSION" >> "$PROPS_FILE"

echo ">> Configuration saved to $PROPS_FILE"

# 5. Run Gradle
echo ">> Starting Gradle Build..."
gradle AutoBuildForDeployment

echo -e "\n !!!!! build All Tasks For Deployment DONE !!!!! \n"
echo -e "!!!!!!!!!!!!!!!!!!!! ALL BUILD DONE !!!!!!!!!!!!!!!!!!!!!!!!! \n"
echo "--- Version '$FINAL_VERSION' is released at '$FINAL_BUILD_PATH' ---"