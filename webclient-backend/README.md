Hoccer XO WebClient Backend
===============

This is a Hoccer XO client that is also an HTTP server providing a REST and WebSocket API to access the client database.
At the moment, it is capable of receiving messages and downloading incoming attachments. The files are decrypted and saved in a specified directory.

**[Provisioning repository](https://github.com/hoccer/hoccer-receiver-puppet-configuration)**

**[Deployment repository](https://github.com/hoccer/receiver-deployment)**

## Dependencies

Besides dependencies available at archiva.hoccer.de its necessary to install the latest version of the `hoccer-talk-client` artifact into your local Maven repository (on Mac OSX: `~/.m2/repository/hoccer/hoccer-talk-client`). Pull `hoccer-talk-spike` and run `mvn install` in its repository. After that you can use `mvn install` from within the `talk-webclient-backend`directory.

## Configuration

Copy `backend.conf_template` to `backend.conf` and configure it to your needs.

## Starting

### IntelliJ

Edit your IntelliJ runtime configuration as follows:

![Screenshot](/doc/images/RunConfiguration.png)

It adds the arguments `-c backend.conf` to point to the configuration file within the working directory. You can use this runtime configuration to run the application within IntelliJ.

### Command Line

After building/installing you will find an appropriate *jar* file under *talk-webclient-backend/target*. You can directly run from the `talk-webclient-backend` directory with:

`java -jar target/talk-webclient-backend-<VERSION>-jar-with-dependencies.jar -c backend.conf`
