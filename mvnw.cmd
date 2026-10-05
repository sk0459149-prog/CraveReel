@REM Turnkey Maven Wrapper for RecipeReels
@echo off
set "MAVEN_HOME=%~dp0..\maven"
if exist "%MAVEN_HOME%\bin\mvn.cmd" (
    "%MAVEN_HOME%\bin\mvn.cmd" %*
) else (
    mvn %*
)
