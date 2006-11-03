@echo off
rem
rem Configuration variables
rem
rem JAVA_HOME
rem   Home of Java installation.
rem
rem JAVA_OPTIONS
rem   Extra options to pass to the JVM
rem

rem --- First two utilities for exiting --------------------------------------------

goto endUtils

:usage
echo Usage: %0 [options]
echo where [options] include:
echo.
echo  /h print this message and exit
echo.
echo  /c _configuration_ the configuration that longwell should start
echo     default: longwell
echo.
echo  /p _path_ is the paths where longwell will look for configurations
echo     default:  ./src/main/webapp/
echo.
echo  /d _database_ the directory were the triple store files reside
echo     default:  ./src/main/webapp/WEB-INF/database
echo.
echo  /a _action_ is what to do, the default is run
echo   supported _action_s are:
echo    run          Run longwell
echo    debug        Run longwell and turn on JVM remote debug
echo    profile      Run longwell and turn on JVM profiling with HProf
echo    shark        Run longwell and turn on JVM profiling with Apple Shark (MacOSX only)
echo    yourkit      Run longwell and turn on JVM profiling with YourKit
echo    jmx          Run longwell and turn on JVM monitoring with JMX and JConsole
echo.
echo  /r _rdf_ is the directory where the RDF data to load is
echo   default: no data is loaded
goto end

:fail
echo See: '%0 /h' for usage.
goto end

:endUtils

if not "%JAVA_HOME%" == "" goto gotJavaHome
echo You must set JAVA_HOME to point at your Java Development Kit installation
goto fail
:gotJavaHome

set MAVEN_OPTS=-Djava.awt.headless=true
set ACTION=run

rem --- Argument parsing --------------------------------------------

:loop
if ""%1"" == """" goto endArgumentParsing
if ""%1"" == ""/h"" goto usage
if ""%1"" == ""/d"" goto arg-d
if ""%1"" == ""/c"" goto arg-c
if ""%1"" == ""/p"" goto arg-p
if ""%1"" == ""/r"" goto arg-r
if ""%1"" == ""/a"" goto arg-a
echo ERROR: Unknown Argument: '%1'
goto fail

:arg-c
set MAVEN_OPTS=%MAVEN_OPTS% -Dlongwell.configuration=%2
goto shift2loop

:arg-d
set MAVEN_OPTS=%MAVEN_OPTS% -Dlongwell.store.dir=%~s2
goto shift2loop 

:arg-p
set MAVEN_OPTS=%MAVEN_OPTS% -Dlongwell.configuration.path=%~s2
goto shift2loop 

:arg-r
set MAVEN_OPTS=%MAVEN_OPTS% -Dlongwell.data=%~s2
goto shift2loop 

:arg-a
set ACTION="$2"
goto shift2loop 

:shift2loop
shift
shift
goto loop

:endArgumentParsing


rem --- Fold in Environment Vars --------------------------------------------

if not "%JAVA_OPTIONS%" == "" goto gotJavaOptions
set JAVA_OPTIONS=-Xms32M -Xmx256M
:gotJavaOptions
set MAVEN_OPTS=%MAVEN_OPTS% %JAVA_OPTIONS%

if not "%JETTY_PORT%" == "" goto gotJettyPort
set JETTY_PORT=8080
:gotJettyPort
set MAVEN_OPTS=%MAVEN_OPTS% -Djetty.port=%JETTY_PORT%


rem ----- Respond to the action ----------------------------------------------------------

if ""%ACTION%"" == ""run"" goto doRun
if ""%ACTION%"" == ""debug"" goto doDebug
if ""%ACTION%"" == ""profile"" goto doProfile
if ""%ACTION%"" == ""shark"" goto doShark
if ""%ACTION%"" == ""yourkit"" goto doYourKit
if ""%ACTION%"" == ""jmx"" goto doJMX

:doRun
goto doIt

:doDebug
set MAVEN_OPTS=%MAVEN_OPTS% -Xdebug -Xrunjdwp:transport=dt_socket,address=8000,server=y,suspend=n
goto doIt

:doProfile
set MAVEN_OPTS=%MAVEN_OPTS% -Xrunhprof:heap=all,cpu=samples,thread=y,depth=3
goto doIt

:doShark
set MAVEN_OPTS=%MAVEN_OPTS% -Xrunshark
goto doIt

:doYourKit
set MAVEN_OPTS=%MAVEN_OPTS% -agentlib:yjpagent
goto doIt

:doJMX
set MAVEN_OPTS=%MAVEN_OPTS% -Dcom.sun.management.jmxremote
goto doIt

:doIt

echo [INFO]: MAVEN_OPTS=%MAVEN_OPTS%

if not "%MAVEN_PARAMS%" == "" goto withMavenParams
mvn jetty:run
goto end

:withMavenParams
mvn %MAVEN_PARAMS% jetty:run

:end

