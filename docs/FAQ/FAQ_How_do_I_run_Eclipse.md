FAQ How do I run Eclipse?
=========================

Contents
--------

*   [1 Starting Eclipse](#Starting-Eclipse)
*   [2 Find the JVM](#Find-the-JVM)
*   [3 eclipse.ini](#eclipse.ini)
*   [4 See Also:](#See-Also:)
*   [5 User Comments](#User-Comments)

Starting Eclipse
----------------

* | unzip Eclipse, 
  * directory layout

        eclipse/
           features/			''the directory containing Eclipse features''
           plugins/			''the directory containing Eclipse plugins''
           eclipse.exe		''platform executable''
           eclipse.ini
           eclipsec.exe              ''(windows only) console executable''
           epl-v10.html		''the EPL license''
           jre/			''the JRE to run Eclipse with''
           notice.html	
           readme	

* if you want to start Eclipse -> run
  * | Windows,
    * eclipse.exe
      * how does it work?
        * find the JVM
        * loads the JVM
    * if you want improved command line behavior -> use eclipsec.exe 
  * | OTHER platforms, 
    * eclipse
      * how does it work?
        * find the JVM
        * loads the JVM
  * | ALL platforms
    ```
    java -jar eclipse/plugins/org.eclipse.equinox.launcher_1.0.0.v20070606.jar 
    ```
    * org.eclipse.equinox.launcher's version == version / shipped with Eclipse 
    * see [Starting Eclipse Commandline With Equinox Launcher](Starting_Eclipse_Commandline_With_Equinox_Launcher.md "Starting Eclipse Commandline With Equinox Launcher").
  * | Java  v9+,
    * make SOME non-default system modules available
      * see the release notes | supported Java versions / Eclipse version
      * _Example:_ `--add-modules ALL-SYSTEM`

Find the JVM
------------

* if a JVM is installed | eclipse/jre directory -> Eclipse will use it
* otherwise, the launcher -- will consult the --
  * eclipse.ini file
  * system path variable
    * ⚠️!= JAVA_HOME environment variable ⚠️
* if you want to specify a JVM -> use the `-vm` CL argument
  ```
  eclipse -vm c:\\jre\\bin\\javaw.exe              ''start Java by executing the specified java executable
  eclipse -vm c:\\jre\\bin\\client\\jvm.dll         ''start Java by loading the jvm in the eclipse process
  ``` 
* see [Equinox launcher](Equinox_Launcher.md)

eclipse.ini
-----------

* == configuration file
* uses
  * put startup configuration | ["eclipse.ini"](Eclipse.ini.md) / 's location == Eclipse executable's location 
    * 💡MOST recommended way to specify a JVM / run Eclipse 💡
* ALTERNATIVE TO
  * CL
* if you want to specify a JVM | this configuration file -> add `-vm` argument 
  * _Example:_
    ```
    -vm
    c:/jre/bin/javaw.exe        // NO quotes -- as -- CLI
    
    ```
* priority
  * eclipse.ini
  * CL's arguments
* | start Eclipse,
  * you are prompted -- to -- choose a workspace location
* see [Eclipse.ini](Eclipse.ini.md)

See Also:
---------

*   [FAQ How do I increase the heap size available to Eclipse?](./FAQ_How_do_I_increase_the_heap_size_available_to_Eclipse.md "FAQ How do I increase the heap size available to Eclipse?")
*   [FAQ How do I increase the permgen size available to Eclipse?](./FAQ_How_do_I_increase_the_permgen_size_available_to_Eclipse.md "FAQ How do I increase the permgen size available to Eclipse?")
*   [FAQ Who shows the Eclipse splash screen?](./FAQ_Who_shows_the_Eclipse_splash_screen.md "FAQ Who shows the Eclipse splash screen?")
*   Running Eclipse 3.3M5+

*   [Starting Eclipse Commandline With Equinox Launcher](/Starting_Eclipse_Commandline_With_Equinox_Launcher "Starting Eclipse Commandline With Equinox Launcher")
*   [Automated PDE JUnit Testing With Eclipse 3.3M5](/Automated_PDE_JUnit_Testing_With_Eclipse_3.3M5 "Automated PDE JUnit Testing With Eclipse 3.3M5")


