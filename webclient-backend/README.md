Hoccer XO WebClient Backend
===============

This is a Hoccer XO client that is also an HTTP server providing a REST and WebSocket API to access the client database.
At the moment, it is capable of downloading incoming attachments. The files are decrypted and saved in a specified directory.
The WebClient Backend is currently used by the [Hoccer Wall](https://github.com/hoccer/wall-deployment).

## Configuration

Copy `backend.conf_template` to `backend.conf` and configure it to your needs.

## Starting

### IntelliJ

Edit your IntelliJ runtime configuration as follows:

![Screenshot](/doc/images/RunConfiguration-WebClient-Backend.png)

It adds the arguments `-c backend.conf` to point to the configuration file within the working directory. You can use this runtime configuration to run the application within IntelliJ.

### Command Line

After building/installing you will find an appropriate *jar* file under *webclient-backend/target*. You can directly run from the `webclient-backend` directory with:

`java -jar target/webclient-backend-<VERSION>-jar-with-dependencies.jar -c backend.conf`
