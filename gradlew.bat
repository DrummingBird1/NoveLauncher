@if "%DEBUG%"=="" @echo off
set APP_HOME=%~dp0
set CLASSPATH=%APP_HOME%\gradle\wrapper\gradle-wrapper.jar
set JAVA_EXE=java.exe
if defined JAVA_HOME set JAVA_EXE=%JAVA_HOME%/bin/java.exe
"%JAVA_EXE%" "-Xmx64m" "-Xms64m" -classpath "%CLASSPATH%" org.gradle.wrapper.GradleWrapperMain %*
