DARIDRYAM - STUDENT FINANCIAL SURVIVAL SIMULATOR

README

RUN THE PROJECT USING COMMAND PROMPT

Step 1: Open the project folder - Open the folder containing Main.java,
DB.java, and Engine.java. - Click the address bar in File Explorer. -
Type cmd and press Enter. - Command Prompt will open in that folder.

Step 2: Compile the Java files

If the Connector/J JAR is inside the lib folder, run:

javac -cp “.;mysql-connector-j-26.10.0.jar” *.java

Replace with the actual version in the JAR file name.

Step 3: Run the application

java -cp “.;mysql-connector-j-26.10.0.jar” Main

If Main.java declares a package, use the fully qualified class name and
compile/run from the correct parent folder instead. The commands above
assume the source files are in the same folder and do not declare a
package.

IMPORTANT: - The semicolon (;) separates classpath entries on Windows. -
On macOS or Linux, use a colon (:) instead of a semicolon.

ABOUT THE PROJECT

Daridryam is a Java desktop application built using Java Swing. It helps
a student track money, record income and expenses, check spending, set
savings goals, and try different spending scenarios.

Main source files: - Main.java : Main application window and user
interface. - DB.java : Database connection and database operations. -
Engine.java : Financial calculations, alerts, simulations, and
predictions.

The project uses Java and MySQL. The MySQL JDBC Connector is needed so
Java can communicate with the MySQL database.

WHAT YOU NEED

Install or prepare these items:

Java Development Kit (JDK)
    -   Install a JDK, preferably JDK 17 or a newer version supported by
        your code.
    -   The JDK includes the Java compiler (javac) and Java runtime
        (java).
    -   A JDK is required to compile the source code.
 MySQL Server
    -   Install MySQL Server.
    -   Remember the MySQL username and password you create during
        installation.
    -   Make sure the MySQL server is running before starting the
        application.

 Project source files
    -   Keep all required .java files in the project folder.
    -   Make sure any additional classes used by the project are also
        included.

INSTALL JAVA ON WINDOWS

Step 1: Download the JDK - Open a browser. - Visit
https://www.oracle.com/java/technologies/downloads/ or
https://adoptium.net/. - Download the Windows x64 installer for the
required JDK version. - Run the installer and complete the installation.

Step 2: Find the JDK installation folder - A common installation
location is: C:Files- - The exact folder name depends on the JDK version
and installer.

Step 3: Add Java to PATH - Open Windows Search. - Search for: Edit the
system environment variables. - Open it and click Environment
Variables. - Under System variables, select Path and click Edit. - Click
New. - Add the JDK bin folder. Example: C:Files - Replace the example
path with the actual bin folder on your computer. - Click OK in all open
windows.

Optional: Set JAVA_HOME - In Environment Variables, under System
variables, click New. - Variable name: JAVA_HOME - Variable value: the
JDK folder, without . Example: C:Files - Click OK. - JAVA_HOME is useful
for some development tools. The PATH entry is what allows commands such
as java and javac to run from Command Prompt.

Step 4: Check the installation - Open a NEW Command Prompt window. -
Run:

     java -version

-   Then run:

    javac -version

-   Both commands should display the installed Java version.

-   If Windows says the command is not recognized, check that the PATH
    entry points to the JDK’s bin folder, then open a new Command
    Prompt.

INSTALL MYSQL AND PREPARE THE DATABASE

-   Install MySQL Server using the official MySQL installer:
    https://dev.mysql.com/downloads/installer/
-   During setup, remember the database username and password.
-   Start the MySQL server.
-   Open MySQL Workbench or the MySQL command-line client.
-   Check DB.java for the database name, host, port, username, password,
    and table-creation code.
-   Configure DB.java with the correct connection details for your
    computer.
-   Do not share your real database password publicly or upload it to
    GitHub.

If the application creates its tables automatically when it connects,
allow it to do so. Otherwise, create the database required by DB.java
before running the application.


RUN THE PROJECT IN VS CODE

1.  Install Visual Studio Code.
2.  Install the “Extension Pack for Java” extension by Microsoft.
3.  Open the project folder in VS Code using File > Open Folder.
4.  Make sure VS Code detects the installed JDK.
5.  Add the MySQL Connector/J JAR to the project’s referenced libraries.
    In a simple VS Code Java project, you can use the Java Projects view
    or configure the project’s referenced libraries.
6.  Open Main.java.
7.  Use the Run button above the main method, if VS Code has detected
    the project and all dependencies.
8.  Make sure MySQL Server is running and DB.java has the correct
    connection settings.

If VS Code shows missing-class or driver errors, check that all required
.java files and the Connector/J JAR are included in the project.

COMMON ERRORS

‘java’ is not recognized - Java is not installed correctly or the JDK
bin folder is missing from PATH. - Check PATH and open a new Command
Prompt.

‘javac’ is not recognized - A JRE may have been installed instead of a
JDK, or the JDK bin folder is missing from PATH. - Install a JDK and
check PATH.

No suitable driver / JDBC driver not found - The MySQL Connector/J JAR
is missing from the classpath or VS Code libraries. - Add the JAR and
run the application again.

Communications link failure - MySQL Server may not be running. - Check
the host, port, username, password, and database settings in DB.java.

Access denied for user - Check the MySQL username and password. - Make
sure that user has permission to access the database.

Could not find or load main class - Run the command from the correct
folder. - Check the class name and package declaration. - Compile the
source files before running.

Compilation error: cannot find symbol - A required Java class or library
may be missing. - Check that all project source files and dependencies
are included.

IMPORTANT NOTES

-   Use a JDK, not only a JRE, because the project must be compiled.
-   Start MySQL before running the application.
-   Keep the MySQL Connector/J JAR available to both the compiler and
    the Java runtime.
-   Keep database passwords private.
-   Back up the database if it contains important information.
-   The exact database setup depends on the connection settings and SQL
    code present in DB.java.

USEFUL DOWNLOAD LINKS

Java JDK: https://www.oracle.com/java/technologies/downloads/

Eclipse Temurin JDK: https://adoptium.net/

MySQL Installer for Windows: https://dev.mysql.com/downloads/installer/

MySQL Connector/J: https://dev.mysql.com/downloads/connector/j/

Visual Studio Code: https://code.visualstudio.com/

Java Extension Pack for VS Code:
https://marketplace.visualstudio.com/items?itemName=vscjava.vscode-java-pack

END OF README