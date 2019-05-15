### How to setup project

I assume that you have installed node.js if not the best way is to use node version manager
 
Windows link: <https://github.com/nvm-sh/nvm>  
Mac/Linux link: <https://github.com/nvm-sh/nvm>

then you need to install yarn package manager. 

```
npm i yarn -g
```  

then install react native cli
```
npm install -g react-native-cli
```

then you need install Android Studio, you can find instruction here <https://facebook.github.io/react-native/docs/getting-started.html>

after you install Android Studio you should setup NDK you can find how to do this here <https://developer.android.com/ndk/guides#download-ndk>

after you finished installations go into project directory and run next command

```
yarn install
```

### How to run project
there are two ways how to build and run project. you can do this from command line or from Android Studio

##### Run command line
```
 npm run run-android
```
you should have connected phone or setuped android simulator

##### Run from Android Studio

you can find android directory in project directory and open it using Android Studio
But also you need to run next command to build javascript sources
```
 npm run android-dev
```

### Project structure

##### cpp-src 
contains c++ sources of simple sdl program see demo.cpp

#### App.js
contains javascript code of simple React Native app 

##### android 
Folder that contains android project

 - ReactSDLViewManager.java - implements custom ReactNative UI Component
 - SDLBridge.java - implements bridge between UI component and SDL
 - SDLSurface.java - implements SurfaceView witch SDL used for rendering 
 - jni sub folder contains make files that configure build of c++ files
 