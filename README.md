# Multi-thread-Web-Server-
=============================================================================
Comp 2322 Computer Networking
Project: Multi-thread Web Server
=============================================================================
1. Project Description
-----------------------------------------------------------------------------
This project is a multi-threaded Web server implemented in Java from scratch 
using basic socket programming. It supports GET and HEAD HTTP methods, 
handles persistent and non-persistent connections, processes 
conditional requests, and generates a log file 
recording all client requests.

-----------------------------------------------------------------------------
2. Directory Structure Requirements
-----------------------------------------------------------------------------
Before running the server, please ensure the following directory structure 
exists in your project root:

MultiThreadWebServer/
├── src/
│   └── WebServer.java      (The main server source code)
├── www/                    (The document root directory for the server)
│   ├── index.html          (Sample text file)
│   └── image.jpg           (Sample image file)
├── server.log              (Will be generated automatically upon requests)
└── README.txt              (This file)

-----------------------------------------------------------------------------
3. How to Compile and Run (in IntelliJ IDEA)
-----------------------------------------------------------------------------
1. Open IntelliJ IDEA and select "Open".
2. Navigate to the project root folder and open the project.
3. In the Project Explorer window (left panel), expand the `src` folder.
4. Double-click on `WebServer.java` to open it.
5. Click the green "Play/Run" button next to the `public static void main` 
   method, or right-click the file and select "Run 'WebServer.main()'".
6. The console will display "Web Server is running on port 8080..."

-----------------------------------------------------------------------------
4. How to Compile and Run (via Command Line / Terminal)
-----------------------------------------------------------------------------
If you prefer using the command line:
1. Open your terminal or command prompt.
2. Navigate to the `src` folder of the project:
   cd path/to/project/src
3. Compile the Java file:
   javac WebServer.java
4. Run the compiled class:
   java WebServer

-----------------------------------------------------------------------------
5. How to Test the Web Server
-----------------------------------------------------------------------------
Once the server is running, you can test it using a standard web browser:

- Test GET (Text File): 
  Open a browser and go to: http://127.0.0.1:8080/index.html
- Test GET (Image File): 
  Open a browser and go to: http://127.0.0.1:8080/image.jpg
- Test 404 Not Found: 
  Open a browser and go to: http://127.0.0.1:8080/notexist.html

You can also check the `server.log` file in the project root to see the 
recorded history of your HTTP requests and server responses.
