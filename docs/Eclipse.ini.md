https://wiki.eclipse.org/Eclipse.ini

# Overview
* `$ECLIPSE_HOME/eclipse.ini`'s options -- are controlled by -- Eclipse startup
  * if `$ECLIPSE_HOME` is NOT defined -> default eclipse.ini | 
    * your Eclipse installation directory 
    * Mac, Eclipse.app/Contents/MacOS
* eclipse.ini
  * == 💡text file / contain CL options💡 / 
    * | start up Eclipse, are added | CL
    * [AVAILABLE options](https://github.com/eclipse-platform/eclipse.platform.releng.aggregator/blob/master/eclipse.platform.common/bundles/org.eclipse.platform.doc.isv/reference/misc/runtime-options.html)
    * 👀EACH option & option's argument / its own line 👀
    * 👀ALL lines AFTER `-vmargs` -- are passed as -- arguments | JVM 👀
      * -> ALL eclipse's arguments and options -- MUST be -- specified | BEFORE `-vmargs` 
        * == use arguments | CL 
    * 👀`-vmargs` | CL -- replaces -- ALL `-vmargs` settings | .ini file 👀
      * EXCEPT TO, specifying `--launcher.appendVmargs` | 
        * .ini file OR
        * CL
    * ⚠️`-XX VM` arguments -- are subject to -- change WITHOUT notice ⚠️ 
      * if the JVM exits with code 2 -> remove them
  * recommendations
    * | BEFORE modifying it, test it -- from your -- Command Prompt/Terminal
    * make a `backup--keep` copy of the original contents
      * -> avoid downloading it ALL AGAIN
  * 's content -- varies, based on -- 
    * OS
    * Eclipse package / you have
  * default one
    ```
    -startup
    ../../../plugins/org.eclipse.equinox.launcher_1.2.0.v20110502.jar
    --launcher.library
    ../../../plugins/org.eclipse.equinox.launcher.cocoa.macosx.x86_64_1.1.100.v20110502
    -product
    org.eclipse.epp.package.jee.product
    --launcher.defaultAction
    openFile
    -showsplash
    org.eclipse.platform
    --launcher.XXMaxPermSize
    256m
    --launcher.defaultAction
    openFile
    -vmargs
    -Dosgi.requiredJavaVersion=1.5
    -XX:MaxPermSize=256m
    -Xms40m
    -Xmx512m
    ```

# Specifying the JVM
* see [How do I run Eclipse?](FAQ_How_do_I_run_Eclipse.md)
* 
  ```
  //specificEclipseOptions
  -product
  productValue
  ...
  -vm
  absolutePathToJavaExecutableInSeparateLine
  -vmargs
  -arg1
  -arg2
  ```
  * | Windows
    * `absolutePathToJavaExecutableInSeparateLine` == `absolutePathToJavaw.exe`
      ```
      -vm
      C:\jdk1.7.0_21\bin\javaw.exe
      ```
  * | Linux
    * `absolutePathToJavaExecutableInSeparateLine` == `absolutePathToJava`
      ```
      -vm
      /opt/sun-jdk-1.6.0.02/bin/java
      ```
  * | MacOs
    * Mac OS X 10.7+
      * `absolutePathToJavaExecutableInSeparateLine` == `absolutePathToJavaBin` 
        ```
        -vm
        /Library/Java/JavaVirtualMachines/<JRE_NAME>/Contents/Home/bin
        ```
    * Mac standard
      * `absolutePathToJavaExecutableInSeparateLine` == `/usr/bin` -> 💡if you change JVM -> NO need to update it 💡
        ```
        -vm
        /usr/bin
        ```
    * JDK / 
      * WITH macOS directory layout
        * `absolutePathToJavaExecutableInSeparateLine` == `absolutePathToJavaBinJava`
          ```
          -vm
          /<PATH_TO_YOUR_JDK>/Contents/Home/bin/java
          ```
      * WITHOUT macOS directory layout -- _Example:_ installed -- via -- SDKMAN --
        * `absolutePathToJavaExecutableInSeparateLine` == `PathTo libjli.dylib`
          * JDK v11+ -- `<JDK_11+_HOME>/lib/jli/libjli.dylib` --
            ```
            -vm
            /Users/<YOUR_USER>/.sdkman/candidates/java/11.0.8.hs-adpt/lib/jli/libjli.dylib
            ```
          * JDK v8 -- `<JDK_8_HOME>/jre/lib/jli/libjli.dylib` --
            ```
            -vm
            /Users/<YOUR_USER>/.sdkman/candidates/java/8.0.265.hs-adpt/jre/lib/jli/libjli.dylib
            ```