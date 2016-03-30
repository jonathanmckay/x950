## Build
1. Install [Android Studio](http://developer.android.com/sdk/index.html); go to the SDK manager (File -> Settings -> Appearance & Behavior -> System Settings -> Android SDK) and ensure you have API level 21 installed.
2. Clone the x950 repository:

        git clone [URL]

3. You will need the DSLV repository on the x950 branch, so:

        cd x950
        git submodule update --init
        cd libraries/drag-sort-list-view
        git checkout x950

## Common Build Issues
* SDK version should be consistent across modules. Go to File -> Project Structure and ensure each module SDK is Android API 21, not Maven Android API 21.
* Android Support v4 might be compiled multiple times; go to File -> Project Structure and change the scope from compile to provided for DateTimePicker and DSLV.
