> Historical machine-specific commands, including known inconsistencies. Current troubleshooting: [ADB guide](../../guides/Debugging.md). Do not execute this record as a current setup recipe.

# Shared Android SDK/Gradle setup history

Historical command sequence; machine/user/group paths and original inconsistencies are retained.
------------------------------
## Phase 1: Group Creation & Directory Setup
Shared group/location/permissions for both users.

# 1. Created the global shared folder structure
sudo mkdir -p /opt/android-shared
# 2. Created the developer security group
sudo groupadd android-devs
# 3. Added both of your users to the new group
sudo usermod -aG android-devs work
sudo usermod -aG android-devs mhmmd
# 4. Changed the group ownership of the shared directory to your group
sudo chown -R :android-devs /opt/android-shared
# 5. Set read/write/execute directory permissions for the group
sudo chmod -R 775 /opt/android-shared
# 6. Applied the "SetGID" bit so new files inherit the group automatically
sudo chmod g+s /opt/android-shared

------------------------------
## Phase 2: Locating & Moving Android Studio + the SDK
Locate/move Studio and SDK; restore permissions.

# 1. Searched the mhmmd user directory to find where studio.sh was hiding
sudo find /home/mhmmd/ -name "studio.sh" 2>/dev/null# Output found it at: /home/mhmmd/Apps/android-studio/bin/studio.sh
# 2. Moved the SDK from mhmmd's home to the shared workspace
sudo mv /home/mhmmd/Android/Sdk /opt/android-shared/Sdk
# 3. Moved the Android Studio binaries out of mhmmd's private Apps folder
sudo mv /home/mhmmd/Apps/android-studio /opt/android-shared/android-studio
# 4. Reset permissions on the moved folders so both users could read/write them
sudo chown -R :android-devs /opt/android-shared/Sdk
sudo chmod -R 775 /opt/android-shared/Sdk
sudo chown -R :android-devs /opt/android-shared/android-studio
sudo chmod -R 755 /opt/android-shared/android-studio

------------------------------
## Phase 3: Setting Environment Variables
Configure Java, shared Gradle home and SDK tools for work user.

# 1. Added variables directly to the end of the work user's bash config file
echo 'export ANDROID_HOME=/opt/android-shared/Sdk' >> ~/.bashrc
echo 'export GRADLE_USER_HOME=/opt/android-shared/.gradle' >> ~/.bashrc
echo 'export PATH=$PATH:$ANDROID_HOME/cmdline-tools/latest/bin:$ANDROID_HOME/platform-tools' >> ~/.bashrc
# 2. Refreshed the terminal profile to load the new settings instantly
source ~/.bashrc
# 3. Verified the path variable was mapped successfully
echo $GRADLE_USER_HOME

------------------------------
## Phase 4: Fixing Desktop Integration & Shortcuts
Create Kubuntu launcher; remove duplicates.

# 1. Created the directories and opened the desktop file editor
mkdir -p ~/.local/share/applications && nano ~/.local/share/applications/android-studio.desktop
# 2. Made the newly created shortcut launcher executable
chmod +x ~/.local/share/applications/android-studio.desktop
# 3. Cleaned up a duplicate automated shortcut that was causing double items
rm ~/.local/share/applications/android-studio.desktop
# 4. Refreshed Kubuntu's Application Menu cache to update the icons
kbuildsycoca6  # (or kbuildsycoca5 depending on system version)

------------------------------
## Phase 5: Solving the Gradle Permission & Lock Errors
Copied-cache ownership/locks: repair ACLs and clear blocking logs.

# 1. Made sure the hidden .gradle cache directory existed inside the shared folder
sudo mkdir -p /opt/android-shared/.gradle
# 2. Ensured the 'acl' utility package was installed on your system
sudo apt update && sudo apt install -y acl
# 3. Switched the group ownership on the shared .gradle directory (corrected typo)
sudo chown -R :android-dev /opt/android-shared/.gradle
sudo chmod -R 775 /opt/android-shared/.gradle
# 4. Configured ACL rules to permanently force new files to grant full rwx access to the group
sudo setfacl -R -d -m g:android-dev:rwx /opt/android-shared/.gradle
sudo setfacl -R -m g:android-dev:rwx /opt/android-shared/.gradle
# 5. Erased conflicting background daemon cache and file locks (*.lck) owned by mhmmd
sudo rm -rf /opt/android-shared/.gradle/daemon/
sudo rm -rf /opt/android-shared/.gradle/wrapper/dists/gradle-8.7-bin/bhs2wmbdwecv87pi65oeuq5iu/*.lck
# 6. Forced the active terminal window to assume the group permissions instantly
newgrp android-dev
# 7. Cleared out project-level cached configurations inside your project folder
cd "/run/media/work/Common/IT Projects/Quiz/Android"
rm -rf .gradle/ build/ app/build/
# 8. Successfully ran the wrapper build!
./gradlew

Follow-up discussed: gradle.properties configuration for the lateinit property reportFile crash.

